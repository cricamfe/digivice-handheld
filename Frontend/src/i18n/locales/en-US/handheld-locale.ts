import seabed from "./seabed.json";
import enemy from "./enemy/enemy.json";
import species from "./enemy/specie.json";
import conditions from "./enemy/condition.json";
import drops from "./enemy/drop.json";
import cardType from "./tcg/card-type.json";
import regularAttacks from "./enemy/enemy-regular-attack.json";
// The enemies' techniques with the game's technique names as placeholders the app fills from the disc (game-words.ts).
import enemyTechniques from "./handheld-enemy-techniques.json";
import techniqueTypes from "./digievolution/technique-type.json";
import digimon from "./digimon.json";
import field from "./field.json";
import stat from "./stat.json";
import digivolution from "./digievolution/digievolution.json";
import journal from "./journal.json";
import party from "./party.json";
import auction from "./auction.json";
import npc from "./npc.json";
import npcs from "./npcs.json";
import handheld from "./handheld.json";
// The quest texts the handheld shows, with the game names as placeholders the app fills from the disc (quest-words.ts).
import quests from "./handheld-quests.json";

import fallbacks from "./handheld-fallbacks.json";

export default {
  ...fallbacks,
  ...seabed,
  ...enemy,
  ...species,
  ...conditions,
  ...drops,
  ...cardType,
  ...regularAttacks,
  ...enemyTechniques,
  ...techniqueTypes,
  ...digimon,
  ...field,
  ...stat,
  ...digivolution,
  ...journal,
  ...party,
  ...auction,
  ...npc,
  ...npcs,
  ...handheld,
  ...quests,
};
