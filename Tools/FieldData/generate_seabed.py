#!/usr/bin/env python3
"""Generates the Seabed data of the app from the matching decompilation of the European version, ReGame-Labs/dw3_decomp
(MIT):

  Frontend/src/database/seabed/seabed-route.json   the tunnel maps of each route (GAME.place 1-9, the Asuka server) and
                                                   what leaves each one: exits to the surface, and left and right.
  Frontend/src/database/seabed/seabed-enemy.json   the enemies of each route, 1-9 (Asuka) and 11-19 (Amaterasu).
  Frontend/src/handheld/seabed-docks.ts            the docks of each route, in order along it, for both servers, and
                                                   where each dock lands the player on its picture.
  Frontend/src/database/seabed/seabed-dock.json    adds the docks it lacks (the existing ones are left as they are).
  Frontend/src/database/map.json                   marks as `dock` the maps that are docks and were not.

The notes of `seabed-docks.ts` say what a route has besides its way through: the chests of WSTAG860, the forge of WSTAG845
(a stage's characters that only appear on a route, `PLACE(n)` in their conditions), and the tunnels with one way in and
nothing else. Item and smith names are stored as references to the game's text on the disc (itmnam:117, talk04:879).

The names of the docks are stored as references to the game's place names on the disc (`stname:28`, through DW3_DDW3, a copy
of the ddw3 checkout), so the app ships no place text.

Usage: DW3_DDW3=ddw3 python3 Tools/FieldData/generate_seabed.py <path to dw3_decomp> <path to the game's esitmnam.toml>

How the Seabed works (src/field/stages/underground/wstag825.c and the others up to wstag860.c, map ids 0x2E0-0x2E7):
  - A dock is a SLOT_WARP0 of a surface stage whose destination is a tunnel map and whose place is the route
    (GAME.place). It is open from SPECIAL_STORY_SINCE_DE_SINCERITY on, in every dock.
  - A tunnel map has `placePoints`, keyed by (place, placeArg): the points the stage copies to its slots one by one
    (copyPlacePoints), so slot i leads to point i: a map and the (place, placeArg) it is entered with. The first
    destination below 0x2E0 or above 0x2E7 is the surface.
  - The battles of a place are `stageBattles`, found by place (FieldState.findBattles); area 0 holds the random ones, up to
    eight battles each, every one an index of FIELDSTG_encounters (up to three BattleEnemy of FIELDSTG_enemies).
The decompilation holds where each slot leads, but not where the trigger of the slot is on the map (that is in the map
file on the disc), so which side of the screen each exit opens on is the table SIDES below, taken from the app's
data for routes 1-4 and 7-9, which this script checks against the game for every slot those routes use.
The extra chapter of the European version (STORY_EXTRA, WSTAG960-967, the stages 9xx that warp into it) is not
included: its tunnels would go beside the ones here, with the same reading, in `EXTRA_CHAPTER` below.

It reports to stderr what differs from the data in the repository before it wrote anything.
"""
import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import stage_data  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
KNOWN_WORDS = {w["key"] for w in json.loads(stage_data.TEXT_KEYS.read_text(encoding="utf-8"))["words"]}
DATABASE = ROOT / "Frontend/src/database"
MAPS = ROOT / "Frontend/src/assets/maps"
MAP_VIEW = ROOT / "Frontend/src/handheld/map-view.ts"
TUNNELS = range(0x2E0, 0x2E8)
AMATERASU_OFFSET = 10
ASUKA_ROUTES = range(1, 10)
SMITHS = {0xA8: "talk04:879", 0xAA: "talk04:883"}  # the characters of WSTAG845's forge: the line of their talk text with their name
# The extra chapter (STORY_EXTRA, WSTAG960-967) is left out on purpose: the European version replaces some surface
# stages with stages 9xx whose warps go into those tunnels. They would be read here the same way.
EXTRA_CHAPTER = None

# Where each slot of a tunnel map opens on the screen: ("emerge", where) for the exit to the surface, or "left"/"right" for
# the next tunnel map. Slot index as in `stageSlots`. Not in the decompilation: see the module comment.
SIDES = {
    0x2E0: [("emerge", "topRight"), "left"],
    0x2E1: [("emerge", "topLeft"), ("emerge", "top"), "right"],
    0x2E2: [("emerge", "topLeft"), "right"],
    0x2E3: [("emerge", "topRight"), "left"],
    0x2E4: ["right", "left"],
    0x2E5: [("emerge", "top"), "right", "left"],
    0x2E6: ["right", "left"],
    0x2E7: ["right"],
}


def key(mode, arg):
    return f"{mode:04X}-{arg}"


def server_copies():
    """[(first copy, last copy, first original)] of ServerCopies in map-view.ts."""
    text = MAP_VIEW.read_text(encoding="utf-8")
    block = text[text.index("const ServerCopies"):]
    block = block[:block.index("];")]
    return [tuple(int(v, 16) for v in m) for m in re.findall(r"\[0x([0-9a-f]+), 0x([0-9a-f]+), 0x([0-9a-f]+)\]", block)]


def copy_of(mode, copies):
    """The map the second server repeats `mode` as, or None."""
    for first, last, original in copies:
        if original <= mode <= original + (last - first):
            return first + mode - original
    return None


class Game:
    def __init__(self, decomp):
        self.decomp = decomp
        self.stage_of = stage_data.european_stages(decomp)
        self.mode_of = {number: mode for mode, number in self.stage_of.items()}
        self.copies = server_copies()
        self.tunnel_text = {m: stage_data.stage_text(decomp, self.stage_of[m]) for m in TUNNELS}
        self.points = {m: stage_data.parse_points(t) for m, t in self.tunnel_text.items()}
        self.slots = {m: stage_data.parse_slots(t) for m, t in self.tunnel_text.items()}
        self.battles = {m: stage_data.parse_battles(t) for m, t in self.tunnel_text.items()}
        self.enemies, self.encounters = stage_data.enemies_and_encounters(decomp)

    def docks(self):
        """{route: [(surface mode, tunnel mode, placeArg)]} from the warps of the European stages that enter the Seabed."""
        found = defaultdict(list)
        for mode, number in sorted(self.stage_of.items()):
            if mode in TUNNELS:
                continue
            text = stage_data.stage_text(self.decomp, number)
            if "StageSlot stageSlots[]" not in text:
                continue
            for slot in stage_data.parse_slots(text):
                if slot["type"] == "SLOT_WARP0" and slot["arg"] in TUNNELS:
                    entry = (mode, slot["arg"], slot["placeArg"], slot["place"])
                    if entry not in found[slot["place"]]:
                        found[slot["place"]].append(entry)
        return {place: [(m, t, a) for m, t, a, _p in entries] for place, entries in found.items()}

    def exits(self, mode, place, arg):
        """Where each slot of the tunnel map leads, for the way it was entered: [(mode, place, placeArg)]."""
        chain = self.points[mode].get((place, arg))
        if chain is None:
            raise KeyError(f"{mode:04X} has no place points for ({place}, {arg})")
        return [(p[0], p[1], p[2]) for p in chain]

    def route_states(self, route, starts):
        """The tunnel states (mode, arg) a route passes through, walking out from the docks' tunnel maps."""
        seen, todo = [], [(t, a) for _m, t, a in starts]
        while todo:
            state = todo.pop(0)
            if state in seen:
                continue
            seen.append(state)
            for mode, _place, arg in self.exits(state[0], route, state[1]):
                if mode in TUNNELS:
                    todo.append((mode, arg))
        return seen

    def graph(self, route, starts):
        """The data of a route in the shape of seabed-route.json: {"02E0-1": {emerge, left, right}}."""
        states = self.route_states(route, starts)
        exits = {s: self.exits(s[0], route, s[1]) for s in states}

        def emerge_of(state):
            return [(i, m) for i, (m, _p, _a) in enumerate(exits[state]) if m not in TUNNELS]

        def beyond(state, came_from):
            """The first dock past `state`, going on away from `came_from`; None at a dead end."""
            seen = {came_from}
            while True:
                if emerge_of(state):
                    return f"{emerge_of(state)[0][1]:04X}"
                seen.add(state)
                onward = [(m, a) for m, _p, a in exits[state] if m in TUNNELS and (m, a) not in seen]
                if not onward:
                    return None
                state = onward[0]

        maps = {}
        for state in states:
            sides = SIDES[state[0]]
            entry = {"emerge": [], "left": [], "right": []}
            for index, (mode, _place, arg) in enumerate(exits[state]):
                side = sides[index]
                if mode not in TUNNELS:
                    entry["emerge"].append({"on": side[1], "location": f"{mode:04X}"})
                    continue
                lines = [f"{mode:04X}"]
                dock = beyond((mode, arg), state)
                if dock is not None:
                    lines.append(dock)
                entry[side] = lines
            maps[key(*state)] = entry
        return maps, states, exits

    def dock_order(self, states, exits):
        """The docks in order along the route, from one end of its line of tunnels."""
        ends = [s for s in states if sum(1 for m, _p, _a in exits[s] if m in TUNNELS) <= 1]
        state, came, order = ends[0], None, []
        while state is not None:
            for m, _p, _a in exits[state]:
                if m not in TUNNELS and f"{m:04X}" not in order:  # a map can have two exits to the same dock
                    order.append(f"{m:04X}")
            onward = [(m, a) for m, _p, a in exits[state] if m in TUNNELS and (m, a) != came]
            came, state = state, (onward[0] if onward else None)
        return order

    def route_encounters(self, route, states):
        """Encounters of area 0 to 2 of every tunnel map of the route, in the order the tunnels are walked."""
        found, per_state = [], {}
        for mode, _arg in states:
            areas = self.battles[mode].get(route)
            here = []
            if areas is not None:
                for _count, battles in areas[:3]:
                    for encounter in battles:
                        if encounter != 0 and encounter not in here:
                            here.append(encounter)
            per_state[(mode, _arg)] = here
            for encounter in here:
                if encounter not in found:
                    found.append(encounter)
        return sorted(found), per_state


def resolve(game, encounters, enemy_table):
    """Enemy ids of enemy.json for the encounters: its memoryId is the fighter, and level and HP tell the copies apart."""
    by_memory = defaultdict(list)
    for enemy_id, enemy in enemy_table.items():
        by_memory[enemy.get("memoryId")].append(enemy_id)
    ids, notes = [], []
    for encounter in encounters:
        for index in game.encounters[encounter]:
            fighter, level = game.enemies[index]
            hp = game.enemy_hp[index]
            candidates = [i for i in by_memory.get(fighter, []) if enemy_table[i]["level"] == level and enemy_table[i]["hp"] == hp]
            if not candidates:
                candidates = [i for i in by_memory.get(fighter, []) if enemy_table[i]["level"] == level]
            if len(candidates) != 1:
                notes.append(f"fighter {fighter} level {level} hp {hp}: candidates {candidates}")
            if candidates and candidates[0] not in ids:
                ids.append(candidates[0])
    return ids, notes


sys.path.insert(0, str(ROOT / "Tools/ServerCopies"))
from generate_server_copies import stage_name, webp_size  # noqa: E402


def item_ref(item_id):
    """Reference of an item name in the game's item names (`itmnam:117`), which the app reads from the disc."""
    reference = f"itmnam:{item_id}"
    if reference not in KNOWN_WORDS:
        sys.exit(f"{reference} is not a word the extractor reads")
    return reference


def partner_ids(decomp):
    """{PARTNER_* name: partner id} of the PartnerId enum (include/engine/game_state.h): KOTEMON is 0."""
    text = (Path(decomp) / "include/engine/game_state.h").read_text()
    body = text[text.index("enum PartnerId {"):]
    names = re.findall(r"PARTNER_(\w+)", body[:body.index("};")])
    return {name: index for index, name in enumerate(names) if name != "COUNT"}


def route_notes(decomp, item_names, routes):
    """{route: [note]}: the chests and the forge of the stages, for both servers, and the dead ends of the routes."""
    notes = defaultdict(list)
    partners = partner_ids(decomp)
    stages = Path(decomp) / "src/field/stages/underground"
    source = (stages / "wstag860.c").read_text()
    for actor, item in re.findall(r"u16 (actor\d+)Talk0Actions\[\] = \{[^}]*?ITEM\(1, (0x[0-9A-Fa-f]+)\)", source):
        place = re.search(rf"u16 {actor}Conditions\[\] = \{{.*?PLACE\((0x[0-9A-Fa-f]+|\d+)\)", source, re.S).group(1)
        notes[int(place, 0)].append({"kind": "chest", "item": item_ref(int(item, 16))})

    source = (stages / "wstag845.c").read_text()
    for actor, character in re.findall(r"FieldActorEntry (actor\d+) = \{ *\w+, *\w+, *(0x[0-9A-Fa-f]+),", source):
        if int(character, 16) not in SMITHS:
            continue
        cond = re.search(rf"u16 {actor}Conditions\[\] = \{{(.*?)\}};", source, re.S).group(1)
        place = re.search(r"PLACE\((0x[0-9A-Fa-f]+|\d+)\)", cond).group(1)
        partner = partners[re.search(r"SPECIAL_(\w+)_IN_PARTY", cond).group(1)]
        level = re.search(r"SPECIAL_\w+_LEVEL_(\d+)", cond).group(1)
        has = re.search(r"ITEM\(3, (0x[0-9A-Fa-f]+)\), 1", cond).group(1)
        makes = re.search(r"ITEM\(3, (0x[0-9A-Fa-f]+)\), 0", cond).group(1)
        uses = re.search(rf"u16 {actor}Talk3Actions\[\] = \{{.*?ITEM\(2, (0x[0-9A-Fa-f]+)\), 0", source, re.S).group(1)
        notes[int(place, 0)].append(
            {
                "kind": "forge",
                "smith": SMITHS[int(character, 16)],
                "partner": partner,
                "level": int(level),
                "from": item_ref(int(has, 16)),
                "with": item_ref(int(uses, 16)),
                "to": item_ref(int(makes, 16)),
            }
        )

    for route, data in routes.items():
        for tunnel in data["maps"].values():
            if not tunnel["emerge"] and bool(tunnel["left"]) + bool(tunnel["right"]) <= 1:
                for number in (int(route), int(route) + AMATERASU_OFFSET):
                    notes[number].append({"kind": "deadEnd"})
    return notes


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("decomp", type=Path, help="the dw3_decomp checkout")
    parser.add_argument("item_names", type=Path, help="the game's item names, esitmnam.toml")
    args = parser.parse_args()
    item_names = re.findall(r'^\s*"(.*)",\s*$', args.item_names.read_text(encoding="utf-8"), re.M)
    game = Game(args.decomp)

    # HP of each BattleEnemy, to tell apart enemies of the same fighter and level.
    text = stage_data.preprocess((args.decomp / "src/field/field_mode/data/fieldstg.c").read_text(encoding="utf-8"))
    block = text[text.index("BattleEnemy FIELDSTG_enemies[] = {"):]
    block = block[:block.index("};")]
    game.enemy_hp = [int(hp) for _f, _l, hp, _m, _s in re.findall(r"\{(-?\d+), (\d+), (\d+), (\d+), (\d+)\}", block)]

    enemy_table = json.loads((DATABASE / "enemy.json").read_text(encoding="utf-8"))
    old_routes = json.loads((DATABASE / "seabed/seabed-route.json").read_text(encoding="utf-8"))
    enemy_file = DATABASE / "seabed/seabed-enemy.json"
    old_enemy_table = json.loads(enemy_file.read_text(encoding="utf-8")) if enemy_file.exists() else {}
    docks = game.docks()

    routes, orders, enemies = {}, {}, {}
    problems = []
    for route in ASUKA_ROUTES:
        maps, states, exits = game.graph(route, docks[route])
        routes[str(route)] = {"maps": maps}
        orders[route] = game.dock_order(states, exits)

        # The Amaterasu twin (route + 10) must be the same graph, with the maps of the second server on the surface.
        twin_maps, twin_states, twin_exits = game.graph(route + AMATERASU_OFFSET, docks[route + AMATERASU_OFFSET])
        if twin_states != states:
            problems.append(f"route {route + 10}: tunnel maps {twin_states} differ from route {route}'s {states}")
        for state in states:
            for (mode, _p, arg), (twin_mode, _tp, twin_arg) in zip(exits[state], twin_exits[state]):
                if mode in TUNNELS:
                    if (mode, arg) != (twin_mode, twin_arg):
                        problems.append(f"route {route} {state}: leads to {(mode, arg)} but its twin to {(twin_mode, twin_arg)}")
                elif copy_of(mode, game.copies) != twin_mode:
                    problems.append(f"route {route} {state}: the dock {mode:04X} is repeated as {copy_of(mode, game.copies)}, not {twin_mode:04X}")
        twin_order = game.dock_order(twin_states, twin_exits)
        orders[route + AMATERASU_OFFSET] = twin_order
        if [copy_of(int(d, 16), game.copies) for d in orders[route]] != [int(d, 16) for d in twin_order]:
            problems.append(f"route {route}: the docks {orders[route]} and {twin_order} are not copies of each other in the same order")

        for number, number_states in ((route, states), (route + AMATERASU_OFFSET, twin_states)):
            found, per_state = game.route_encounters(number, number_states)
            if len({tuple(v) for v in per_state.values()}) > 1:
                problems.append(f"route {number}: the tunnel maps do not all have the same battles: { {f'{m:04X}-{a}': v for (m, a), v in per_state.items()} }")
            ids, notes = resolve(game, found, enemy_table)
            problems += [f"route {number}: {note}" for note in notes]
            enemies[str(number)] = {"walking": ids, "levels": [game.enemies[i][1] for e in found for i in game.encounters[e]]}

    # Check the generated graph and enemies against what the repository had.
    for route, old in old_routes.items():
        new = routes[route]["maps"]
        if old["maps"] != new:
            problems.append(f"route {route}: the graph of the app differs from the game's:\n  app:  {json.dumps(old['maps'])}\n  game: {json.dumps(new)}")
        old_enemies = old_enemy_table.get(route, old.get("enemies", {})).get("walking", [])
        if old_enemies != enemies[route]["walking"]:
            problems.append(f"route {route}: the app lists enemies {old_enemies}, the game has {enemies[route]['walking']}")
    for route in sorted(set(routes) - set(old_routes), key=int):
        problems.append(f"route {route}: not in the app's data, added ({sorted(routes[route]['maps'])})")

    # The overview (seabed-direction.json, numbered by game route) must only hold docks the game has on that route; the
    # docks of a route it has no point for are listed without a line.
    overview = json.loads((DATABASE / "seabed/seabed-direction.json").read_text(encoding="utf-8"))
    for route in ASUKA_ROUTES:
        placed = [d["location"] for d in overview.get(str(route), {}).get("docks", [])]
        extra = [d for d in placed if d not in orders[route]]
        if extra:
            problems.append(f"overview route {route}: {extra} are not docks of the route in the game ({orders[route]})")
        unplaced = [d for d in orders[route] if d not in placed]
        if unplaced:
            problems.append(f"overview route {route}: no point on the picture for {unplaced}, listed without a line")

    for line in problems:
        print(line, file=sys.stderr)

    (DATABASE / "seabed/seabed-route.json").write_text(json.dumps(routes, indent=2) + "\n", encoding="utf-8")
    (DATABASE / "seabed/seabed-enemy.json").write_text(
        json.dumps({r: {"walking": e["walking"]} for r, e in sorted(enemies.items(), key=lambda kv: int(kv[0]))}, indent=2) + "\n",
        encoding="utf-8")

    # Where each dock lands the player on its picture: the point of the way out of the tunnel (x, y in pixels of the map).
    map_table = json.loads((DATABASE / "map.json").read_text(encoding="utf-8"))
    landings = {}
    for route in sorted(orders):
        for state in game.route_states(route, docks[route]):
            for mode, _place, _arg, x, y, _dir in game.points[state[0]][(route, state[1])]:
                if mode not in TUNNELS:
                    landings[(mode, route)] = (x, y)

    words = stage_data.stname_words()
    sizes, names = {}, {}
    for mode, _route in landings:
        number = game.stage_of[mode]
        image = stage_data.map_picture(f"{mode:04X}")
        if image is None or not (MAPS / f"{image}.webp").exists():
            image = f"S{number}PACK"
        sizes[mode] = webp_size(MAPS / f"{image}.webp")
        name = stage_name(args.decomp, number)
        if name not in words:
            sys.exit(f"{mode:04X}: \"{name}\" is not a place name of the stname table")
        names[mode] = words[name]

    lines = [
        "// Generated by Tools/FieldData/generate_seabed.py: do not edit.",
        "// The docks of the Seabed routes of both servers, from the matching decompilation of the European version,",
        "// ReGame-Labs/dw3_decomp (MIT): the surface stages whose exit warps into the tunnels (SLOT_WARP0, with the route as",
        "// GAME.place) and the tunnels' place points, where each way out lands the player. The extra chapter (STORY_EXTRA)",
        "// is not here: its stages would add their own warps (see EXTRA_CHAPTER in the generator).",
        "",
        "/** The docks of each route in order along it, as map ids: Asuka routes 1 to 9, Amaterasu routes 11 to 19. */",
        "export const SeabedRouteDocks: Readonly<Record<number, readonly string[]>> = {",
    ]
    for route in sorted(orders):
        lines.append(f"  {route}: [{', '.join(json.dumps(d) for d in orders[route])}],")
    lines += [
        "};",
        "",
        "/**",
        " * Where leaving the Seabed at a dock lands the player, as a percent of the picture of the place, keyed by the",
        " * map and the route (`\"0249:5\"`): a dock can have a way out of its own for each route (Lake of Ice has two).",
        " */",
        "export const SeabedLandings: Readonly<Record<string, { x: number; y: number }>> = {",
    ]
    for (mode, route), (x, y) in sorted(landings.items()):
        width, height = sizes[mode]
        lines.append(f"  \"{mode:04X}:{route}\": {{ x: {round(x / width * 100, 1)}, y: {round(y / height * 100, 1)} }},")
    lines += ["};", ""]
    notes = route_notes(args.decomp, item_names, routes)
    lines += [
        "export type SeabedNote =",
        '  | { kind: "chest"; item: string }',
        '  | { kind: "forge"; smith: string; partner: number; level: number; from: string; with: string; to: string }',
        '  | { kind: "deadEnd" };',
        "",
        "/**",
        " * What a route has besides its way through, for both servers: its chests, its forge and its dead ends. Items and smiths",
        " * are references to the game's text on the disc (`itmnam:117`), for `gameWord`; the partner is its id (0 Kotemon to 7",
        " * Patamon, as digimon.json numbers them), named from the disc too.",
        " */",
        "export const SeabedRouteNotes: Readonly<Record<number, readonly SeabedNote[]>> = {",
    ]
    for route in sorted(notes):
        lines.append(f"  {route}: {json.dumps(notes[route])},")
    lines += ["};", ""]
    lines += [
        "",
        "/** Name (a reference to the game's place names, for `gameWord`) of each dock of the Asuka server, from the name of its stage; its picture is the one of its map id. */",
        "export const SeabedDockPlaces: Readonly<Record<string, { word: string }>> = {",
    ]
    for mode in sorted(m for m in names if m < 0x270):
        lines.append(f"  \"{mode:04X}\": {{ word: {json.dumps(names[mode])} }},")
    lines += ["};", ""]
    (ROOT / "Frontend/src/handheld/seabed-docks.ts").write_text("\n".join(lines), encoding="utf-8")

    # The docks the app's tables lack: the pin of the dock (seabed-dock.json, as the app rounds them, to half a percent)
    # and the `dock` mark of its map.
    dock_table = json.loads((DATABASE / "seabed/seabed-dock.json").read_text(encoding="utf-8"))
    added, marked = [], []
    for (mode, route), (x, y) in sorted(landings.items()):
        location = f"{mode:04X}"
        if mode >= 0x270 or location in dock_table or location not in map_table:
            continue
        width, height = sizes[mode]
        dock_table[location] = {"coordinates": {"x": round(x / width * 200) / 2, "y": round(y / height * 200) / 2}}
        added.append(location)
    # map.json is edited as text, so the rest of the file keeps its layout.
    map_text = (DATABASE / "map.json").read_text(encoding="utf-8")
    for location in sorted({f"{m:04X}" for m, _r in landings if m < 0x270}):
        if location in map_table and not map_table[location].get("dock"):
            start = map_text.index(f'\n  "{location}": {{')
            end = map_text.index("\n  }", start)
            map_text = map_text[:end] + ',\n    "dock": true' + map_text[end:]
            marked.append(location)
    (DATABASE / "seabed/seabed-dock.json").write_text(json.dumps(dock_table, indent=2) + "\n", encoding="utf-8")
    (DATABASE / "map.json").write_text(map_text, encoding="utf-8")
    print(f"docks added to seabed-dock.json: {added}; marked `dock` in map.json: {marked}", file=sys.stderr)
    for route, entry in sorted(enemies.items(), key=lambda kv: int(kv[0])):
        print("enemies of route", route, [(stage_data.digimon_name(enemy_table[i]), enemy_table[i]["level"]) for i in entry["walking"]], file=sys.stderr)
    print(f"wrote {len(routes)} routes, {len(enemies)} enemy lists, {len(landings)} landings", file=sys.stderr)


if __name__ == "__main__":
    main()
