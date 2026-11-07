"""Software model of what FIELDSTG draws for a stage's map: tiles (3 depths), stage objects from the stage sheet,
background colour, on a 15-bit PSX framebuffer the size of the whole map. Built from the decompilation's
map_streamer.c / stream.c / map_objects.c / sprite_drawer.c / layer.c (OT order: highest depth first, newest
primitive of an entry first)."""
import os, sys, struct, tomllib
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import env  # noqa
from iso import Iso
from discfs import all_files
from mapx import unrlen, read_map
from sprite import pack_entries

LBAS = tomllib.load(open(os.environ['DW3_DDW3'] + '/iso/dw2003-bootstrap/lbas.toml', 'rb'))['lbas']
_ISO = None; _FILES = None; _BYBASE = None


def iso_files():
    global _ISO, _FILES, _BYBASE
    if _ISO is None:
        _ISO = Iso(); _FILES = {n: (e, s) for n, e, s in all_files(_ISO)}
        _BYBASE = {n.rsplit('/', 1)[-1].replace('.', '_'): v for n, v in _FILES.items()}
    return _ISO, _FILES, _BYBASE


def file_by_index(i):
    iso, files, by = iso_files()
    nm = LBAS[i]
    if nm not in by: return None
    e, s = by[nm]
    return iso.read(e, s)


# ---- TIM -----------------------------------------------------------------
def parse_tim(b):
    flags = struct.unpack_from('<I', b, 4)[0]
    bpp = flags & 3; p = 8; clut = None
    if flags & 8:
        tot, x, y, w, h = struct.unpack_from('<IHHHH', b, p)
        raw = b[p + 12:p + tot]; raw += b'\0' * (w * h * 2 - len(raw))
        clut = (w, h, np.frombuffer(raw, '<u2', w * h).reshape(h, w)); p += tot
    tot, x, y, w, h = struct.unpack_from('<IHHHH', b, p)
    raw = b[p + 12:p + tot]; raw += b'\0' * max(0, w * h * 2 - len(raw))
    img = np.frombuffer(raw, '<u2', w * h).reshape(h, w)
    return dict(bpp=bpp, clut=clut, w=w, h=h, img=img, pos=(x, y))


def tim_pixels16(t, row=0):
    """The TIM's picture as 16-bit PSX colours (indexed through CLUT row `row`)."""
    w, h, img = t['w'], t['h'], t['img']
    if t['bpp'] == 2: return img.copy()
    a = img.view(np.uint8).reshape(h, w * 2)
    if t['bpp'] == 0:
        idx = np.stack([a & 15, a >> 4], -1).reshape(h, w * 4)
    else:
        idx = a
    pal = t['clut'][2][row]
    if idx.max() >= len(pal): pal = np.concatenate([pal, np.zeros(idx.max() + 1 - len(pal), np.uint16)])
    return pal[idx]


class Vram:
    def __init__(self): self.m = np.zeros((512, 1024), np.uint16)

    def load(self, x, y, w, h, data):
        # LoadImage wraps at the edges; clip here
        w2 = min(w, 1024 - x); h2 = min(h, 512 - y)
        self.m[y:y + h2, x:x + w2] = data[:h2, :w2]

    def load_tim(self, t, imx, imy, clx, cly):
        if t['clut'] is not None and t['bpp'] in (0, 1):
            w, h, c = t['clut']; self.load(clx, cly, w, h, c)
        self.load(imx, imy, t['w'], t['h'], t['img'])

    def load_archive(self, arch, imx, imy, clx, cly):
        for e in pack_entries_game(arch):
            if e[:4] == b'RLEN': e = unrlen(e)
            self.load_tim(parse_tim(e), imx, imy, clx, cly)
            imx += 0x40


def pack_entries_game(d):
    """getArchiveEntry(i): offsets table; an offset of 0 ends it (the entry is then the archive itself)."""
    out = []; i = 0
    first = struct.unpack_from('<I', d, 0)[0]
    n = first // 4
    off = struct.unpack_from('<%dI' % n, d, 0)
    for k, s in enumerate(off):
        if s == 0: break
        e = next((x for x in off[k + 1:] if x != 0), len(d))
        out.append(d[s:e])
    return out


# ---- sprite sheet ----------------------------------------------------------
class Sheet:
    def __init__(self, b):
        self.b = b
        self.po, self.io = struct.unpack_from('<2I', b, 0)
        n = (self.po - 8) // 4
        self.ids = list(b[self.io:self.io + n])
        self.nparts = (self.io - self.po) // 14
        self.parts = [struct.unpack_from('<7h', b, self.po + 14 * i) for i in range(self.nparts)]

    def frame(self, fid):
        """-> (frameRow, abr, [(part, x, y)] in file order) or None when the sheet has no such frame"""
        try: i = self.ids.index(fid)
        except ValueError: return None
        off = struct.unpack_from('<I', self.b, 8 + 4 * i)[0]
        cnt, row, abr = struct.unpack_from('<3h', self.b, off)
        pl = [struct.unpack_from('<3h', self.b, off + 6 + 6 * j) for j in range(cnt)]
        return row, abr, pl


def c16_split(c):
    c = c.astype(np.int32)
    return c & 31, (c >> 5) & 31, (c >> 10) & 31, (c >> 15) & 1


class Canvas:
    """15-bit framebuffer: values 0..31 per channel, int16."""
    def __init__(self, w, h, bg=(0, 0, 0)):
        self.w, self.h = w, h
        self.px = np.empty((h, w, 3), np.int16); self.px[:] = bg
        self.touched = np.zeros((h, w), bool)   # something opaque/blended from tiles or objects was drawn here
        self.tile_px = np.zeros((h, w), bool)   # a tile pixel was drawn here

    def blit(self, x, y, rgb, mask, stp, abr, tile=False):
        """rgb (h,w,3) 5-bit, mask (h,w) drawn texels, stp (h,w) texel stp bit, abr semitrans mode or -1 (opaque primitive)"""
        h, w = mask.shape
        x0, y0 = max(x, 0), max(y, 0); x1, y1 = min(x + w, self.w), min(y + h, self.h)
        if x1 <= x0 or y1 <= y0: return
        sx0, sy0 = x0 - x, y0 - y; sx1, sy1 = sx0 + (x1 - x0), sy0 + (y1 - y0)
        m = mask[sy0:sy1, sx0:sx1]; c = rgb[sy0:sy1, sx0:sx1].astype(np.int16)
        dst = self.px[y0:y1, x0:x1]
        if abr >= 0:
            s = stp[sy0:sy1, sx0:sx1].astype(bool)
            b = dst.astype(np.int16)
            if abr == 0: bl = (b + c) // 2
            elif abr == 1: bl = b + c
            elif abr == 2: bl = b - c
            else: bl = b + c // 4
            bl = np.clip(bl, 0, 31)
            c = np.where(s[..., None], bl, c)
        dst[m] = c[m]
        self.touched[y0:y1, x0:x1] |= m
        if tile: self.tile_px[y0:y1, x0:x1] |= m

    def to_rgb8(self):
        p = self.px.astype(np.uint8)
        return ((p << 3) | (p >> 2)).astype(np.uint8)


def modulate(rgb, col):
    if col is None or tuple(col[:3]) == (128, 128, 128): return rgb
    r = rgb.astype(np.int32) * np.array(col[:3], np.int32) // 128
    return np.clip(r, 0, 31).astype(np.int16)


def fetch_part(vram, part, drawer_clutx, drawer_cluty, altx, alty, frame_row, clut_row):
    """Texels of one sheet part as 16-bit colours (h,w). Mirrors getTPage / sprite u0,v0 / getClut of spriteDrawerDraw."""
    u, v, w, h, cx, cy, mode = part
    if mode:
        hw = 0x140 + u // 2
        page_b = (hw // 64) * 64 * 2          # bytes
        u0 = u & 0x7F
        clx = (altx + cx); cly = (alty + cy + frame_row + clut_row)
    else:
        hw = 0x140 + u // 4
        page_b = (hw // 64) * 64 * 4          # nibbles
        u0 = u & 0xFF
        clx = (drawer_clutx + cx); cly = (drawer_cluty + cy + frame_row + clut_row)
    cly &= 511; clx &= ~15
    rows = vram.m[(0x100 + (part[1] & 0xFF) + np.arange(h)) & 511]
    ux = (u0 + np.arange(w)) & 255
    if mode:
        bi = page_b + ux
        bytes_ = rows.view(np.uint8).reshape(h, 2048)
        idx = bytes_[:, bi % 2048]
        pal = vram.m[cly, clx:clx + 256]
    else:
        ni = page_b + ux
        by = rows.view(np.uint8).reshape(h, 2048)[:, (ni // 2) % 2048]
        idx = np.where(ni % 2 == 0, by & 15, by >> 4)
        pal = vram.m[cly, clx:clx + 16]
    return pal[idx]


def render_stage(info, want_objects=True, include_anim_ff=False, verbose=False, tile_depths=(7, 3, 11)):
    """info: stagedef.load_stage() dict. Returns (rgb8 image, report)."""
    rep = dict(objects=len(info['objects']), sheet=False, mask=False, drawn_objects=0, skipped=[], notes=list(info['notes']))
    mapb = file_by_index(info['mapFile'])
    w, h, ents = read_map(mapb)
    W, H = 128 * w, 128 * h
    vram = Vram()
    # --- the stage's image archive and sheet (FIELDSTG_loadFieldFiles)
    sheet = None
    arch = None
    if info.get('imageEntry'):
        fb = file_by_index(info['imageEntry'] >> 16)
        if fb is not None:
            ents_ = pack_entries_game(fb); k = info['imageEntry'] & 0xFFFF
            vram.load_archive(ents_[k], 0x140, 0x100, 0, 0x1F0); rep['mask'] = True
    if info.get('imageFile'):
        arch = file_by_index(info['imageFile'])
        if arch is not None:
            vram.load_archive(arch, 0x140, 0x100, 0, 0x1F0); rep['mask'] = True
    if info.get('sheetEntry'):
        fb = file_by_index(info['sheetEntry'] >> 16)
        if fb is not None:
            ents_ = pack_entries_game(fb); k = info['sheetEntry'] & 0xFFFF
            if k < len(ents_):
                sheet = Sheet(ents_[k]); rep['sheet'] = True
    col = info.get('color')
    cd = col[3] if col else 0
    cv = Canvas(W, H, bg=(0, 0, 0))   # setBgColor(1,1,1) -> 5-bit 0
    # draw list: (depth, seq, kind, payload)
    cmds = []
    seq = 0
    # ---- tiles (FIELDSTG_drawStream): layer i at depth (7,3,11), sprites j in order
    rep['layer_sprites'] = [0, 0, 0]
    for ti in range(w * h):
        e = ents[ti] if ti < len(ents) else b''
        if e[:4] == b'RLEN': e = unrlen(e)
        if len(e) < 8: continue
        hl = struct.unpack_from('<I', e, 0)[0]
        p = 4; lists = []
        for i in range(3):
            n = struct.unpack_from('<I', e, p)[0]; p += 4
            lst = [struct.unpack_from('<6h', e, p + 12 * j) for j in range(n)]; p += 12 * n
            lists.append(lst); rep['layer_sprites'][i] += n
        t = parse_tim(e[hl:])
        pix16 = tim_pixels16(t, 0)        # 8-bit tile image, CLUT row 0
        ox, oy = 128 * (ti % w), 128 * (ti // w)
        for i in range(3):
            if i == 2: tint = col if col else [0, 0, 0, 0]   # drawn with spriteColor whatever cd is (zero if the stage never set it)
            else: tint = col if (cd != 0 and col) else None
            for j, (sx, sy, u, v, sw, sh) in enumerate(lists[i]):
                cmds.append((tile_depths[i], seq, 'tile', (ox + sx, oy + sy, pix16[v:v + sh, u:u + sw], tint))); seq += 1
    # ---- objects
    objs = info['objects']
    unsorted = []; sorted_ = []
    for k, o in enumerate(objs):
        if not o['visible']: rep['skipped'].append((k, 'invisible')); continue
        if o['anim'] == 0xFF and not include_anim_ff:
            rep['skipped'].append((k, 'effect sprite (anim 0xFF)')); continue
        (sorted_ if o['sortY'] != 0 else unsorted).append((k, o))
    if want_objects and sheet is not None:
        for k, o in unsorted:  # added during the task update, in list order
            cmds.append((o['depth'], seq, 'obj', (k, o))); seq += 1
        for k, o in sorted(sorted_, key=lambda t: -t[1]['sortY']):  # callbacks run highest sortY first, afterwards
            cmds.append((o['depth'], seq, 'obj', (k, o))); seq += 1
    # OT: highest depth drawn first; within an entry the newest primitive first
    cmds.sort(key=lambda c: (-c[0], -c[1]))
    abr_default = -1
    for depth, sq, kind, pl in cmds:
        if kind == 'tile':
            x, y, pix, tint = pl
            r, g, b, s = c16_split(pix)
            mask = pix != 0
            rgb = np.stack([r, g, b], -1).astype(np.int16)
            rgb = modulate(rgb, tint)
            cv.blit(x, y, rgb, mask, s, -1, tile=True)
        else:
            k, o = pl
            fr = sheet.frame(o['frame'])
            if fr is None:
                rep.setdefault('missing_frames', []).append((k, o['frame'])); continue
            frow, abr, parts = fr
            tint = col if (cd != 0 and col) else None
            # parts are added last to first, so they draw first to last
            for (pi, px, py) in parts:
                part = sheet.parts[pi]
                pix = fetch_part(vram, part, 0x140, 0x100, 0, 0xF0, frow, o['clutRow'])
                r, g, b, s = c16_split(pix)
                rgb = modulate(np.stack([r, g, b], -1).astype(np.int16), tint)
                cv.blit(o['x'] + px, o['y'] + py, rgb, pix != 0, s, abr)
            rep['drawn_objects'] += 1
    return cv, rep, (w, h)


if __name__ == '__main__':
    import stagedef
    from PIL import Image
    n = int(sys.argv[1])
    info = stagedef.load_stage(n)
    cv, rep, wh = render_stage(info)
    Image.fromarray(cv.to_rgb8()).save(sys.argv[2])
    print(rep)


# ---- walk map (FieldMap) and the camera's reach --------------------------------
def walk_mask(info):
    """Pixel mask of map cells with a non-zero value in the floor-0 map (TMPK entry 1), at 8-pixel resolution."""
    fl = info['floor0']
    fb = file_by_index(fl >> 16)
    e = pack_entries_game(fb)[fl & 0xFFFF]
    off = struct.unpack_from('<6I', e, 0)
    grid = e[off[0]:]
    gw, gh = grid[0], grid[1]
    g = np.frombuffer(grid, np.uint8, gw * gh, 2).reshape(gh, gw)
    c64 = np.frombuffer(e, np.uint8, (off[2] - off[1]), off[1])
    c32 = np.frombuffer(e, '<i2', (off[3] - off[2]) // 2, off[2])
    c16 = np.frombuffer(e, '<i2', (off[4] - off[3]) // 2, off[3])
    c8 = np.frombuffer(e, '<i2', (off[5] - off[4]) // 2, off[4])
    px = np.frombuffer(e, np.uint8, len(e) - off[5], off[5])
    out = np.zeros((gh * 16, gw * 16), bool)   # 8-px blocks -> 16 per 128 cell
    for gy in range(gh):
        for gx in range(gw):
            i0 = int(g[gy, gx])
            for q64 in range(4):
                i1 = int(c64[i0 * 4 + q64]) if i0 * 4 + q64 < len(c64) else 0
                for q32 in range(4):
                    i2 = int(c32[i1 * 4 + q32]) if i1 * 4 + q32 < len(c32) else 0
                    for q16 in range(4):
                        i3 = int(c16[i2 * 4 + q16]) if i2 * 4 + q16 < len(c16) else 0
                        for q8 in range(4):
                            i4 = int(c8[i3 * 4 + q8]) if i3 * 4 + q8 < len(c8) else 0
                            bx = gx * 16 + (q64 & 1) * 8 + (q32 & 1) * 4 + (q16 & 1) * 2 + (q8 & 1)
                            by = gy * 16 + (q64 >> 1) * 8 + (q32 >> 1) * 4 + (q16 >> 1) * 2 + (q8 >> 1)
                            blk = px[i4 * 64:i4 * 64 + 64]
                            if blk.size: out[by, bx] = (blk != 0).any()
    return out


def reach_mask(walk, W, H, step=1):
    """Pixels the camera can show (view 320x240, centre offset (0xA0,0x8C), clamped) while the player stands on a walkable 8-px block."""
    vis = np.zeros((H, W), bool)
    ys, xs = np.where(walk)
    seen = set()
    for by, bx in zip(ys, xs):
        cx, cy = bx * 8 + 4, by * 8 + 4
        x = min(max(cx - 0xA0, 0), W - 320); y = min(max(cy - 0x8C, 0), H - 240)
        k = (x // 4, y // 4)
        if k in seen: continue
        seen.add(k)
        vis[y:y + 240, x:x + 320] = True
    return vis
