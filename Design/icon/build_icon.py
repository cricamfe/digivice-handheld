#!/usr/bin/env python3
"""The app's icon, drawn pixel by pixel: a Digivice of our own, in the palette of the interface.

An adaptive icon is 108 dp with the 66 dp in the middle always shown. On a grid of 54 cells each density is a whole number of
pixels per cell, so no density blurs it. Writes the layers under Android/app/src/main/res, and a preview next to this file.

Usage (needs Pillow): python3 Design/icon/build_icon.py
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "Android/app/src/main/res"
GRID = 54
DENSITIES = {"mdpi": 2, "hdpi": 3, "xhdpi": 4, "xxhdpi": 6, "xxxhdpi": 8}

OUTLINE = "#040716"
BODY = "#e9eef8"
BODY_LIGHT = "#ffffff"
BODY_SHADE = "#a9b6d3"
BODY_DARK = "#6f7fa6"
RING = "#1b6fe0"
RING_LIGHT = "#7fe8ff"
RING_DARK = "#0b2a78"
GLASS = "#9ce26a"
GLASS_LIGHT = "#c9f59a"
GLASS_DARK = "#1c4a2a"
GOLD = "#ffd52e"
GOLD_DARK = "#b06a10"
RED = "#e8402c"
RED_DARK = "#7a1420"
BLUE = "#3fa9ff"
BLUE_DARK = "#0b2a78"
BACKGROUND = "#0b2a78"
BACKGROUND_DARK = "#071c56"


def rect(image, x0, y0, x1, y1, color):
    """Fills the cells from x0, y0 to x1, y1, both ends in."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            image.putpixel((x, y), hex_color(color))


def hex_color(color):
    return (int(color[1:3], 16), int(color[3:5], 16), int(color[5:7], 16), 255)


def rounded(image, x0, y0, x1, y1, cut, color):
    """A rectangle with its corners cut in steps of `cut` cells."""
    for step in range(cut):
        inset = cut - step
        rect(image, x0 + inset, y0 + step, x1 - inset, y0 + step, color)
        rect(image, x0 + inset, y1 - step, x1 - inset, y1 - step, color)
    rect(image, x0, y0 + cut, x1, y1 - cut, color)


def button(image, x, y, color, dark):
    rounded(image, x - 1, y - 1, x + 4, y + 4, 1, OUTLINE)
    rounded(image, x, y, x + 3, y + 3, 1, dark)
    rect(image, x, y, x + 2, y + 2, color)
    image.putpixel((x, y), hex_color(dark))
    image.putpixel((x + 1, y), hex_color(BODY_LIGHT))


def foreground():
    image = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))

    # The antenna, over the body's right shoulder.
    rect(image, 33, 9, 38, 14, OUTLINE)
    rect(image, 34, 10, 37, 14, RING)
    rect(image, 34, 10, 35, 14, RING_LIGHT)

    # The body, with light on its top left and shade on its bottom right.
    rounded(image, 12, 13, 41, 42, 4, OUTLINE)
    rounded(image, 13, 14, 40, 41, 4, BODY_SHADE)
    rounded(image, 13, 14, 38, 39, 4, BODY)
    rect(image, 18, 15, 33, 15, BODY_LIGHT)
    rect(image, 15, 18, 15, 33, BODY_LIGHT)
    rect(image, 19, 41, 34, 41, BODY_DARK)
    rect(image, 40, 19, 40, 34, BODY_DARK)

    # The ring around the screen.
    rounded(image, 15, 18, 33, 36, 3, OUTLINE)
    rounded(image, 16, 19, 32, 35, 3, RING_DARK)
    rounded(image, 16, 19, 31, 34, 3, RING)
    rect(image, 19, 19, 28, 19, RING_LIGHT)
    rect(image, 16, 22, 16, 31, RING_LIGHT)

    # The screen.
    rounded(image, 18, 21, 30, 33, 1, OUTLINE)
    rect(image, 19, 22, 29, 32, GLASS)
    rect(image, 19, 22, 29, 22, GLASS_LIGHT)
    rect(image, 19, 22, 19, 32, GLASS_LIGHT)

    # On the screen, a Digi-Egg with its spots.
    for y, (left, right) in enumerate(
        [(23, 25), (22, 26), (21, 27), (21, 27), (21, 27), (21, 27), (22, 26), (23, 25)]
    ):
        rect(image, left, 24 + y, right, 24 + y, GLASS_DARK)
    for x, y in [(23, 26), (25, 27), (22, 28), (24, 29), (26, 29)]:
        image.putpixel((x, y), hex_color(GLASS))

    # Three buttons on its right, the game's colours.
    button(image, 35, 20, GOLD, GOLD_DARK)
    button(image, 35, 26, RED, RED_DARK)
    button(image, 35, 32, BLUE, BLUE_DARK)
    return image


def background():
    """Plain blue with the checks of the interface, so the device stands out on any launcher."""
    image = Image.new("RGBA", (GRID, GRID), hex_color(BACKGROUND))
    for y in range(0, GRID, 6):
        for x in range(0, GRID, 6):
            if (x + y) % 12 == 0:
                rect(image, x, y, x + 5, y + 5, BACKGROUND_DARK)
    return image


def write(image, name):
    for density, cell in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        image.resize((GRID * cell, GRID * cell), Image.NEAREST).save(folder / f"{name}.png")


def main():
    front, back = foreground(), background()
    write(front, "ic_launcher_foreground")
    write(back, "ic_launcher_background")
    (RES / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
    (RES / "mipmap-anydpi-v26/ic_launcher.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@mipmap/ic_launcher_background" />\n'
        '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
        "</adaptive-icon>\n",
        encoding="utf8",
    )
    # What a launcher shows: the 66 dp in the middle and a little more.
    preview = Image.alpha_composite(back, front).crop((8, 8, 46, 46)).resize((380, 380), Image.NEAREST)
    preview.save(Path(__file__).parent / "preview.png")


if __name__ == "__main__":
    main()
