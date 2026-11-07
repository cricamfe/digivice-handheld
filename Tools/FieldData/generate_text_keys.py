#!/usr/bin/env python3
"""Generate text locations and charset facts, without copying game dialogue into Git.

Usage: generate_text_keys.py <ddw3 checkout> [--references <private folder>]
The optional private fixtures contain game text and must stay outside the repository.

Every line of the tables in WHOLE_TABLES is a word too, by its own reference (`starea:12`), even when an earlier table
already has the same English: the app names those places by the line the game uses for them.

The speakers the Mapa names are read too: every talk line `ActorNames` of Frontend/src/handheld/field-actors.ts refers to
(Tools/RomExtract/field_actors.py) is a word with the selector `speaker`, and it fails when one of them names nobody in a
language.

The lines the app's own sentences name (APP_WORD_FILES: the game names of the quest texts) are words too, the talk lines
with the selector `speaker`; it fails when one of them holds no name in a language.
"""
import argparse
import difflib
import functools
import json
import re
import tomllib
from pathlib import Path

from generate_locales import GAME_FILES, LANGUAGES, LOCALES, NOT_SHOWN, ROOT, SIMILAR, SPEAKER, WORD_TABLES, flat, leaves, norm, other_prefix, put, real, table


def messages(english, other, prefix):
    refs, own = {}, set()
    for file in sorted((LOCALES / "en-US").rglob("*.json")):
        relative = file.relative_to(LOCALES / "en-US").as_posix()
        if relative.startswith("quest/") or relative in NOT_SHOWN or relative == "handheld.json":
            continue
        names = GAME_FILES.get(relative)
        lines = {}
        for name in names or []:
            for index, (one, two) in enumerate(zip(table(english, "es", name), table(other, prefix, name))):
                if real(one) and real(two):
                    lines.setdefault(norm(one), {"table": name, "line": index})
        for path, text in leaves(json.loads(file.read_text(encoding="utf8"))):
            if not text.strip():
                continue
            key = norm(text)
            ref = lines.get(key)
            if ref is None and names and len(key) > 20:
                close = difflib.get_close_matches(key, lines, n=1, cutoff=SIMILAR)
                ref = lines[close[0]] if close else None
            if ref is not None:
                refs[path] = ref
            elif names:
                own.add(relative + ":" + ".".join(path))
    return refs, own


def word_references(english, other, prefix):
    refs = {}
    for name in WORD_TABLES:
        for index, (one, two) in enumerate(zip(table(english, "es", name), table(other, prefix, name))):
            if not real(one) or not real(two):
                continue
            parts = one.split("\n"), two.split("\n")
            if len(parts[0]) == len(parts[1]) and len(parts[0]) > 1:
                for part, (first, _) in enumerate(zip(*parts)):
                    refs.setdefault(flat(first), {"table": name, "line": index, "selector": f"part:{part}"})
            refs.setdefault(flat(one), {"table": name, "line": index})
    for file in sorted(english.rglob("*.toml")):
        relative = file.relative_to(english)
        twin = other / relative.parent / (other_prefix(other) + file.name[2:])
        if not twin.exists():
            continue
        first, second = (tomllib.loads(p.read_text(encoding="utf8")).get("strings", []) for p in (file, twin))
        for index, (one, two) in enumerate(zip(first, second)):
            speaker, translated = SPEAKER.match(one), SPEAKER.match(two)
            if speaker and translated and flat(speaker[1]):
                key = flat(speaker[1]).rstrip(".")
                if key not in refs:
                    if len(relative.parts) != 1:
                        raise ValueError("A speaker needs a non-root disc table")
                    refs[key] = {"table": file.stem[2:], "line": index, "selector": "speaker"}
    return refs


FIELD_ACTORS = ROOT / "Frontend/src/handheld/field-actors.ts"


def actor_speaker_lines():
    """The talk lines whose speaker the app shows for a character of the field: the references of `ActorNames`."""
    text = FIELD_ACTORS.read_text(encoding="utf8")
    block = text[text.index("export const ActorNames"):]
    block = block[:block.index("};")]
    found = sorted(set(re.findall(r'"(talk\d\d):(\d+)"', block)), key=lambda ref: (ref[0], int(ref[1])))
    if not found:
        raise ValueError("ActorNames has no talk line references")
    return [(name, int(line)) for name, line in found]


# The app's modules that refer to lines of the disc by themselves (`stname:40`, `talk06:690`), besides ActorNames: the
# game names of the quest texts and of the app's other sentences. Every reference of these files is a word.
APP_WORD_FILES = [ROOT / "Frontend/src/handheld" / name for name in ("quest-words.ts", "game-words.ts", "composed-messages.ts")]
NAME_TABLES = {"stname", "starea", "askmap", "amtmap", "htlnam", "shpnam", "itmnam", "sklnam", "cardnm", "dignam", "status", "itshop", "mfight", "crdshp"}


def is_talk(name):
    """A table of lines that start with their speaker: the talk tables and the tables of the story scenes, which ddw3
    splits from their archives as `dmg800/9` (entry 9 of ESDMG800.BIN)."""
    return name.startswith("talk") or re.fullmatch(r"dmg\d{3}/\d+", name) is not None


def app_word_lines():
    """[(table, line)] of every reference in APP_WORD_FILES, the talk lines (their speakers) and the name tables."""
    found = set()
    for file in APP_WORD_FILES:
        for name, line in re.findall(r'"([a-z]{6}|talk\d\d|dmg\d{3}/\d+):(\d+)"', file.read_text(encoding="utf8")):
            if is_talk(name) or name in NAME_TABLES:
                found.add((name, int(line)))
    if not found:
        raise ValueError("The app's word files have no references")
    return sorted(found, key=lambda ref: (ref[0], ref[1]))


def name_references(source, lines):
    """{stable key: ref} of the name table lines `lines`, after checking each one holds a name in all five languages."""
    refs = {}
    strings_of = functools.cache(table)
    for name, index in lines:
        for folder, first in [(source / "eng", "es"), *((source / language, prefix) for language, prefix in LANGUAGES.values())]:
            strings = strings_of(folder, first, name)
            if index >= len(strings) or not real(strings[index]):
                raise ValueError(f"{name}:{index} names nothing in {folder.name}")
        refs[f"{name}:{index}"] = {"table": name, "line": index}
    return refs


def speaker_references(other, prefix, lines):
    """{stable key: ref} of the talk lines `lines`, after checking each one names a speaker in English and in `other`."""
    refs = {}
    strings_of = functools.cache(table)
    for name, index in lines:
        for folder, first in ((other.parent / "eng", "es"), (other, prefix)):
            strings = strings_of(folder, first, name)
            speaker = SPEAKER.match(strings[index]) if index < len(strings) else None
            if speaker is None or not flat(speaker[1]).rstrip("."):
                raise ValueError(f"{name}:{index} names no speaker in {folder.name}")
        refs[f"{name}:{index}"] = {"table": name, "line": index, "selector": "speaker"}
    return refs


# The tables the app reads whole, line by line: the sector names (starea) the map names of the sectors use, and the
# Digimon names (dignam), which the app's tables refer to by line (`nameId`, from the battle and form tables of the disc).
WHOLE_TABLES = ["starea", "dignam"]


def whole_table_references(source, name):
    """{stable key: ref} of every line of the table `name` that holds something in all five languages."""
    tables = [table(source / "eng", "es", name)] + [table(source / language, prefix, name) for language, prefix in LANGUAGES.values()]
    if len({len(lines) for lines in tables}) != 1:
        raise ValueError(f"{name} has a different number of lines in some language")
    return {f"{name}:{index}": {"table": name, "line": index}
            for index in range(len(tables[0])) if all(real(lines[index]) for lines in tables)}


def stable_key(ref):
    key = f"{ref['table']}:{ref['line']}"
    # Two pieces of htlnam:17 are also names in their own right. Keep the
    # selector in this facts table, rather than reconstructing keys in the app.
    if ref.get("selector", "").startswith("part:"):
        key += ":" + ref["selector"]
    return key


def charset(checkout):
    rust = (checkout / "tools/ddw3-lang-file/src/codepoint.rs").read_text(encoding="utf8")
    pattern = r'^\s*((?:0x[0-9a-fA-F]{2}\s*,?\s*)+)=>\s*\w+\s*,\s*"[^"]*"\s*,\s*"((?:[^"\\]|\\.)*)"\s*;'
    result = {}
    for match in re.finditer(pattern, rust, re.M):
        code = "".join(re.findall(r"0x([0-9a-fA-F]{2})", match[1])).lower()
        if code in result:
            raise ValueError("Duplicate charset code")
        result[code] = json.loads('"' + match[2] + '"')
    if len(result) != 358:
        raise ValueError(f"Expected 358 charset codes, found {len(result)}")
    return result


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=1) + "\n", encoding="utf8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("checkout", type=Path)
    parser.add_argument("--references", type=Path)
    args = parser.parse_args()
    if args.references and args.references.resolve().is_relative_to(ROOT.resolve()):
        parser.error("Private text fixtures must be outside the repository")
    source = args.checkout / "lang_file/dw2003"
    english = source / "eng"
    message_refs, own = messages(english, english, "es")
    word_refs = None
    speakers = None
    app_lines = app_word_lines()
    lines = sorted(set(actor_speaker_lines()) | {ref for ref in app_lines if is_talk(ref[0])},
                   key=lambda ref: (ref[0], ref[1]))
    for _, (language, prefix) in LANGUAGES.items():
        actual_messages, actual_own = messages(english, source / language, prefix)
        actual_words = word_references(english, source / language, prefix)
        actual_speakers = speaker_references(source / language, prefix, lines)
        if actual_messages != message_refs or actual_own != own:
            raise ValueError("Message references differ between languages")
        if word_refs is None:
            word_refs, speakers = actual_words, actual_speakers
        elif actual_words != word_refs:
            raise ValueError("Word references differ between languages")
    keys = [stable_key(ref) for ref in word_refs.values()]
    if len(set(keys)) != len(keys):
        raise ValueError("Stable word reference collision")
    # The lines of ActorNames that no name of the tables already reads (a speaker named like a card, for one).
    all_words = {stable_key(ref): ref for ref in word_refs.values()}
    for key, ref in speakers.items():
        if all_words.setdefault(key, ref) != ref:
            raise ValueError(f"{key} is read as another word")
    whole = {key: ref for name in WHOLE_TABLES for key, ref in whole_table_references(source, name).items()}
    for key, ref in whole.items():
        if all_words.setdefault(key, ref) != ref:
            raise ValueError(f"{key} is read as another word")
    # The name table lines the app's sentences name by themselves (APP_WORD_FILES) that no word above reads already.
    named = name_references(source, [ref for ref in app_lines if not is_talk(ref[0])])
    for key, ref in named.items():
        if all_words.setdefault(key, ref) != ref:
            raise ValueError(f"{key} is read as another word")
    resources = ROOT / "Android/extractor/src/main/resources/extractor"
    write_json(resources / "charset.json", charset(args.checkout))
    write_json(resources / "text-keys.json", {
        "messages": [{"path": list(path), **ref} for path, ref in sorted(message_refs.items())],
        "words": [{"key": key, **ref} for key, ref in sorted(all_words.items())],
    })
    (Path(__file__).parent / "en-us-own-texts.txt").write_text(
        "# App message paths without a game-table match at similarity >= 0.88.\n"
        "# These retain the app's English fallback. No game strings are stored here.\n" +
        "\n".join(sorted(own)) + "\n", encoding="utf8")
    if args.references:
        write_json(args.references / "legacy-word-refs.json", {key: stable_key(ref) for key, ref in word_refs.items()})
        for locale, (language, prefix) in {"en-US": ("eng", "es"), **LANGUAGES}.items():
            tables = {file.stem[2:]: tomllib.loads(file.read_text(encoding="utf8"))["strings"]
                      for file in sorted((source / language).glob("*.toml"))}
            write_json(args.references / locale / "tables.json", tables)
            if locale == "en-US":
                game = {}
                for path, ref in message_refs.items():
                    put(game, path, flat(tables[ref["table"]][ref["line"]]))
                write_json(args.references / locale / "game.generated.json", game)
    print(f"text facts: {len(message_refs)} messages, {len(all_words)} words ({len(set(speakers) - set(keys))} speakers of the field only, "
          f"{len(set(whole) - set(keys) - set(speakers))} lines of {', '.join(WHOLE_TABLES)} only, "
          f"{len(set(named) - set(keys) - set(speakers) - set(whole))} name lines of the app's sentences only), {len(own)} fallback paths, 358 charset codes")


if __name__ == "__main__":
    main()
