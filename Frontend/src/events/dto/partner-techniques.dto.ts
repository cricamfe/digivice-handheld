/**
 * For each partner of the party, the Digimon in its slots and their `PartnerEntry.skills` as the RAM has them. Only the
 * Android core sends it (`PartnerTechniquesChanged`), with the full cycle, when it changes.
 */
export interface PartnerTechniquesDTO {
  partners: PartnerTechniquesPartnerDTO[];
}

export interface PartnerTechniquesPartnerDTO {
  digimonId: number;
  forms: { id: number; skills: number[] }[];
}
