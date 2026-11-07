#!/usr/bin/env python3
"""Read-only, bounded heuristics for release assets; this is not a licence audit."""
import argparse
from collections import Counter
from contextlib import contextmanager
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import struct
import subprocess
import subprocess
import sys
import unicodedata
import zipfile

MAX_BLOB = 16 * 1024 * 1024
MAX_TOTAL = 512 * 1024 * 1024
MAX_ENTRIES = 100_000
MAX_METADATA = 32 * 1024 * 1024
MAX_CANDIDATES = 200_000
MAX_LITERAL = 65536
IMAGE_EXTENSIONS = {'.png', '.webp', '.jpg', '.jpeg', '.gif', '.bmp', '.svg', '.ico', '.avif', '.tif', '.tiff'}
INLINE_PNG = re.compile(r'data\s*:\s*image\s*/\s*png\s*;\s*base64\s*,', re.I)
HEX = frozenset('0123456789abcdefABCDEF')
LIMITATIONS = ('Heuristic audit: a PASS is not proof of provenance or licensing. '
               'Only known normalized text samples are matched; dynamic strings, encoded/compressed payloads '
               'and renamed unknown formats may escape detection. Allowed icons/ui paths are not provenance checks.')


class AuditError(Exception):
    pass


def normalized(text):
    return re.sub(r'\s+', ' ', unicodedata.normalize('NFKC', text).casefold()).strip()


def load_samples(path):
    # Default samples: 32 stable, sorted hashes per locale, from private game-dev/texts values.
    # Values have 32..512 normalized characters; these UI tables do not cover every game dialogue.
    if path.stat().st_size > 1024 * 1024:
        raise AuditError('sample hash file exceeds 1 MiB')
    lines = path.read_text(encoding='ascii').splitlines()
    if not lines or any(not re.fullmatch(r'[0-9a-f]{64}', line) for line in lines):
        raise AuditError('sample file must contain only one lowercase SHA-256 hash per line')
    return set(lines)


def safe_path(name):
    return (bool(name) and len(name) <= 4096 and not name.startswith('/') and '\\' not in name
            and not re.match(r'^[A-Za-z]:', name) and not any(ord(c) < 32 or ord(c) == 127 for c in name)
            and all(p and p not in {'.', '..'} for p in name.rstrip('/').split('/')))


# Android resources of an APK by their names, since the build shortens the paths of the files: the launcher icon and the
# notification backgrounds of androidx.core. The setup screen is a web page now, so its images are web assets.
ALLOWED_RESOURCES = re.compile(
    r'mipmap/ic_launcher(?:_foreground|_background|_round)?|drawable/notification_bg(?:_low)?_(?:normal|pressed)'
    r'|drawable/notification_bg_normal_pressed|drawable/notify_panel_notification_icon_bg')
JS_EXTENSIONS = {'.js', '.mjs', '.cjs'}


def allowed_image(name, resource=None):
    if resource is not None:
        return ALLOWED_RESOURCES.fullmatch(resource) is not None
    parts = PurePosixPath(name.lower()).parts
    if 'icons' in parts[:-1] or 'ui' in parts[:-1]:
        return True
    # Android launcher resources are the sole file-name exception, in their resource directory.
    if len(parts) >= 3 and parts[-3] == 'res' and parts[-2].startswith('drawable'):
        return ALLOWED_RESOURCES.fullmatch(f'drawable/{parts[-1].split(".")[0]}') is not None
    return (len(parts) >= 3 and parts[-3] == 'res' and parts[-2].startswith('mipmap')
            and re.fullmatch(r'ic_launcher(?:_foreground|_background|_round)?\.(png|webp|xml)', parts[-1]) is not None)


def apk_resource_names(path):
    """File in the APK -> `type/name`, read with aapt2, or None when aapt2 is not at hand."""
    candidates = [os.environ.get('AAPT2')] + sorted(
        Path(os.environ.get('ANDROID_HOME', Path.home() / 'Library/Android/sdk')).glob('build-tools/*/aapt2'))
    tool = next((str(c) for c in reversed(candidates) if c and Path(c).is_file()), None)
    if tool is None:
        return None
    try:
        dump = subprocess.run([tool, 'dump', 'resources', str(path)], capture_output=True, text=True, timeout=120,
                              check=True).stdout
    except (OSError, subprocess.SubprocessError):
        return None
    names, current = {}, None
    for line in dump.splitlines():
        match = re.match(r'\s+resource 0x[0-9a-f]+ (\S+)', line)
        if match:
            current = match.group(1)
            continue
        match = re.search(r'\(file\) (\S+)', line)
        if match and current is not None:
            names[match.group(1)] = current
    return names


def js_literals(text):
    """The literals of a JavaScript file through its tokenizer, or None when node or acorn is not at hand."""
    script = Path(__file__).with_name('audit_js_literals.mjs')
    try:
        result = subprocess.run(['node', str(script)], input=text, capture_output=True, text=True, timeout=300,
                                check=True)
        return json.loads(result.stdout)
    except (OSError, subprocess.SubprocessError, ValueError):
        return None


def image_signature(data):
    prefix = data[:512].lstrip().lower()
    return (data.startswith((b'\x89PNG\r\n\x1a\n', b'\xff\xd8\xff', b'GIF87a', b'GIF89a', b'BM', b'\0\0\1\0'))
            or (data.startswith(b'RIFF') and data[8:12] == b'WEBP')
            or prefix.startswith(b'<svg') or (prefix.startswith(b'<?xml') and b'<svg' in prefix))


def js_unescape(raw):
    """Decode literal escapes without evaluating JavaScript or interpolating templates."""
    result = []
    i = 0
    short = {'n': '\n', 'r': '\r', 't': '\t', 'b': '\b', 'f': '\f', 'v': '\v'}
    while i < len(raw):
        c = raw[i]
        i += 1
        if c != '\\' or i == len(raw):
            result.append(c)
            continue
        c = raw[i]
        i += 1
        if c in '\r\n':
            if c == '\r' and i < len(raw) and raw[i] == '\n':
                i += 1
            continue
        if c == 'u' and i < len(raw) and raw[i] == '{':
            end = raw.find('}', i + 1, i + 8)
            digits = raw[i + 1:end] if end >= 0 else ''
            if digits and all(v in HEX for v in digits) and int(digits, 16) <= 0x10ffff:
                result.append(chr(int(digits, 16)))
                i = end + 1
                continue
        count = 4 if c == 'u' else 2 if c == 'x' else 0
        if count and len(raw) - i >= count and all(v in HEX for v in raw[i:i + count]):
            result.append(chr(int(raw[i:i + count], 16)))
            i += count
        elif c in '01234567':
            end = i
            while end < min(i + 2, len(raw)) and raw[end] in '01234567':
                end += 1
            result.append(chr(int(c + raw[i:end], 8)))
            i = end
        else:
            result.append(short.get(c, c))
    # A JavaScript UTF-16 surrogate pair represents a single Unicode code point.
    return ''.join(result).encode('utf-16', 'surrogatepass').decode('utf-16', 'replace')


def literal_candidates(text):
    # Adjacent unescaped delimiters are inspected in each quote stream, irrespective of comments/regexes.
    # Their spans do not overlap within a stream, so hostile unmatched quotes cannot cause quadratic scans.
    previous = {'"': None, "'": None, '`': None}
    backslashes = 0
    for i, c in enumerate(text):
        if c in previous and backslashes % 2 == 0:
            start = previous[c]
            previous[c] = i
            if start is not None:
                yield js_unescape(text[start + 1:i]) if i - start - 1 <= MAX_LITERAL else None
        backslashes = backslashes + 1 if c == '\\' else 0


def json_strings(value):
    pending = [value]
    while pending:
        item = pending.pop()
        if isinstance(item, str):
            yield item
        elif isinstance(item, dict):
            pending.extend(item.values())
        elif isinstance(item, list):
            pending.extend(item)


class Auditor:
    def __init__(self, samples):
        self.samples = samples
        self.counts = Counter()
        self.bytes = 0
        self.entries = 0

    def issue(self, code, name, detail=''):
        self.counts[code] += 1
        if sum(self.counts.values()) <= 10_000:
            print(code, json.dumps(name[:512], ensure_ascii=True), detail)

    def entry(self, name, size, regular=True):
        self.entries += 1
        if self.entries > MAX_ENTRIES:
            raise AuditError('entry count exceeds 100,000')
        if not safe_path(name):
            self.issue('PATH', name, 'unsafe or ambiguous path; skipped')
            return False
        suffix = PurePosixPath(name.lower()).suffix
        if regular and (suffix in {'.iso', '.chd'} or (suffix == '.bin' and size > 1_000_000)):
            self.issue('ROM', name, f'{size} bytes')
        return True

    def reserve(self, size, name):
        if size > MAX_BLOB:
            self.issue('LIMIT', name, 'over 16 MiB; content not scanned')
            return False
        self.bytes += size
        if self.bytes > MAX_TOTAL:
            raise AuditError('scan exceeds 512 MiB; incomplete audit')
        return True

    def blob(self, name, data, display=None, resource=None):
        label = display or name
        if (PurePosixPath(name.lower()).suffix in IMAGE_EXTENSIONS or image_signature(data)) and not allowed_image(
                name, resource):
            self.issue('IMAGE', label, 'outside icons/ui or Android launcher resources')
        try:
            text = data.decode('utf-8-sig')
        except UnicodeError:
            return
        found = set()
        inline = INLINE_PNG.search(text) is not None
        def candidate(value):
            nonlocal inline
            inline |= INLINE_PNG.search(value) is not None
            digest = hashlib.sha256(normalized(value).encode('utf-8')).hexdigest()
            if digest in self.samples:
                found.add(digest)
        try:
            document = json.loads(text) if len(text) <= 2 * 1024 * 1024 else None
        except (ValueError, RecursionError):
            document = None
        tokens = js_literals(text) if document is None and PurePosixPath(name.lower()).suffix in JS_EXTENSIONS else None
        if document is not None:
            candidates = json_strings(document)
        elif tokens is not None:
            candidates = iter(tokens)
        else:
            candidates = literal_candidates(text)
        for count, value in enumerate(candidates, 1):
            if count > MAX_CANDIDATES:
                self.issue('LIMIT', label, 'over 200,000 string candidates; incomplete content scan')
                break
            if value is None or len(value) > MAX_LITERAL:
                self.issue('LIMIT', label, 'literal exceeds 64 KiB; incomplete string scan')
            else:
                candidate(value)
        # Also match an entire plain-text file and complete lines; no substrings or arbitrary n-grams.
        if len(text) <= MAX_LITERAL:
            candidate(text)
        for count, line in enumerate(text.splitlines(), 1):
            if count > MAX_CANDIDATES:
                self.issue('LIMIT', label, 'over 200,000 lines; incomplete content scan')
                break
            if len(line) <= MAX_LITERAL:
                candidate(line)
        if inline:
            self.issue('INLINE_PNG', label, 'inline PNG data URI')
        for digest in sorted(found):
            self.issue('TEXT_SAMPLE', label, digest)

    def directory(self, root):
        flags = os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW
        def visit(fd, prefix, depth):
            if depth > 64:
                raise AuditError('directory nesting exceeds 64')
            with os.scandir(fd) as children:
                for child in children:
                    if child.name == '.git':
                        continue
                    name = prefix + child.name
                    info = child.stat(follow_symlinks=False)
                    if not self.entry(name, info.st_size, stat.S_ISREG(info.st_mode)):
                        continue
                    if stat.S_ISLNK(info.st_mode):
                        self.issue('SYMLINK', name, 'not followed')
                    elif stat.S_ISDIR(info.st_mode):
                        if not safe_path(name):
                            self.issue('PATH', name, 'directory not followed')
                            continue
                        childfd = os.open(child.name, flags, dir_fd=fd)
                        try:
                            visit(childfd, name + '/', depth + 1)
                        finally:
                            os.close(childfd)
                    elif stat.S_ISREG(info.st_mode) and self.reserve(info.st_size, name):
                        opened = os.open(child.name, os.O_RDONLY | os.O_NOFOLLOW, dir_fd=fd)
                        with os.fdopen(opened, 'rb') as stream:
                            actual = os.fstat(stream.fileno())
                            if (actual.st_dev, actual.st_ino, actual.st_size) != (info.st_dev, info.st_ino, info.st_size):
                                raise AuditError('file changed during audit')
                            data = stream.read(MAX_BLOB + 1)
                            if len(data) != info.st_size:
                                raise AuditError('file changed during audit')
                            self.blob(name, data)
                    elif not stat.S_ISREG(info.st_mode):
                        self.issue('SPECIAL', name, 'not a regular file; skipped')
        fd = os.open(root, flags)
        try:
            visit(fd, '', 0)
        finally:
            os.close(fd)

    def apk(self, path):
        fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
        with os.fdopen(fd, 'rb') as stream:
            size = os.fstat(fd).st_size
            if size > MAX_TOTAL:
                raise AuditError('archive exceeds 512 MiB')
            # Bound the central directory before ZipFile allocates ZipInfo objects. APKs do not need ZIP64.
            # Include the locator even with the maximum 65,535-byte ZIP comment.
            stream.seek(max(0, size - 65577))
            tail = stream.read(65577)
            at = tail.rfind(b'PK\x05\x06')
            if at < 0 or len(tail) - at < 22:
                raise AuditError('invalid ZIP end record')
            if at >= 20 and tail[at - 20:at - 16] == b'PK\x06\x07':
                raise AuditError('ZIP64 locator is unsupported; archive not opened')
            disk, start, on_disk, entries, index_size, index_offset, comment = struct.unpack_from('<4H2IH', tail, at + 4)
            if (disk or start or on_disk != entries or entries == 65535 or entries > MAX_ENTRIES
                    or index_size > 8 * 1024 * 1024 or index_offset + index_size > size
                    or at + 22 + comment != len(tail)):
                raise AuditError('unsupported or excessive ZIP directory (including ZIP64)')
            stream.seek(0)
            resources = apk_resource_names(path)
            if resources is None:
                self.issue('LIMIT', str(path), 'aapt2 not found; resource images judged by their shortened paths')
            with zipfile.ZipFile(stream) as archive:
                if len(archive.infolist()) > MAX_ENTRIES:
                    raise AuditError('archive has too many entries')
                if sum(info.file_size for info in archive.infolist()) > MAX_TOTAL:
                    raise AuditError('archive expands beyond 512 MiB')
                seen = set()
                for info in archive.infolist():
                    name = info.orig_filename
                    if name in seen:
                        self.issue('PATH', name, 'duplicate ZIP member; skipped')
                        continue
                    seen.add(name)
                    if not self.entry(name, info.file_size):
                        continue
                    if stat.S_ISLNK(info.external_attr >> 16):
                        self.issue('SYMLINK', name, 'ZIP symlink not followed')
                    elif not info.is_dir() and self.reserve(info.file_size, name):
                        with archive.open(info) as member:
                            data = member.read(MAX_BLOB + 1)
                        if len(data) != info.file_size:
                            raise AuditError('ZIP member size mismatch')
                        self.blob(name, data, resource=(resources or {}).get(name))

    def history(self, root):
        command = ['git', '--no-pager', '--no-replace-objects', '--no-lazy-fetch', '-C', str(root)]
        env = os.environ.copy()
        for key in ['GIT_DIR', 'GIT_WORK_TREE', 'GIT_INDEX_FILE', 'GIT_OBJECT_DIRECTORY', 'GIT_ALTERNATE_OBJECT_DIRECTORIES']:
            env.pop(key, None)
        env.update(GIT_NO_LAZY_FETCH='1', GIT_TERMINAL_PROMPT='0', GIT_OPTIONAL_LOCKS='0')
        with git_process(command + ['rev-parse', '--is-shallow-repository'], env) as process:
            shallow = process.stdout.read(16).strip()
            if process.wait() != 0 or shallow not in {b'true', b'false'}:
                raise AuditError('repository completeness could not be checked')
            if shallow == b'true':
                raise AuditError('shallow repository has hidden history; no objects fetched')
        # --raw -z preserves all path bytes; no checkout, textconv, filters, temporary blobs or ref updates.
        with git_process(command + ['log', '--all', '--root', '-m', '--format=', '--raw', '-z',
                                    '--no-abbrev', '--no-renames', '--no-ext-diff', '--no-textconv'], env) as process:
            metadata = process.stdout.read(MAX_METADATA + 1)
            if len(metadata) > MAX_METADATA:
                raise AuditError('Git history metadata exceeds 32 MiB')
            if process.wait() != 0:
                raise AuditError('Git history could not be enumerated')
        records = metadata.split(b'\0')
        objects = {}
        object_paths = 0
        i = 0
        while i < len(records):
            record = records[i].lstrip(b'\n')
            i += 1
            if not record:
                continue
            fields = record.split()
            if len(fields) != 5 or not fields[0].startswith(b':') or i == len(records):
                raise AuditError('unexpected Git raw history record')
            name = records[i].decode('utf-8', 'surrogateescape')
            i += 1
            for mode, oid in [(fields[0][1:], fields[2]), (fields[1], fields[3])]:
                if mode == b'000000':
                    continue
                if mode == b'120000':
                    self.issue('SYMLINK', 'git:' + name, 'historical symlink not followed')
                elif mode == b'160000':
                    self.issue('UNSCANNED', 'git:' + name, 'submodule must be audited separately')
                elif mode not in {b'100644', b'100755'} or not re.fullmatch(rb'[0-9a-f]{40}|[0-9a-f]{64}', oid):
                    raise AuditError('unexpected Git object metadata')
                else:
                    paths = objects.setdefault(oid, set())
                    if name not in paths:
                        paths.add(name)
                        object_paths += 1
                    if object_paths > MAX_ENTRIES:
                        raise AuditError('Git history exceeds 100,000 blob paths')
        with git_process(command + ['cat-file', '--batch-command'], env) as process:
            for oid, names in objects.items():
                def request(verb):
                    process.stdin.write(verb + b' ' + oid + b'\n')
                    process.stdin.flush()
                    header = process.stdout.readline(256).split()
                    if len(header) != 3 or header[0] != oid or header[1] != b'blob' or not header[2].isdigit():
                        raise AuditError('Git blob unavailable; no objects are fetched')
                    return int(header[2])
                size = request(b'info')
                valid = [name for name in sorted(names) if self.entry(name, size)]
                if not valid or not self.reserve(size, 'git:' + valid[0]):
                    continue
                if request(b'contents') != size:
                    raise AuditError('Git blob size changed')
                data = process.stdout.read(size)
                if len(data) != size or process.stdout.read(1) != b'\n':
                    raise AuditError('truncated Git blob')
                for name in valid:
                    self.blob(name, data, 'git:' + oid.decode()[:12] + ':' + name)


@contextmanager
def git_process(command, env):
    process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=env)
    try:
        yield process
    finally:
        process.stdin.close()
        process.stdout.close()
        try:
            process.wait(timeout=2)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, epilog=LIMITATIONS + '\n'
        'Text normalization: Unicode NFKC, casefold, collapsed whitespace. Complete JSON values and JS literals '
        '(including escaped/minified ones) are sampled. Limits fail closed: 16 MiB/blob, 512 MiB total, '
        '100,000 entries, literals 64 KiB, ZIP index 8 MiB; ZIP64 unsupported. --history requires Git >=2.49 and scans reachable '
        'commits from all refs; shallow repositories fail (not the index, working tree, reflogs or unreachable objects); '
        'audit the folder separately.')
    parser.add_argument('target', type=Path, help='APK/ZIP, folder, or Git repository with --history')
    parser.add_argument('--history', action='store_true', help='audit reachable historical blobs read-only; never fetch or rewrite')
    parser.add_argument('--samples', type=Path, default=Path(__file__).with_name('audit_samples.txt'), help='SHA-256-only sample list')
    args = parser.parse_args(argv)
    auditor = None
    try:
        auditor = Auditor(load_samples(args.samples))
        if args.target.is_symlink():
            raise AuditError('target symlink is not followed')
        if args.history:
            auditor.history(args.target)
        elif args.target.is_dir():
            auditor.directory(args.target)
        else:
            auditor.apk(args.target)
    except (AuditError, OSError, ValueError, zipfile.BadZipFile, RuntimeError, RecursionError) as error:
        # Never echo untrusted file contents, exception messages or Git stderr.
        message = str(error) if isinstance(error, AuditError) else type(error).__name__
        print('ERROR', message)
        print(LIMITATIONS)
        return 2 if auditor is None else 1
    print(('FAIL' if auditor.counts else 'PASS') + ' ' + json.dumps({
        'entries': auditor.entries, 'bytes_scanned': auditor.bytes, 'findings': dict(sorted(auditor.counts.items()))}, sort_keys=True))
    print(LIMITATIONS)
    return 1 if auditor.counts else 0


if __name__ == '__main__':
    sys.exit(main())
