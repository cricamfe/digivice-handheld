#!/usr/bin/env python3
"""Generates Backend/Memory/Definitions/quest-conditions.json: for each quest of the journal that is not the main one,
the game's own conditions for starting it, so that the app can evaluate them on live RAM.

Each quest has `ways`, and it can be started if any way holds:
    { "map": "0226", "character": 59, "conditions": [[code, value], ...], "intro": [[code, value], ...] }
  - `map` is the mode of the stage, `character` the FieldActorEntry.id (null for a trigger or a stage's own event).
  - `conditions` and `intro` are (code, value) pairs as checkCondition takes them (src/engine/game/events.c).
  - `conditions` hold: the entry's conditions, those of the talk or slot that sets the quest's flag, the negation of an
    earlier talk of runTalk's order (src/field/field_mode/actor_control.c) that would answer instead and cannot lead
    onward, and which chapter loads the stage: FIELDSTG_pickStage (src/field/field_mode/state.c) picks
    FIELDSTG_extraStages at STORY_EXTRA, so its stages need that progress and the stages they replace need another.
  - `intro` are the conditions that talking to that same actor first makes true, up the chain of talks. They are met by
    talking, so they don't block the quest and are left out of `conditions`.
  - `place` (only when there is one) are the PLACE and PLACE_ARG pairs of the way, and any flag of FLAGS_00 (group 0,
    cleared when the mode changes) that no talk of the actor sets: true only while the player stands there.
Nothing about whether the player can walk there is here: this table holds what the scripts say.

What is by hand is CURATED below: which stage, actor and talk (or slot, or stage event) sets each quest's flag. The rest
is read: the flag from the quest's first step in Backend/Memory/Definitions/Quests, turned into its code with GameState's
layout and config/eu/symbols.txt; the lists, the macros and the names from the sources of ReGame-Labs/dw3_decomp (MIT).
The script stops if a curated talk doesn't set the flag, and if any stage sets one of these flags where CURATED doesn't
say. Nothing of the decompilation is run; it is only read as text.

Usage: python3 Tools/FieldData/generate_quest_conditions.py <path to dw3_decomp> [--out <file>] [--report]
  --report prints to stdout each way with its names and source lines, and the table of the SPECIAL ids it uses.
It needs no copy of the disc.
"""
import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from stage_data import number, preprocess  # noqa: E402

DEFAULT_OUT = ROOT / "Backend/Memory/Definitions/quest-conditions.json"
QUESTS = ROOT / "Backend/Memory/Definitions/Quests"
RAM_BASE = 0x80000000

GROUP_FLAGS_00, GROUP_PLACE, GROUP_SPECIAL = 0x00, 0x7E, 0x70
GROUPS_CARD_BATTLE = (0x76, 0x78)
GROUP_START_EVENT = 0x90  # START_EVENT(index), an index of FIELDSTG_eventIds
SLOT_EVENT_NAME = "SLOT_EVENT"


@dataclass(frozen=True)
class Talk:
    """Talk `talk` of each of `actors` sets the flag; through the end of `event`, when given."""
    stage: int
    actors: tuple
    talk: int
    event: int | None = None


@dataclass(frozen=True)
class Slot:
    """Trigger `slot` of the stage starts `event`, whose end sets the flag."""
    stage: int
    slot: int
    event: int


@dataclass(frozen=True)
class StageEvent:
    """The stage starts `event` by itself when it loads (updateStage), and its end sets the flag."""
    stage: int
    event: int


def actors(first, last):
    return tuple(range(first, last + 1))


CURATED = {
    "folderBag": [Talk(270, (31,), 0, event=1415), Talk(934, (1,), 1)],
    "fishingPole": [Talk(340, (2, 3), 2)],
    "treeBoots": [Slot(345, 14, 1264)],
    "digimonOnline": [StageEvent(925, 1600)],
    "eternally": [Talk(375, actors(6, 16), 0), Talk(972, (2,), 0)],
    "invincible": [Talk(405, (4,), 1)],
    "muramasa": [Talk(535, actors(0, 9), 0), Talk(537, actors(0, 13), 0), Talk(972, (1,), 0)],
    "punishment": [Talk(605, (8,), 1)],
    "superNova": [Talk(325, (1,), 0)],
    "driAgentAgumon": [Talk(405, (0, 2), 3)],
    "driAgentGuilmon": [Talk(420, (0, 2), 3)],
    "driAgentPatamon": [Talk(330, (3, 5), 3)],
    "driAgentRenamon": [Talk(330, (7, 9), 3)],
    "driAgentKotemon": [Talk(635, (0,), 3)],
    "driAgentKumamon": [Talk(635, (2,), 3)],
    "driAgentMonmon": [Talk(635, (7,), 3)],
    "driAgentVeemon": [Talk(375, (3, 5), 0), Talk(970, (3,), 2)],
    "asukaTrophy": [Talk(385, (20,), 3)],
    "asukaRematch": [Talk(385, (21,), 3)],
    "amaterasuTrophy": [Talk(386, (19,), 3)],
    "amaterasuRematch": [Talk(386, (20,), 3)],
    "cardKing": [Talk(951, (12,), 4)],
}

CODE_TOKEN = r"(?:\w+\([^)]*\)|\w+)"


def strip_comments(text):
    return re.sub(r"/\*.*?\*/", "", text, flags=re.S)


def header_lines(path, keep_comments=False):
    """A header for the European version, without its include guard, which preprocess doesn't take."""
    text = path.read_text(encoding="utf-8")
    text = re.sub(r"/\*(?! 0x\w+ \*/).*?\*/", "", text, flags=re.S) if keep_comments else strip_comments(text)
    text = re.sub(r"^#ifndef \w+\n", "", text, count=1, flags=re.M)
    last = text.rindex("#endif")
    return preprocess(text[:last] + text[last + len("#endif"):])


class Names:
    """The numeric value of a token of the sources: its macros and names come from the headers."""

    def __init__(self, decomp):
        self.macros, self.names = {}, {}
        for header in ("include/engine/game_state.h", "include/engine/story.h", "include/field/field_map.h"):
            text = header_lines(decomp / header)
            for name, params, body in re.findall(r"^#define (\w+)\(([^)]*)\) (.+)$", text, re.M):
                self.macros[name] = ([p.strip() for p in params.split(",")], body.strip())
            for name, body in re.findall(r"^#define (\w+) (\S.*)$", text, re.M):
                self.names[name] = body.strip()
            for name, value in re.findall(r"^\s*(STORY_\w+) = (0x\w+),", text, re.M):
                self.names[name] = value

    def value(self, token):
        token = token.strip()
        if re.fullmatch(r"0[xX][0-9a-fA-F]+|\d+", token):
            return number(token)
        if token in self.names:
            return self.value(self.names[token])
        call = re.fullmatch(r"(\w+)\((.*)\)", token)
        if not call or call.group(1) not in self.macros:
            raise KeyError(f"cannot resolve {token!r}")
        params, body = self.macros[call.group(1)]
        arguments = [self.value(a) for a in call.group(2).split(",")]
        # The macros' bodies are integer expressions of the headers, the same in Python.
        return eval(body, {"__builtins__": {}}, dict(zip(params, arguments)))


class Layout:
    """Where GameState's flag groups and items are in RAM, in the European version."""

    def __init__(self, decomp, names):
        self.names = names
        symbols = (decomp / "config/eu/symbols.txt").read_text(encoding="utf-8")
        self.game = int(re.search(r"^GAME = 0x(\w+);", symbols, re.M).group(1), 16) - RAM_BASE
        text = header_lines(decomp / "include/engine/game_state.h", keep_comments=True)
        end = text.index("} GameState;")
        state = text[text.rindex("typedef struct", 0, end):end]
        fields = re.findall(r"/\* (0x\w+) \*/ \w+ (\w+)\[(0x\w+|\d+)\];", state)
        self.fields = {name: (int(offset, 16), number(size)) for offset, name, size in fields}

    def code_of(self, address, masks):
        """The condition code a step's address and bit mask read: a flag of a group, or an item's count."""
        for field, (offset, size) in self.fields.items():
            start = self.game + offset
            if not start <= address < start + size:
                continue
            if field == "items":
                return self.names.value(f"ITEM(0, {address - start})")
            group = re.fullmatch(r"flags([0-9A-F]{2})", field)
            if group and len(masks) == 1:
                bit = number(masks[0]).bit_length() - 1
                return self.names.value(f"FLAG(0x{group.group(1)}, {(address - start) * 8 + bit})")
        raise ValueError(f"no flag or item at {address:#x} {masks}")


def first_step_codes(layout):
    """{quest id: [codes]}: the flags its first step's address, and its alternatives, read."""
    found = {}
    for path in QUESTS.glob("*/*.json"):
        quest = json.loads(path.read_text(encoding="utf-8"))
        step = next(s for s in quest["Steps"] if s["Number"] == 1)
        marks = [step] + step.get("Alternatives", [])
        found[quest["Id"]] = [layout.code_of(number(m["Address"]), m["BitMasks"]) for m in marks]
    return found


def extra_chapter_stages(decomp):
    """{mode: stage} of the European FIELDSTG_extraStages and FIELDSTG_stages, and its FIELDSTG_eventIds."""
    text = preprocess((decomp / "src/field/field_mode/data/fieldstg.c").read_text(encoding="utf-8"))
    entry = r"\{(\d+), \d+, WSTAG(\d+)_startStage\}"
    start = text.index("StageEntry FIELDSTG_extraStages[]")
    extra = {int(m): int(s) for m, s in re.findall(entry, text[start:text.index("};", start)])}
    start = text.index("StageEntry FIELDSTG_stages[]")
    main = {int(m): int(s) for m, s in re.findall(entry, text[start:text.index("};", start)])}
    start = text.index("s16 FIELDSTG_eventIds[]")
    body = strip_comments(text[text.index("{", start) + 1:text.index("};", start)])
    event_ids = [number(v) for v in re.findall(r"-?0x\w+|-?\d+", body)]
    return extra, main, event_ids


class Stage:
    """The lists of a stage's source, as tokens and as numbers."""

    def __init__(self, decomp, number_, names):
        self.path = next((decomp / "src/field/stages").glob(f"**/wstag{number_}.c"))
        self.raw = self.path.read_text(encoding="utf-8")
        self.text = preprocess(self.raw)
        self.number, self.names = number_, names
        self.lists = {}
        for name, body in re.findall(r"u16 (\w+(?:Conditions|Actions))\[\] = \{(.*?)\};", self.text, re.S):
            self.lists[name] = [(t, names.value(t), number(v)) for t, v in re.findall(rf"({CODE_TOKEN}),\s*(\w+)", body)]
        self.talks = {}
        for name, body in re.findall(r"FieldTalk (\w+)\[\] = \{(.*?)\n\};", self.text, re.S):
            self.talks[name] = re.findall(r"\{ (\w+), (\w+), (\w+) \}", body)
        self.actors = {}
        for index, cond, talks, ident in re.findall(r"FieldActorEntry actor(\d+) = \{ (\w+), (\w+), (\w+),", self.text):
            self.actors[int(index)] = (cond, talks, number(ident))

    def pairs(self, name):
        return [] if name == "NULL" else self.lists[name]

    def actor_talks(self, index):
        return self.talks[self.actors[index][1]][:-1]  # The last is the empty record that ends the list.

    def line_of(self, pattern):
        match = re.search(pattern, self.raw)
        return self.raw[:match.start()].count("\n") + 1 if match else 0

    def end_function_body(self, event):
        """The body of the function that the event's table entry runs at its end."""
        entry = re.search(rf"\{{ {event}, \w+, [^,]+, \w+, (\w+) \}}", self.text)
        function = re.search(rf"void {entry.group(1)}\(void\) \{{(.*?)\n\}}", self.text, re.S)
        return function.group(1)

    def event_sets(self, event, code):
        body = self.end_function_body(event)
        applied = re.findall(rf"FLAGS_00\.applyAction\(({CODE_TOKEN}), (\w+)\)", body)
        return any(self.names.value(t) == code and number(v) != 0 for t, v in applied)

    def slot(self, index):
        block = self.text[self.text.index("StageSlot stageSlots[]"):]
        rows = re.findall(r"\{ \{ (\{ [^}]*\}, \{ [^}]*\}) \}, (\w+), (\w+),", block[:block.index("\n};")])
        conditions, kind, arg = rows[index]
        pairs = re.findall(rf"\{{ ({CODE_TOKEN}), (\w+) \}}", conditions)
        return [(t, self.names.value(t), number(v)) for t, v in pairs if t != "CODES_END"], kind, number(arg)


def group_of(code):
    return (code >> 8) & 0xFE


def negated(pair):
    token, code, value = pair
    return (token, code, 0 if value else 1)


class Builder:
    def __init__(self, decomp, names):
        self.decomp, self.names = decomp, names
        self.stages = {}
        self.extra, self.main, self.event_ids = extra_chapter_stages(decomp)
        self.battle_ended = names.value("FLAG_CARD_BATTLE_ENDED")
        self.battle_won = names.value("FLAG_CARD_BATTLE_WON")
        self.problems = []
        self.covered = set()

    def stage(self, number_):
        if number_ not in self.stages:
            self.stages[number_] = Stage(self.decomp, number_, self.names)
        return self.stages[number_]

    def chapter(self, number_):
        """(mode, pair): the stage's mode, and the pair that says which chapter loads it (FIELDSTG_pickStage)."""
        progress = "PROGRESS(STORY_EXTRA)"
        code = self.names.value(progress)
        for mode, stage in self.extra.items():
            if stage == number_:
                return mode, (progress, code, 1)
        mode = next(m for m, s in self.main.items() if s == number_)
        return mode, (progress, code, 0) if mode in self.extra else None

    def provides(self, stage, talk):
        """What a talk makes true for the next one: its actions, and a card battle's result (updateModeFlags)."""
        given = {(c, 1 if v else 0) for _t, c, v in stage.pairs(talk[1])}
        if any(group_of(c) in GROUPS_CARD_BATTLE for c, _v in given):
            given |= {(self.battle_ended, 1), (self.battle_won, 1)}
        return given

    def talk_way(self, stage, index, target_talk):
        """(conditions, intro) of an actor's talk, as named pairs (token, code, value)."""
        talks = stage.actor_talks(index)
        cond_name, _talks, _ident = stage.actors[index]
        conditions = list(stage.pairs(cond_name))
        intro, chain, seen = [], set(), set()

        def walk(position):
            """Splits a talk's conditions: those an earlier talk gives go to the intro, and that talk is walked too."""
            chain.add(position)
            for pair in stage.pairs(talks[position][0]):
                providers = [p for p in range(position) if (pair[1], pair[2]) in self.provides(stage, talks[p])]
                if providers:
                    if (pair[1], pair[2]) not in seen:
                        seen.add((pair[1], pair[2]))
                        intro.append(pair)
                    for provider in providers[-1:]:
                        if provider not in chain:
                            walk(provider)
                else:
                    required.append(pair)

        required = []
        walk(target_talk)
        given_codes = {c: v for _t, c, v in intro}
        conditions += [p for p in required if given_codes.get(p[1], p[2]) == p[2]]

        for position in range(target_talk):
            if position in chain:
                continue
            pairs = stage.pairs(talks[position][0])
            if not pairs:
                self.problems.append(f"wstag{stage.number} actor{index}: talk {position} always answers first")
                continue
            everything = conditions + intro
            known = {(c, v) for _t, c, v in everything}
            if any((c, 0 if v else 1) in known for _t, c, v in pairs):
                continue  # It can't hold together with this way.
            own = {(c, 1 if v else 0) for _t, c, v in pairs}
            if any((c, 0 if v else 1) in own for c, v in self.provides(stage, talks[position])):
                continue  # It leads onward: it stops holding once it has answered.
            open_pairs = [p for p in pairs if (p[1], p[2]) not in known]
            if len(open_pairs) != 1:
                self.problems.append(f"wstag{stage.number} actor{index}: talk {position} answers first unless one of "
                                     f"{[p[0] for p in open_pairs]} fails, which one list of pairs cannot say")
                continue
            conditions.append(negated(open_pairs[0]))
        return conditions, intro

    def assemble(self, number_, character, conditions, intro, source):
        mode, chapter = self.chapter(number_)
        pairs = ([chapter] if chapter else []) + conditions
        kept, place, seen = [], [], set()
        for pair in pairs:
            if (pair[1], pair[2]) in seen:
                continue
            seen.add((pair[1], pair[2]))
            if group_of(pair[1]) in (GROUP_PLACE, GROUP_FLAGS_00):
                place.append(pair)
            else:
                kept.append(pair)
        contradiction = [p for p in kept if (p[1], 0 if p[2] else 1) in seen]
        if contradiction:
            self.problems.append(f"{source}: cannot hold, it asks for both values of {contradiction[0][0]}")
        return {"map": f"{mode:04X}", "character": character, "conditions": kept, "intro": intro, "place": place,
                "source": source}

    def ways(self, quest, codes):
        ways = []
        for entry in CURATED[quest]:
            stage = self.stage(entry.stage)
            if isinstance(entry, Talk):
                for index in entry.actors:
                    talk = stage.actor_talks(index)[entry.talk]
                    set_here = next((c for c in codes if self.sets(stage, talk, entry, c)), None)
                    if set_here is None:
                        self.problems.append(f"{quest}: wstag{entry.stage} actor{index} talk {entry.talk} sets no flag")
                        continue
                    self.covered.add((set_here, entry.stage, f"actor{index}", entry.talk))
                    if entry.event:
                        self.covered.add((set_here, entry.stage, "event", entry.event))
                    conditions, intro = self.talk_way(stage, index, entry.talk)
                    line = stage.line_of(rf"FieldActorEntry actor{index} = ")
                    source = f"wstag{entry.stage}.c:{line} actor{index} talk {entry.talk}"
                    ways.append(self.assemble(entry.stage, stage.actors[index][2], conditions, intro, source))
            elif isinstance(entry, Slot):
                pairs, kind, arg = stage.slot(entry.slot)
                if kind != SLOT_EVENT_NAME or arg != entry.event:
                    self.problems.append(f"{quest}: wstag{entry.stage} slot {entry.slot} doesn't start {entry.event}")
                ways.append(self.event_way(quest, stage, entry, codes, pairs, f"slot {entry.slot}"))
            else:
                ways.append(self.event_way(quest, stage, entry, codes, self.stage_event_pairs(stage, entry), "updateStage"))
        unique, seen = [], set()
        for way in ways:
            key = json.dumps([way["map"], way["character"], [p[1:] for p in way["conditions"]],
                              [p[1:] for p in way["intro"]], [p[1:] for p in way["place"]]])
            if key not in seen:
                seen.add(key)
                unique.append(way)
        return unique

    def event_way(self, quest, stage, entry, codes, pairs, where):
        set_here = next((c for c in codes if stage.event_sets(entry.event, c)), None)
        if set_here is None:
            self.problems.append(f"{quest}: event {entry.event} of wstag{entry.stage} sets no flag")
        self.covered.add((set_here, entry.stage, "event", entry.event))
        return self.assemble(entry.stage, None, list(pairs), [], f"wstag{entry.stage}.c event {entry.event} {where}")

    def stage_event_pairs(self, stage, entry):
        """The conditions updateStage tests right before it starts the event."""
        function = re.search(r"void updateStage\(.*?\n\}", stage.text, re.S).group(0)
        call = re.search(rf"FIELDSTG_startEvent\({entry.event:#x}\)", function, re.I)
        if not call:
            raise ValueError(f"updateStage of wstag{entry.stage} doesn't start event {entry.event}")
        tests = re.findall(rf"FLAGS_00\.checkCondition\(({CODE_TOKEN}), (\w+)\)", function[:call.start()])
        return [(t, self.names.value(t), number(v)) for t, v in tests]

    def sets(self, stage, talk, entry, code):
        actions = stage.pairs(talk[1])
        if entry.event is None:
            return any(c == code and v != 0 for _t, c, v in actions)
        started = [self.event_ids[c & 0xFF] for _t, c, _v in actions if group_of(c) == GROUP_START_EVENT]
        return entry.event in started and stage.event_sets(entry.event, code)

    def check_every_setter(self, codes):
        """Any stage that sets one of the flags where CURATED doesn't say is a way this table would miss."""
        for path in sorted((self.decomp / "src/field/stages").glob("**/wstag*.c")):
            number_ = int(re.search(r"wstag(\d+)", path.name).group(1))
            stage = self.stage(number_)
            referenced = set()
            for index in stage.actors:
                if stage.actors[index][1] == "NULL":
                    continue
                for position, talk in enumerate(stage.actor_talks(index)):
                    referenced.add(talk[1])
                    for _t, code, value in stage.pairs(talk[1]):
                        if code in codes and value and (code, number_, f"actor{index}", position) not in self.covered:
                            self.problems.append(f"wstag{number_} actor{index} talk {position} sets {code:#x}: not curated")
            for name, pairs in stage.lists.items():
                if name.endswith("Actions") and name not in referenced and any(c in codes and v for _t, c, v in pairs):
                    self.problems.append(f"wstag{number_}: list {name} sets a quest's flag and no curated talk runs it")
            for token, value in re.findall(rf"FLAGS_00\.applyAction\(({CODE_TOKEN}), (\w+)\)", stage.text):
                code = self.names.value(token)
                if code in codes and number(value):
                    if not any(k[0] == code and k[1] == number_ and k[2] == "event" for k in self.covered):
                        self.problems.append(f"wstag{number_}: a function sets {token} and no curated event ends in it")


def plain(pairs):
    return [[code, value] for _t, code, value in pairs]


def to_json(table):
    """One line per way: the file is read by people as well, and has hundreds of pairs."""
    lines = ["{"]
    quests = list(table.items())
    for q, (quest, ways) in enumerate(quests):
        lines.append(f'  "{quest}": {{ "ways": [')
        for w, way in enumerate(ways):
            record = {"map": way["map"], "character": way["character"], "conditions": plain(way["conditions"]),
                      "intro": plain(way["intro"])}
            if way["place"]:
                record["place"] = plain(way["place"])
            lines.append("    " + json.dumps(record) + ("," if w < len(ways) - 1 else ""))
        lines.append("  ] }" + ("," if q < len(quests) - 1 else ""))
    lines.append("}")
    return "\n".join(lines) + "\n"


def special_table(decomp, names, used):
    """Rows of SPECIAL_CONDITIONS and PROGRESS_RANGES for the ids `used`, read from events.c."""
    text = preprocess((decomp / "src/engine/game/events.c").read_text(encoding="utf-8"))
    start = text.index("SpecialCondition SPECIAL_CONDITIONS[]")
    rows = {number(i): (number(c), number(a)) for i, c, a in
            re.findall(r"\{ (0x\w+), (0x\w+), (0x\w+) \}", text[start:text.index("};", start)])}
    start = text.index("u8 PROGRESS_RANGES[][2]")
    ranges = re.findall(r"\{ (\w+), (\w+) \}", text[start:text.index("};", start)])
    by_value = {}
    for name, value in names.names.items():
        if name.startswith("SPECIAL_") and re.fullmatch(r"SPECIAL\(\w+\)", value):
            by_value.setdefault(names.value(name), name)
    lines = []
    for code in sorted(used):
        check, arg = rows[code & 0xFF]
        low = f"{by_value.get(code, '?')} | {code & 0xFF:#04x} | {check:#04x} | {arg:#04x}"
        if check & 0xF0 == 0x30:
            first, last = ranges[arg]
            low += f" | progress in [{names.value(first):#04x}, {names.value(last):#04x}] ({first}, {last})"
        lines.append(f"| {low} |")
    return lines


def report(table, decomp, names):
    used = set()
    for quest, ways in table.items():
        print(f"\n{quest}: {len(ways)} ways")
        for way in ways:
            print(f"  {way['map']} character {way['character']} [{way['source']}]")
            print("    conditions:", ", ".join(f"{t}={v}" for t, _c, v in way["conditions"]))
            print("    intro:", ", ".join(f"{t}={v}" for t, _c, v in way["intro"]))
            if way["place"]:
                print("    place:", ", ".join(f"{t}={v}" for t, _c, v in way["place"]))
            used |= {c for _t, c, _v in way["conditions"] if group_of(c) == GROUP_SPECIAL}
    print("\n| name | id | check | arg | range |\n|---|---|---|---|---|")
    print("\n".join(special_table(decomp, names, used)))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("decomp", type=Path)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--report", action="store_true")
    args = parser.parse_args()

    names = Names(args.decomp)
    layout = Layout(args.decomp, names)
    codes = first_step_codes(layout)
    builder = Builder(args.decomp, names)
    table = {quest: builder.ways(quest, codes[quest]) for quest in CURATED}
    missing = sorted(set(codes) - set(CURATED))
    if missing:
        builder.problems.append(f"quests with no curated way: {missing}")
    builder.check_every_setter({c for quest in CURATED for c in codes[quest]})

    if builder.problems:
        print("\n".join(sorted(set(builder.problems))), file=sys.stderr)
        sys.exit(1)
    args.out.write_text(to_json(table), encoding="utf-8")
    if args.report:
        report(table, args.decomp, names)
    ways = [w for quest in table.values() for w in quest]
    print(f"{args.out}: {len(table)} quests, {len(ways)} ways", file=sys.stderr)


if __name__ == "__main__":
    main()
