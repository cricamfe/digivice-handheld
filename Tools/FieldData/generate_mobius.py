#!/usr/bin/env python3
"""Generates the Mobius Desert data of the Amaterasu server from the matching decompilation of the European version,
ReGame-Labs/dw3_decomp (MIT), and checks the Asuka server's data in the repository against the same stages:

  Frontend/src/database/mobius-desert/mobius-desert-areas-amaterasu.json      the grid, with the notes of this server
  Frontend/src/database/mobius-desert/mobius-desert-areas-map-amaterasu.json  its two maps and their areas, with the
                                                                              point of each character on the picture
  Frontend/src/database/mobius-desert/mobius-desert-enemy.json                the enemies of the second server's maps

Usage: python3 Tools/FieldData/generate_mobius.py <path to dw3_decomp>

The Mobius Desert is two maps of eight areas each (0x258 and 0x259, WSTAG635 and WSTAG640; their copies on the Amaterasu
server are 0x2C1 and 0x2C2, WSTAG636 and WSTAG641). The area is the place argument (GAME.placeArg) and `placePoints`
of the stage says where its four exits lead, in the slot order north, east, south, west. The characters are the stage's
`FieldActorEntry` with a PLACE_ARG(n) condition: the area they stand in, and their point (x, y) in pixels of the picture
(1152 x 1024). Their names are the ones Tools/RomExtract/field_actors.py read from the game's own talk text
(`ActorNames` in Frontend/src/handheld/field-actors.ts, the talk lines that name them, read in ddw3's English tables:
DW3_DDW3). The battles are the stage's area 0 (one place for the whole map).

It reports to stderr every difference with the data of the repository before it writes anything.
"""
import argparse
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import stage_data  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
DATABASE = ROOT / "Frontend/src/database"
MAPS = ROOT / "Frontend/src/assets/maps"
SERVERS = {0x258: 0x2C1, 0x259: 0x2C2}  # Asuka map -> the same map on the Amaterasu server
DIRECTIONS = ["north", "east", "south", "west"]  # the order of the slots of each area
NEIGHBORS = {0x257: "noiseDesertS", 0x25A: "mirageTower"}  # the maps outside the 16 areas the exits lead to
PICTURE = (1152, 1024)

sys.path.insert(0, str(ROOT / "Tools/ServerCopies"))
from generate_server_copies import webp_size  # noqa: E402


def actor_references():
    """{map id: {actor id: talk line that names it}} from `ActorNames` in field-actors.ts."""
    text = (ROOT / "Frontend/src/handheld/field-actors.ts").read_text(encoding="utf-8")
    block = text[text.index("export const ActorNames"):]
    block = block[:block.index("};")]
    references = {}
    for map_id, body in re.findall(r'"([0-9A-F]{4})": \{([^}]*)\}', block):
        references[map_id] = {int(i): n for i, n in re.findall(r'(\d+): "([^"]*)"', body)}
    return references


def actor_names():
    """{map id: {actor id: English name}} from `ActorNames` in field-actors.ts, whose talk lines are read in ddw3."""
    references = actor_references()
    english = stage_data.talk_speakers({r for by_actor in references.values() for r in by_actor.values()})
    return {map_id: {i: english[r] for i, r in by_actor.items()} for map_id, by_actor in references.items()}


def percent(value, size):
    """Half-percent steps, as the repository's coordinates are written."""
    return round(value / size * 200) / 2


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("decomp", type=Path, help="the dw3_decomp checkout")
    args = parser.parse_args()
    stage_of = stage_data.european_stages(args.decomp)
    asuka = json.loads((DATABASE / "mobius-desert/mobius-desert-areas-map.json").read_text(encoding="utf-8"))
    grid = json.loads((DATABASE / "mobius-desert/mobius-desert-areas.json").read_text(encoding="utf-8-sig"))
    map_table = json.loads((DATABASE / "map.json").read_text(encoding="utf-8"))
    enemy_table = json.loads((DATABASE / "enemy.json").read_text(encoding="utf-8"))
    names = actor_names()
    references = actor_references()
    problems = []

    stages = {mode: stage_data.stage_text(args.decomp, stage_of[mode]) for mode in list(SERVERS) + list(SERVERS.values())}
    points = {mode: stage_data.parse_points(text) for mode, text in stages.items()}
    actors = {mode: stage_data.parse_actors(text) for mode, text in stages.items()}

    # 1. The exits of the 64 areas of the Asuka server, against the graph of the repository.
    label_of = {(int(mode, 16), int(variant)): cell["label"] for mode, cells in asuka.items() for variant, cell in cells.items()}
    for mode in SERVERS:
        for variant in range(1, 9):
            cell = asuka[f"{mode:04X}"][str(variant)]
            for direction, (dest, place, arg, _x, _y, _dir) in zip(DIRECTIONS, points[mode][(1, variant)]):
                expected = NEIGHBORS.get(dest) or label_of.get((dest, arg))
                if place not in (0, 1) or expected != cell[direction]:
                    problems.append(f"{mode:04X} area {variant} ({cell['label']}) {direction}: the game leads to {expected}, the app has {cell[direction]}")

    # 2. The Amaterasu maps wire the same way: each exit leads to the copy of the same map and area.
    for mode, copy in SERVERS.items():
        for variant in range(1, 9):
            for (dest, _p, arg, *_), (twin, _tp, twin_arg, *_) in zip(points[mode][(1, variant)], points[copy][(1, variant)]):
                original = {0x2C0: 0x257, 0x2C3: 0x25A, 0x2C1: 0x258, 0x2C2: 0x259}[twin]
                if (dest, arg) != (original, twin_arg):
                    problems.append(f"{copy:04X} area {variant}: leads to {twin:04X}/{twin_arg}, the Asuka map leads to {dest:04X}/{arg}")

    # 3. The characters of each map: the area they stand in and where, from the stage.
    def characters(mode):
        found = {}
        for conditions, ident, x, y, _d in actors[mode]:
            variants = [int(re.fullmatch(r"PLACE_ARG\((\d+)\)", c).group(1)) for c, _v in conditions if c.startswith("PLACE_ARG")]
            for variant in variants:
                found.setdefault(variant, {}).setdefault(ident, (x, y))
        return found

    for mode in SERVERS:
        size = webp_size(MAPS / f"{stage_data.map_picture(f'{mode:04X}')}.webp")
        if size != PICTURE:
            problems.append(f"{mode:04X}: the picture is {size[0]}x{size[1]}, not {PICTURE[0]}x{PICTURE[1]}")
        copy = SERVERS[mode]
        copy_size = webp_size(MAPS / f"S{stage_of[copy]}PACK.webp")
        if copy_size != PICTURE:
            problems.append(f"{copy:04X}: the picture is {copy_size[0]}x{copy_size[1]}, not {PICTURE[0]}x{PICTURE[1]}")

    # The Asuka notes against the stage (areas with a character, and where). The notes are the talk lines that name
    # the DRI agents (`talk02:829`), read here in ddw3's English tables.
    asuka_notes = {c["label"]: c["note"] for row in grid["areas"] for c in row if c.get("note") and c["type"] == "normal"}
    asuka_english = stage_data.talk_speakers(set(asuka_notes.values()))
    asuka_notes = {label: asuka_english[note] for label, note in asuka_notes.items()}
    for variant, found in sorted(characters(0x258).items()):
        label = asuka["0258"][str(variant)]["label"]
        stage_names = sorted({names["0258"].get(i) for i in found})
        point = list(found.values())[0]
        coordinates = asuka["0258"][str(variant)].get("coordinates")
        if asuka_notes.get(label) not in stage_names:
            problems.append(f"0258 area {variant} ({label}): the stage has {stage_names}, the app's note is {asuka_notes.get(label)}")
        if coordinates is None or abs(coordinates["x"] - point[0] / PICTURE[0] * 100) > 1.5 or abs(coordinates["y"] - point[1] / PICTURE[1] * 100) > 1.5:
            problems.append(f"0258 area {variant} ({label}): the stage puts the character at {point}, the app has {coordinates}")
    for label in asuka_notes:
        if not any(asuka["0258"][str(v)]["label"] == label for v in characters(0x258)):
            problems.append(f"the app has a note at {label} that no character of the stage stands in")

    # 4. The Amaterasu data.
    out_map = {}
    amaterasu_notes = {}
    for mode, copy in SERVERS.items():
        cells = json.loads(json.dumps(asuka[f"{mode:04X}"]))
        found = characters(copy)
        for variant in range(1, 9):
            cell = cells[str(variant)]
            cell.pop("coordinates", None)
            if variant in found:
                ident, (x, y) = next(iter(found[variant].items()))
                note = names[f"{copy:04X}"].get(ident)
                if note is None:
                    problems.append(f"{copy:04X} area {variant}: no name for the character {ident}")
                    continue
                cell["coordinates"] = {"x": percent(x, PICTURE[0]), "y": percent(y, PICTURE[1])}
                # The note is the talk line that names each character (`talk07:848`), read from the disc by the app.
                amaterasu_notes[cell["label"]] = "/".join(dict.fromkeys(references[f"{copy:04X}"][i] for i in found[variant]
                                                                        if i in references[f"{copy:04X}"]))
        out_map[f"{copy:04X}"] = cells
    out_grid = json.loads(json.dumps(grid))
    for row in out_grid["areas"]:
        for cell in row:
            cell.pop("note", None)
            if cell["type"] == "normal" and cell["label"] in amaterasu_notes:
                cell["note"] = amaterasu_notes[cell["label"]]

    # 5. The enemies: area 0 of the stage, 0x258/0x259 against map.json, the copies as new data.
    enemies, encounters = stage_data.enemies_and_encounters(args.decomp)
    text = stage_data.preprocess((args.decomp / "src/field/field_mode/data/fieldstg.c").read_text(encoding="utf-8"))
    block = text[text.index("BattleEnemy FIELDSTG_enemies[] = {"):]
    hp = [int(h) for _f, _l, h, _m, _s in re.findall(r"\{(-?\d+), (\d+), (\d+), (\d+), (\d+)\}", block[:block.index("};")])]
    out_enemy = {}
    for mode in list(SERVERS) + list(SERVERS.values()):
        battles = stage_data.parse_battles(stages[mode])
        found = []
        for _count, area in battles[0][:3]:
            for encounter in area:
                if encounter and encounter not in found:
                    found.append(encounter)
        ids = []
        for encounter in sorted(found):
            for index in encounters[encounter]:
                fighter, level = enemies[index]
                candidates = [k for k, e in enemy_table.items()
                              if e.get("memoryId") == fighter and e["level"] == level and e["hp"] == hp[index]]
                if len(candidates) != 1:
                    problems.append(f"{mode:04X}: fighter {fighter} level {level}: candidates {candidates}")
                ids += [c for c in candidates[:1] if c not in ids]
        print(f"{mode:04X} enemies:", [(stage_data.digimon_name(enemy_table[i]), enemy_table[i]['level']) for i in ids], file=sys.stderr)
        if mode in SERVERS:
            app = map_table[f"{mode:04X}"].get("enemies", {}).get("walking", [])
            if sorted(app) != sorted(ids):
                problems.append(f"{mode:04X}: the app lists enemies {app}, the game has {ids}")
        else:
            out_enemy[f"{mode:04X}"] = {"walking": ids}

    for line in problems:
        print(line, file=sys.stderr)

    folder = DATABASE / "mobius-desert"
    grid_text = json.dumps(out_grid, indent=2)
    grid_text = re.sub(r'\{\s+("label": "[^"]*",\s+"type": "[^"]*"(?:,\s+"note": "[^"]*")?)\s+\}',
                       lambda m: "{ " + re.sub(r"\s+", " ", m.group(1)) + " }", grid_text)  # a cell on a line, as in the Asuka file
    (folder / "mobius-desert-areas-amaterasu.json").write_text(grid_text + "\n", encoding="utf-8")
    (folder / "mobius-desert-areas-map-amaterasu.json").write_text(json.dumps(out_map, indent=2) + "\n", encoding="utf-8")
    (folder / "mobius-desert-enemy.json").write_text(json.dumps(out_enemy, indent=2) + "\n", encoding="utf-8")
    print(f"notes of the Amaterasu server: {amaterasu_notes}", file=sys.stderr)


if __name__ == "__main__":
    main()
