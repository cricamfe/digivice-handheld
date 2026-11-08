<div align="center">

<img src="https://github.com/cricamfe/digivice-handheld/releases/download/v0.1.0/banner.png" width="100%" alt="Digivice for AYN Thor: a dual-screen handheld with a game on the top screen and the Digivice map on the bottom screen">

# Digivice for AYN Thor

**The second screen for Digimon World 2003, on your dual-screen Android handheld.**

The game on top; your party, the map and your next objective below. Live, in pixel art, and read-only.

[![Android 13+](https://img.shields.io/badge/Android-13%2B-3ddc84?style=flat-square&logo=android&logoColor=white)](#requirements)
[![RetroArch + SwanStation](https://img.shields.io/badge/RetroArch-SwanStation-1b6fe0?style=flat-square)](#requirements)
[![SLES-03936](https://img.shields.io/badge/game-SLES--03936%20(EU)-7f32f2?style=flat-square)](#requirements)
[![5 languages](https://img.shields.io/badge/languages-EN%20%C2%B7%20ES%20%C2%B7%20FR%20%C2%B7%20DE%20%C2%B7%20IT-d12fde?style=flat-square)](#what-you-get)
[![License GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square)](LICENSE)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-support-ff5e5b?style=flat-square&logo=ko-fi&logoColor=white)](https://ko-fi.com/cricamfe)

<img src="https://github.com/cricamfe/digivice-handheld/releases/download/v0.1.0/thor-map.webp" width="420" alt="An AYN Thor with the game on the top screen and Digivice on the bottom screen, whose live map follows the player across Asuka Bridge and switches to Central Park when they cross into it">

[What you get](#what-you-get) · [Requirements](#requirements) · [Install](#install) · [Build](#build) · [Credits](#credits)

</div>

> [!NOTE]
> Tested with RetroArch nightly + SwanStation on an AYN Thor (Android 13), the only device tested so far.

Based on **[Digivice by Jean Carlos Heck](https://github.com/jeanheck/digivice)**, the companion for Windows and DuckStation, rebuilt for Android with a touch interface.

## What you get

<table>
  <tr>
    <td width="50%" valign="top" align="center">
      <img src="https://github.com/cricamfe/digivice-handheld/releases/download/v0.1.0/dual-journal.png" width="100%" alt="Top screen: the game in a city. Bottom screen: Digivice's journal with the chapter objective and the quests on offer"><br>
      <b>Journal</b> · the next objective and the quests on offer, without pausing the game.
    </td>
    <td width="50%" valign="top" align="center">
      <img src="https://github.com/cricamfe/digivice-handheld/releases/download/v0.1.0/thor-battle.webp" width="100%" alt="Top screen: a battle against Triceramon. Bottom screen: switching partners shows which attack is very effective"><br>
      <b>Battle</b> · the enemy's weak points and how well each attack lands.
    </td>
  </tr>
</table>

<table>
  <tr>
    <td width="25%" valign="top" align="center">
      <img src="https://github.com/cricamfe/digivice-handheld/releases/download/v0.1.0/screen-party.png" width="100%" alt="Party screen"><br>
      <b>Party</b><br><sub>Stats, resistances, equipment.</sub>
    </td>
    <td width="25%" valign="top" align="center">
      <img src="https://github.com/cricamfe/digivice-handheld/releases/download/v0.1.0/screen-map.png" width="100%" alt="Map screen"><br>
      <b>Map</b><br><sub>You, the characters and exits.</sub>
    </td>
    <td width="25%" valign="top" align="center">
      <img src="https://github.com/cricamfe/digivice-handheld/releases/download/v0.1.0/screen-evol.png" width="100%" alt="Digivolution tree screen"><br>
      <b>Digivolutions</b><br><sub>Every form and its requirements.</sub>
    </td>
    <td width="25%" valign="top" align="center">
      <img src="https://github.com/cricamfe/digivice-handheld/releases/download/v0.1.0/screen-wiki.png" width="100%" alt="Wiki screen"><br>
      <b>Wiki</b><br><sub>Enemies, cards, items, places.</sub>
    </td>
  </tr>
</table>

Also cards and auctions, the Mobius Desert and Seabed routes, in five languages with the game's own names.

- **Read-only:** it reads the game's memory through RetroArch; it never writes to it or touches your saves.
- **No game files included:** maps, sprites and text are generated on your console from your own copy.

## Requirements

| | |
| --- | --- |
| **Console** | AYN Thor, Android 13 or later. |
| **Game** | Digimon World 2003, European release (`SLES-03936`), your own copy as ISO, single-track BIN or CHD v5. |
| **Emulator** | RetroArch **nightly** ARM64 from 2026-10-25 or later, with the SwanStation core. |

> [!WARNING]
> Stable RetroArch (1.22.2 and older) does not work, and a new nightly may break memory reads. Keep a copy of a nightly that works.

## Install

1. **Enable RetroArch's network commands.** With RetroArch closed, add to `retroarch.cfg`:
   ```ini
   network_cmd_enable = "true"
   network_cmd_port = "55355"
   network_cmd_bind_address = "127.0.0.1"
   ```
2. **Install the APK** from the [latest release](../../releases/latest).
3. **Open Digivice and choose your copy of the game.** It prepares its resources on the console, in about two minutes from a CHD. Grant **Display over other apps** when asked.
4. **Play.** Start the game in RetroArch; Digivice appears on the bottom screen. Long-press it for language, hide, exit and licences.

**Known limits:** tested on the Thor only, without a full campaign yet; some journal steps are inferred; Flawe Mod 2.0 is recognised (prepare from the original disc anyway) and other mods are untested; no North American or Japanese version.

## Build

<details>
<summary><b>Build, tests and signing</b></summary>

<br>

You need Node.js (`^20.19.0` or `>=22.12.0`), the Android SDK with platform 37, NDK 28.2.13676358, CMake 3.22.1 and a JDK (Android Studio's works).

```sh
git submodule update --init
(cd Frontend && npm ci && npm run test:handheld)
cd Android && ./gradlew :core:test :extractor:test :app:assembleRelease
```

Tests that need a disc or RAM dumps are skipped when those files are missing; they are never in the repository.

Without configuration, `release` is signed with the debug key. To use your own, create a keystore outside the repository and point `Android/keystore.properties` (ignored by git) at it with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`.

More details in the [Android technical guide](Android/README.md) (in Spanish).

</details>

## Credits

<table>
  <tr>
    <td width="50%" valign="top">
      <b><a href="https://github.com/cricamfe">Cristian Campos</a></b> · author of this Android version<br>
      Android port, dual-screen interface, disc extractor and setup wizard.<br>
      <a href="https://ko-fi.com/cricamfe">Support this version on Ko-fi</a>
    </td>
    <td width="50%" valign="top">
      <b><a href="https://github.com/jeanheck/digivice">Jean Carlos Heck</a></b> · author of the original Digivice<br>
      The domain core, many tables, presenters and reference sources come from his work, and his history is kept in this repository. He authorised this Android version.<br>
      <a href="https://ko-fi.com/R5R028MD8Y">Support Jean on Ko-fi</a>
    </td>
  </tr>
</table>

Also thanks to Digivice's data sources (listed in [`NOTICE`](NOTICE)), [dw3_decomp](https://github.com/ReGame-Labs/dw3_decomp) by ReGame Labs, [ddw3](https://github.com/markisha64/ddw3) by markisha64, [libchdr](https://github.com/rtissera/libchdr), the Press Start 2P font, and Libretro, RetroArch and SwanStation.

## License

[GNU GPL v3.0 or later](LICENSE). If you distribute a modified version, keep the credits in [`NOTICE`](NOTICE) and say what you changed.

## Legal

Unofficial fan project, not affiliated with Bandai Namco, AYN or the emulator authors. Digimon and Digimon World 2003 belong to their owners. This repository and the APK contain no game files: each user generates them on their own console from their own copy.
