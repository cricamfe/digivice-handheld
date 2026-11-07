import type { EventsMap } from "./events.map";

/**
 * Anything that can deliver typed game events: the native bridge on Android handhelds,
 * or a simulator during development.
 */
export interface GameEventSource {
  on<K extends keyof EventsMap>(eventName: K, handler: (data: EventsMap[K]) => void): void;
}
