/** Where the source code of this version is published, as the GPL asks the app to say. */
export const SourceUrl = "https://github.com/cricamfe/digivice-handheld";

/** The version of the app, from `versionName` of the Android build. */
export const AppVersion: string = __APP_VERSION__;

/** A licence text the app ships, read only when it is opened. */
export interface LicenceDoc {
  readonly id: string;
  /** Its name, the same in every language. */
  readonly name: string;
  readonly load: () => Promise<string>;
}

const text = (module: Promise<{ default: string }>): Promise<string> =>
  module.then((loaded) => loaded.default);

/** The licence of the app, its credits and the licences of what it bundles, in that order. */
export const LicenceDocs: readonly LicenceDoc[] = [
  { id: "gpl", name: "GNU GPL v3", load: () => text(import("../../../LICENSE?raw")) },
  { id: "notice", name: "NOTICE", load: () => text(import("../../../NOTICE?raw")) },
  {
    id: "chd",
    name: "libchdr",
    load: () => text(import("../../../Android/app/src/main/assets/licenses/chd.txt?raw")),
  },
  {
    id: "font",
    name: "Press Start 2P",
    load: () =>
      text(import("../../../Android/app/src/main/assets/licenses/press-start-2p.txt?raw")),
  },
];
