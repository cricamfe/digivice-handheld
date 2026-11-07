# What the quest-conditions table means, and how to read it from RAM

Table: `Backend/Memory/Definitions/quest-conditions.json`, 22 quests and 65 ways. Generator: `Tools/FieldData/generate_quest_conditions.py <path to dw3_decomp>`; `--report` prints each way with its names and source line.

Lines cite `refs/dw3_decomp`, European version. RAM addresses are offsets into the PS1's 2 MB, without `0x80000000`. Nothing here was run in the game or the app. The checks are at the end.

## Where the file lives

`Android/core/src/main/assets` does not exist. Core's JSON definitions live in `Backend/Memory/Definitions`, which `Android/app/build.gradle.kts:94` copies into the app's assets as `definitions/`. On the device the file is `definitions/quest-conditions.json`.

It is at the top of that folder and not in `Quests/`, because `AddressesRepository.kt:83` parses every file of the quest folders as `QuestAddresses`. Nothing loads it yet. `Tauri/binaries/Memory/Definitions` was a tracked copy of the folder for the Windows app; it left the repository with that app.

## Shape

```json
{ "ways": [ { "map": "0226", "character": 59, "conditions": [[code, value]], "intro": [[code, value]], "place": [[code, value]] } ] }
```

A quest can be started if every pair of `conditions` holds in any one way.

- **`map`:** the mode, the same number as `GAME.mode` (`PlayerAddresses.MapId`).
- **`character`:** `FieldActorEntry.id`. It is null for `treeBoots` (a trigger) and `digimonOnline` (the stage's own event).
- **`conditions`:** see the composition below. All values are 0 or 1.
- **`intro`:** what talking to that actor makes true, up the chain of talks. It never blocks.
- **`place`:** only on 6 ways, and only PLACE and PLACE_ARG pairs. No other flag ended up here: every FLAGS_00 pair in the table is an `intro` pair.

`conditions` is, in order:

1. The chapter pair, on 8 ways (next section).
2. The actor entry's list, which `field.c:344` tests when the field is created.
3. The list of the talk or slot that sets the flag, which `runTalk` tests when the player talks (`actor_control.c:1304`), first match wins.
4. Negations of earlier talks. The code handles this, but no way needed one: every earlier talk that cannot lead onward already has a pair directly opposite to the way.

### The chapter pair

This is one thing added beyond the entry, talk and negation lists. `FIELDSTG_pickStage` (`src/field/field_mode/state.c:26`) searches `FIELDSTG_extraStages` first while `GAME.progress == STORY_EXTRA` (0x2D). So the same mode loads another stage in that chapter, and the condition exists in no stage's list.

| Pair | Ways |
|---|---|
| `PROGRESS(STORY_EXTRA)=1`, stage only in the extra table | folderBag 027F, digimonOnline, eternally 02EC, muramasa 02EC, driAgentVeemon 02EA, cardKing |
| `PROGRESS(STORY_EXTRA)=0`, the extra table replaces this mode | amaterasuTrophy, amaterasuRematch |

Without it, `cardKing` and `digimonOnline` would read as open for the whole main chapter.

## The codes

`checkCondition` (`src/engine/game/events.c:509`) takes `group = (code >> 8) & 0xFE` and `id = code & 0x1FF`. A group it doesn't know returns 1 (`:555`). These are the kinds the table uses:

| Group | Macro | Where in the table | Test |
|---|---|---|---|
| 0x00 | `FLAG(0, n)` | `intro` only | `testBit` on `FLAGS_00.bits` |
| 0x02, 0x0A, 0x10, 0x1A, 0x1C, 0x40 | `FLAG(g, n)` | `conditions` (0x1A also in `intro`) | `testBit` on `GAME.flagsNN` |
| 0x60 | `PROGRESS(n)` | `conditions` | `checkProgress`, `:322` |
| 0x70 | `SPECIAL(n)` | `conditions` | `checkSpecialCondition`, `:285` |
| 0x7E | `PLACE(n)`, `PLACE_ARG(n)` | `place` only | `checkWarpArg`, `:397` |
| 0x80, 0x86 | `ITEM(kind, n)` | `conditions` | `checkItem`, `:339` |

Macros: `include/engine/game_state.h:68-82`.

`PARTY_STAT` (0x72) and `CARD` (0x92) appear nowhere in the table. Every code in the table can be evaluated from RAM, so nothing was left out for that reason.

### Flags

`testBit` (`events.c:5`): `byte = id >> 3`, `mask = 1 << (id & 7)`. A value other than 0 asks for the bit set, and 0 asks for it clear.

| Group | Field | RAM | Bytes | Declared |
|---|---|---|---|---|
| 0x00 | `FLAGS_00.bits` | 0x48ABC | 4 | `game_state.h:49` |
| 0x02 | `flags02` | 0x4B378 | 0x12 | `:555` |
| 0x04 | `flags04` | 0x4B38A | 2 | `:556` |
| 0x06 | `flags06` | 0x4B38C | 1 | `:557` |
| 0x08 | `flags08` | 0x4B38D | 1 | `:558` |
| 0x0A | `flags0A` | 0x4B38E | 4 | `:559` |
| 0x0C | `flags0C` | 0x4B392 | 8 | `:560` |
| 0x0E | `flags0E` | 0x4B39A | 0xC | `:561` |
| 0x10 | `flags10` | 0x4B3A6 | 4 | `:562` |
| 0x18 | `flags18` | 0x4B3AA | 2 | `:563` |
| 0x1A | `flags1A` | 0x4B3AC | 9 | `:564` |
| 0x1C | `flags1C` | 0x4B3B5 | 0xB | `:565` |
| 0x20 | `flags20` | 0x4B3C0 | 0x1E | `:566` |
| 0x40 | `flags40` | 0x4B3DE | 0x1A | `:567` |

Base addresses: `FLAGS_00` = 0x80048ABC and `GAME` = 0x80048D34 (`config/eu/symbols.txt:807`, `:814`). The fields are offsets from `GAME`. They agree with the addresses in `Backend/Memory/Definitions/Quests`, and the generator turns those addresses back into the same flags.

FLAGS_00 is temporary. Bytes 0 to 2 are cleared when a new mode starts (`updateModeFlags`, `events.c:678`), and a card battle's return sets `FLAG_CARD_BATTLE_ENDED` (`FLAG(0, 0x11)`) and `FLAG_CARD_BATTLE_WON` (`FLAG(0, 0x10)`).

### Progress

`GAME.progress` is an `s32` at 0x4B370 (`game_state.h:518`). `checkProgress` (`events.c:322`) tests equality: `PROGRESS(n), 1` means exactly n, and `PROGRESS(n), 0` means anything else. A range only comes through `SPECIAL` (below).

### Items and cards

- **Items:** `s8` counts at 0x48DB0 + id, `items[0x193]` (`game_state.h:512`). Equipped copies are at 0x48F43 + id (`:513`). `checkItem` (`events.c:339`) counts either: value 1 is "in the bag or equipped", value 0 is "in neither". The kind in `ITEM(kind, id)` is not read: the index is `code & 0x1FF`.
- **Cards** (none in the table): `s8` at 0x490D6 + id, `cards[0x13D]` (`game_state.h:514`), tested by `checkCard` (`events.c:353`).

### Place

`checkWarpArg` (`events.c:397`): for an id under 30, `GAME.place == id + 1` (`u16` at 0x48D78). Otherwise `GAME.placeArg == id - 29` (`u16` at 0x48D7A). These are the same addresses as `PlayerAddresses.SeabedRoute` and `MapVariant`. They keep their last value when the player leaves, so they are only meaningful while on the map, which is why they stay out of `conditions`.

The values in the table: `0x7E00` = place 1, `0x7E04` = place 5, `0x7E1E`, `0x7E21`, `0x7E24`, `0x7E25` = arg 1, 4, 7, 8.

### Party and partners

- **Party:** `s32` partner ids at 0x48DA4, 0x48DA8, 0x48DAC (`game_state.h:511`). An empty slot is -1, whose low byte is the 0xFF of `PartyAddresses.EmptySlotId`.
- **Partners:** `GAME.partners[i]` at 0x49490 + 0x3DC × i (`:517`; the 0x3DC agrees with `DigimonsAddresses.json`).
  - `unlocked`: `s32` at +4 (`:347`), so 0x49494 + 0x3DC × i. That is `MemoryBlockAddress - 8`.
  - level: `s16` at +0x28, `info.stats[STAT_LEVEL]` (`:349`, `:306`, `:279`). That is `MemoryBlockAddress + 0x1C`, the core's `Level`. It is the raw stat: no equipment or `computeStats`.
- **Index:** 0 Kotemon, 1 Kumamon, 2 Monmon, 3 Agumon, 4 Veemon, 5 Guilmon, 6 Renamon, 7 Patamon (`:464`).

## The SPECIAL ids used: 26

The EU table is `SPECIAL_CONDITIONS` (`events.c:732`), rows `{ id, check, arg }`. `checkSpecialCondition` (`:285`) splits `check`: the high nibble picks the function and the low nibble is `op`. It returns `expected == result`.

### Progress ranges

Check 0x30, `checkProgressRange` (`:182`): `min <= GAME.progress <= max`, both ends inclusive, from `PROGRESS_RANGES[arg]` (`:773`). 0x63 means past the end of the story. Gaps in the numbering are real: 0x2A and 0x2C are never set.

| id | Name | arg | Range |
|---|---|---|---|
| 0x09 | `SPECIAL_STORY_SINCE_GENJI_BEATEN` | 0x06 | 0x04-0x63 |
| 0x15 | `SPECIAL_STORY_SEIRYU_BADGE_TO_ZANBAMON_DRIVEN_OFF` | 0x0E | 0x05-0x0A |
| 0x16 | `SPECIAL_STORY_DE_SINCERITY_TO_LISA_SAVED` | 0x0F | 0x0F-0x12 |
| 0x17 | `SPECIAL_STORY_BYAKKO_LEADER_FOUGHT_TO_CONTROL_ROOM` | 0x10 | 0x14-0x16 |
| 0x18 | `SPECIAL_STORY_KNIGHTMON_REPROGRAMMED_TO_BULBMON_FOUND` | 0x11 | 0x18-0x1A |
| 0x19 | `SPECIAL_STORY_JUNGLE_SOUND_TO_AMATERASU_BRIDGE` | 0x12 | 0x1B-0x25 |
| 0x1A | `SPECIAL_STORY_ASUKA_HEADQUARTERS_TO_OPERATION_SIGMA` | 0x13 | 0x27-0x28 |
| 0x22 | `SPECIAL_STORY_GENJI_BEATEN_TO_PRESIDENT_ARRESTED` | 0x1A | 0x04-0x26 |
| 0x24 | `SPECIAL_STORY_TEDDY_FOUND_TO_PRESIDENT_ARRESTED` | 0x1C | 0x0E-0x26 |
| 0x3B | `SPECIAL_STORY_BYAKKO_BADGE_TO_PRESIDENT_ARRESTED` | 0x1D | 0x11-0x26 |
| 0x93 | `SPECIAL_STORY_SINCE_DE_SINCERITY` | 0x0C | 0x0F-0x63 |

### Party level totals

Check 0x15, `checkPartner` op 5 (`:102`): the sum of `stats[STAT_LEVEL]` over the party's members with an id >= 0, compared with `total >= arg * 15 + 30`.

| id | Name | arg | True at |
|---|---|---|---|
| 0x25 | `SPECIAL_PARTY_LEVEL_TOTAL_30` | 0 | >= 30 |
| 0x26 | `SPECIAL_PARTY_LEVEL_TOTAL_45` | 1 | >= 45 |
| 0x27 | `SPECIAL_PARTY_LEVEL_TOTAL_60` | 2 | >= 60 |
| 0x28 | `SPECIAL_PARTY_LEVEL_TOTAL_75` | 3 | >= 75 |
| 0x29 | `SPECIAL_PARTY_LEVEL_TOTAL_90` | 4 | >= 90 |

### Partners

Check 0x16, `checkPartner` op 6 (`:114`): `GAME.partners[arg].unlocked == 0`.

| id | Name | arg | Reads |
|---|---|---|---|
| 0x2A | `SPECIAL_AGUMON_LOCKED` | 3 | 0x4A028 |
| 0x2C | `SPECIAL_GUILMON_LOCKED` | 5 | 0x4A7E0 |
| 0x2D | `SPECIAL_PATAMON_LOCKED` | 7 | 0x4AF98 |
| 0x2E | `SPECIAL_KOTEMON_LOCKED` | 0 | 0x49494 |
| 0x2F | `SPECIAL_MONMON_LOCKED` | 2 | 0x49C4C |
| 0x30 | `SPECIAL_KUMAMON_LOCKED` | 1 | 0x49870 |
| 0x36 | `SPECIAL_RENAMON_LOCKED` | 6 | 0x4ABBC |

The other two:

- **0x0D `SPECIAL_VEEMON_UNLOCKED`:** check 0x11, arg 4, op 1 (`:80`): `GAME.partners[4].unlocked != 0`, reading 0x4A404.
- **0x95 `SPECIAL_ALL_PARTNERS_LEVEL_45`:** check 0x17, arg 0, op 7 (`:120`): true unless an unlocked partner has `stats[STAT_LEVEL] < 0x2D`. It walks all eight, party or not.

### The item set

**0x3A `SPECIAL_HAS_FISHING_POLE_PARTS`:** check 0x00, arg 0, `checkItemSet` op 0 (`:29`): items 7, 0x59 and 0xA7 are each in the bag or equipped.

## What the table does not say

- **The Folder Bag on the three Duel Islands.** The bag is in no list of `asukaTrophy`, `asukaRematch`, `amaterasuTrophy`, `amaterasuRematch` or `cardKing`, so the table says they can start without it. What stops the player is two characters standing in the way, present while `ITEM(0, 0x192)=0`: `wstag385.c:807-808`, `wstag386.c:702-703`, `wstag951.c:386-387`. Only their collision makes that a gate, and that comes from my floor fill, so by the rule for this table it stays out.
- **Whether the player can get there.** `superNova`, `asukaTrophy`, `asukaRematch` and both Amaterasu duels have no story gate in their lists, so they read true at any progress, even 0. `muramasa` reads true from 0x07. For when they really open, see `quest-availability.json`.
- **That the step is not already done.** `conditions` only has the flag when the game tests it. These never test their own first-step flag, so the talk can repeat: the seven DRI agents other than Veemon, `invincible` and `fishingPole`. The app has to skip a quest that has already started.
- **A way around a way.** `fishingPole` reads false while the player holds all three parts, because `SPECIAL_HAS_FISHING_POLE_PARTS=0` is on the talk that sets the flag. The next talk then gives the pole without ever setting it (`wstag340.c:219`). Only the talk that sets the flag counts as a way here.

## Corrections to what was expected

- **`eternally` has 11 Pharaohmon copies, not 3:** actors 6 to 16 (`wstag375.c:603-613`), plus the chest in the Circuit Board, so 12 ways. The windows are progress 0x04-0x0A, 0x0C, 0x0E-0x12, 0x14-0x16, 0x18-0x28, 0x2B, then the chest at 0x2D. `muramasa` has 25 ways.
- **`muramasa` has one more gap than `quest-availability.json` says:** no Zanbamon at 0x20 (`STORY_REAL_WORLD`), besides 0x0B, 0x0D, 0x13 and 0x17.

## Checks

**The generator stops if the curated table is wrong.** It reads each quest's flag from its first step in `Backend/Memory/Definitions/Quests`, alternatives included. It then checks that the curated talk, slot or event really sets it, following `START_EVENT` through `FIELDSTG_eventIds` to the event's end function. It also scans every stage for other setters of these flags. I broke the table four ways to test this, and each one was caught: a dropped copy (actor16), a wrong talk, a wrong slot and a dropped event.

**By hand against the C**, with a throwaway emulation of `checkCondition` on a synthetic RAM image (not in the repo):

| Quest | Table | Source | Behaviour checked |
|---|---|---|---|
| driAgentAgumon | `0x6004,0  0x8006,0  0x7022,1  0x7025,1  0x7027,0  0x702A,1`, intro `0x1A2D,1`; second way `0x602B,1  0x8006,0  0x702A,1  0x7025,1  0x7027,0`, same intro | `wstag405.c:362`, `:374`, `:239`, `:290` | Party total 29 false, 30 true, 59 true, 60 false. Agumon unlocked or DDNA held false. Progress 0x04 and 0x27 false, 0x05, 0x26 and 0x2B true. Two members adding to 30 with the third slot empty true |
| eternally | Each copy is its window plus `0x0A00,0`; chest `0x602D,1  0x027C,0  0x868D,0  0x868E,0  0x868F,0  0x8690,0`, place `0x7E00,1  0x7E25,1` | `wstag375.c:579-589`, `:400`; `wstag972.c:1070`, `:182` | Sweep matches the windows above. Setting the flag closes it at 0x05. At 0x2D only the chest flag closes it |
| asukaTrophy | `0x1A37,0  0x8012,0`, intro `0x11,1  0x10,1` | `wstag385.c:805`, `:612`, `:620-630` | Trophy held moves it to asukaRematch (`0x8012,1`). The flag set closes it |
| cardKing | `0x602D,1  0x1016,0`, intro `0x11,1  0x05,1  0x10,1` | `wstag951.c:385`, `:288`, `state.c:26` | False at 0x26, true at 0x2D, false once beaten |
| treeBoots | `0x1A22,0  0x8192,1`, character null | `wstag345.c:458`, `:12-15`, `:465` | No bag false, bag true, event done false |

**On real RAM**, the same evaluator ran over two snapshots of `Tools/MemoryScanner/Snapshots` at progress 0x16. Only `muramasa`, `driAgentVeemon`, `asukaTrophy` and `amaterasuTrophy` read open. That fits the save, where Veemon is the only partner still locked and the bag is held. `amaterasuTrophy` reads open only because the table holds no reachability. This is a smoke test, not a check of behaviour in the game.
