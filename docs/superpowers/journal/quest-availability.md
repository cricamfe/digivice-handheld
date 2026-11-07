# When each quest's first step can really be done

Data: `availability.json`, same folder. Lines are EU, in `refs/dw3_decomp`. Nothing was run in the game or the app.

## How it was derived

- **Scripts.** Each step's address was turned into its flag and every setter was found. I then read the actor's entry conditions and the talk's or slot's conditions.
- **Getting there.** Slot and Seabed/Circuit Board conditions were walked as a graph across all stages.
- **Floors.** Where the scripts set no gate, I flood-filled the disc's floor and trigger layers, using `render/tools` from an earlier session, read only. Present actors blocked walking, with collision sizes from `FIELDSTG_actorWidths`. This is my own model of walking: a diamond of half-width `width >> 1`, grown by 8 px for the player's facing tile. `verdict: inferred` means the number depends on this fill, not on a script condition alone.
- **Step mapping.** Each STORY value is tied to the main step that marks it, or to the nearest earlier step that forces it:
  - 0x05 → step 4. The flag turns on at the start of the fight (`wstag420.c:30`), and the progress follows the win (`:41`).
  - 0x0E → step 36. `wstag305.c:88`, then events 351 and 352 play by themselves (`wstag305.c:36`, `wstag212.c:143`, `:187`).
  - 0x0F → step 43.
  - 0x11 → step 48. The flag is set after event 430 (`wstag655.c:33`), the progress after 431 (`:42`).
  - 0x1B → step 93, 0x27 → step 122, 0x2D → step 133.

## What each first step really asks

| Quest | Flag | What the player does |
|---|---|---|
| folderBag | item 0x192 | Talk once to Divermon in the Yellow Cruiser; the event gives the bag |
| fishingPole | FLAG(0x1A,0x21) | Talk to Tai Kong Wang twice, without holding all three parts |
| treeBoots | FLAG(0x1A,0x22) | Holding the bag, step on the trigger beside Soccer Kid Hide; no talk |
| digimonOnline | FLAG(0x40,0xCD) | Enter the Login Room; the event plays by itself |
| eternally | FLAG(0xA,0) | Talk to Pharaohmon; the flag turns on as the fight starts |
| invincible | FLAG(0xA,1) | Talk to MasterTyranno after the Seiryu Leader refused; same talk as main step 3 |
| muramasa | FLAG(0xA,2) | Talk to Zanbamon in the Catacomb; the flag turns on as the fight starts |
| punishment | FLAG(0xA,4) | Talk to HiAndromon after step 59; the flag turns on as the fight starts |
| superNova | FLAG(0xA,3) | Talk to Datamon in the Control Room; the flag turns on as the fight starts |
| DRI (not Veemon) | FLAG(6,n) | Talk twice, the second time with the party level total in range; that talk asks for the DDNA |
| driAgentVeemon | FLAG(0x1A,0x29) | Talk once to DRI Bob, at any level |
| asukaTrophy | FLAG(0x1A,0x37) | Beat the first Divermon at cards; set on the win |
| amaterasuTrophy | FLAG(0x1A,0x3C) | Beat the first Etemon at cards; set on the win |
| rematches | same flags | Same battles with the trophy held (other actors, same spot) |
| cardKing | FLAG(0x10,0x16) | Beat Tamer Haruka at cards; two talks, then the battle |

The app's text for the DRI first step ("Look for Dri X") is not what its flag marks. Meeting him sets only the `_TALKED` flag; the step's flag needs the second talk in the level range.

## Things the app has wrong or missing

- **driAgentPatamon, driAgentRenamon.** The requisite `submarimon` (step 43) is later than the game's point, step 36 (STORY_TEDDY_FOUND).
- **Party level ranges.** The game tests the sum of the party's levels and so does the app, but these windows differ:
  - Agumon, Guilmon: game 30 to 59, app 30-45.
  - Patamon, Renamon: game 45 to 74, app 45-60.
  - Kotemon, Kumamon, Monmon: game 60 to 89, app 60-90 (off only at exactly 90).
  - Above the window the talk only gives a brush-off. So a player at 50 can do Agumon's step while the app shows the requisite as not met.
- **Folder Bag on Duel Island.** asukaTrophy, amaterasuTrophy and cardKing have no requisite for it. The fill says the first duelist cannot be reached without it, on all three islands.
- **Muramasa's map.** The step's location is 0243, but from progress 0x1A Zanbamon's Catacomb is map 0244 (`wstag530.c:45`, `wstag537.c`).
- **Amaterasu's i18n** names `etemonMike` as a requisite and the database has none. That is right for step 1: only the fourth Etemon asks for the mic (item 0x18D, `wstag386.c:289`).

## Uncertain

1. **muramasa = 25 rests on the fill.** The scripts put Zanbamon in the Catacomb from progress 0x07. Only his actor in the Jungle Grave keeps the player out, and he stays until step 25's talk (`wstag480.c:439`, `:431`). In the fill his collision closes the way to Phoenix Bay at every player size tried above zero: 8, 16 and 24 px of extra margin. At exactly 0 extra it is open, so a real player's collision would have to be zero for this to be wrong. The story text agrees. No script line tests it.
2. **superNova = 43 rests on the fill.** The scripts allow Datamon at any progress. The upper part of the Asuka Sewers (door from the Underground Path, Mischievous Mat) is 48 px from the rest in the floor layer, with no slot joining them, so the Control Room door is reached only from the Seabed or from Admin B1F. That agrees with the app's requisite. I did not rule out a slide or object not in the slot table, though none is listed.
3. **amaterasuTrophy = 94 rests on the fill.** First access to the Amaterasu server is through the Bug Maze exit (`wstag540.c:207`). The fill says that exit needs the way through the Pit, and that event 710's trigger spans the Pit. The same holds for event 720 in Forest Inn BF. Step 93 is the fight; step 94 is the event that cannot be walked around. If a trigger can be skirted, the true number is 92 or 93. The earlier report's warning about trigger boxes applies.
4. **`until: 125` is a nearest step, not an exact one.** The real point of no return is boarding the Gunslinger (`wstag311.c:1204`), since stages 810, 815 and 820 have no exit off the ship. No step marks boarding: step 124 (progress 0x28) still lets the player go back, and step 125 is set just inside. A player between the two sees the quest as still open.
5. **treeBoots at progress 0x27-0x28.** Hide's actor is not on the map then, while the trigger still fires. The script places him itself (`wstag345.c:65`), but I did not check how a place command behaves with no such actor. If it misbehaves, `until` should be 122.
6. **Gaps, not `until`.** folderBag (steps 122 to 133), eternally and muramasa (125 to 133) close and reopen. They have `until: null` and the gap in `otherConditions`. The app may want to show them as closed there.
7. **After step 133 the first-step flags stop being settable for many quests,** though the reward may still come another way: WSTAG942 gives the pole, WSTAG943 the boots, and partners unlock in the Circuit Board. Only eternally, muramasa, folderBag and Veemon have a route that also marks step 1. I did not trace the other partner unlocks, as they do not touch these flags.
8. **Yellow Cruiser access at progress 0x14-0x17.** The Asuka gate is closed from outside then (`wstag200.c:996`). Not an `until`, as the player is routed back in by the story.
9. **Floor fill scope.** I checked the rooms holding each first step and the doors leading to them, not every room on the way. Rooms on the way have unconditional exits in the slot tables, but a story-gated actor there could still block.
