import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { AppVersion, LicenceDocs, SourceUrl } from "../about";

describe("licences page", () => {
  it("shows the version of the Android build", () => {
    const gradle = readFileSync(
      new URL("../../../../Android/app/build.gradle.kts", import.meta.url),
      "utf8",
    );
    expect(gradle).toContain(`versionName = "${AppVersion}"`);
  });

  it("points at the public repository", () => {
    expect(SourceUrl).toBe("https://github.com/cricamfe/digivice-handheld");
  });

  it("ships the GPL, the NOTICE and the licences of what the app bundles", async () => {
    const texts = Object.fromEntries(
      await Promise.all(LicenceDocs.map(async (doc) => [doc.id, await doc.load()] as const)),
    );
    expect(Object.keys(texts)).toEqual(["gpl", "notice", "chd", "font"]);
    expect(texts.gpl).toContain("GNU GENERAL PUBLIC LICENSE");
    expect(texts.gpl).toContain("Version 3, 29 June 2007");
    expect(texts.notice).toContain("Copyright (C) 2026 Cristian Campos");
    expect(texts.notice).toContain("Jean Carlos Heck");
    expect(texts.chd).toContain("libchdr (BSD 3-Clause)");
    expect(texts.font).toContain("SIL Open Font License, Version 1.1");
  });
});
