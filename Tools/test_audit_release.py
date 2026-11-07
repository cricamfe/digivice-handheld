"""Synthetic release-audit checks; no game data is used by these fixtures."""
import hashlib
import contextlib
import io
import json
import os
from pathlib import Path
import subprocess
import struct
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile
from Tools import audit_release as audit

SCRIPT = Path(__file__).with_name('audit_release.py')
SAMPLE = 'The imaginary copper fox carries thirteen paper lanterns across the quiet silver bridge.'
DIGEST = hashlib.sha256(SAMPLE.casefold().encode()).hexdigest()
PNG = b'\x89PNG\r\n\x1a\nsynthetic image bytes'


class AuditReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.samples = self.root / 'samples.txt'
        self.samples.write_text(DIGEST + '\n')

    def run_audit(self, target, *options):
        result = subprocess.run([sys.executable, str(SCRIPT), '--samples', str(self.samples),
                                 *options, str(target)], capture_output=True, text=True, timeout=30)
        self.assertNotIn(SAMPLE, result.stdout + result.stderr)
        return result

    def put(self, name, data):
        path = self.root / 'release' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        return path

    def test_clean_directory_passes_and_preserves_input(self):
        files = [self.put('icons/home.png', PNG), self.put('ui/own.webp', b'RIFFxxxxWEBPown'),
                 self.put('res/mipmap-hdpi/ic_launcher_foreground.png', PNG),
                 self.put('app.js', b'const own="A message written for this fictional app.";')]
        before = {p: p.read_bytes() for p in files}
        result = self.run_audit(self.root / 'release')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn('heuristic', result.stdout.lower())
        self.assertEqual(before, {p: p.read_bytes() for p in files})

    def test_directory_detects_images_roms_encoded_text_and_inline_png(self):
        self.put('noticons/game.png', PNG)
        self.put('small.ISO', b'not a real disc')
        self.put('memory.bin', b'\0' * 1_000_001)
        encoded = ''.join('\\u%04x' % ord(c) for c in SAMPLE)
        path = self.put('minified.js', ('let a="' + encoded + '";let b="\\x64ata:image/png;base64,AAAA";').encode())
        before = path.read_bytes()
        result = self.run_audit(self.root / 'release')
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        for code in ['IMAGE', 'ROM', 'TEXT_SAMPLE', 'INLINE_PNG']:
            self.assertIn(code, result.stdout)
        self.assertEqual(before, path.read_bytes())

    def test_json_normalization_and_single_quoted_js_are_detected(self):
        self.put('table.json', json.dumps({'nested': ['  ' + SAMPLE.upper().replace(' ', '\n  ') + '  ']}).encode())
        self.put('bundle.js', ("/* an unmatched quote: '\n */const a='" + SAMPLE + "';const b=`" + SAMPLE + '`;').encode())
        result = self.run_audit(self.root / 'release')
        self.assertEqual(1, result.returncode)
        self.assertGreaterEqual(result.stdout.count('TEXT_SAMPLE'), 2)

    def test_32_character_sample_matches_unicode_whitespace_and_js_escapes(self):
        phrase = 'Café lanterns glow by copper bay'
        self.assertEqual(32, len(phrase))
        self.samples.write_text(hashlib.sha256(phrase.casefold().encode()).hexdigest() + '\n')
        self.put('bundle.js', b'const q="  CAFE\\u0301\\tLANTERNS\\nGLOW  BY COPPER BAY  ";')
        result = self.run_audit(self.root / 'release')
        self.assertEqual(1, result.returncode)
        self.assertIn('TEXT_SAMPLE', result.stdout)
        self.assertNotIn(phrase, result.stdout + result.stderr)

    def make_apk(self, entries):
        apk = self.root / 'fixture.apk'
        with zipfile.ZipFile(apk, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
            for name, data in entries:
                archive.writestr(name, data)
        return apk

    def test_clean_apk_passes_and_stays_unchanged(self):
        apk = self.make_apk([('assets/icons/home.png', PNG), ('classes.dex', b'dex\nsynthetic')])
        before = apk.read_bytes()
        result = self.run_audit(apk)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(before, apk.read_bytes())

    def test_apk_detects_game_content_without_extracting_it(self):
        apk = self.make_apk([('assets/map.webp', b'RIFFxxxxWEBPown'),
                             ('assets/messages.json', json.dumps({'value': SAMPLE}).encode()),
                             ('assets/game.chd', b'fake')])
        before = sorted(p.name for p in self.root.iterdir())
        result = self.run_audit(apk)
        self.assertEqual(1, result.returncode)
        for code in ['IMAGE', 'ROM', 'TEXT_SAMPLE']:
            self.assertIn(code, result.stdout)
        self.assertEqual(before, sorted(p.name for p in self.root.iterdir()))

    def test_zip_traversal_and_symlinks_fail(self):
        link = zipfile.ZipInfo('assets/link')
        link.create_system = 3
        link.external_attr = 0o120777 << 16
        apk = self.make_apk([('../escape.js', b'own'), (link, b'/outside')])
        result = self.run_audit(apk)
        self.assertEqual(1, result.returncode)
        self.assertIn('PATH', result.stdout)
        self.assertIn('SYMLINK', result.stdout)
        self.assertFalse((self.root / 'escape.js').exists())

    def test_directory_symlink_is_not_followed(self):
        outside = self.root / 'outside'
        outside.mkdir()
        (outside / 'secret.json').write_text(json.dumps({'value': SAMPLE}))
        release = self.root / 'release'
        release.mkdir()
        (release / 'link').symlink_to(outside, target_is_directory=True)
        result = self.run_audit(release)
        self.assertEqual(1, result.returncode)
        self.assertIn('SYMLINK', result.stdout)
        self.assertNotIn('TEXT_SAMPLE', result.stdout)

    def test_oversized_zip_entry_fails_closed(self):
        apk = self.make_apk([('large.js', b' ' * (16 * 1024 * 1024 + 1))])
        result = self.run_audit(apk)
        self.assertEqual(1, result.returncode)
        self.assertIn('LIMIT', result.stdout)

    def test_zip64_footer_without_classic_sentinels_is_rejected_before_zipfile(self):
        for comment in [b'', b'c' * 65535]:
            with self.subTest(comment_bytes=len(comment)):
                buffer = io.BytesIO()
                with zipfile.ZipFile(buffer, 'w', compression=zipfile.ZIP_STORED) as archive:
                    archive.writestr('own.txt', b'Own notes')
                    archive.comment = comment
                data = buffer.getvalue()
                end = data.rfind(b'PK\x05\x06')
                fields = list(struct.unpack('<4s4H2IH', data[end:end + 22]))
                count, index_size, index_offset = fields[4:7]
                footer = struct.pack('<4sQ2H2I4Q', b'PK\x06\x06', 44, 45, 45, 0, 0,
                                     count, count, index_size, index_offset)
                locator = struct.pack('<4sIQI', b'PK\x06\x07', 0, end, 1)
                fields[3:7] = [0, 0, 0, 0]
                crafted = data[:end] + footer + locator + struct.pack('<4s4H2IH', *fields) + comment
                with zipfile.ZipFile(io.BytesIO(crafted)) as archive:
                    self.assertEqual(1, len(archive.infolist()))
                path = self.root / 'zip64.apk'
                path.write_bytes(crafted)
                with patch.object(audit.zipfile, 'ZipFile', side_effect=AssertionError('ZIP parser reached')) as parser:
                    with self.assertRaisesRegex(audit.AuditError, 'ZIP64'):
                        audit.Auditor({DIGEST}).apk(path)
                    parser.assert_not_called()
                self.assertEqual(crafted, path.read_bytes())

    def test_renamed_image_is_detected_by_signature(self):
        self.put('payload.dat', PNG)
        result = self.run_audit(self.root / 'release')
        self.assertEqual(1, result.returncode)
        self.assertIn('IMAGE', result.stdout)

    def test_empty_directories_also_consume_the_entry_budget(self):
        release = self.root / 'release'
        (release / 'one').mkdir(parents=True)
        (release / 'two').mkdir()
        with patch.object(audit, 'MAX_ENTRIES', 1), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(audit.AuditError):
                audit.Auditor({DIGEST}).directory(release)

    def git(self, repo, *args):
        return subprocess.check_output(['git', '-C', str(repo), *args], stderr=subprocess.DEVNULL)

    def test_history_checks_deleted_blobs_and_preserves_repository(self):
        repo = self.root / 'repo'
        repo.mkdir()
        self.git(repo, 'init', '-q')
        self.git(repo, 'config', 'user.name', 'Audit Test')
        self.git(repo, 'config', 'user.email', 'audit@example.invalid')
        (repo / 'own.txt').write_text('Synthetic application notes.')
        self.git(repo, 'add', '.')
        self.git(repo, 'commit', '-qm', 'Own source')
        self.assertEqual(0, self.run_audit(repo, '--history').returncode)
        (repo / 'removed source.js').write_text('const value=' + json.dumps(SAMPLE) + ';')
        self.git(repo, 'add', '.')
        self.git(repo, 'commit', '-qm', 'Synthetic sample')
        self.git(repo, 'rm', '-q', 'removed source.js')
        self.git(repo, 'commit', '-qm', 'Remove synthetic sample')
        before = (self.git(repo, 'rev-parse', 'HEAD'), self.git(repo, 'status', '--porcelain'),
                  (repo / 'own.txt').read_bytes())
        result = self.run_audit(repo, '--history')
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn('TEXT_SAMPLE', result.stdout)
        self.assertEqual(before, (self.git(repo, 'rev-parse', 'HEAD'), self.git(repo, 'status', '--porcelain'),
                                  (repo / 'own.txt').read_bytes()))

    def test_invalid_samples_fail_without_echoing_their_content(self):
        self.samples.write_text('private sample that must never be echoed')
        self.put('own.txt', b'own')
        result = self.run_audit(self.root / 'release')
        self.assertEqual(2, result.returncode)
        self.assertNotIn('private sample', result.stdout + result.stderr)

    def test_shallow_history_fails_without_fetching_hidden_commits(self):
        repo = self.root / 'complete'
        repo.mkdir()
        self.git(repo, 'init', '-q')
        self.git(repo, 'config', 'user.name', 'Audit Test')
        self.git(repo, 'config', 'user.email', 'audit@example.invalid')
        (repo / 'old.js').write_text('const value=' + json.dumps(SAMPLE) + ';')
        (repo / 'own.txt').write_text('Own notes')
        self.git(repo, 'add', '.')
        self.git(repo, 'commit', '-qm', 'Historical sample')
        self.git(repo, 'rm', '-q', 'old.js')
        self.git(repo, 'commit', '-qm', 'Remove sample')
        full = self.run_audit(repo, '--history')
        self.assertEqual(1, full.returncode)
        self.assertIn('TEXT_SAMPLE', full.stdout)
        clone = self.root / 'shallow'
        self.git(self.root, 'clone', '-q', '--depth=1', repo.as_uri(), str(clone))
        boundary = clone / '.git/shallow'
        before = (boundary.read_bytes(), self.git(clone, 'rev-parse', 'HEAD'))
        result = self.run_audit(clone, '--history')
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn('shallow', result.stdout.lower())
        self.assertEqual(before, (boundary.read_bytes(), self.git(clone, 'rev-parse', 'HEAD')))
        self.assertFalse((clone / '.git/FETCH_HEAD').exists())


if __name__ == '__main__':
    unittest.main()
