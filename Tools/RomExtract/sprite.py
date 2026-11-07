import struct, sys
sys.path.insert(0, __file__.rsplit('/',1)[0])
from iso import Iso
from discfs import all_files
import numpy as np
from PIL import Image

def pack_entries(d):
    """ddw3 PackReader: u32 offsets, first non-null offset gives header size; zero offset = empty file."""
    off=[]; p=0
    first=struct.unpack_from('<I',d,0)[0]
    i=0
    while first==0:
        i+=1; first=struct.unpack_from('<I',d,4*i)[0]
    n=first//4
    off=list(struct.unpack_from('<%dI'%n,d,0))
    out=[]
    for k,s in enumerate(off):
        if s==0: out.append(b''); continue
        e=next((x for x in off[k+1:] if x!=0),len(d))
        out.append(d[s:e])
    return out

def tim_info(b):
    flags=struct.unpack_from('<I',b,4)[0]; bpp=flags&3; has=bool(flags&8)
    p=8; clut=None
    if has:
        tot,x,y,w,h=struct.unpack_from('<IHHHH',b,p)
        clut=(x,y,w,h,np.frombuffer(b,'<u2',w*h,p+12)); p+=tot
    tot,x,y,w,h=struct.unpack_from('<IHHHH',b,p)
    return dict(bpp=bpp,clut=clut,pos=(x,y),w=w,h=h,data=b[p+12:p+tot])

def tim_to_rgba(b, row=0):
    t=tim_info(b); w,h=t['w'],t['h']
    data=t['data']+b'\0'*max(0,w*h*2-len(t['data']))
    a=np.frombuffer(data,np.uint8)
    if t['bpp']==0: idx=np.stack([a&15,a>>4],-1).reshape(h,w*4)
    elif t['bpp']==1: idx=a[:w*h*2].reshape(h,w*2)
    else: raise NotImplementedError
    cx,cy,cw,ch,pal=t['clut']
    pal=pal.reshape(ch,cw)[row]
    c=pal.astype(np.uint32)
    r=c&31;g=(c>>5)&31;bl=(c>>10)&31;stp=c>>15
    black=(r==0)&(g==0)&(bl==0)
    al=np.where(black,np.where(stp==1,255,0),255)    # hardware: only 0x0000 is transparent; stp bit means semi-transparent blend, drawn opaque here
    lut=np.stack([(r<<3)|(r>>2),(g<<3)|(g>>2),(bl<<3)|(bl>>2),al],-1).astype(np.uint8)
    return lut[idx], t
