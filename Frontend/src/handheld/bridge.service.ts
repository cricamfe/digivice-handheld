import type { EventsMap } from "@/events/events.map";
import type { GameEventSource } from "@/events/event-source";
import { createLogger } from "@/events/logger";

const bridgeLogger = createLogger("Bridge", "#e67e22");

type HandlersMap = { [K in keyof EventsMap]?: ((data: EventsMap[K]) => void)[] };

// Compile-time exhaustive: adding an event to EventsMap forces a new entry here.
const knownEventTypes: Record<keyof EventsMap, true> = {
  HealthChanged: true,
  InitialState: true,
  PlayerChanged: true,
  PlayerPositionChanged: true,
  FieldActorsChanged: true,
  FieldChanging: true,
  PartnerTechniquesChanged: true,
  QuestOffersChanged: true,
  ImportantItemsChanged: true,
  PartyChanged: true,
  DigimonBattleChanged: true,
  CardBattleChanged: true,
  AuctionsChanged: true,
  NpcsChanged: true,
  JournalChanged: true,
  HubConnectionStatusChanged: true,
};

function isKnownEventType(eventType: string): eventType is keyof EventsMap {
  return Object.hasOwn(knownEventTypes, eventType);
}

/**
 * Event source for the Android handheld: the native side pushes events through
 * `window.__digivice.receive(eventType, payloadJson)`.
 */
class BridgeService implements GameEventSource {
  private handlers: HandlersMap = {};

  public on<K extends keyof EventsMap>(eventName: K, handler: (data: EventsMap[K]) => void): void {
    const eventHandlers: ((data: EventsMap[K]) => void)[] = this.handlers[eventName] ?? [];
    eventHandlers.push(handler);
    // TS cannot correlate the generic K on indexed writes to a mapped type.
    this.handlers[eventName] = eventHandlers as HandlersMap[K];
  }

  /**
   * Entry point for native events. Never throws: an unknown type or malformed
   * payload is logged and ignored so the native caller is never affected.
   */
  public receive(eventType: string, payloadJson: string): void {
    try {
      if (!isKnownEventType(eventType)) {
        bridgeLogger.warn(`Ignored unknown event type: ${String(eventType)}`);
        return;
      }

      let payload: unknown;
      try {
        payload = JSON.parse(payloadJson);
      } catch (err) {
        bridgeLogger.warn(`Ignored ${eventType}: malformed JSON payload.`, err);
        return;
      }

      bridgeLogger.debug(`Bridge Event [${eventType}]`, payload);
      this.emit(eventType, payload as EventsMap[typeof eventType]);
    } catch (err) {
      bridgeLogger.error(`Unexpected failure handling ${String(eventType)}.`, err);
    }
  }

  public emit<K extends keyof EventsMap>(eventName: K, data: EventsMap[K]): void {
    const eventHandlers: ((data: EventsMap[K]) => void)[] | undefined = this.handlers[eventName];
    eventHandlers?.forEach((handler) => {
      try {
        handler(data);
      } catch (err) {
        bridgeLogger.error(`Handler for ${eventName} failed.`, err);
      }
    });
  }
}

export const bridgeService = new BridgeService();

declare global {
  interface Window {
    __digivice?: {
      receive(eventType: string, payloadJson: string): void;
    };
    DigiviceNative?: {
      setMapOnScreen?: (shown: boolean) => void;
      onReady?(): void;
      /** Sends the overlay to the background. */
      hide?(): void;
      /** Closes the whole app. */
      exit?(): void;
      /** Only the launcher activity (upper screen) has it: opens the system screen for the overlay permission. */
      openSettings?(): void;
      /** Only the setup activity (upper screen) has it: runs the preparation button's action. */
      setupAction?(action: string): void;
    };
  }
}

/** Exposes `window.__digivice.receive` for the native side. */
export function installBridge(): void {
  window.__digivice = {
    receive: (eventType, payloadJson) => bridgeService.receive(eventType, payloadJson),
  };
}

/** Tells the native side that handlers are in place, so it can send InitialState. */
export function signalReady(): void {
  try {
    window.DigiviceNative?.onReady?.();
  } catch (err) {
    bridgeLogger.error("DigiviceNative.onReady failed.", err);
  }
}
