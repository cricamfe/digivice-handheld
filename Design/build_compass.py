"""Pixel compass dial, 44x44, meant to be shown at 88x88."""
import math
import os
import sys

SIZE = 44
CENTER = 21.5
INK = "#040716"
BRASS = ("#fff1a0", "#f0b020", "#a86410")
FACE_A, FACE_B = "#0b2a78", "#0b3a90"
HUB = "#00103a"
NORTH = ("#ffb09a", "#ec3b2e", "#8f1420")
STEEL = ("#ffffff", "#c3d0ea", "#69748c")

cells = {}
for y in range(SIZE):
    for x in range(SIZE):
        dx = x - CENTER
        dy = y - CENTER
        radius = math.hypot(dx, dy)
        if radius > 22:
            continue
        if radius > 21:
            cells[(x, y)] = INK
        elif radius > 18:
            light = dx + dy
            cells[(x, y)] = BRASS[0] if light < -12 else (BRASS[2] if light > 12 else BRASS[1])
        elif radius > 17:
            cells[(x, y)] = INK
        else:
            cells[(x, y)] = FACE_A if (x // 2 + y // 2) % 2 == 0 else FACE_B


def triangle(tip, base_a, base_b, colors):
    """Fills a triangle; the side closest to the top left gets the light tone."""
    min_x = int(min(tip[0], base_a[0], base_b[0]))
    max_x = int(max(tip[0], base_a[0], base_b[0])) + 1
    min_y = int(min(tip[1], base_a[1], base_b[1]))
    max_y = int(max(tip[1], base_a[1], base_b[1])) + 1

    def sign(p, a, b):
        return (p[0] - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (p[1] - b[1])

    for y in range(min_y, max_y + 1):
        for x in range(min_x, max_x + 1):
            p = (x + 0.5, y + 0.5)
            d1, d2, d3 = sign(p, tip, base_a), sign(p, base_a, base_b), sign(p, base_b, tip)
            if (d1 < 0 or d2 < 0 or d3 < 0) and (d1 > 0 or d2 > 0 or d3 > 0):
                continue
            middle = (tip[0] + (base_a[0] + base_b[0]) / 2) / 2
            middle_y = (tip[1] + (base_a[1] + base_b[1]) / 2) / 2
            shade = (x - middle) + (y - middle_y)
            cells[(x, y)] = colors[0] if shade < -2 else (colors[2] if shade > 2 else colors[1])


c = CENTER + 0.5
reach, base, half = 16, 8, 5
triangle((c, c - reach), (c - half, c - base), (c + half, c - base), NORTH)
triangle((c, c + reach), (c - half, c + base), (c + half, c + base), STEEL)
triangle((c + reach, c), (c + base, c - half), (c + base, c + half), STEEL)
triangle((c - reach, c), (c - base, c - half), (c - base, c + half), STEEL)

for angle in (45, 135, 225, 315):
    x = int(CENTER + math.cos(math.radians(angle)) * 15)
    y = int(CENTER + math.sin(math.radians(angle)) * 15)
    for ox, oy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        cells[(x + ox, y + oy)] = BRASS[1]

for y in range(SIZE):
    for x in range(SIZE):
        radius = math.hypot(x - CENTER, y - CENTER)
        if radius <= 9:
            cells[(x, y)] = HUB
        elif radius <= 10:
            cells[(x, y)] = BRASS[1]

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
svg = (
    f'<svg xmlns="http://www.w3.org/2000/svg" width="{SIZE}" height="{SIZE}" viewBox="0 0 {SIZE} {SIZE}" '
    'shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n"
)
out = sys.argv[1]
with open(os.path.join(out, "compass.svg"), "w") as handle:
    handle.write(svg)
big = svg.replace(f'width="{SIZE}" height="{SIZE}"', 'width="176" height="176"')
mid = svg.replace(f'width="{SIZE}" height="{SIZE}"', 'width="88" height="88"')
person = open(os.path.join(out, "person.svg")).read()
person_big = person.replace('width="22" height="22"', 'width="132" height="132"')
person_mid = person.replace('width="22" height="22"', 'width="44" height="44"')
page = (
    "<!doctype html><body style='margin:0;padding:16px;background:#000a2b;display:flex;gap:24px;align-items:center'>"
    + big + mid + person_big + person_mid + person
)
with open(os.path.join(out, "_compass.html"), "w") as handle:
    handle.write(page)
print("ok")
