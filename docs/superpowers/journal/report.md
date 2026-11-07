# Journal steps from the decompilation

`story-steps.json` holds the proposed steps with their file:line evidence in `dw3_decomp`. What went into the app differs in these points:

- **Main Quest:** steps 89 to 133, ending with Galacticmon. The player's live RAM (story stage 0x27) read 89 to 123 done and 124 onward not done.
- **Steps 134, 138 to 142** are the side quest `digimonOnline`, and **143 to 148** the Duel Island quest `cardKing`: they can be done in any order, and the Main Quest marks earlier steps done when a later one is.
- **Steps 135 and 136** are not steps: their chests are alternative marks of the first step of `eternally` and `muramasa`. **Step 137** is left out: the Legendary Weapons tab covers those items.
- **Steps 14 and 65 keep their flags.** Group 0x20 is not unused, as the report below says: `FIELD_VISITED_FLAG(mode)` sets it on entering a field (`field_mode.h:660`), so they read "entered 0x22D" and "entered 0x256". Step 61 does change, since 0x255 is a later room.
- **Step 60** uses the `alternative`, with the story stage. **Veemon's step 1** is `anyOf`, through `Alternatives`.
- **`orItem` on the trophy steps is not used:** `DuelIslandAssembler` already marks them done once the last step is.

# Story steps after the Game Master: report

Data: `story-steps.json` in this folder. It has 60 new main steps (89 to 148), 5 proposed chapters, 11 corrections, and the two Amaterasu Duel Island quests of 6 steps each. Every step lists file:line evidence in `refs/dw3_decomp` (EU lines, read line by line against the source). Nothing was run in the game or the app.

## How it was derived

**Flags (verified).** `address = 0x48D34 + group offset + (id >> 3)`, `mask = 1 << (id & 7)`, with the EU offsets of `game_state.h:555-567`. It gives the known pairs: FLAG(0x40,0x4F) = 0x4B3E7/0x80, FLAG(0x1C,0x55) = 0x4B3BF/0x20, FLAG(6,7) = 0x4B38C/0x80. Key items are at `0x48DB0 + id`, equipped ones at `0x48F43 + id`. Progress is the byte at 0x4B370. `StepReader` already has `AtLeast` for it.

**Coordinates (verified).** x, y are percent of the picture: pixel / picture size * 100. Checked on existing data: Keith 56.2, 34.5 against 56, 33; Game Master 74.3, 36.7 against 74.5, 36; the six Asuka duelists within 0.5; the Sewers trigger 49.0, 41.3 against 48.5, 41.5; Lisa 56.2, 50.8 against 56.5, 50.5; HiAndromon 73.3, 24.1 against 73, 23. Each step's `coordinateSource` says whether the point is an actor, a script point, or the centre of a trigger box read from the disc's trigger layer. Picture sizes match the stage's floor layer for every stage I checked, including the extra chapter's (925, 926, 927, 949, 954, 956, 951). For wstag972 the disc lookup failed, so its chest points rest on the main chapter twin's map (wstag885, same size as S885PACK).

## Inferred or unproven

1. **Guards on Amaterasu's Admin Center, steps 115 to 118.** The game does not say which guards block the way. I flood-filled the floor layer between doors with each guard as a 14 px disc. On Asuka's 2F this finds the troopers of steps 75 to 77, and the three Amaterasu guards stand at the same pixels. The radius is my assumption. I did not check Basement Stairs or B1F, since the lobby door leads straight to 1F.
2. **Panels of Gunslinger 1F, steps 125 to 127.** Each one removes a block. I did not prove all three are needed, because launch pads defeat the flood fill. Order is free.
3. **Order on the Gunslinger, steps 128 to 130.** Taken from event numbers only (1020, 1030, 1035). Nothing enforces it.
4. **Step 131's opponent.** Event 1050's text names nobody. Encounter 144 is the battle, but I could not tie it to a name. `enemy.json` lists 144 as Vilemon, and I could not prove its keys are encounter numbers, so the text says "the enemy".
5. **Step 105's order and need.** Order against step 104 comes from event numbers. No slot requires this fight.
6. **Trigger boxes can perhaps be walked around** (steps 92 to 95, 97 to 101, 111 to 114, 123, 128, 129). I only know they fire on stepping in. Only the Phoenix Bay Trooper was tested this way.
7. **Step 123 may be skipped.** Nothing later tests the flag, and I did not check whether the Seabed offers another way to Amaterasu.
8. **Chapter names and starts** are my wording. Step 89 finishes the Game Master's fight, so chapter 4 could end at 89 and chapter 5 start at 90.
9. **Duelist numbering (first to fourth Etemon)** is the walking order, as in the Asuka quests. The game calls them all Etemon.

## Marks that turn on early

These flags are set when the event before the battle ends, so they read true while the fight runs: steps 114 to 118, 128, 129, 131, 138 to 142. No event follows those wins, so there is no better mark. A loss goes to the title screen (`menu.c:558`) and a reload replaces RAM, so a wrong reading does not last (the reload part is reasoned, not tested).

Elsewhere I used the flag set by the event after the win (for example FLAG(0x40,0x50) for step 89).

Step 133 is the opposite: 0x2B is set as the last fight starts, so I used 0x2D, which comes after the ending movie. It turns on minutes after the win.

## Steps that can be skipped or reordered

- 98 to 101: free order. The dungeons test only the Digi-Egg of Knowledge, and the Chamber Officer tests only the Genbu Badge.
- 103 to 105: skipped if the 180 seconds run out. Step 106 holds on both paths.
- 108, 109: possible from STORY_JUNGLE_SOUND, so they can read done before the steps above them. Step 110 works on both paths (events 893 and 895).
- 135, 136: the chests only appear if the player holds neither the item nor a later form (`wstag972.c:1062`, `:1072`). A player who beat Pharaohmon or Zanbamon never sees them, so these steps stay open forever for them. They only appear at PLACE 1, PLACE_ARG 8. Mark them optional, or leave them out of "quest done".
- 138 to 141: free order.
- 143 to 148: optional, nothing later needs them. Decide if they belong in the Main Quest or in a quest of their own; I kept them last so they cannot block anything.

## Things the data needs from the app

- **New condition forms.** `allItems` (step 137, bag or equipped for five items), `orItem` (trophy steps 1 to 5), `anyOf` (Veemon step 1). `StepReader` reads a single address. Without them: step 137 has no mark at all, and Veemon step 1 can use just FLAG_DRI_BOB_TALKED.
- **Map ids missing from `map.json`**, listed with picture and name in `mapIdsMissingFromMapJson`: 0244, 0246, 026A to 026E, 02DA to 02DF, 02EC. Pictures exist for all but 02DE: there is no S815PACK, so steps 128, 129 and 131 have null coordinates and their pixel point in `coordinateSource`.
- **Step 137 has null location.** The forging is spread over the Circuit Board and Seabed and needs all partners unlocked at level 45. The Legendary Weapons quests only cover the first material.

## Findings on the corrections

- **Step 7: the flag is right, and the earlier audit was misled.** The comments in `story.h:1337` and `:1305` say Etemon, but event 101 is the post-win talk with Keith (`wstag205.c:113`, and the event's text). FLAG(0x40,6) turns on before the battle. Keep (0x40,0x16). Only the name changes: the game labels him Mystery Player, then Keith.
- **Step 28: I kept 023E as the location**, with new coordinates, because the player has no control in mode 0x204. The JSON has the Main Lobby point as well if you prefer the place where the flag is set.
- **Step 60: decide between two versions.** Keeping the flag means the text must include going back out to the factory. The `alternative` keeps the current text with progressAtLeast 0x12, which is more exact. I lean to the alternative, but gave the flag version first as you asked for text, location and coordinates.
- **Steps 14, 61, 65: no script touches group 0x20 at all** (`FLAG(0x20` has no match in `src/`), so those bits never change. Each has a real flag, in the JSON.
- **Step 14's flag turns on one room late**, in the gondola, because event 142 sets nothing.
- **Veemon step 1.** FLAG_DRI_BOB_TALKED holds at any party level. It does not hold on the extra chapter's way through Piledrimon, which never meets Bob. Veemon is in no starter pack, so `isStarterOwnedPattern` is always wrong for this quest.
- **Not in your list: `asukaTrophy` steps 1 to 5.** Winning the trophy clears their flags (`wstag385.c:415-419`). Step 1 then reads false and step 6 true, which is the same pattern, so the finished quest should show as unavailable until the rematch starts. From the code only; I did not see it in the app. The Amaterasu trophy steps carry `orItem` to avoid this.
- **Reset:** wstag385's reset does not touch the Amaterasu flags. wstag386 has its own reset, for its own five.

## Left out on purpose

- **Automatic events** have no step: 683 to 685, 742, 743, 885, 886, 891 to 895 (part of steps), 921, 922, 934, 935.
- **Event 730, Knightmon on the bridge:** a battle that fires if the player walks onto the bridge before having the Code Passes. The story does not need it.
- **Event 745, the Trooper at Phoenix Bay:** the flood fill says he does not block the way to Zhu Que City. `story.h:108` says that city opens with the pass, but nothing I found tests his flag.
- **Event 950, Airdramon:** an optional talk that needs a flag from Qing Long Tower (`wstag431.c:94`). It changes which NPCs stand in some rooms; no story event or slot I found depends on it.
- **Black Kingz and the DDNA battles of the extra chapter:** side content.

## Do not trust these names

Names in `story.h` describe the wrong event in several places; the JSON keeps them because they are the decomp's names:

- FLAG_ZHU_QUE_CHIEF_WIN_SEEN is Suzaku Leader at the hole.
- FLAG_TEDDY_FOREST_INN_SEEN is Kenny.
- FLAG_KNOWLEDGE_EGG_FOUND is the Zhu Que Chief.
- FLAG_ETEMON_MASK_QUESTION_SEEN is Keith.
- FLAG_MEGADEATH_TAUNT_SEEN is a Metal Soldier.
