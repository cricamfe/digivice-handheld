#!/usr/bin/env python3
"""Generate numeric EU stage setup for the Kotlin extractor; see Tools/RomExtract/README.md."""
import gzip
import io
import json
import os
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "Tools/RomExtract/render"))
import stagedef

FIELDS = ("mapFile", "sheetEntry", "imageFile", "imageEntry", "floor0", "color", "objects")

# No character has an alias named after it: the app asks for the character of a tamer, an NPC, a boss, a card shop or
# Duel Island as `actors/<character>-<direction>` (the `fieldActor` of the app's tables), so the release ships no name.


def characters():
    source = Path(os.environ["DW3_DECOMP"], "src/field/field_mode/data/fieldstg.c").read_text()
    source = source.split("#elif VERSION_EU\ns32 FIELDSTG_fileEntries[]", 1)[1].split("};", 1)[0]
    entries = [int(token, 16) for token in re.findall(r"0x[0-9A-Fa-f]+|\b0\b", source.split("{", 1)[1])]
    frames = (ROOT / "Frontend/src/handheld/field-actors.ts").read_text().split("export const ActorFrames", 1)[1].split("};", 1)[0]
    ids = [int(token) for token in re.findall(r"^  (\d+):", frames, re.M)]
    actors = {key: [entries[key] >> 16, entries[key] & 0xFFFF] for key in ids}
    item_source = (ROOT / "Frontend/src/handheld/item-icons.ts").read_text()
    items = sorted({int(token, 16) for token in re.findall(r"item-([0-9a-f]{2})", item_source)})
    if len(actors) != 309 or len(items) != 24:
        raise ValueError("Expected 309 actors and 24 item frames")
    return {"actors": actors, "items": items}


def numeric(value):
    if value is None or type(value) is int:
        return True
    if isinstance(value, list):
        return all(numeric(v) for v in value)
    if isinstance(value, dict):
        return all(numeric(v) for v in value.values())
    return False


def main():
    paths = Path(os.environ["DW3_DECOMP"], "src/field/stages").rglob("wstag*.c")
    modes = sorted({int(m.group(1)) for p in paths if (m := re.fullmatch(r"wstag(\d+)\.c", p.name))})
    stages = {}
    for mode in modes:
        info = stagedef.load_stage(mode)
        if info is None:
            raise ValueError(f"Cannot read stage {mode}")
        stages[mode] = {key: info.get(key) for key in FIELDS}
    if len(stages) != 294 or not numeric(stages):
        raise ValueError("Expected 294 numeric EU stages, without text values")
    payload = json.dumps(stages, separators=(",", ":"), sort_keys=True).encode("utf-8")
    output = io.BytesIO()
    with gzip.GzipFile(fileobj=output, mode="wb", filename="", mtime=0, compresslevel=9) as archive:
        archive.write(payload)
    target = ROOT / "Android/extractor/src/main/resources/extractor/stages.json.gz"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(output.getvalue())
    print(f"{len(stages)} stages, {len(payload)} JSON bytes, {target.stat().st_size} gzip bytes")
    target.with_name("characters.json").write_text(json.dumps(characters(), sort_keys=True, separators=(",", ":")) + "\n")


if __name__ == "__main__":
    main()
