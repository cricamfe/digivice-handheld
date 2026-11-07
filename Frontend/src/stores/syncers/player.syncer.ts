import type { Player } from "@/models";
import type { PlayerDTO } from "@/events/dto/player.dto";
import type { PlayerPositionDTO } from "@/events/dto/player-position.dto";

export class PlayerSyncer {
  public static syncPosition(previousPlayer: Player, position: PlayerPositionDTO): void {
    previousPlayer.positionX = position.x;
    previousPlayer.positionY = position.y;
  }

  public static sync(previousPlayer: Player, newPlayerDto: PlayerDTO): void {
    if (newPlayerDto.bits !== undefined) {
      previousPlayer.bits = newPlayerDto.bits;
    }
    if (newPlayerDto.mapId !== undefined) {
      previousPlayer.mapId = newPlayerDto.mapId;
    }
    if (newPlayerDto.previousMapId !== undefined) {
      previousPlayer.previousMapId = newPlayerDto.previousMapId;
    }
    if (newPlayerDto.seabedRoute !== undefined) {
      previousPlayer.seabedRoute = newPlayerDto.seabedRoute;
    }
    if (newPlayerDto.mapVariant !== undefined) {
      previousPlayer.mapVariant = newPlayerDto.mapVariant;
    }
    if (newPlayerDto.positionX !== undefined) {
      previousPlayer.positionX = newPlayerDto.positionX;
    }
    if (newPlayerDto.positionY !== undefined) {
      previousPlayer.positionY = newPlayerDto.positionY;
    }
    if (newPlayerDto.extraChapter !== undefined) {
      previousPlayer.extraChapter = newPlayerDto.extraChapter;
    }
  }
}
