"""Read a stage's setup values and its stageObjects[] table from the decompilation (data only), EU values."""
import re, glob, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import env  # noqa
from parse_stages import preprocess, num
DECOMP = os.environ["DW3_DECOMP"]


def strip_comments(t):
    t = re.sub(r'/\*.*?\*/', ' ', t, flags=re.S)
    return re.sub(r'//[^\n]*', ' ', t)


def split_top(s):
    out = []; d = 0; cur = ''
    for ch in s:
        if ch == '{': d += 1
        if ch == '}': d -= 1
        if ch == ',' and d == 0:
            out.append(cur); cur = ''
        else: cur += ch
    if cur.strip(): out.append(cur)
    return out


def load_stage(n):
    fs = glob.glob(DECOMP + '/src/field/stages/*/wstag%03d.c' % n) + glob.glob(DECOMP + '/src/field/stages/wstag%03d.c' % n)
    if len(fs) != 1: return None
    raw = open(fs[0]).read()
    t = strip_comments(preprocess(raw))
    defs = {}
    for d in re.finditer(r'#define\s+(\w+)\s+([^\n]+)', t):
        try: defs[d.group(1)] = num(d.group(2).strip().rstrip('\\'), defs)
        except Exception: pass
    # function-local helper: resolve a line-oriented text with several definitions of same name keeps the last (already EU-filtered)
    info = dict(file=fs[0], n=n, notes=[])
    st = {}
    for m in re.finditer(r'FIELDSTG_state\.(mapFile|sheetEntry|imageFile|imageEntry)\s*=\s*([^;]+);', t):
        k = m.group(1)
        try: v = num(m.group(2), defs)
        except Exception: info['notes'].append('cannot evaluate %s = %s' % (k, m.group(2).strip())); continue
        if k in st and st[k] != v: info['notes'].append('%s assigned twice (%s, %s)' % (k, hex(st[k]), hex(v)))
        st[k] = v
    info.update(st)
    m = re.search(r'FIELDSTG_map\.setFile\(\s*FIELD_MAP_FLOOR0\s*,\s*([^;]+?)\)\s*;', t)
    info['floor0'] = None
    if m:
        try: info['floor0'] = num(m.group(1), defs)
        except Exception: info['notes'].append('cannot evaluate floor0')
    # sprite colour
    info['color'] = None
    if re.search(r'FIELDSTG_state\.spriteColor\s*=\s*stageColor', t):
        m = re.search(r'CVECTOR\s+stageColor\s*=\s*\{([^}]*)\}', t)
        if m:
            info['color'] = [num(x, defs) for x in m.group(1).split(',')]
    elif 'spriteColor' in t:
        info['notes'].append('spriteColor set another way')
    # objects
    m = re.search(r'StageTile\s+stageObjects\s*\[\s*\]\s*=\s*\{', t)
    objs = []
    if m:
        i = m.end(); d = 1; j = i
        while d:
            c = t[j]
            if c == '{': d += 1
            elif c == '}': d -= 1
            j += 1
        body = t[i:j - 1]
        for rec in split_top(body):
            rec = rec.strip()
            if not rec.startswith('{'): continue
            vals = [num(x, defs) for x in rec.strip('{} \n').split(',') if x.strip()]
            if len(vals) < 14: vals += [0] * (14 - len(vals))
            keys = ['visible', 'anim', 'margin', 'depth', 'frame', 'cycle', 'cycleFirst', 'cycleLast', 'cycleDelay', 'clutRow', 'x', 'y', 'sortY', 'cycleTime']
            o = dict(zip(keys, vals))
            if o['margin'] == 0: break
            objs.append(o)
    else:
        info['notes'].append('no stageObjects table')
    info['objects'] = objs
    return info


if __name__ == '__main__':
    for n in map(int, sys.argv[1:]):
        i = load_stage(n)
        print({k: (hex(v) if isinstance(v, int) and k not in ('n',) else v) for k, v in i.items() if k != 'objects'}, len(i['objects']))
