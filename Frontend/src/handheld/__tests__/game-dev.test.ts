import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { mkdtemp, mkdir, rm, symlink, writeFile } from "node:fs/promises";
import { createServer as createHttpServer, request } from "node:http";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createServer, type ViteDevServer } from "vite";
import config, { gameDevAssets } from "../../../vite.handheld.config";

describe("private development game files", () => {
  let temporary: string;
  let vite: ViteDevServer;
  let http: ReturnType<typeof createHttpServer>;
  let port: number;

  beforeAll(async () => {
    temporary = await mkdtemp(join(tmpdir(), "digivice-game-dev-"));
    const root = join(temporary, "game-dev");
    await mkdir(join(root, "npc"), { recursive: true });
    await writeFile(join(root, "manifest.json"), '{"version":1,"assets":{}}');
    await writeFile(join(root, "owned.png"), "synthetic png");
    await writeFile(join(root, "npc", "Señor Bob.webp"), "synthetic webp");
    await writeFile(join(temporary, "outside.json"), "private outside data");
    await symlink(join(temporary, "outside.json"), join(root, "escape.json"));
    await symlink(temporary, join(root, "outside"));
    vite = await createServer({
      configFile: false,
      root: temporary,
      plugins: [gameDevAssets(root)],
      appType: "custom",
      server: { middlewareMode: true, watch: null },
      optimizeDeps: { noDiscovery: true },
    });
    vite.middlewares.use((_req, res) => {
      res.end("app fallback");
    });
    http = createHttpServer(vite.middlewares);
    await new Promise<void>((resolve) => http.listen(0, "127.0.0.1", resolve));
    const address = http.address();
    if (!address || typeof address === "string") throw new Error("Missing test listener");
    port = address.port;
  });

  afterAll(async () => {
    http?.closeAllConnections();
    if (http)
      await new Promise<void>((resolve, reject) =>
        http.close((error) => (error ? reject(error) : resolve())),
      );
    await vite?.close();
    if (temporary) await rm(temporary, { recursive: true, force: true });
  });

  function get(path: string, method = "GET") {
    return new Promise<{ status: number; type: string | undefined; body: string }>(
      (resolve, reject) => {
        request({ hostname: "127.0.0.1", port, path, method }, (response) => {
          let body = "";
          response.setEncoding("utf8");
          response.on("data", (data) => {
            body += data;
          });
          response.on("end", () =>
            resolve({ status: response.statusCode!, type: response.headers["content-type"], body }),
          );
        })
          .on("error", reject)
          .end();
      },
    );
  }

  it("serves JSON, PNG and encoded Unicode WebP paths without rewriting filenames", async () => {
    expect(await get("/game/manifest.json?cache=1")).toMatchObject({
      status: 200,
      type: "application/json",
      body: '{"version":1,"assets":{}}',
    });
    expect(await get("/game/owned.png")).toMatchObject({
      status: 200,
      type: "image/png",
      body: "synthetic png",
    });
    expect(await get("/game/npc/Se%C3%B1or%20Bob.webp")).toMatchObject({
      status: 200,
      type: "image/webp",
      body: "synthetic webp",
    });
    expect(await get("/game/owned.png", "HEAD")).toMatchObject({
      status: 200,
      type: "image/png",
      body: "",
    });
  });

  it("responds with a real 404 instead of the page fallback for missing or unsupported files", async () => {
    for (const path of [
      "/game",
      "/game/",
      "/game/missing.png",
      "/game/npc",
      "/game/file.html",
      "/game/manifest.json/",
    ]) {
      expect(await get(path), path).toMatchObject({ status: 404, body: "" });
    }
    expect(await get("/game/manifest.json", "POST")).toMatchObject({ status: 404, body: "" });
  });

  it("confines decoded paths and symlink targets to game-dev", async () => {
    for (const path of [
      "/game/../outside.json",
      "/game/%2e%2e/outside.json",
      "/game/npc/%2e%2e/%2e%2e/outside.json",
      "/game/%2f../outside.json",
      "/game/%5c../outside.json",
      "/game/%00.json",
      "/game/%ZZ.json",
      "/game//manifest.json",
      "/game/escape.json",
      "/game/outside/outside.json",
    ]) {
      expect(await get(path), path).toMatchObject({ status: 404, body: "" });
    }
  });

  it("leaves other application routes to Vite", async () => {
    expect(await get("/other-route")).toMatchObject({ status: 200, body: "app fallback" });
  });

  it("preserves only the actual owned icons and UI directories in the bundle", () => {
    const output = config.build?.rollupOptions?.output;
    if (!output || Array.isArray(output) || typeof output.assetFileNames !== "function")
      throw new Error("Missing output naming rule");
    const assetFileNames = output.assetFileNames;
    const name = (sources: string[]) =>
      assetFileNames({
        type: "asset",
        name: undefined,
        names: ["image.svg"],
        originalFileName: null,
        originalFileNames: sources,
        source: "synthetic own image",
      });
    expect(name(["src/handheld/assets/icons/unknown.svg"])).toBe("icons/[name]-[hash][extname]");
    expect(name(["/checkout/Frontend/src/handheld/assets/ui/compass.svg"])).toBe(
      "ui/[name]-[hash][extname]",
    );
    expect(name(["src/handheld/assets/actors/1.png"])).toBe("assets/[name]-[hash][extname]");
    expect(
      name(["src/handheld/assets/icons/unknown.svg", "src/handheld/assets/actors/1.png"]),
    ).toBe("assets/[name]-[hash][extname]");
    expect(gameDevAssets().apply).toBe("serve");
    expect(config.build?.assetsInlineLimit).toBe(0);
  });
});
