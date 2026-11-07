"""Parse FIELDSTG_stages (EU) and each wstag's name / EU map file from the dw3d sources (data only)."""
import re, glob, json, tomllib, os
DECOMP=os.environ["DW3_DECOMP"]
DDW3=os.environ["DW3_DDW3"]
LBAS=tomllib.load(open(DDW3+'/iso/dw2003-bootstrap/lbas.toml','rb'))['lbas']

def eu_table():
    lines=open(DECOMP+'/src/field/field_mode/data/fieldstg.c').read().split('\n')
    start=None
    for i,l in enumerate(lines):
        if l.startswith('#elif VERSION_EU') and i+1<len(lines) and 'FIELDSTG_stages' in lines[i+1]: start=i+2;break
    out=[]
    for l in lines[start:]:
        m=re.match(r'\s*\{(\d+),\s*(\d+),\s*WSTAG(\d+)_startStage\}',l)
        if m: out.append(tuple(map(int,m.groups())))
        elif '{0, 0, 0}' in l: break
    return out

def ev_cond(expr):
    e=expr.strip()
    e=re.sub(r'defined\s*\(?\s*(\w+)\s*\)?',lambda m:'0',e)
    e=e.replace('||',' or ').replace('&&',' and ').replace('!',' not ')
    e=re.sub(r'VERSION_(\w+)',lambda m:'1' if m.group(1)=='EU' else '0',e)
    try: return bool(eval(e,{'__builtins__':{}}))
    except Exception: return False

def preprocess(text):
    out=[];stack=[]  # (active_parent, taken, active)
    active=True
    for l in text.split('\n'):
        s=l.strip()
        if s.startswith('#if'):
            c=ev_cond(s[3:]) if not s.startswith('#ifdef') and not s.startswith('#ifndef') else False
            stack.append((active,c,active and c)); active=active and c
        elif s.startswith('#elif'):
            pa,taken,_=stack.pop(); c=(not taken) and ev_cond(s[5:])
            stack.append((pa,taken or c,pa and c)); active=pa and c
        elif s.startswith('#else'):
            pa,taken,_=stack.pop(); stack.append((pa,True,pa and not taken)); active=pa and not taken
        elif s.startswith('#endif'):
            pa,_,_=stack.pop(); active=pa
        elif active: out.append(l)
    return '\n'.join(out)

def num(e,defs):
    e=re.sub(r'\b(\w+)\b',lambda m: str(defs[m.group(1)]) if m.group(1) in defs else m.group(1),e)
    e=re.sub(r'\b0[xX][0-9a-fA-F]+\b',lambda m:str(int(m.group(0),16)),e)
    return int(eval(e,{'__builtins__':{}}))

def stage_info(n):
    fs=glob.glob(DECOMP+'/src/field/stages/*/wstag%03d.c'%n)
    if len(fs)!=1: return None
    raw=open(fs[0]).read()
    m=re.match(r'/\*(.*?)\*/',raw,re.S)
    c=' '.join(x.strip().lstrip('*').strip() for x in m.group(1).split('\n')).strip()
    c=re.sub(r'^WSTAG%03d:\s*'%n,'',c)
    desc=c
    name=re.split(r',|:|\.(?=\s)|\.$',c)[0].strip()
    t=preprocess(raw)
    defs={}
    for d in re.finditer(r'#define\s+(\w+)\s+(.+)',t):
        try: defs[d.group(1)]=num(d.group(2).split('/*')[0].strip().rstrip('\\'),defs)
        except Exception: pass
    mf=re.search(r'FIELDSTG_state\.mapFile\s*=\s*([^;]+);',t)
    mapfile=None
    if mf:
        try: mapfile=num(mf.group(1),defs)
        except Exception: pass
    return dict(file=os.path.relpath(fs[0],DECOMP),name=name,desc=desc,mapfile=mapfile,area=os.path.basename(os.path.dirname(fs[0])))

if __name__=='__main__':
    T=eu_table(); print(len(T), len(set(t[2] for t in T)), len(set(t[0] for t in T)))
    res=[]
    for mode,file,n in T:
        i=stage_info(n)
        pk=LBAS[i['mapfile']] if i and i['mapfile'] is not None and i['mapfile']<len(LBAS) else None
        res.append((mode,n,i and i['name'],i and i['mapfile'],pk, LBAS[file]))
    for r in res[:15]+res[-15:]: print(r)
    bad=[r for r in res if r[4] is None or r[4]!='S%03dPACK_BIN'%r[1]]
    print(len(bad)); 
    for r in bad: print(r)
