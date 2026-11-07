"""Reads the facts the Seabed and Mobius Desert data need from the matching decompilation of the European version,
ReGame-Labs/dw3_decomp (MIT). The decompilation is data: nothing in it is run, it is only read as text.

What it reads:
  - src/field/field_mode/data/fieldstg.c: FIELDSTG_stages (mode -> WSTAG number, European list), FIELDSTG_enemies and the
    European FIELDSTG_encounters (an encounter is up to three enemies, each {fighter, level, hp, mp, strength}).
  - src/field/stages/**/wstagNNN.c: the place points (where each exit leads, by place and place argument), the slots
    (exits and warps), the battles of each place (BattleList of AreaBattle {encounter, stage, music}) and the actors.
"""
import json
import os
import re
import tomllib
from pathlib import Path

TEXT_KEYS = Path(__file__).resolve().parents[2] / "Android/extractor/src/main/resources/extractor/text-keys.json"
# The file name (in Frontend/src/assets/maps, the original project's pictures) of each map of map.json, by map id. Only the
# tools read it: the app finds a map's picture by its id.
MAP_PICTURES = Path(__file__).resolve().parent / "map-pictures.json"


def digimon_name(raw):
    """The line of the game's Digimon names (`dignam:212`) of a row of enemy.json, digievolution.json or digimon.json,
    with its variant, for reports: the tables keep no names."""
    name = f"dignam:{raw['nameId']}"
    return f"{name}({raw['variant']})" if "variant" in raw else name


def map_picture(location):
    """The name of the picture of a map of map.json (`0202`) in Frontend/src/assets/maps, without extension, or None."""
    return json.loads(MAP_PICTURES.read_text(encoding="utf-8")).get(location)


def preprocess(text, version="EU"):
    """Keeps the lines of the chosen version: handles #if/#elif/#else/#endif over VERSION_US, VERSION_JP and VERSION_EU."""
    defined = {f"VERSION_{name}": int(name == version) for name in ("US", "JP", "EU")}

    def evaluate(expression):
        expression = re.sub(r"\bVERSION_\w+", lambda m: str(defined.get(m.group(0), 0)), expression)
        expression = expression.replace("||", " or ").replace("&&", " and ").replace("!", " not ")
        return bool(eval(expression, {"__builtins__": {}}))  # only digits and and/or/not, built above

    out = []
    stack = []  # [parent active, taken, active]
    for line in text.split("\n"):
        stripped = line.strip()
        if stripped.startswith("#if "):
            parent = all(frame[2] for frame in stack)
            value = evaluate(stripped[4:])
            stack.append([parent, value, parent and value])
        elif stripped.startswith("#elif "):
            frame = stack[-1]
            value = not frame[1] and evaluate(stripped[6:])
            frame[1] = frame[1] or value
            frame[2] = frame[0] and value
        elif stripped == "#else":
            frame = stack[-1]
            frame[2] = frame[0] and not frame[1]
            frame[1] = True
        elif stripped == "#endif":
            stack.pop()
        elif stripped.startswith("#if") and not stripped.startswith("#include"):
            raise ValueError(f"unsupported conditional: {stripped}")
        elif all(frame[2] for frame in stack):
            out.append(line)
    return "\n".join(out)


def number(token):
    token = token.strip()
    return int(token, 16) if token.lower().startswith("0x") else int(token)


def european_stages(decomp):
    """{mode: stage number} from the European FIELDSTG_stages."""
    text = (decomp / "src/field/field_mode/data/fieldstg.c").read_text(encoding="utf-8")
    start = text.index("#elif VERSION_EU\nStageEntry FIELDSTG_stages[]")
    table = text[start:text.index("};", start)]
    return {int(mode): int(n) for mode, _file, n in re.findall(r"\{(\d+), (\d+), WSTAG(\d+)_startStage\}", table)}


def stage_text(decomp, stage):
    found = list((decomp / "src/field/stages").glob(f"**/wstag{stage}.c"))
    if not found:
        raise FileNotFoundError(f"wstag{stage}.c")
    return preprocess(found[0].read_text(encoding="utf-8"))


def enemies_and_encounters(decomp):
    """(enemies, encounters): enemies[i] = (fighter, level); encounters[i] = list of enemy indices (0 = none dropped)."""
    text = preprocess((decomp / "src/field/field_mode/data/fieldstg.c").read_text(encoding="utf-8"))
    start = text.index("BattleEnemy FIELDSTG_enemies[] = {")
    block = text[start:text.index("};", start)]
    enemies = [(int(f), int(level)) for f, level, _hp, _mp, _str in
               re.findall(r"\{(-?\d+), (\d+), (\d+), (\d+), (\d+)\}", block)]
    start = text.index("Encounter FIELDSTG_encounters[] = {")
    block = text[start:text.index("\n};", start)]
    encounters = []
    for a, b, c in re.findall(r"\{\{&FIELDSTG_enemies\[(\d+)\], &FIELDSTG_enemies\[(\d+)\], &FIELDSTG_enemies\[(\d+)\]\}", block):
        encounters.append([int(i) for i in (a, b, c) if int(i) != 0])
    return enemies, encounters


def parse_points(text):
    """{(place, arg): [(mode, place, arg, x, y, dir), ...]}: the points a stage copies to its slots, in slot order."""
    points = {}
    for name, a, b, c, x, y, d, nxt in re.findall(
            r"StagePoint (\w+) = \{ (\w+), (\w+), (\w+), (\w+), (\w+), (\w+), (&\w+|NULL) \};", text):
        points[name] = (tuple(number(v) for v in (a, b, c, x, y, d)), None if nxt == "NULL" else nxt[1:])
    table = {}
    for _name, place, arg, first in re.findall(r"StagePoints (\w+) = \{ (\w+), (\w+), &(\w+) \};", text):
        chain, current = [], first
        while current is not None:
            values, current = points[current]
            chain.append(values)
        table[(number(place), number(arg))] = chain
    return table


def parse_slots(text):
    """Slots of stageSlots: [{conditions, type, arg, x, y, dir, place, placeArg}] up to the empty record."""
    start = text.index("StageSlot stageSlots[] = {")
    block = text[start:text.index("\n};", start)]
    slots = []
    for conditions, kind, rest in re.findall(r"\{ \{ (\{ [^}]*\}, \{ [^}]*\}) \}, (\w+), ([^}]*)\},", block):
        values = [number(v) for v in rest.split(",")]
        slots.append({
            "conditions": re.findall(r"\{ (\w+), (\w+) \}", conditions),
            "type": kind, "arg": values[0], "x": values[1], "y": values[2], "dir": values[3],
            "place": values[5], "placeArg": values[6],
        })
    return slots


def parse_battles(text):
    """{place: [[encounter, ...] for each of the four areas]}: the encounters of the eight battles of each area."""
    battles = {name: number(enc) for name, enc in re.findall(r"AreaBattle (\w+) = \{ (\w+), \w+, [^}]*\};", text)}
    lists = {}
    for name, count, members in re.findall(r"BattleList (\w+) = \{\s*(\w+),\s*\{([^}]*)\},?\s*\};", text):
        lists[name] = (number(count), [battles[m] for m in re.findall(r"&(\w+)", members)])
    start = text.index("FieldBattles stageBattles[] = {")
    block = text[start:text.index("\n};", start)]
    places = {}
    for _serial, place, areas in re.findall(r"\{ (\w+), (\w+), 0, \{([^}]*)\} \}", block):
        places[number(place)] = [lists[n] for n in re.findall(r"&(\w+)", areas)]
    return places


def parse_actors(text):
    """[(conditions, id, x, y, dir)] of the stage's actors; conditions as the list of (code, value) pairs."""
    conditions = {}
    for name, body in re.findall(r"u16 (\w+Conditions)\[\] = \{(.*?)\};", text, re.S):
        conditions[name] = re.findall(r"(\w+(?:\([^)]*\))?), (\w+)", body.replace("\n", " "))
    actors = []
    for cond, ident, x, y, d in re.findall(
            r"FieldActorEntry actor\d+ = \{ (\w+), \w+, (\w+), \w+, (\w+), (\w+), (\w+) \};", text):
        actors.append((conditions.get(cond, []), number(ident), number(x), number(y), number(d)))
    return actors


def stname_words(ddw3=None):
    """{English place name: reference} for the place names of the game (`stname:12`, a line of the stname table on the
    disc), so the app can store the reference and read the name from the user's disc. The English table is the one in
    ddw3 (`lang_file/dw2003/eng`; DW3_DDW3 when no path is given). Only references the extractor knows (the `words` of
    text-keys.json) are returned, the first line of a name that repeats being the one it uses."""
    ddw3 = Path(ddw3 if ddw3 is not None else os.environ["DW3_DDW3"])
    lines = tomllib.loads((ddw3 / "lang_file/dw2003/eng/esstname.toml").read_text(encoding="utf-8"))["strings"]
    known = {word["key"] for word in json.loads(TEXT_KEYS.read_text(encoding="utf-8"))["words"]}
    words = {}
    for index, line in enumerate(lines):
        if f"stname:{index}" in known:
            words.setdefault(line.strip(), f"stname:{index}")
    return words


def talk_speakers(references, ddw3=None):
    """{reference: English name} of talk lines (`talk04:879`, as `ActorNames` of field-actors.ts stores them): the
    speaker of the line in the English tables of ddw3 (DW3_DDW3 when no path is given), as the extractor writes it."""
    ddw3 = Path(ddw3 if ddw3 is not None else os.environ["DW3_DDW3"])
    tables = {}
    names = {}
    for reference in references:
        table, line = reference.split(":")
        if table not in tables:
            path = ddw3 / f"lang_file/dw2003/eng/es{table}.toml"
            tables[table] = tomllib.loads(path.read_text(encoding="utf-8"))["strings"]
        match = re.match(r"\[name\](.*?)\[name\]", tables[table][int(line)])
        if match is None:
            raise ValueError(f"{reference} names no speaker")
        name = re.sub(r"\s+", " ", match.group(1).replace("\\n", " ")).strip().rstrip(".")
        names[reference] = name[:1].upper() + name[1:]
    return names
