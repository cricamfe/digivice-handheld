import table from "@/handheld/card-table";
import type { CardTable } from "@/repositories/tables/tcg/card.table";
import type { CardRaw } from "@/repositories/tables/raws/tcg/card.raw";

export class CardRepository {
  private static readonly cardTable: CardTable = table;

  public static getCardById(cardId: string): CardRaw | undefined {
    return this.cardTable[cardId];
  }

  public static getCardTable(): CardTable {
    return this.cardTable;
  }

  public static getCardIds(): string[] {
    return Object.keys(this.cardTable);
  }
}
