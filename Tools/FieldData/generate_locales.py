#!/usr/bin/env python3
"""
The names and descriptions of the game in each of its languages, from the game's own text.

The game has the same tables in every language, line by line. A text of the app that is a line of the English table is
replaced with that line of the other language; what is not there stays out, for `ui.json` or the English fallback.

Writes private reference fixtures into <out>/<locale>/ (never into Frontend):
  - game.generated.json: the messages of the app that the game has, with the keys of en-US.
  - words.generated.json: English line to its line in that language, for the names the app's own tables have in English
    (exits, characters, techniques).
and leaves in <out> what is left to translate (<locale>.todo.json), for whoever does.

Usage: python3 Tools/FieldData/generate_locales.py <folder lang_file/dw2003> <out>
"""
import difflib
import json
import re
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOCALES = ROOT / "Frontend/src/i18n/locales"
LANGUAGES = {"es-ES": ("spn", "ss"), "fr-FR": ("fra", "fs"), "de-DE": ("ger", "ds"), "it-IT": ("ita", "is")}

# The app's files that hold the game's data, with the tables they come from first.
GAME_FILES = {
    "equipment/equipment.json": ["itmnam", "itminf"],
    "consumable-item/consumable-item.json": ["itmnam", "itminf"],
    "digievolution/technique.json": ["sklnam", "sklinf"],
    "enemy/drop.json": ["itmnam"],
    "tcg/card.json": ["cardnm", "cardst", "cardgm", "crdabm"],
    "tcg/folder.json": ["crddek", "itmnam"],
    "tcg/card-shop.json": ["crdshp", "shpnam"],
    "map.json": ["stname", "askmap", "amtmap", "htlnam", "shpnam"],
}
# What the app's own tables name in English. Later tables never replace a line an earlier one gave.
WORD_TABLES = ["stname", "askmap", "amtmap", "htlnam", "shpnam", "itmnam", "itminf", "sklnam", "sklinf", "cardnm", "dignam", "diginf"]
# en-US files with the app's own text, which the game never has (none at the moment).
NOT_SHOWN: set[str] = set()
SPEAKER = re.compile(r"^\[name\](.*?)\[name\]")
SIMILAR = 0.88


def norm(text):
    return re.sub(r"[^a-z0-9]+", " ", text.lower()).strip()


def flat(text):
    # The game's font draws the German sharp s with the Greek letter's code.
    return re.sub(r"\s+", " ", text.replace("\\n", " ")).strip().replace("β", "ß")


def real(line):
    """Whether a line of a table holds something: the game fills its empty ones with a single letter."""
    return len(norm(line)) > 1


def table(folder, prefix, name):
    return tomllib.loads((folder / f"{prefix}{name}.toml").read_text(encoding="utf8")).get("strings", [])


def leaves(node, path=()):
    if isinstance(node, dict):
        for key, value in node.items():
            yield from leaves(value, (*path, key))
    elif isinstance(node, str):
        yield path, node


def put(tree, path, value):
    for key in path[:-1]:
        tree = tree.setdefault(key, {})
    tree[path[-1]] = value


def speakers(english, other):
    """English name of each who speaks to their name in the language, from all the lines with a speaker."""
    found = {}
    for file in sorted(english.rglob("*.toml")):
        twin = other / file.relative_to(english).parent / (other_prefix(other) + file.name[2:])
        if not twin.exists():
            continue
        lines, others = (tomllib.loads(path.read_text(encoding="utf8")).get("strings", []) for path in (file, twin))
        for line, translated in zip(lines, others):
            one, two = SPEAKER.match(line), SPEAKER.match(translated)
            if one and two and flat(one[1]):
                # Some languages end the name with a full stop, or start it in lower case.
                name = flat(two[1]).rstrip(".")
                found.setdefault(flat(one[1]).rstrip("."), name[:1].upper() + name[1:])
    return found


def other_prefix(folder):
    return next(iter(sorted(folder.glob("*.toml")))).name[:2]


def words_of(english, other, prefix):
    words = {}
    for name in WORD_TABLES:
        for line, translated in zip(table(english, "es", name), table(other, prefix, name)):
            if not real(line) or not real(translated):
                continue
            parts = line.split("\n"), translated.split("\n")
            if len(parts[0]) == len(parts[1]) and len(parts[0]) > 1:
                for one, two in zip(*parts):
                    words.setdefault(flat(one), flat(two))
            words.setdefault(flat(line), flat(translated))
    for one, two in speakers(english, other).items():
        words.setdefault(one, two)
    return words


def main():
    source, out = Path(sys.argv[1]), Path(sys.argv[2])
    if out.resolve().is_relative_to(ROOT.resolve()):
        raise SystemExit("Private text references must be outside the repository")
    out.mkdir(parents=True, exist_ok=True)
    english = source / "eng"
    en_files = sorted((LOCALES / "en-US").rglob("*.json"))

    for locale, (language, prefix) in LANGUAGES.items():
        other = source / language
        game, todo, matched, left = {}, {}, 0, 0
        for file in en_files:
            relative = file.relative_to(LOCALES / "en-US").as_posix()
            if relative.startswith("quest/") or relative in NOT_SHOWN or relative == "handheld.json":
                continue
            tables = GAME_FILES.get(relative)
            lines = {}
            for name in tables or []:
                for line, translated in zip(table(english, "es", name), table(other, prefix, name)):
                    if real(line) and real(translated):
                        lines.setdefault(norm(line), flat(translated))
            for path, text in leaves(json.loads(file.read_text(encoding="utf8"))):
                if not text.strip():
                    continue
                key = norm(text)
                value = lines.get(key)
                if value is None and tables and len(key) > 20:
                    close = difflib.get_close_matches(key, lines, n=1, cutoff=SIMILAR)
                    value = lines[close[0]] if close else None
                if value is None:
                    put(todo, path, text)
                    left += 1
                else:
                    put(game, path, value)
                    matched += 1
        folder = out / locale
        folder.mkdir(parents=True, exist_ok=True)
        (folder / "game.generated.json").write_text(json.dumps(game, ensure_ascii=False, indent=1) + "\n", encoding="utf8")
        words = dict(sorted(words_of(english, other, prefix).items()))
        (folder / "words.generated.json").write_text(json.dumps(words, ensure_ascii=False, indent=0) + "\n", encoding="utf8")
        (out / f"{locale}.todo.json").write_text(json.dumps(todo, ensure_ascii=False, indent=1) + "\n", encoding="utf8")
        print(locale, "from the game", matched, "left", left, "words", len(words))


if __name__ == "__main__":
    main()
