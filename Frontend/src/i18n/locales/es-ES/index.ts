import seabed from "./seabed.json";
import enemy from "./enemy/enemy.json";
import species from "./enemy/specie.json";
import conditions from "./enemy/condition.json";
import drops from "./enemy/drop.json";
import cardType from "./tcg/card-type.json";
import techniqueTypes from "./digievolution/technique-type.json";
import digimon from "./digimon.json";
import field from "./field.json";
import stat from "./stat.json";
import digivolution from "./digievolution/digievolution.json";
import journal from "./journal.json";
import party from "./party.json";
import auction from "./auction.json";
import npc from "./npc.json";
import handheld from "./handheld.json";

// The interface text. Names and descriptions of the game and the quests are in `handheld-locale.ts`.
export default {
  ...seabed,
  ...enemy,
  ...species,
  ...conditions,
  ...drops,
  ...cardType,
  ...techniqueTypes,
  ...digimon,
  ...field,
  ...stat,
  ...digivolution,
  ...journal,
  ...party,
  ...auction,
  ...npc,
  ...handheld,
};
