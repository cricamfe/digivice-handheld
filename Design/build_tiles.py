"""Mobius Desert terrain tiles, 27x27, meant to be shown at 54x54."""
import os
import random
import sys

SIZE = 27
out = sys.argv[1]
os.makedirs(out, exist_ok=True)


def save(name, cells):
    rects = []
    for y in range(SIZE):
        x = 0
        while x < SIZE:
            color = cells.get((x, y))
            if color is None:
                x += 1
                continue
            run = 1
            while cells.get((x + run, y)) == color:
                run += 1
            rects.append(f'<rect x="{x}" y="{y}" width="{run}" height="1" fill="{color}"/>')
            x += run
    with open(os.path.join(out, f"{name}.svg"), "w") as handle:
        handle.write(
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{SIZE}" height="{SIZE}" viewBox="0 0 {SIZE} {SIZE}" '
            'shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n"
        )


def base(fill, light, dark, ink):
    cells = {(x, y): fill for y in range(SIZE) for x in range(SIZE)}
    for i in range(SIZE):
        cells[(i, 0)] = light
        cells[(0, i)] = light
        cells[(i, SIZE - 1)] = dark
        cells[(SIZE - 1, i)] = dark
    cells[(SIZE - 1, 0)] = ink
    cells[(0, SIZE - 1)] = ink
    return cells


def dune(cells, seed, shadow, shine, specks):
    rng = random.Random(seed)
    for _ in range(3):
        start_x = rng.randint(2, 10)
        y = rng.randint(4, 21)
        length = rng.randint(9, 15)
        for offset in range(length):
            x = start_x + offset
            wave = y + (1 if 2 < offset < length - 3 else 0) - (1 if offset > length - 3 else 0)
            if 1 <= x < SIZE - 1 and 1 <= wave < SIZE - 2:
                cells[(x, wave)] = shadow
                cells[(x, wave - 1)] = shine
    for _ in range(14):
        x, y = rng.randint(2, SIZE - 3), rng.randint(2, SIZE - 3)
        if cells[(x, y)] not in (shadow, shine):
            cells[(x, y)] = specks


for index in range(3):
    tile = base("#dcb860", "#f6dc94", "#8f6a2c", "#5e4418")
    dune(tile, 20 + index, "#b48c40", "#f0d488", "#c8a050")
    save(f"tile-sand-{index}", tile)

# Wrap-around cells: same desert seen through the night, clearly out of bounds.
wall = base("#1c2444", "#333f66", "#0c1024", "#060a22")
for y in range(1, SIZE - 1):
    for x in range(1, SIZE - 1):
        if (x + y) % 6 == 0:
            wall[(x, y)] = "#28325a"
save("tile-wrap", wall)

# Noise Desert S: exit to the green zone.
grass = base("#2fae58", "#7ce88a", "#0f6a44", "#04302c")
rng = random.Random(7)
for _ in range(11):
    x, y = rng.randint(3, SIZE - 4), rng.randint(4, SIZE - 4)
    grass[(x, y)] = "#7ce88a"
    grass[(x - 1, y + 1)] = "#1a8a4e"
    grass[(x + 1, y + 1)] = "#1a8a4e"
save("tile-noise", grass)

# Mirage Tower: a tower rising from the sand.
tower = base("#dcb860", "#f6dc94", "#8f6a2c", "#5e4418")
dune(tower, 42, "#b48c40", "#f0d488", "#c8a050")
for y in range(4, 22):
    half = 3 + (y - 4) // 6
    for x in range(13 - half, 14 + half):
        tower[(x, y)] = "#7fe8ff" if x < 13 else "#2b9fd0"
    tower[(13 - half - 1, y)] = "#07305e"
    tower[(14 + half, y)] = "#07305e"
for x in range(9, 18):
    tower[(x, 3)] = "#07305e"
    tower[(x, 22)] = "#07305e"
for y in (8, 13, 18):
    for x in range(11, 16):
        tower[(x, y)] = "#d6fbff"
save("tile-mirage", tower)

# Selection cursor: gold corners, transparent centre.
cursor = {}
GOLD, DARK = "#ffd52e", "#4f2606"
for horizontal, vertical in ((0, 0), (1, 0), (0, 1), (1, 1)):
    for step in range(7):
        for thick in range(2):
            x = step if not horizontal else SIZE - 1 - step
            y = thick if not vertical else SIZE - 1 - thick
            cursor[(x, y)] = GOLD
            x = thick if not horizontal else SIZE - 1 - thick
            y = step if not vertical else SIZE - 1 - step
            cursor[(x, y)] = GOLD
for (x, y) in list(cursor):
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        near = (x + dx, y + dy)
        if near not in cursor and 0 <= near[0] < SIZE and 0 <= near[1] < SIZE:
            cursor[near] = DARK
save("tile-cursor", cursor)

names = ["tile-sand-0", "tile-sand-1", "tile-sand-2", "tile-wrap", "tile-noise", "tile-mirage"]
page = "<!doctype html><body style='margin:0;padding:12px;background:#000a2b'><div style='display:grid;grid-template-columns:repeat(6,54px);position:relative'>"
layout = ["tile-wrap", "tile-wrap", "tile-wrap", "tile-noise", "tile-wrap", "tile-wrap", "tile-wrap", "tile-sand-0", "tile-sand-1", "tile-sand-2", "tile-sand-0", "tile-wrap", "tile-mirage", "tile-sand-1", "tile-sand-2", "tile-sand-0", "tile-sand-1", "tile-wrap", "tile-wrap", "tile-sand-2", "tile-sand-0", "tile-sand-1", "tile-sand-2", "tile-wrap"]
for name in layout:
    page += f"<img src='{name}.svg' width=54 height=54 style='image-rendering:pixelated'>"
page += "</div><img src='tile-cursor.svg' width=54 height=54 style='position:absolute;left:174px;top:66px;image-rendering:pixelated'>"
with open(os.path.join(out, "_tiles.html"), "w") as handle:
    handle.write(page)
print("ok")
