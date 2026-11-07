"""Pixel UI kit for Digivice: nine-slice frames drawn at double pixel, plus connectors."""
import os
import sys

INK = "#040716"

FRAMES = {
    "win": {"light": "#7cc0ff", "dark": "#123a9e", "mid": "#2277ee", "rivet": "#d6fbff", "a": "#000a2b", "b": "#001040"},
    "win-soft": {"light": "#2f7fe0", "dark": "#0b2a78", "mid": "#144aa8", "rivet": "#7cc0ff", "a": "#000e3f", "b": "#00154a"},
    "plate": {"light": "#fff1a0", "dark": "#a86410", "mid": "#f0b020", "rivet": "#ffffff", "a": "#1a1040", "b": "#221650"},
    "good": {"light": "#b4f7a8", "dark": "#0f7a52", "mid": "#26b45a", "rivet": "#effff8", "a": "#04261c", "b": "#063524"},
    "bad": {"light": "#ffb09a", "dark": "#7a1030", "mid": "#dd2c2e", "rivet": "#fff0e8", "a": "#26061a", "b": "#340a22"},
    "lock": {"light": "#7b86a4", "dark": "#232a44", "mid": "#4d5878", "rivet": "#a8b4ca", "a": "#0c1024", "b": "#12172e"},
    "btn": {"light": "#a8dcff", "dark": "#06124a", "mid": "#2277ee", "rivet": "#d6fbff", "a": "#0b2a78", "b": "#0e3288"},
    "btn-on": {"light": "#fffbd0", "dark": "#c47a10", "mid": "#ffd52e", "rivet": "#ffffff", "a": "#00a0f0", "b": "#06a6f4"},
    "slot": {"light": "#2f7fe0", "dark": "#06124a", "mid": "#144aa8", "rivet": "#7cc0ff", "a": "#000e3f", "b": "#001248"},
    "slot-on": {"light": "#fffbd0", "dark": "#c47a10", "mid": "#ffd52e", "rivet": "#ffffff", "a": "#002a77", "b": "#003088"},
    # One frame for each element's field (the neutral one uses win-soft), same construction and ramp as the rest.
    "fire": {"light": "#ffc08a", "dark": "#8a2410", "mid": "#f0561a", "rivet": "#fff0d8", "a": "#2a0a06", "b": "#3a0e08"},
    "water": {"light": "#9ae0ff", "dark": "#0a3a8a", "mid": "#1ea0e0", "rivet": "#e0f8ff", "a": "#00203a", "b": "#002a4a"},
    "ice": {"light": "#e0fbff", "dark": "#3a7a9a", "mid": "#8ae0f5", "rivet": "#ffffff", "a": "#06223a", "b": "#0a2c48"},
    "wind": {"light": "#b8f7d8", "dark": "#0f6a5a", "mid": "#2cc8a0", "rivet": "#f0fff8", "a": "#04261f", "b": "#06322a"},
    "thunder": {"light": "#fffbb0", "dark": "#8a7a00", "mid": "#e8e030", "rivet": "#ffffff", "a": "#262606", "b": "#323208"},
    "machine": {"light": "#d0d8e8", "dark": "#3a4460", "mid": "#8a9ab8", "rivet": "#ffffff", "a": "#12161f", "b": "#1a1f2c"},
    "dark": {"light": "#d0a8ff", "dark": "#40106a", "mid": "#8a3ad8", "rivet": "#f6e8ff", "a": "#12041f", "b": "#1a062c"},
    "bar": {"light": "#0b1a48", "dark": "#2a4a80", "mid": "#000000", "rivet": "#000000", "a": "#00103a", "b": "#00103a"},
}
SLICE = 4
SIZE = SLICE * 3


def frame_svg(style):
    rects = []
    last = SIZE - 1
    for y in range(SIZE):
        for x in range(SIZE):
            depth = min(x, y, last - x, last - y)
            corner = min(x + y, (last - x) + y, x + (last - y), (last - x) + (last - y))
            if corner < 1:
                continue
            if corner == 1 or depth == 0:
                color = INK
            elif depth == 1:
                color = style["light"] if x + y < last else style["dark"]
            elif depth == 2:
                color = style["rivet"] if corner == 4 else style["mid"]
            elif depth == 3:
                color = INK
            else:
                color = style["a"] if (x + y) % 2 == 0 else style["b"]
            rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="{color}"/>')
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{SIZE}" height="{SIZE}" viewBox="0 0 {SIZE} {SIZE}" '
        'shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n"
    )


def fork_svg():
    """24x190: stem enters at mid height, two branches leave at 42 and 148."""
    width, height = 24, 190
    top, bottom, middle = 42, 148, 95
    light, mid, dark = "#d6fbff", "#5fdcf5", "#1a93c4"
    cells = {}

    def block(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                cells[(x, y)] = color

    def bar_h(y, x0, x1):
        block(x0, y - 3, x1, y - 1, light)
        block(x0, y - 1, x1, y + 1, mid)
        block(x0, y + 1, x1, y + 3, dark)

    block(4, top - 3, 6, bottom + 3, light)
    block(6, top - 3, 8, bottom + 3, mid)
    block(8, top - 3, 10, bottom + 3, dark)
    bar_h(top, 4, 14)
    bar_h(bottom, 4, 14)
    bar_h(middle, 0, 4)
    for y_center in (top, bottom):
        for step in range(5):
            half = 10 - step * 2
            block(14 + step * 2, y_center - half, 16 + step * 2, y_center + half, mid)
            block(14 + step * 2, y_center - half, 16 + step * 2, y_center - half + 2, light)
            block(14 + step * 2, y_center + half - 2, 16 + step * 2, y_center + half, dark)
    outline = {}
    for (x, y) in cells:
        for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2), (1, 0), (-1, 0), (0, 1), (0, -1)):
            near = (x + dx, y + dy)
            if near not in cells and 0 <= near[0] < width and 0 <= near[1] < height:
                outline[near] = "#07305e"
    cells = {**outline, **cells}
    rects = [f'<rect x="{x}" y="{y}" width="1" height="1" fill="{c}"/>' for (x, y), c in sorted(cells.items())]
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" '
        'shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n"
    )


out = sys.argv[1]
os.makedirs(out, exist_ok=True)
for name, style in FRAMES.items():
    with open(os.path.join(out, f"frame-{name}.svg"), "w") as handle:
        handle.write(frame_svg(style))
with open(os.path.join(out, "fork.svg"), "w") as handle:
    handle.write(fork_svg())

css = "".join(
    f".{name}{{border:8px solid transparent;border-image:url(frame-{name}.svg) 4 fill / 8px round;image-rendering:pixelated;box-sizing:border-box}}"
    for name in FRAMES
)
page = (
    "<!doctype html><style>body{margin:0;padding:12px;background:#000030;color:#fff;font:12px sans-serif;"
    "display:grid;grid-template-columns:repeat(4,1fr);gap:10px}" + css + "</style>"
    + "".join(f"<div class='{name}' style='height:80px;padding:4px'>{name}</div>" for name in FRAMES)
    + "<img src='fork.svg' style='height:190px;width:24px;image-rendering:pixelated'>"
)
with open(os.path.join(out, "_frames.html"), "w") as handle:
    handle.write(page)
print(len(FRAMES), "frames")
