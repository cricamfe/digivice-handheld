#!/usr/bin/env python3
"""Generates Frontend/src/handheld/server-copies.ts: the name of each map the game repeats for its
second server (the ids in ServerCopies of Frontend/src/handheld/map-view.ts).

The facts come from the matching decompilation of the European version, ReGame-Labs/dw3_decomp (MIT):
  - src/field/field_mode/data/fieldstg.c: FIELDSTG_stages (the European list) holds {mode, file, WSTAGnnn_startStage},
    where mode is the map id in decimal.
  - src/field/stages/**/wstagnnn.c: its first comment names the stage ("WSTAG206: Amaterasu Bridge, Central Sector.").
  - the picture of a stage is Frontend/src/assets/maps/SnnnPACK.webp, used only to check its size (the app finds a
    copy's picture by its map id).

The name is stored as a reference to the game's own place name table on the disc (`stname:118`), so the app ships no
text: the app reads it from the user's disc. The English table that links a name to its line is the one of ddw3
(DW3_DDW3, a copy of the ddw3 checkout; see stname_words in Tools/FieldData/stage_data.py).

Usage: DW3_DDW3=ddw3 python3 Tools/ServerCopies/generate_server_copies.py <path to dw3_decomp> [--out <file>]

It also reports to stderr the pictures whose size is not a whole number of the game's 128 pixel cells, and those whose
size differs from the picture of the original map (the copies repeat the original's layout, so the sizes must agree
for the player's position to land on both). Reads only; the decompilation is data, nothing in it is run.
"""
import argparse
import json
import re
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "Tools/FieldData"))
from stage_data import map_picture, stname_words  # noqa: E402

MAP_VIEW = ROOT / "Frontend/src/handheld/map-view.ts"
MAPS = ROOT / "Frontend/src/assets/maps"
DEFAULT_OUT = ROOT / "Frontend/src/handheld/server-copies.ts"
CELL = 128


def server_copies():
    """The `[first id, last id, id of the first original]` ranges of ServerCopies, as {copy id: original id}."""
    text = MAP_VIEW.read_text(encoding="utf-8")
    block = text[text.index("const ServerCopies"):]
    block = block[:block.index("];")]
    copies = {}
    for first, last, original in re.findall(r"\[0x([0-9a-f]+), 0x([0-9a-f]+), 0x([0-9a-f]+)\]", block):
        first, last, original = int(first, 16), int(last, 16), int(original, 16)
        for map_id in range(first, last + 1):
            copies[map_id] = original + map_id - first
    return copies


def european_stages(decomp):
    """{mode: stage number} from the European FIELDSTG_stages."""
    text = (decomp / "src/field/field_mode/data/fieldstg.c").read_text(encoding="utf-8")
    start = text.index("#elif VERSION_EU\nStageEntry FIELDSTG_stages[]")
    table = text[start:text.index("};", start)]
    return {int(mode): int(number) for mode, _file, number in
            re.findall(r"\{(\d+), (\d+), WSTAG(\d+)_startStage\}", table)}


def stage_name(decomp, number):
    """The name in the first comment of wstagNNN.c: what comes before its first ", " or ". ", or None."""
    found = list((decomp / "src/field/stages").glob(f"**/wstag{number}.c"))
    if not found:
        return None
    text = found[0].read_text(encoding="utf-8")
    comment = text[text.index("/*") + 2:text.index("*/")]
    comment = " ".join(line.strip().lstrip("*").strip() for line in comment.splitlines()).strip()
    match = re.match(rf"WSTAG{number}:\s*(.+)", comment)
    if match is None:
        return None
    return re.split(r"[,:.](?:\s|$)", match.group(1))[0].strip()


def webp_size(path):
    """Width and height of a WebP, read from its header (VP8, VP8L or VP8X)."""
    data = path.read_bytes()[:40]
    kind = data[12:16]
    if kind == b"VP8X":
        return (int.from_bytes(data[24:27], "little") + 1, int.from_bytes(data[27:30], "little") + 1)
    if kind == b"VP8L":
        bits = struct.unpack("<I", data[21:25])[0]
        return ((bits & 0x3FFF) + 1, ((bits >> 14) & 0x3FFF) + 1)
    if kind == b"VP8 ":
        width, height = struct.unpack("<HH", data[26:30])
        return (width & 0x3FFF, height & 0x3FFF)
    raise ValueError(f"{path}: not a WebP")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("decomp", type=Path, help="the dw3_decomp checkout")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    stages = european_stages(args.decomp)
    words = stname_words()
    entries = []
    compared = 0
    for map_id, original in sorted(server_copies().items()):
        number = stages.get(map_id)
        name = stage_name(args.decomp, number) if number is not None else None
        if name is None:
            print(f"no stage or name for {map_id:04X}", file=sys.stderr)
            continue

        image = f"S{number}PACK"
        if not (MAPS / f"{image}.webp").exists():
            print(f"{map_id:04X} WSTAG{number}: no picture {image}.webp, it shows the original's", file=sys.stderr)
            image = None
        else:
            size = webp_size(MAPS / f"{image}.webp")
            twin = map_picture(f"{original:04X}")
            twin_size = webp_size(MAPS / f"{twin}.webp") if twin and (MAPS / f"{twin}.webp").exists() else None
            if size[0] % CELL or size[1] % CELL:
                print(f"{map_id:04X} {image}: {size[0]}x{size[1]} is not a whole number of {CELL} px cells", file=sys.stderr)
            compared += twin_size is not None
            if twin_size is not None and twin_size != size:
                print(f"{map_id:04X} {image}: {size[0]}x{size[1]} but the original {original:04X} ({twin}) is "
                      f"{twin_size[0]}x{twin_size[1]}", file=sys.stderr)
        if name not in words:
            print(f"{map_id:04X}: \"{name}\" is not a place name of the stname table", file=sys.stderr)
            continue
        entries.append((f"{map_id:04X}", words[name]))

    lines = [
        "// Generated by Tools/ServerCopies/generate_server_copies.py: do not edit.",
        "// The maps the game repeats for its second server, with the name of their own (a reference to the game's place names,",
        "// read from the user's disc). The facts are from the matching decompilation of the European version,",
        "// ReGame-Labs/dw3_decomp (MIT). A copy's picture is the one of its own id, when the map tables have it.",
        "",
        "export interface ServerCopy {",
        "  /** Reference of its name in the game's place names (`stname:118`), for `gameWord`. */",
        "  word: string;",
        "}",
        "",
        "export const ServerCopyData: Readonly<Record<string, ServerCopy>> = {",
    ]
    for map_id, word in entries:
        lines.append(f"  {json.dumps(map_id)}: {{ word: {json.dumps(word)} }},")
    lines += ["};", ""]
    args.out.write_text("\n".join(lines), encoding="utf-8")
    print(f"{len(entries)} copies written to {args.out}; {compared} sizes compared with the original's", file=sys.stderr)


if __name__ == "__main__":
    main()
