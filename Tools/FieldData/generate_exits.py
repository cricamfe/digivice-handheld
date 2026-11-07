#!/usr/bin/env python3
"""Generates Frontend/src/handheld/map-exits.ts: for each map of the game (both servers), the places where the player
leaves it for another, as a point on the map picture and the id of the destination. The name of the exit is not stored:
the app names it as it names the map (the second-server copy's name, or `locationName`), from the user's disc.

Where the facts come from (nothing is placed by hand):
  - src/field/stages/**/wstagNNN.c of the matching decompilation of the European version, ReGame-Labs/dw3_decomp (MIT):
    `stageSlots` is the table of what the player can trigger on the map. A slot of type SLOT_EXIT, SLOT_LAUNCH_OUT or a
    warp (SLOT_WARP0, SLOT_WARP1) leaves the map for the mode `arg`, which is the id of the destination map
    (include/field/field_map.h, trigger.c). Its `x`, `y` are where the player lands THERE, not where the slot is here.
  - Where the slot is here is the map's trigger layer: `FIELD_MAP_TRIGGERS`, the file `STAGE_FILE << 16 | 2` of the
    stage (the `SnnnTMPK` file on the disc), a map file like the walk map: a grid of 128 pixel cells that go down a
    tree to 8 x 8 pixel blocks (FieldMap.getCell, src/field/field_mode/map.c). Each pixel holds `direction << 5 | slot`,
    or 0 for none (FIELDSTG_findTrigger, trigger.c). The map's pixels are those of its picture, as the player's
    position is. This needs a copy of the disc, as the other scripts of Tools/RomExtract do.
  - The mode of each map: FIELDSTG_stages (the European list), `mode -> WSTAG number`, in fieldstg.c.
  - Destinations the app cannot name from its `map.<id>` messages (text-keys.json; the Circuit Board sectors share the
    one of 02EC, as `locationName` does) nor from a second-server copy (Frontend/src/handheld/server-copies.ts) get the
    reference of their name in the game's place names (`stname:94`) in `PlaceWords`, found from the first comment
    of their stage and the English table of ddw3.

A slot's trigger is every block of the layer that holds its number; the blocks that touch make one place, and its point is
the centre of its box. Places of different slots that lead to the same map are merged when their boxes (grown by
MERGE_MARGIN) overlap, and each gets its own label otherwise.

Usage: DW3_ISO=game.iso DW3_DDW3=ddw3 python3 Tools/FieldData/generate_exits.py <path to dw3_decomp> [--out <file>]
  then `npx prettier --write` on the file, which the repository keeps formatted.
  DW3_ISO is the disc image as user data of 2048 bytes per sector; DW3_DDW3 is a copy of ddw3 (for its list of files and
  the English place names). Needs numpy.
It reports to stderr what it could not place or name. Reads only; nothing of the decompilation is run.
"""
import argparse
import json
import os
import re
import struct
import sys
import tomllib
from collections import deque
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(ROOT / "Tools/RomExtract"))
from stage_data import TEXT_KEYS, european_stages, number, preprocess, stage_text, stname_words  # noqa: E402

DEFAULT_OUT = ROOT / "Frontend/src/handheld/map-exits.ts"
SERVER_COPIES = ROOT / "Frontend/src/handheld/server-copies.ts"

SLOT_EXIT, SLOT_WARP1, SLOT_WARP0, SLOT_LAUNCH_OUT = 1, 9, 10, 14
LEAVING = {SLOT_EXIT: "exit", SLOT_WARP1: "warp", SLOT_WARP0: "warp", SLOT_LAUNCH_OUT: "launch"}
BLOCK = 8
MERGE_MARGIN = 16
CIRCUIT_FIRST, CIRCUIT_LAST = 0x2E8, 0x2EE


def evaluate(expression, defs):
    """A constant expression of a stage (`STAGE_FILE << 16 | 2`) with its #defines, as an int."""
    expression = re.sub(r"\b(\w+)\b", lambda m: str(defs[m.group(1)]) if m.group(1) in defs else m.group(1), expression)
    expression = re.sub(r"\b0[xX][0-9a-fA-F]+\b", lambda m: str(int(m.group(0), 16)), expression)
    return int(eval(expression, {"__builtins__": {}}))  # digits and operators only, after the replacements above


def trigger_file(text):
    """The file number of the stage's FIELD_MAP_TRIGGERS map, or None when the stage has none."""
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    defs = {}
    for name, value in re.findall(r"#define\s+(\w+)\s+([^\n]+)", text):
        try:
            defs[name] = evaluate(value.strip().rstrip("\\"), defs)
        except Exception:
            pass
    match = re.search(r"FIELDSTG_map\.setFile\(\s*FIELD_MAP_TRIGGERS\s*,\s*([^;]+?)\)\s*;", text)
    return None if match is None else evaluate(match.group(1), defs)


class Disc:
    """Files of the disc by the name the file list of ddw3 gives them."""

    def __init__(self):
        from discfs import all_files
        from iso import Iso

        self.iso = Iso()
        self.by_name = {name.rsplit("/", 1)[-1].replace(".", "_"): (lba, size) for name, lba, size in all_files(self.iso)}
        lbas = Path(os.environ["DW3_DDW3"]) / "iso/dw2003-bootstrap/lbas.toml"
        self.names = tomllib.loads(lbas.read_text(encoding="utf-8"))["lbas"]

    def file(self, index):
        found = self.by_name.get(self.names[index])
        return None if found is None else self.iso.read(*found)


def archive_entries(data):
    """The entries of a file with a table of offsets (getArchiveEntry): an offset of 0 is an entry the file does not have."""
    count = struct.unpack_from("<I", data, 0)[0] // 4
    offsets = struct.unpack_from(f"<{count}I", data, 0)
    entries = []
    for k, start in enumerate(offsets):
        end = next((o for o in offsets[k + 1:] if o != 0), len(data))
        entries.append(data[start:end] if start != 0 else b"")
    return entries


def read_map_file(entry):
    """The value of every pixel of a map file, as FieldMap.getCell reads it: an array (height, width) of u8."""
    offsets = struct.unpack_from("<6I", entry, 0)
    grid = entry[offsets[0]:]
    width, height = grid[0], grid[1]
    cells = np.frombuffer(grid, np.uint8, width * height, 2).reshape(height, width)
    cells64 = np.frombuffer(entry, np.uint8, offsets[2] - offsets[1], offsets[1])
    cells32 = np.frombuffer(entry, "<i2", (offsets[3] - offsets[2]) // 2, offsets[2])
    cells16 = np.frombuffer(entry, "<i2", (offsets[4] - offsets[3]) // 2, offsets[3])
    cells8 = np.frombuffer(entry, "<i2", (offsets[5] - offsets[4]) // 2, offsets[4])
    pixels = np.frombuffer(entry, np.uint8, len(entry) - offsets[5], offsets[5])
    out = np.zeros((height * 128, width * 128), np.uint8)
    for gy in range(height):
        for gx in range(width):
            first = int(cells[gy, gx])
            for q64 in range(4):
                second = int(cells64[first * 4 + q64])
                for q32 in range(4):
                    third = int(cells32[second * 4 + q32])
                    for q16 in range(4):
                        fourth = int(cells16[third * 4 + q16])
                        for q8 in range(4):
                            block = int(cells8[fourth * 4 + q8])
                            bx = gx * 16 + (q64 & 1) * 8 + (q32 & 1) * 4 + (q16 & 1) * 2 + (q8 & 1)
                            by = gy * 16 + (q64 >> 1) * 8 + (q32 >> 1) * 4 + (q16 >> 1) * 2 + (q8 >> 1)
                            out[by * 8:by * 8 + 8, bx * 8:bx * 8 + 8] = pixels[block * 64:block * 64 + 64].reshape(8, 8)
    return out


def places_of(layer, slot):
    """The places a slot has on the trigger layer: the boxes (left, top, right, bottom, in pixels) of the pixels that hold
    its number and touch. A cell is not always whole 8 x 8 blocks (a slope's edge), so it is read by the pixel."""
    ys, xs = np.nonzero((layer != 0) & ((layer & 31) == slot))
    left = set(zip(ys.tolist(), xs.tolist()))
    boxes = []
    while left:
        start = left.pop()
        queue, found = deque([start]), [start]
        while queue:
            y, x = queue.popleft()
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    near = (y + dy, x + dx)
                    if near in left:
                        left.remove(near)
                        queue.append(near)
                        found.append(near)
        fy, fx = zip(*found)
        boxes.append((min(fx), min(fy), max(fx) + 1, max(fy) + 1))
    return boxes


def overlap(a, b):
    grown = (a[0] - MERGE_MARGIN, a[1] - MERGE_MARGIN, a[2] + MERGE_MARGIN, a[3] + MERGE_MARGIN)
    return grown[0] < b[2] and b[0] < grown[2] and grown[1] < b[3] and b[1] < grown[3]


def merge(places):
    """Merges the places (box, gated, kind) that lead to the same map and overlap."""
    places = list(places)
    merged = True
    while merged:
        merged = False
        for i in range(len(places)):
            for j in range(i + 1, len(places)):
                if overlap(places[i][0], places[j][0]):
                    a, b = places[i], places[j]
                    box = (min(a[0][0], b[0][0]), min(a[0][1], b[0][1]), max(a[0][2], b[0][2]), max(a[0][3], b[0][3]))
                    places[i] = (box, a[1] and b[1], a[2] if a[2] == b[2] else "exit")
                    del places[j]
                    merged = True
                    break
            if merged:
                break
    return places


def try_number(token):
    try:
        return number(token)
    except ValueError:
        return 0  # a name the stage defines: not a destination


def slot_tables(text):
    """The names of the slot tables of a stage: `stageSlots`, or `stageSlots0`, `stageSlots1`... when the stage picks one by
    the story (the same trigger layer serves them all)."""
    return re.findall(r"StageSlot (stageSlots\d*)\[\] = \{", text)


def parse_slot_table(text, table):
    """Slots of a table as [{gated, type, kind, arg}] in order, up to the empty record, with the type as a number
    (the stages write it as SLOT_EXIT and the like, or as 0 for the end)."""
    start = text.index(f"StageSlot {table}[] = {{")
    block = text[start:text.index("\n};", start)]
    slots = []
    for conditions, kind, rest in re.findall(r"\{ \{ (\{ [^}]*\}, \{ [^}]*\}) \}, (\w+), ([^}]*)\},", block):
        values = [try_number(v) for v in rest.split(",")]
        slots.append({
            "gated": any(code != "CODES_END" for code, _value in re.findall(r"\{ (\w+), (\w+) \}", conditions)),
            "type": {"SLOT_EXIT": 1, "SLOT_WARP1": 9, "SLOT_WARP0": 10, "SLOT_LAUNCH_OUT": 14}.get(kind, 0 if kind == "0" else -1),
            "kind": kind, "arg": values[0],
        })
    return slots


def stage_name(decomp, stage):
    found = list((decomp / "src/field/stages").glob(f"**/wstag{stage}.c"))
    if not found:
        return None
    text = found[0].read_text(encoding="utf-8")
    comment = text[text.index("/*") + 2:text.index("*/")]
    comment = " ".join(line.strip().lstrip("*").strip() for line in comment.splitlines()).strip()
    match = re.match(rf"WSTAG{stage}:\s*(.+)", comment)
    return None if match is None else re.split(r"[,:.](?:\s|$)", match.group(1))[0].strip()


def server_copy_ids():
    return set(re.findall(r'"([0-9A-F]{4})": \{ image: ', SERVER_COPIES.read_text(encoding="utf-8")))


def named_maps():
    """The ids of the maps the app names with a `map.<id>` message of the game's text."""
    keys = json.loads(TEXT_KEYS.read_text(encoding="utf-8"))["messages"]
    return {entry["path"][1] for entry in keys if entry["path"][0] == "map"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("decomp", type=Path)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()
    for name in ("DW3_ISO", "DW3_DDW3"):
        if name not in os.environ:
            sys.exit(f"{name} is not set; see the header")

    disc = Disc()
    stages = european_stages(args.decomp)
    copies = server_copy_ids()
    map_messages = named_maps()
    place_words = stname_words()
    extra_words = {}

    def name_source(mode):
        """Where the app gets the name of a destination from, adding its place word when only the stage knows it."""
        key = f"{mode:04X}"
        if key in copies:
            return "server copy"
        if ("02EC" if CIRCUIT_FIRST <= mode <= CIRCUIT_LAST else key) in map_messages:
            return "map message"
        word = place_words.get(stage_name(args.decomp, stages[mode])) if mode in stages else None
        if word is None:
            return "none"
        extra_words[key] = word
        return "stage comment"

    result, report = {}, {"slots": 0, "kinds": {}, "gated": 0, "unnamed": [], "no_trigger": [], "self": 0, "no_cells": [],
                          "sources": {}, "stages": 0, "placeholder": 0}
    for mode in sorted(stages):
        text = stage_text(args.decomp, stages[mode])
        tables = [parse_slot_table(text, name) for name in slot_tables(text)]
        # A stage with several tables opens different things with the story: what only some of them have is gated.
        variants = {}
        for table in tables:
            for i, s in enumerate(table):
                if s["type"] in LEAVING:
                    variants.setdefault((i, s["arg"], s["type"], s["kind"]), []).append(s["gated"])
        leaving = [
            (i, {"arg": arg, "type": kind_number, "kind": kind, "gated": any(flags) or len(flags) < len(tables)})
            for (i, arg, kind_number, kind), flags in sorted(variants.items(), key=lambda item: item[0][:3])
        ]
        if not leaving:
            continue
        report["stages"] += 1
        file_number = trigger_file(text)
        data = None if file_number is None else disc.file(file_number >> 16)
        if data is None:
            report["no_trigger"].append(f"{mode:04X}")
            continue
        entries = archive_entries(data)
        if (file_number & 0xFFFF) >= len(entries) or not entries[file_number & 0xFFFF]:
            report["no_trigger"].append(f"{mode:04X} (file {file_number:#x}: {len(entries)} entries)")
            continue
        layer = read_map_file(entries[file_number & 0xFFFF])
        by_destination = {}
        for index, slot in leaving:
            report["slots"] += 1
            report["kinds"][slot["kind"]] = report["kinds"].get(slot["kind"], 0) + 1
            destination = slot["arg"]
            if destination == 0:
                # Filled at run time from the place points of the stage (the Seabed's tunnels, the desert).
                report["placeholder"] += 1
                continue
            if destination == mode:
                report["self"] += 1
                continue
            if slot["gated"]:
                report["gated"] += 1
            boxes = places_of(layer, index)
            if not boxes:
                report["no_cells"].append(f"{mode:04X} slot {index}")
            for box in boxes:
                by_destination.setdefault(destination, []).append((box, slot["gated"], LEAVING[slot["type"]]))

        exits = []
        for destination, places in by_destination.items():
            source = name_source(destination)
            report["sources"][source] = report["sources"].get(source, 0) + 1
            if source == "none":
                report["unnamed"].append(f"{mode:04X} -> {destination:04X}")
                continue
            for box, gated, kind in merge(places):
                exits.append({
                    "x": (box[0] + box[2]) // 2, "y": (box[1] + box[3]) // 2, "to": f"{destination:04X}",
                    "gated": gated, "kind": kind,
                })
        if exits:
            result[f"{mode:04X}"] = sorted(exits, key=lambda e: (e["y"], e["x"]))

    lines = [
        "// Generated by Tools/FieldData/generate_exits.py: do not edit.",
        "// Where the player leaves each map for another (a door, a bridge end, a dock), as a point of its picture in pixels, and",
        "// the destination (an exit is named as its destination map is, see `mapName`). From the matching decompilation of the",
        "// European version, ReGame-Labs/dw3_decomp (MIT): the slots of each stage, and the trigger layer of its map on the",
        "// disc. `gated` is a slot with a story condition.",
        "",
        "export interface MapExit {",
        "  x: number;",
        "  y: number;",
        "  /** Id of the map it leads to. */",
        "  to: string;",
        "  /** Only opens under a story condition. */",
        "  gated: boolean;",
        '  /** A door or edge of the map, a warp (a dock, a lift) or a launch. */',
        '  kind: "exit" | "warp" | "launch";',
        "}",
        "",
        "export const MapExits: Readonly<Record<string, readonly MapExit[]>> = {",
    ]
    for key, exits in result.items():
        lines.append(f'  "{key}": [')
        for e in exits:
            lines.append(
                f'    {{ x: {e["x"]}, y: {e["y"]}, to: "{e["to"]}", '
                f'gated: {"true" if e["gated"] else "false"}, kind: "{e["kind"]}" }},')
        lines.append("  ],")
    lines += [
        "};",
        "",
        "/**",
        " * The destinations whose name the map messages do not have, and the reference of their name in the game's place names",
        " * (for `gameWord`), by map id.",
        " */",
        "export const PlaceWords: Readonly<Record<string, string>> = {",
    ]
    for key, word in sorted(extra_words.items()):
        lines.append(f"  {json.dumps(key)}: {json.dumps(word)},")
    lines.append("};")
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")

    total = sum(len(v) for v in result.values())
    print(f"{args.out}: {total} exits on {len(result)} maps", file=sys.stderr)
    for key, value in report.items():
        print(f"  {key}: {value}", file=sys.stderr)


if __name__ == "__main__":
    main()
