"""Sprites and names of the characters the field places, for the live actors of the Mapa.

Writes, from a copy of the game and the matching decompilations (formats of markisha64/ddw3 and
ReGame-Labs/dw3_decomp, MIT):
  - one transparent PNG per character and direction (0 to 7), native size, the first frame of the stand animation
    (ACTOR_ANIM_STAND). Directions 5 to 7 are the mirror of 3 to 1, as FIELDSTG_drawActor draws them.
  - a generated TypeScript file with, per character and direction, where the frame sits against the actor's point
    (Actor.tile): the game draws the frame at tile + (frame x offset, frame y offset), and mirrored at
    tile + (-(width + x offset), y offset). And the names, per map id.

Which characters: every id that an EU field stage places, in its `stageActors` list or with SCRIPT_PLACE.

Names come only from the game's own talk text. A stage's FieldActorEntry lists the talks of a character
(FieldTalk.entry is a line of the stage's talk text file); a line that starts `[name]Natsumi[name]` names who speaks. A
character gets the name when every talk of it in that stage names the same speaker; with no line that names a speaker, or
with two different ones, it has no name. Nothing is matched by position or by sprite. The app ships no name: `ActorNames`
holds, for each name, the reference of a talk line that gives it (`talk04:879`, chosen by speaker_references), and the app
reads the speaker of that line from the text it extracts from the user's disc. Everyone the game names
alike has the same reference. `ActorWiki` holds the id of the Wiki sheet (en-US npcs.json and tamer.json) that has exactly
that name, when one does and no other has it.

Roles come only from the scripts, per map id and character, never from a name, a sprite or the text; the first of this
order that the code proves wins, and what can't be proved is "talk". What a talk does is FieldTalk.actions (FLAGS_00's
applyActions, events.c applyAction); the talks that count are those runTalk can reach (up to the first without conditions)
of every FieldActorEntry of the character in the stage. An event of a talk is the action START_EVENT (FIELDSTG_eventIds, in
the stage's table), with the functions its start and end reach and the events those start.
  - shop: an action 0x7A with an id applyAction sends to MODE_ITEM_SHOP or MODE_CARD_SHOP.
  - service: an action 0x7A with any other id (FIELDSTG_openInn), 0x7C00 (Digi Lab), 0x7C01 (naming), 0x94 (training); a
    talk's event that leaves for the memory card screen (SCRIPT_LEAVE of mode 0xC00+: the saves); a talk's event that is a
    menu whose answers are only scripts (no flag, item, battle, story point or exit: the guides); or who speaks in an event
    that starts by itself where an answer takes BIT and gives an item (the Cargo Tower's auctions, END_CHOICE).
  - battle: an action 0x74 (event battle), 0x76 or 0x78 (card battle), or a talk's event that starts one; or who speaks in
    an event that starts by itself (a SLOT_EVENT, the stage's code) and starts one, when only one character speaks. A
    message box (SCRIPT_TALK kind 4, which the game shows with no one's name) counts when nobody else speaks.
  - story: a talk gives or takes an item, a card, BIT or a partner, or starts any other event; or it sets a flag that a
    slot, the code or another character reads; or the conditions of a talk or an entry test an item, a card, a partner, a
    set of items, BIT, or a flag that a talk of another character, or an event a talk started, sets; or the character
    speaks in an event that changes a flag, an item or GAME.progress, or is one of several speakers of a battle's event
    (where the code doesn't say who fights).
  - talk: the rest. Fixed lines, or lines that only change with GAME.progress (PROGRESS, SPECIAL_STORY_ ranges), the place,
    the party's strength, a flag only self-started events set (generic chatter per chapter), a flag only that character
    reads (it spoke already), FLAGS_00's flags among characters (which a map change clears), or a flag more than
    WORLD_FLAG_READERS characters read (FLAG_OINKMON_CITIZENS_RESTORED, a state of the world as a chapter is). Conditions
    don't count for a character that says one line and does nothing: they only say where it stands. An event that changes
    nothing but leaves for another map is a scene, and speaking in it isn't a role. This is what the Oinkmon crowds are.
Not given a role: the characters that lead the party instead of the player (1, 0x6A, 0x146, 0x147: createParty), which are
not talked to. Chests are stage characters too (runTalk's isTreasure), and have the role chest. Mode 528,
the Cargo Tower, has roles and no names: FIELDSTG runs it from its own tables (parse_field_stage).

Usage (needs Pillow and numpy; paths as in the README):
  DW3_ISO=game.iso DW3_DECOMP=dw3_decomp DW3_DDW3=ddw3 python3 Tools/RomExtract/field_actors.py [--png DIR] [--ts FILE] [--proofs FILE]
"""
import argparse
import collections
import json
import os
import re
import sys
import tomllib
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image  # noqa: E402

from actors import Sprites  # noqa: E402
from parse_stages import DECOMP, DDW3, eu_table, preprocess  # noqa: E402

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "FieldData"))
from generate_locales import LANGUAGES, SPEAKER, WORD_TABLES, real  # noqa: E402
from generate_locales import flat as text_flat, table as game_table  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_PNG = ROOT / "Frontend/src/handheld/assets/actors"
DEFAULT_TS = ROOT / "Frontend/src/handheld/field-actors.ts"
WIKI_NAMES = [ROOT / "Frontend/src/i18n/locales/en-US/npcs.json", ROOT / "Frontend/src/i18n/locales/en-US/tamer.json"]
NUMBER = r"0[xX][0-9A-Fa-f]+|\d+"


def number(text):
    return int(text, 0)


def stage_source(stage):
    found = list(Path(DECOMP, "src/field/stages").glob(f"*/wstag{stage:03d}.c"))
    return preprocess(found[0].read_text()) if len(found) == 1 else None


def stage_talk_file(source):
    """The estalkNN.toml the stage's talk entries index, or None when the stage uses a file that is not a TALKnn."""
    define = re.search(r"#define\s+STAGE_TEXT\s+(.+)", source)
    match = re.search(r"TEXT_TALKS_(\d\d)", define.group(1)) if define else None
    return f"estalk{match.group(1)}.toml" if match else None


def stage_talks(source):
    """{name of a FieldTalk array: [entries]} of the stage."""
    talks = {}
    for match in re.finditer(r"FieldTalk\s+(\w+)\[\]\s*=\s*\{(.*?)\n\};", source, re.S):
        entries = re.findall(rf"\{{[^{{}}]*?,[^{{}}]*?,\s*({NUMBER})\s*\}}", match.group(2))
        talks[match.group(1)] = [number(entry) for entry in entries if number(entry) != 0]
    return talks


def stage_actors(source):
    """[(character id, talk array name or None)] of the entries of `stageActors`, and the ids the scripts place."""
    entries = {}
    for match in re.finditer(
        rf"FieldActorEntry\s+(\w+)\s*=\s*\{{\s*(\w+),\s*(\w+),\s*({NUMBER})\s*,", source
    ):
        entries[match.group(1)] = (number(match.group(4)), None if match.group(3) == "NULL" else match.group(3))
    listed = re.search(r"stageActors\[\]\s*=\s*\{(.*?)\};", source, re.S)
    names = re.findall(r"&(\w+)", listed.group(1)) if listed else []
    placed = {number(m) for m in re.findall(rf"SCRIPT_PLACE\(\s*({NUMBER})\s*,", source)}
    return [entries[name] for name in names if name in entries], placed


def speaker(line):
    """Who a talk line starts as speaking (`[name]Natsumi[name]...`), or None when it names nobody (or the player)."""
    match = re.match(r"\[name\](.*?)\[name\]", line)
    if match is None:
        return None
    name = match.group(1).strip()
    return name if name and "[" not in name else None


def speaker_key(text):
    """A name as the app's text keys have it (Tools/FieldData/generate_text_keys.py): one line, without a final full stop."""
    return text_flat(text).rstrip(".")


def shown_speaker(line):
    """The speaker of a talk line as the extractor writes it (TextOutput.kt), or None when it names nobody."""
    match = SPEAKER.match(line)
    name = speaker_key(match.group(1)) if match else ""
    return name[:1].upper() + name[1:] if name else None


def table_words(folder):
    """{English line: {language folder: its line}} of the name tables the app reads whole (generate_text_keys.py)."""
    words = collections.defaultdict(dict)
    for language, prefix in LANGUAGES.values():
        for name in WORD_TABLES:
            for one, two in zip(game_table(folder / "eng", "es", name), game_table(folder / language, prefix, name)):
                if real(one) and real(two):
                    words[text_flat(one)].setdefault(language, text_flat(two))
    return words


def speaker_references():
    """{English speaker name, as a key: the reference of its talk line, `talk04:879`}.

    The line is the first of the TALK files, in the order of their numbers, that names the speaker in English and names
    someone in the four other languages. A speaker named like a whole name of the tables (a Digimon, a card) is shown as
    that name was before: the first such line whose speakers are that name in every language, when one is.
    """
    folder = Path(DDW3, "lang_file/dw2003")
    lines = collections.defaultdict(list)  # {English name: [(reference, {language folder: speaker})]}
    for path in sorted(folder.glob("eng/estalk*.toml")):
        table = path.stem[2:]
        english = game_table(folder / "eng", "es", table)
        others = {language: game_table(folder / language, prefix, table) for language, prefix in LANGUAGES.values()}
        for index, line in enumerate(english):
            name = shown_speaker(line)
            spoken = {language: shown_speaker(other[index]) if index < len(other) else None for language, other in others.items()}
            if name is not None and all(spoken.values()):
                lines[speaker_key(SPEAKER.match(line).group(1))].append((f"{table}:{index}", spoken))
    words = table_words(folder)
    references = {}
    for name, found in lines.items():
        same = [reference for reference, spoken in found if name in words and spoken == words[name]]
        references[name] = (same or [found[0][0]])[0]
    return references


def wiki_ids():
    """{English name: the one Wiki sheet that has it}, from the app's names of its NPCs and tamers."""
    ids = collections.defaultdict(list)
    for path in WIKI_NAMES:
        for entries in json.loads(path.read_text(encoding="utf-8")).values():
            for key, entry in entries.items():
                if "name" in entry:
                    ids[entry["name"]].append(key)
    return {name: keys[0] for name, keys in ids.items() if len(keys) == 1}


ROLES = ("shop", "service", "battle", "story", "talk")


def is_treasure(character):
    """runTalk's isTreasure (actor_control.c): a chest is a character of the stage, whatever it gives."""
    return character == 0x21 or 0x4D <= character < 0x58 or 0x154 <= character < 0x159 or character == 0x15B


PARTY_LEADERS = {1, 0x6A, 0x146, 0x147}  # createParty: they lead the party instead of the player
PLAYER = 2
MODE_SAVES = 0xC00  # MODE_CONTINUE's overlay; FIELDSTG_runEvent leaves with arg -1, which STGMCARD takes as saving
ACTION_SPECIAL_ICON = "SPECIAL_SHOW_ITEM_GET_ICON"
WORLD_FLAG_READERS = 10  # more characters than this read a flag: it is a state of the world, like a chapter (the Oinkmon's)


def blank_comments(text):
    return re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)


class Symbols(dict):
    """The names a code list uses: the macros of game_state.h, and story.h's names, worked out when first used."""

    def __init__(self):
        super().__init__(
            FLAG=lambda group, flag: group << 8 | flag,
            PROGRESS=lambda n: 0x6000 | n,
            SPECIAL=lambda n: 0x7000 | n,
            PARTY_STAT=lambda n: 0x7200 | n,
            EVENT_BATTLE=lambda n: 0x7400 | n,
            CARD_BATTLE=lambda opponent, kind: (0x7600 + kind * 0x200) | opponent,
            PLACE=lambda n: 0x7E00 | (n - 1),
            PLACE_ARG=lambda n: 0x7E00 | (n + 29),
            ITEM=lambda kind, item: 0x8000 | kind << 9 | item,
            START_EVENT=lambda n: 0x9000 | n,
            CARD=lambda n: 0x9200 | n,
            CODES_END=0xFFFF,
        )
        story = blank_comments(Path(DECOMP, "include/engine/story.h").read_text())
        field = blank_comments(Path(DECOMP, "include/field/field_map.h").read_text())
        self.defines = {m.group(1): m.group(2).strip() for m in re.finditer(r"#define\s+(\w+)\s+(.+)", field + story)}
        self.defines.update(
            {m.group(1): m.group(2) for m in re.finditer(rf"\b(STORY_\w+)\s*=\s*({NUMBER})", story)}
        )

    def __missing__(self, name):
        if name not in self.defines:
            raise KeyError(name)
        self[name] = self.evaluate(self.defines[name])
        return self[name]

    def evaluate(self, expression):
        return eval(expression, {"__builtins__": {}}, self)  # the decompilation's own constants


def split_arguments(text):
    """The arguments of a C list, at its top level."""
    out, depth, current = [], 0, ""
    for char in text:
        depth += char in "([{"
        depth -= char in ")]}"
        if char == "," and depth == 0:
            out.append(current.strip())
            current = ""
        else:
            current += char
    if current.strip():
        out.append(current.strip())
    return out


def call_arguments(text, name):
    """The argument text of each call to `name` in a function's body."""
    for match in re.finditer(re.escape(name) + r"\(", text):
        depth, i = 1, match.end()
        while i < len(text) and depth:
            depth += text[i] == "("
            depth -= text[i] == ")"
            i += 1
        yield text[match.end() : i - 1]


def code_kind(code):
    return (code >> 8) & 0xFE


def is_flag(code):
    return code_kind(code) <= 0x40


def is_item(code):
    return 0x80 <= code_kind(code) < 0x8F or code_kind(code) == 0x92


class Event:
    """What an event of a stage does, with the events it starts after it."""

    def __init__(self):
        self.shop = None
        self.service = None
        self.battle = None
        self.saves = None
        self.menu = None
        self.changes = []  # what it changes: a flag, an item, the point of the story
        self.exits = []  # the maps its scripts leave for
        self.auction = None  # a yes/no answer takes BIT and gives an item (FIELDSTG's END_CHOICE)
        self.flags = set()  # the flags it sets
        self.speakers = {}  # {character: proof}
        self.boxes = {}  # {character: proof}, those the script names in a message box
        self.named = {}  # {character: proof}, those its script moves or poses
        self.seen = set()  # the events it runs


class Stage:
    def __init__(self, stage_number, mode, path, source, raw):
        self.number = stage_number
        self.mode = mode
        self.path = path
        self.raw = raw
        self.source = blank_comments(source)
        self.lists = {}
        self.talks = {}
        self.entries = []
        self.events = {}
        self.scripts = {}
        self.functions = {}
        self.slots = []  # [(conditions, type, arg)]

    def line(self, pattern):
        match = re.search(pattern, self.raw, re.M)
        where = f":{self.raw.count(chr(10), 0, match.start()) + 1}" if match else ""
        return f"{self.path}{where}"

    def proof_list(self, name):
        return self.line(rf"\b{name.lstrip('&').split('[')[0]}\[\]\s*=")

    def proof_entry(self, entry):
        return self.line(rf"FieldActorEntry\s+{entry}\s*=")

    def proof_event(self, event):
        return self.line(rf"\{{\s*{event}\s*,\s*\w+\s*,")

    def proof_function(self, name):
        return self.line(rf"^\S.*\b{name}\(.*\{{\s*$")


def code_list(text, symbols):
    """[(code, name, value)] of a list's pairs, up to CODES_END."""
    tokens = split_arguments(text)
    pairs = []
    for i in range(0, len(tokens) - 1, 2):
        code = symbols.evaluate(tokens[i])
        if code == 0xFFFF:
            break
        pairs.append((code, tokens[i], symbols.evaluate(tokens[i + 1])))
    return pairs


def parse_stage(stage_number, mode, path, raw, symbols):
    stage = Stage(stage_number, mode, path, preprocess(raw), raw)
    source = stage.source
    # What the stages include from common/ is functions, which stay reachable by name.
    for include in re.findall(r'#include\s+"(common/[^"]+)"', source):
        common = Path(DECOMP, "src/field/stages", include)
        if common.exists():
            source += "\n" + blank_comments(preprocess(common.read_text()))
    for match in re.finditer(r"\bu16\s+(\w+)\[\]\s*=\s*\{(.*?)\};", source, re.S):
        try:
            stage.lists[match.group(1)] = code_list(match.group(2), symbols)
        except (KeyError, NameError, SyntaxError, TypeError):
            pass  # a table that isn't made of codes
    for match in re.finditer(r"FieldTalk\s+(\w+)\[\]\s*=\s*\{(.*?)\n\};", source, re.S):
        talks = []
        for talk in re.finditer(rf"\{{\s*(\w+)\s*,\s*(\w+)\s*,\s*({NUMBER})\s*\}}", match.group(2)):
            talks.append((talk.group(1), talk.group(2), number(talk.group(3))))
            if talk.group(1) == "NULL":  # runTalk stops at the first without conditions
                break
        stage.talks[match.group(1)] = talks
    entries = {}
    for match in re.finditer(rf"FieldActorEntry\s+(\w+)\s*=\s*\{{\s*(\w+),\s*(\w+),\s*({NUMBER})\s*,", source):
        entries[match.group(1)] = (match.group(1), match.group(2), match.group(3), number(match.group(4)))
    listed = re.search(r"stageActors\[\]\s*=\s*\{(.*?)\};", source, re.S)
    stage.entries = [entries[n] for n in re.findall(r"&(\w+)", listed.group(1)) if n in entries] if listed else []
    table = re.search(r"FieldEvent\s+\w+\[\]\s*=\s*\{(.*?)\n\};", source, re.S)
    if table and re.search(r"FIELDSTG_state\.events\s*=", source):
        for match in re.finditer(
            rf"\{{\s*({NUMBER})\s*,\s*(\w+)\s*,\s*[^,]+?\s*,\s*(\w+)\s*,\s*(\w+)\s*\}}", table.group(1)
        ):
            stage.events[number(match.group(1))] = (match.group(2), match.group(3), match.group(4))
    stage.scripts = {m.group(1): m.group(2) for m in re.finditer(r"s16\s+(\w+)\[\]\s*=\s*\{(.*?)\};", source, re.S)}
    for match in re.finditer(r"^[A-Za-z_][\w \*]*?[ \*](\w+)\([^;{}]*\)\s*\{\n(.*?)^\}", source, re.S | re.M):
        stage.functions[match.group(1)] = match.group(2)
    table = re.search(r"StageSlot\s+\w+\[\]\s*=\s*\{(.*?)\n\};", source, re.S)
    if table:
        for record in split_arguments(table.group(1)):
            fields = split_arguments(record.strip()[1:-1])
            conditions = []
            for pair in split_arguments(fields[0].strip()[1:-1]):
                code, value = split_arguments(pair.strip()[1:-1])
                code = symbols.evaluate(code)
                if code != 0xFFFF:
                    conditions.append((code, symbols.evaluate(value)))
            stage.slots.append((conditions, fields[1], fields[2]))
    return stage


def number(text):
    return int(text, 0)


def function_closure(stage, names):
    """The stage's functions that the event functions `names` call or hand to a task."""
    seen = set()
    pending = [name for name in names if name != "NULL"]
    while pending:
        name = pending.pop()
        if name in seen or name not in stage.functions:
            continue
        seen.add(name)
        pending += [word for word in re.findall(r"\b\w+\b", stage.functions[name]) if word in stage.functions]
    return seen


def started_events(body, symbols):
    """The event ids a function's body starts, both sides of a ?: included."""
    ids = set()
    for argument in call_arguments(body, "FIELDSTG_startEvent"):
        tail = argument.rsplit("?", 1)[-1]
        for side in tail.split(":"):
            try:
                ids.add(int(symbols.evaluate(side.strip())))
            except (KeyError, NameError, SyntaxError, TypeError):
                pass
    return ids


def function_actions(body, symbols, stage):
    """[(code, value)] that a function's body applies."""
    actions = []
    for argument in call_arguments(body, "FLAGS_00.applyAction"):
        parts = split_arguments(argument)
        try:
            actions.append((symbols.evaluate(parts[0]), symbols.evaluate(parts[1])))
        except (KeyError, NameError, SyntaxError, TypeError, IndexError):
            pass  # the code is worked out at run time
    for argument in call_arguments(body, "FLAGS_00.applyActions"):
        actions += [(code, value) for code, _name, value in stage.lists.get(argument.strip(), [])]
    return actions


def listed_event_ids():
    """FIELDSTG_eventIds, the events of the actions START_EVENT, in the European version."""
    text = Path(DECOMP, "src/field/field_mode/data/fieldstg.c").read_text()
    text = preprocess(text)
    match = re.search(r"s16\s+FIELDSTG_eventIds\[\]\s*=\s*\{(.*?)\};", blank_comments(text), re.S)
    return [number(n) for n in re.findall(rf"-?(?:{NUMBER})", match.group(1))]


def action_shop(code):
    """Which screen an action 0x7A opens, as applyAction decides: "items", "cards" or None for the inn."""
    action = code & 0x1FF
    if action < 30:
        return "items"
    if 0x31 <= action < 0x31 + 0x13 or 0x46 <= action < 0x46 + 5:
        return "cards"
    return None


LOSE_BIT = set()  # the SPECIAL_LOSE_*_BIT codes, filled by classify_roles


def apply_effects(event, actions, proof):
    """What an event's list of actions adds to it."""
    for code, value in actions:
        kind = code_kind(code)
        if kind == 0x7A:
            shop = action_shop(code)
            if shop is not None:
                event.shop = event.shop or f"{shop} shop {code:#06x}: {proof}"
            else:
                event.service = event.service or f"inn {code:#06x}: {proof}"
        elif kind == 0x7C and code & 0x1FF < 2:
            event.service = event.service or f"digi lab or naming {code:#06x}: {proof}"
        elif kind == 0x94:
            event.service = event.service or f"training {code:#06x}: {proof}"
        elif kind in (0x74, 0x76, 0x78):
            event.battle = event.battle or f"battle {code:#06x}: {proof}"
        elif is_flag(code):
            event.flags.add(code)
            event.changes.append(f"flag {code:#06x}: {proof}")
        elif is_item(code) or kind == 0x70:
            event.changes.append(f"item or special {code:#06x}: {proof}")
    takes_bit = any(code_kind(code) == 0x70 and code in LOSE_BIT for code, _value in actions)
    if takes_bit and any(is_item(code) and value for code, value in actions):
        event.auction = event.auction or f"takes BIT for an item: {proof}"


def run_event(stage, event_id, symbols, listed):
    """The Event of an id of the stage's table, with what it starts; None when the table doesn't have it."""
    if event_id not in stage.events:
        return None
    event = Event()
    pending = [event_id]
    while pending:
        current = pending.pop()
        if current in event.seen or current not in stage.events:
            continue
        event.seen.add(current)
        script_name, start, end = stage.events[current]
        proof = stage.proof_event(current)
        script = stage.scripts.get(script_name, "")
        for _box, _entry, character, kind in re.findall(r"SCRIPT_TALK\(([^,]+),([^,]+),([^,]+),([^)]+)\)", script):
            # Kind 4 is a message box (FIELDSTG_runEvent doesn't look for the character): the script names it, no more.
            spoken = event.boxes if number(kind.strip()) == 4 else event.speakers
            spoken.setdefault(number(character.strip()), f"event {current}: {proof}")
        for character in re.findall(r"SCRIPT_(?:PLACE|POSE|WALK)\(\s*([^,]+?)\s*,", script):
            try:
                event.named.setdefault(number(character), f"event {current}: {proof}")
            except ValueError:
                pass
        for mode in re.findall(r"SCRIPT_LEAVE\(\s*([^,]+?)\s*,", script):
            mode = number(mode)
            if mode & 0xFF00 == MODE_SAVES:
                event.saves = event.saves or f"event {current} leaves for {mode:#06x}: {proof}"
            else:
                event.exits.append(f"exit to mode {mode:#06x}, event {current}: {proof}")
        for name in function_closure(stage, (start, end)):
            body = stage.functions[name]
            where = f"{name}, event {current}: {stage.proof_function(name)}"
            apply_effects(event, function_actions(body, symbols, stage), where)
            if re.search(r"GAME\.progress\s*=[^=]", body):
                event.changes.append(f"sets GAME.progress in {where}")
            if "FIELDSTG_startEventBattle" in body or "FIELDSTG_battleFuncs.startEventBattle" in body:
                event.battle = event.battle or f"battle in {where}"
            if re.search(r"\b(StageMenu|StageListMenu|ChoiceChildren)\b", body):
                event.menu = event.menu or f"menu in {where}"
            pending += sorted(started_events(body, symbols))
            for code, _value in function_actions(body, symbols, stage):
                if code_kind(code) == 0x90 and code & 0x1FF < len(listed):
                    pending.append(listed[code & 0x1FF])
    return event


def flag_key(stage, code):
    """FLAGS_00's flags, which every stage has for itself, by stage; the game's by themselves."""
    return (stage.number if code_kind(code) == 0 else None, code)


class FlagIndex:
    """Who reads and who sets each flag, over every stage and the field's own code."""

    def __init__(self):
        self.readers = collections.defaultdict(set)
        self.writers = collections.defaultdict(set)

    def add_stage(self, stage, symbols, listed):
        for _entry, conditions, talks_name, character in stage.entries:
            for code, _name, _value in stage.lists.get(conditions, []):
                if is_flag(code):
                    self.readers[flag_key(stage, code)].add(("character", character))
            for talk_conditions, actions, _text in stage.talks.get(talks_name, []):
                for code, _name, _value in stage.lists.get(talk_conditions, []):
                    if is_flag(code):
                        self.readers[flag_key(stage, code)].add(("character", character))
                for code, _name, _value in stage.lists.get(actions, []):
                    if is_flag(code):
                        self.writers[flag_key(stage, code)].add(("talk", character))
                    if code_kind(code) == 0x90 and code & 0x1FF < len(listed):
                        event = run_event(stage, listed[code & 0x1FF], symbols, listed)
                        for flag in event.flags if event else ():
                            self.writers[flag_key(stage, flag)].add(("talk event", character))
        for conditions, _kind, _arg in stage.slots:
            for code, _value in conditions:
                if is_flag(code):
                    self.readers[flag_key(stage, code)].add(("slot", stage.number))
        for name, body in stage.functions.items():
            for argument in call_arguments(body, "FLAGS_00.checkCondition"):
                try:
                    code = symbols.evaluate(split_arguments(argument)[0])
                except (KeyError, NameError, SyntaxError, TypeError, IndexError):
                    continue
                if is_flag(code):
                    self.readers[flag_key(stage, code)].add(("code", stage.number, name))
            for code, _value in function_actions(body, symbols, stage):
                if is_flag(code):
                    self.writers[flag_key(stage, code)].add(("code", stage.number, name))

    def add_field_code(self, symbols):
        """The flags the rest of the game tests: FIELDSTG's tables and code (the auctions' story events...), the menus."""
        for path in sorted(Path(DECOMP, "src").rglob("*.c")):
            if "stages" in path.parts:
                continue
            text = blank_comments(preprocess(path.read_text(errors="replace")))
            # The Cargo Tower's characters are read with the stages, in add_stage.
            text = re.sub(r"u16\s+FIELDSTG_(?:actor|talk)Flags\[\]\s*=\s*\{.*?\n\};", "", text, flags=re.S)
            for name in re.findall(r"\b(FLAG_\w+)\b", text):
                if name in symbols.defines:
                    self.readers[(None, symbols[name])].add(("code", 0, path.name))
            for argument in call_arguments(text, "FLAGS_00.checkCondition"):
                try:
                    code = symbols.evaluate(split_arguments(argument)[0])
                except (KeyError, NameError, SyntaxError, TypeError, IndexError):
                    continue
                if is_flag(code):
                    self.readers[(None, code)].add(("code", 0, path.name))

    def only_own(self, stage, code, character):
        """No one but a character reads the flag: at most it picks a line. FLAGS_00's flags, which a map change clears,
        are shared by the entries of a person in the stage, as itself and as an Oinkmon, so any character counts."""
        readers = self.readers.get(flag_key(stage, code), set())
        if code_kind(code) == 0:
            return all(reader[0] == "character" for reader in readers)
        return readers <= {("character", character)}

    def set_by_others(self, stage, code, character):
        """A talk of another character, or an event one started, sets the flag: a quest that goes through this one. Not
        FLAGS_00's (see only_own), nor a flag so many read that it says how the world is, as a chapter would."""
        key = flag_key(stage, code)
        if code_kind(code) == 0:
            return None
        if len({r[1] for r in self.readers.get(key, ()) if r[0] == "character"}) > WORLD_FLAG_READERS:
            return None
        return next((w for w in sorted(self.writers.get(key, ()), key=str) if w[0] in ("talk", "talk event") and w[1] != character), None)


def condition_story(stage, code, name, character, flags):
    """Why a condition of a talk or of an entry makes a character story, or None for chatter."""
    if is_item(code):
        return f"tests {name}" if code & 0x1FF else None  # item 0 is nothing: no one has it
    if code_kind(code) == 0x70 and not (name.startswith("SPECIAL_STORY") or name.startswith("SPECIAL_PARTY_LEVEL_TOTAL")):
        return f"tests {name}"
    if is_flag(code):
        other = flags.set_by_others(stage, code, character)
        if other is not None:
            return f"tests {name}, which {other[0]} of character {other[1]} sets"
    return None


def said_the_same(stage, character):
    """The character's talks in the stage are one line, with no action: its conditions only say where it stands."""
    lines = set()
    for _entry, _conditions, talks_name, entry_character in stage.entries:
        if entry_character == character:
            for _talk_conditions, actions, text in stage.talks.get(talks_name, []):
                if actions != "NULL" and stage.lists.get(actions):
                    return False
                if text:
                    lines.add(text)
    return len(lines) <= 1


def classify_stage(stage, symbols, listed, flags, report, uncertain):
    """{character: (role, [proofs of that role])} of a stage."""
    found = collections.defaultdict(lambda: collections.defaultdict(list))
    dead = collections.defaultdict(list)
    scene = collections.defaultdict(list)
    characters = {entry[3] for entry in stage.entries} - PARTY_LEADERS - {PLAYER}

    for entry_name, conditions, talks_name, character in stage.entries:
        if character not in characters:
            continue
        condition_lists = [(conditions, stage.lists.get(conditions, []))]
        for talk_conditions, actions, text in stage.talks.get(talks_name, []):
            condition_lists.append((talk_conditions, stage.lists.get(talk_conditions, [])))
            where = stage.proof_list(actions)
            for code, name, value in stage.lists.get(actions, []):
                kind = code_kind(code)
                if kind == 0x7A:
                    shop = action_shop(code)
                    if shop is not None:
                        found[character]["shop"].append(f"{name} opens the {shop} shop ({actions}): {where}")
                    else:
                        found[character]["service"].append(f"{name} opens the inn ({actions}): {where}")
                elif kind == 0x7C:
                    if code & 0x1FF == 0:
                        found[character]["service"].append(f"{name} opens the Digi Lab ({actions}): {where}")
                    elif code & 0x1FF == 1:
                        found[character]["service"].append(f"{name} opens the naming screen ({actions}): {where}")
                elif kind == 0x94:
                    found[character]["service"].append(f"{name} opens training ({actions}): {where}")
                elif kind == 0x74:
                    found[character]["battle"].append(f"{name} starts a battle ({actions}): {where}")
                elif kind in (0x76, 0x78):
                    found[character]["battle"].append(f"{name} starts a card battle ({actions}): {where}")
                elif kind == 0x90:
                    index = code & 0x1FF
                    event_id = listed[index] if index < len(listed) else None
                    event = run_event(stage, event_id, symbols, listed)
                    if event is None:
                        dead[character].append(f"{name} starts event {event_id}, not in the stage's table ({actions}): {where}")
                        continue
                    label = f"{name}, event {event_id} ({actions}): {where}"
                    if event.shop:
                        found[character]["shop"].append(f"{label}; {event.shop}")
                    if event.service:
                        found[character]["service"].append(f"{label}; {event.service}")
                    if event.saves:
                        found[character]["service"].append(f"{label}; saves: {event.saves}")
                    if event.battle:
                        found[character]["battle"].append(f"{label}; {event.battle}")
                    if event.menu and not (event.changes or event.exits or event.battle or event.shop or event.service or event.saves):
                        found[character]["service"].append(f"{label}; only a guide: {event.menu}")
                    elif event.changes or event.exits or not event.saves:
                        found[character]["story"].append(f"{label}; starts an event")
                    for other, proof in event.speakers.items():
                        if other in characters and other != character and (event.changes or event.battle):
                            found[other]["story"].append(f"speaks in {label}, which changes something; {proof}")
                elif is_item(code):
                    found[character]["story"].append(f"{name} = {value} gives or takes it ({actions}): {where}")
                elif kind == 0x70 and name != ACTION_SPECIAL_ICON:
                    found[character]["story"].append(f"{name} ({actions}): {where}")
                elif is_flag(code) and not flags.only_own(stage, code, character):
                    readers = sorted(map(str, flags.readers[flag_key(stage, code)]))[:3]
                    found[character]["story"].append(f"sets {name}, read by {readers} ({actions}): {where}")
        if said_the_same(stage, character):
            continue  # whatever its conditions test, it says the one line and does nothing
        for list_name, codes in condition_lists:
            for code, name, _value in codes:
                why = condition_story(stage, code, name, character, flags)
                if why:
                    found[character]["story"].append(f"{why} ({list_name}): {stage.proof_list(list_name)}")

    # The events that start by themselves: a slot, or the stage's code outside the events.
    event_functions = set()
    for current, (_script, start, end) in stage.events.items():
        event_functions |= function_closure(stage, (start, end))
    self_started = {}
    for conditions, kind, arg in stage.slots:
        if kind == "SLOT_EVENT":
            self_started.setdefault(number(arg), f"SLOT_EVENT: {stage.line(r'SLOT_EVENT,\s*' + re.escape(arg))}")
    for name, body in stage.functions.items():
        if name not in event_functions:
            for started in started_events(body, symbols):
                self_started.setdefault(started, f"{name}: {stage.proof_function(name)}")
            for code, _value in function_actions(body, symbols, stage):
                if code_kind(code) == 0x90 and code & 0x1FF < len(listed):
                    self_started.setdefault(listed[code & 0x1FF], f"{name}: {stage.proof_function(name)}")
    for started, how in sorted(self_started.items()):
        event = run_event(stage, started, symbols, listed)
        if event is None:
            continue
        speakers = {c: p for c, p in event.speakers.items() if c in characters}
        boxes = {c: p for c, p in event.boxes.items() if c in characters}
        named = {c: p for c, p in event.named.items() if c in characters}
        label = f"event {started} starts by itself ({how})"
        if event.auction:
            for character, proof in speakers.items():
                found[character]["service"].append(f"speaks in {label}; {event.auction}; {proof}")
        elif event.battle:
            fighters = speakers or boxes
            if len(fighters) == 1:
                character, proof = next(iter(fighters.items()))
                found[character]["battle"].append(f"only speaker of {label}; {event.battle}; {proof}")
            elif fighters:
                report["battle events with several speakers"] += 1
                for character, proof in fighters.items():
                    found[character]["story"].append(f"one of the speakers {sorted(fighters)} of {label}, which starts a battle: {proof}")
                    uncertain.append((stage.mode, character, "several speakers of a battle event", f"{label}; {proof}"))
            else:
                report["battle events with no speaker on the map"] += 1
                for character, proof in named.items():
                    found[character]["story"].append(f"moved by the script of {label}, which starts a battle: {proof}")
                    uncertain.append((stage.mode, character, "moved in a battle event where no one speaks", f"{label}; {proof}"))
        else:
            for character, proof in speakers.items():
                if event.saves and not event.changes:
                    found[character]["service"].append(f"speaks in {label}; {event.saves}")
                elif event.changes:
                    found[character]["story"].append(f"speaks in {label}, which changes something ({event.changes[0]}): {proof}")
                else:
                    scene[character].append(f"speaks in {label}, which changes nothing: {proof}")

    roles = {}
    for character in sorted(characters):
        for role in ROLES[:-1]:
            if found[character][role]:
                roles[character] = (role, found[character][role])
                break
        else:
            if dead[character]:
                report["talk, its talk starts an event the stage doesn't have"] += 1
                uncertain.append((stage.mode, character, "starts an event the stage lacks", dead[character][0]))
            if scene[character]:
                report["talk, it only speaks in an event that changes nothing"] += 1
                uncertain.append((stage.mode, character, "speaks in a scene that changes nothing", scene[character][0]))
            roles[character] = ("talk", dead[character] + scene[character])
        if is_treasure(character):
            roles[character] = ("chest", roles[character][1])
    return roles


def parse_field_stage(symbols):
    """Mode 528, the Cargo Tower: FIELDSTG's own stage (FIELDSTG_setupField), whose lists are FIELDSTG's tables and whose
    events' scripts are WSTAG260's; FIELDSTG_runStoryEvents starts them at their point of the story."""
    relative = "src/field/field_mode/data/fieldstg.c"
    raw = Path(DECOMP, relative).read_text()
    stage = Stage(260, 528, relative, preprocess(raw), raw)
    source = stage.source
    lists = {}
    for name in ("FIELDSTG_actorFlags", "FIELDSTG_talkFlags"):
        match = re.search(rf"u16\s+{name}\[\]\s*=\s*\{{(.*?)\n\}};", source, re.S)
        lists[name] = split_arguments(match.group(1))

    def named_list(reference):
        match = re.fullmatch(r"&(\w+)\[(\d+)\]", reference)
        if match is None:
            return "NULL"
        stage.lists[reference] = code_list(",".join(lists[match.group(1)][int(match.group(2)) :]), symbols)
        return reference

    talks = []
    table = re.search(r"FieldTalk\s+FIELDSTG_talks\[\]\s*=\s*\{(.*?)\n\};", source, re.S)
    for match in re.finditer(rf"\{{\s*([^,{{}}]+?)\s*,\s*([^,{{}}]+?)\s*,\s*({NUMBER})\s*\}}", table.group(1)):
        talks.append((named_list(match.group(1)), named_list(match.group(2)), number(match.group(3))))
    table = re.search(r"FieldActorEntry\s+FIELDSTG_actors\[\]\s*=\s*\{(.*?)\n\};", source, re.S)
    for index, record in enumerate(split_arguments(table.group(1))):
        fields = split_arguments(record.strip()[1:-1])
        conditions = named_list(fields[0])
        talks_name = "NULL"
        reference = re.fullmatch(r"&FIELDSTG_talks\[(\d+)\]", fields[1])
        if reference:
            talks_name = fields[1]
            reachable = []
            for talk in talks[int(reference.group(1)) :]:
                reachable.append(talk)
                if talk[0] == "NULL":
                    break
            stage.talks[talks_name] = reachable
        stage.entries.append((f"FIELDSTG_actors[{index}]", conditions, talks_name, number(fields[2])))
    scripts = blank_comments(preprocess(Path(DECOMP, "src/field/stages/wstag260.c").read_text()))
    stage.scripts = {
        "WSTAG260_" + m.group(1): m.group(2) for m in re.finditer(r"s16\s+(\w+)\[\]\s*=\s*\{(.*?)\};", scripts, re.S)
    }
    table = re.search(r"FieldEvent\s+FIELDSTG_events\[\]\s*=\s*\{(.*?)\n\};", source, re.S)
    for match in re.finditer(rf"\{{\s*({NUMBER})\s*,\s*(\w+)\s*,\s*[^,]+?\s*,\s*(\w+)\s*,\s*(\w+)\s*\}}", table.group(1)):
        stage.events[number(match.group(1))] = (match.group(2), match.group(3), match.group(4))

    # The functions the table names, which are FIELDSTG's macros: each question's and each answer's.
    code = blank_comments(Path(DECOMP, "src/field/field_mode/field_stage.c").read_text())
    for n, first, second in re.findall(r"END_CHOICE\((\d+),\s*(\w+),\s*(\w+)\)", code):
        stage.functions[f"FIELDSTG_endChoice{n}Answer0"] = (
            f"FLAGS_00.applyAction({first}, 1);FLAGS_00.applyAction({second}, 1);FLAGS_00.applyAction(0x400 + {n}, 1);"
        )
        stage.functions[f"FIELDSTG_endChoice{n}Answer1"] = f"FLAGS_00.applyAction(0x400 + {n}, 1);"
    table = re.search(r"FIELDSTG_choices\[\d*\]\s*=\s*\{(.*?)\n\};", source, re.S)
    choices = re.findall(rf"\{{\s*(?:{NUMBER})\s*,\s*\{{\s*({NUMBER})\s*,\s*({NUMBER})\s*\}}\s*\}}", table.group(1))
    for n, answers in enumerate(choices):
        stage.functions[f"FIELDSTG_askChoice{n}"] = (
            f"ChoiceChildren;FIELDSTG_startEvent(task ? {answers[0]} : {answers[1]});"
        )
    table = re.search(r"ProgressEvent\s+FIELDSTG_progressEvents\[\]\s*=\s*\{(.*?)\n\};", source, re.S)
    story = ""
    for fields in map(split_arguments, re.findall(r"\{([^{}]+)\}", table.group(1))):
        if fields[0] != "-1":
            story += f"FIELDSTG_startEvent({fields[3]});FIELDSTG_startEvent({fields[4]});"
    stage.functions["FIELDSTG_runStoryEvents"] = story
    return stage


def classify_roles(symbols=None):
    """{map id: {character: (role, [proofs])}} and the report's counts."""
    symbols = symbols or Symbols()
    listed = listed_event_ids()
    stages = []
    for mode, _file, stage_number in eu_table():
        found = list(Path(DECOMP, "src/field/stages").glob(f"*/wstag{stage_number:03d}.c"))
        if len(found) != 1:
            continue
        path = str(found[0].relative_to(DECOMP))
        stages.append(parse_stage(stage_number, mode, path, found[0].read_text(), symbols))
    stages.append(parse_field_stage(symbols))
    LOSE_BIT.update(symbols[name] for name in symbols.defines if name.startswith("SPECIAL_LOSE_"))
    flags = FlagIndex()
    for stage in stages:
        flags.add_stage(stage, symbols, listed)
    flags.add_field_code(symbols)
    report = collections.Counter()
    uncertain = []
    roles = {}
    for stage in stages:
        roles[f"{stage.mode:04X}"] = classify_stage(stage, symbols, listed, flags, report, uncertain)
    return roles, report, uncertain, stages



def write_proofs(path, roles, names, counts, report, uncertain):
    """The long list behind ActorRoles: each map's characters, with every proof of the role they got."""
    out = ["# Proofs of ActorRoles", "", f"Counts: {dict(counts)}", "", f"Counted apart: {dict(report)}", "", "## Uncertain", ""]
    for mode, character, why, proof in uncertain:
        out.append(f"- {mode:04X} {character} ({names.get(f'{mode:04X}', {}).get(character, '?')}): {why}: {proof}")
    out += ["", "## Every character", ""]
    for map_id, by_character in sorted(roles.items()):
        for character, (role, proofs) in sorted(by_character.items()):
            name = names.get(map_id, {}).get(character, "?")
            out.append(f"- {map_id} {character} ({name}): {role}: {'; '.join(proofs[:3]) or 'no action, no story test'}")
    path.write_text("\n".join(out) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--png", type=Path, default=DEFAULT_PNG)
    parser.add_argument("--ts", type=Path, default=DEFAULT_TS)
    parser.add_argument("--proofs", type=Path, help="write each character's role and its proof (file:line) to this file")
    args = parser.parse_args()

    texts = {}
    names = {}  # {map id: {character: name}}
    stage_characters = {}  # {map id: the characters its stageActors place}
    placed = set()
    report = collections.Counter()
    for mode, _file, stage in eu_table():
        source = stage_source(stage)
        if source is None:
            report["stages without source"] += 1
            continue
        entries, scripted = stage_actors(source)
        placed |= {character for character, _talks in entries} | scripted
        talk_file = stage_talk_file(source)
        if talk_file is not None and talk_file not in texts:
            path = Path(DDW3, "lang_file/dw2003/eng", talk_file)
            texts[talk_file] = tomllib.loads(path.read_text())["strings"] if path.exists() else None
        lines = texts.get(talk_file)
        talks = stage_talks(source)

        speakers = collections.defaultdict(set)
        characters = set()
        stage_characters[f"{mode:04X}"] = {character for character, _talks in entries}
        for character, talk_name in entries:
            characters.add(character)
            for entry in talks.get(talk_name, []) if talk_name else []:
                if lines is not None and entry < len(lines):
                    who = speaker(lines[entry])
                    if who is not None:
                        speakers[character].add(who)
        stage_names = {}
        for character in characters:
            if len(speakers[character]) == 1:
                stage_names[character] = next(iter(speakers[character]))
                report["named"] += 1
            elif len(speakers[character]) > 1:
                report["two speakers, no name"] += 1
            else:
                report["no speaker in its talks, no name"] += 1
        if stage_names:
            names[f"{mode:04X}"] = dict(sorted(stage_names.items()))

    sprites = Sprites()
    shipped = {}
    failed = []
    args.png.mkdir(parents=True, exist_ok=True)
    for old in args.png.glob("*.png"):
        old.unlink()
    for character in sorted(placed):
        try:
            frames = {}
            for direction in range(5):
                pixels, info = sprites.stand(character, direction=direction)
                frames[direction] = (Image.fromarray(pixels, "RGBA"), info["offset"])
        except Exception as error:  # a character without a stand animation, or without a sprite file
            failed.append((character, repr(error)))
            continue
        entry = []
        for direction in range(8):
            image, (x, y) = frames[direction if direction < 5 else 8 - direction]
            if direction >= 5:
                image = image.transpose(Image.FLIP_LEFT_RIGHT)
                x = -(image.width + x)
            image.save(args.png / f"{character}-{direction}.png", optimize=True)
            entry.append([x, y, image.width, image.height])
        shipped[character] = entry

    total = sum(path.stat().st_size for path in args.png.glob("*.png"))
    # Only the characters that have a sprite are worth a name.
    names = {map_id: {c: n for c, n in by_character.items() if c in shipped} for map_id, by_character in names.items()}
    names = {map_id: by_character for map_id, by_character in names.items() if by_character}
    references = speaker_references()
    missing = sorted({n for by_character in names.values() for n in by_character.values() if speaker_key(n) not in references})
    if missing:
        sys.exit(f"no talk line names these speakers in every language: {missing}")
    wiki = wiki_ids()
    wiki_targets = {
        map_id: {c: wiki[n] for c, n in by_character.items() if n in wiki} for map_id, by_character in names.items()
    }
    wiki_targets = {map_id: by_character for map_id, by_character in wiki_targets.items() if by_character}

    lines = [
        "// Generated by Tools/RomExtract/field_actors.py: do not edit.",
        "// Sprites of the characters the field places and where their names are, from the data of the game and the",
        "// matching decompilation ReGame-Labs/dw3_decomp (MIT). No text of the game: the names are read from the disc.",
        "",
        "/** Where the frame of a direction sits against the point of the actor, and its size: `[x, y, width, height]` in pixels. */",
        "export type ActorFrame = readonly [number, number, number, number];",
        "",
        "/** By character (`Actor.key1`): the frames of the directions 0 to 7 (5 to 7 are the mirror of 3 to 1). */",
        "export const ActorFrames: Readonly<Record<number, readonly ActorFrame[]>> = {",
    ]
    for character, entry in shipped.items():
        lines.append(f"  {character}: {json.dumps(entry)},")
    lines += [
        "};",
        "",
        "/**",
        " * By map id, where the name of a character is, when every talk of it in that stage names the same speaker: a talk",
        " * line that names it (`talk04:879`), whose speaker the app reads from the text of the disc. Everyone the game names",
        " * alike has the same line. A character that is not here has no name.",
        " */",
        "export const ActorNames: Readonly<Record<string, Readonly<Record<number, string>>>> = {",
    ]
    for map_id, by_character in sorted(names.items()):
        inner = ", ".join(f"{c}: {json.dumps(references[speaker_key(n)])}" for c, n in by_character.items())
        lines.append(f"  {json.dumps(map_id)}: {{ {inner} }},")
    lines += [
        "};",
        "",
        "/** By map id, the Wiki sheet of a character: the one NPC or tamer of the app with exactly the name the game gives it. */",
        "export const ActorWiki: Readonly<Record<string, Readonly<Record<number, string>>>> = {",
    ]
    for map_id, by_character in sorted(wiki_targets.items()):
        inner = ", ".join(f"{c}: {json.dumps(key)}" for c, key in by_character.items())
        lines.append(f"  {json.dumps(map_id)}: {{ {inner} }},")
    roles, role_report, uncertain, _stages = classify_roles()
    roles = {map_id: {c: r for c, r in by_character.items() if c in shipped} for map_id, by_character in roles.items()}
    roles = {map_id: by_character for map_id, by_character in roles.items() if by_character}
    lines += [
        "};",
        "",
        "/** What talking to a character does; the rules, and what can't be proved (talk), are in field_actors.py. */",
        'export type ActorRole = "shop" | "service" | "battle" | "story" | "chest" | "talk";',
        "",
        "/** By map id, the role of each character its stage places, from the stage's scripts. The player's aren't here. */",
        "export const ActorRoles: Readonly<Record<string, Readonly<Record<number, ActorRole>>>> = {",
    ]
    for map_id, by_character in sorted(roles.items()):
        inner = ", ".join(f"{c}: {json.dumps(role)}" for c, (role, _proofs) in sorted(by_character.items()))
        lines.append(f"  {json.dumps(map_id)}: {{ {inner} }},")
    lines += ["};", ""]
    args.ts.write_text("\n".join(lines), encoding="utf-8")
    counts = collections.Counter(role for m in roles.values() for role, _proofs in m.values())
    if args.proofs:
        write_proofs(args.proofs, roles, names, counts, role_report, uncertain)

    named_shipped = sum(len(v) for v in names.values())
    pairs = sum(len(c & shipped.keys()) for c in stage_characters.values())
    print(f"characters placed by a stage: {len(placed)}; with sprites: {len(shipped)}; failed: {failed}", file=sys.stderr)
    print(f"PNG files: {len(shipped) * 8}, {total} bytes in total", file=sys.stderr)
    print(f"stage characters with a sprite: {pairs}, of them with a name: {named_shipped}; before the sprite filter: {dict(report)}", file=sys.stderr)
    shown = {references[speaker_key(n)] for by_character in names.values() for n in by_character.values()}
    print(f"names: {len(shown)} talk lines; with a Wiki sheet: {sum(len(v) for v in wiki_targets.values())}", file=sys.stderr)
    print(f"roles: {dict(counts)}; talk that can't be told further: {dict(role_report)}; uncertain: {len(uncertain)}", file=sys.stderr)


if __name__ == "__main__":
    main()
