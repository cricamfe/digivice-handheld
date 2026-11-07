import type { NpcPartyMemberRaw } from "./npc-party-member.raw";
import type { MainQuestAvailabilityWindowRaw } from "@/repositories/tables/raws/quest/main-quest-availability-window.raw";
import type { NpcType } from "@/types/npc-type.type";

export interface NpcRaw {
  type: Exclude<NpcType, "tamer">;
  locationId: string;
  /** The character of the field that draws them, by character and direction (`57-1`, actors/57-1). */
  fieldActor?: string | null;
  mainQuestAvailabilityWindow?: MainQuestAvailabilityWindowRaw;
  party: NpcPartyMemberRaw[];
  exp: number;
  dvexp: number;
  bit: number;
}
