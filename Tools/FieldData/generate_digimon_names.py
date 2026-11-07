#!/usr/bin/env python3
"""Writes the line of the game's Digimon names (DIGNAM, `dignam:N` for the app) of every Digimon of the app's tables,
from the European disc, so that the tables keep no names and the app reads them from the user's disc:

  - Frontend/src/database/enemy.json: `nameId` of each enemy, from the battle table (FILE_BATTLE_TABLE, 0x1CF on the
    European disc): BattleTableEntry {s16 id, s16 item, s16 itemChance, s16 nameId, ...}, 0x46 bytes each, up to id 0
    (include/battle/battle_mode/battle.h of ReGame-Labs/dw3_decomp, MIT); `id` is the enemy's `memoryId`.
  - Frontend/src/database/digievolution/digievolution.json: `nameId` of each form, from the executable's form table
    (0x8003EF5C, 55 rows of 0x58 bytes: the form id at 0, its name at 0x55), as the extractor's battle catalog reads it.
  - Frontend/src/database/digimon.json: partner i is DIGNAM's line i + 1 (include/engine/game_state.h).

The bosses of the legendary weapons get the field character they stand as (`fieldActor`, BOSS_ACTORS) for their
picture. Colour and letter variants of an enemy (the game names them alike) keep a `variant` of the app's own, shown after the
name in the app's words (`green`, or a letter such as `A`).

A row that still has an English `name` (the original project's tables) loses it: its base name and variant become the
`nameId` and `variant`. Later runs only check the tables against the disc and rewrite the ids.

Usage: DW3_ISO=game.iso DW3_DECOMP=dw3_decomp python3 Tools/FieldData/generate_digimon_names.py
Reads the disc only; nothing of the decompilation is run.
"""
import json
import os
import re
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "Tools/RomExtract"))
from discfs import all_files  # noqa: E402
from iso import Iso  # noqa: E402
from world_map import Files  # noqa: E402

DATABASE = ROOT / "Frontend/src/database"
FILE_BATTLE_TABLE = 0x1CF
BATTLE_ENTRY = 0x46
FORM_TABLE, FORM_COUNT, FORM_ROW = 0x8003EF5C, 55, 0x58
NAME_LINES = 251
# The field characters (character, direction of fieldstg.c's actors) the bosses of the legendary weapons stand as before
# their battle, by their line of DIGNAM: the pictures the original project had of them (Frontend/src/assets/bosses), matched
# by SpriteParityTests. The extractor draws every character as `actors/<character>-<direction>`.
BOSS_ACTORS = {212: "59-0", 213: "90-0", 214: "133-0", 215: "150-0", 216: "151-0", 217: "165-0"}


def battle_names(files):
    """{enemy id (memoryId): DIGNAM line} of the battle table."""
    data = files.read(FILE_BATTLE_TABLE)
    names = {}
    for offset in range(0, len(data) - BATTLE_ENTRY + 1, BATTLE_ENTRY):
        enemy, name = struct.unpack_from("<h", data, offset)[0], struct.unpack_from("<h", data, offset + 6)[0]
        if enemy == 0:
            return names
        if not 0 < name < NAME_LINES or enemy in names:
            raise ValueError(f"battle table entry {enemy}: invalid name {name}")
        names[enemy] = name
    raise ValueError("the battle table has no end")


def form_names(iso):
    """{form id: DIGNAM line} of the executable's form table."""
    lba, size = {name: (lba, size) for name, lba, size in all_files(iso)}["SLES_039.36"]
    exe = iso.sector(lba, (size + 2047) // 2048)[:size]
    if exe[:8] != b"PS-X EXE":
        raise ValueError("not the PAL executable")
    start = FORM_TABLE - struct.unpack_from("<I", exe, 0x18)[0] + 2048
    forms = {}
    for row in range(start, start + FORM_COUNT * FORM_ROW, FORM_ROW):
        forms[struct.unpack_from("<H", exe, row)[0]] = exe[row + 0x55]
    return forms


VARIANT = re.compile(r"^(.*?)\((Gold|White|Pink|Green|Brown|Red|Black|Blue|Silver|Aqua|Purple|[A-H])\)$")


def split_name(name):
    """English name of a table to its base name and its variant id (`green`, `A`) or None."""
    match = VARIANT.match(name)
    if match is None:
        return name, None
    variant = match.group(2)
    return match.group(1), variant if len(variant) == 1 else variant.lower()


def with_name_id(raw, name_id, variant):
    """The row with `nameId` (and `variant`) in place of its English `name`."""
    name = raw.get("name")
    if name is not None:
        variant = split_name(name)[1]
    else:
        variant = raw.get("variant", variant)
    out = {"nameId": name_id}
    if variant is not None:
        out["variant"] = variant
    out.update({key: value for key, value in raw.items() if key not in ("name", "nameId", "variant")})
    return out


def main():
    for name in ("DW3_ISO", "DW3_DECOMP"):
        if name not in os.environ:
            sys.exit(f"{name} is not set; see the header")
    iso = Iso()
    battle = battle_names(Files(iso, os.environ["DW3_DECOMP"]))
    forms = form_names(iso)

    tables = {
        "enemy.json": lambda key, raw: battle[raw["memoryId"]],
        "digievolution/digievolution.json": lambda key, raw: forms[int(key)],
        "digimon.json": lambda key, raw: int(key) + 1,
    }
    for file, name_of in tables.items():
        path = DATABASE / file
        table = json.loads(path.read_text(encoding="utf-8"))
        table = {key: with_name_id(raw, name_of(key, raw), None) for key, raw in table.items()}
        if file == "enemy.json":
            for raw in table.values():
                if raw["nameId"] in BOSS_ACTORS:
                    raw["fieldActor"] = BOSS_ACTORS[raw["nameId"]]
        path.write_text(json.dumps(table, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"{file}: {len(table)} rows, {len({raw['nameId'] for raw in table.values()})} names", file=sys.stderr)


if __name__ == "__main__":
    main()
