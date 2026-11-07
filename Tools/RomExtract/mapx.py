"""Extract & stitch Z_STAGE/SnnnPACK maps (formats from ddw3 tools: unmap / unrlen / unmap-tile / untim)."""
import struct, sys, numpy as np
from PIL import Image
sys.path.insert(0, __file__.rsplit('/',1)[0])
from iso import Iso
from discfs import all_files

def unrlen(b):
    assert b[:4]==b'RLEN'
    total=struct.unpack_from('<I',b,4)[0]
    out=bytearray(); p=8; n=0
    while n<total:
        op=b[p]; p+=1
        if op<0x80: out+=b[p:p+op]; p+=op; n+=op
        else:
            l=op-0x80; out+=bytes([b[p]])*l; p+=1; n+=l
    return bytes(out)

def parse_tile(b):
    hl=struct.unpack_from('<I',b,0)[0]
    p=4; layers=[]
    while True:
        if p+4>len(b): break
        n=struct.unpack_from('<I',b,p)[0]; p+=4
        if p+12*n>hl: break
        objs=[struct.unpack_from('<6H',b,p+12*i) for i in range(n)]; p+=12*n
        layers.append(objs)   # (wx,wy,ix,iy,w,h)
    return layers, b[hl:]

def color_rgba(c16, dedup=False):
    """PSX 16-bit colour -> RGBA8 the way ddw3 untim+PIL does (5-bit<<3, 8-bit trunc)."""
    r=c16&31; g=(c16>>5)&31; bl=(c16>>10)&31; stp=c16>>15
    if r==g==bl==0: a=255 if stp else 0
    elif stp: a=127
    else: a=255
    return (r<<3,g<<3,bl<<3,a)

def parse_tim(b, ddw3_quirk=False):
    assert b[:4]==b'\x10\x00\x00\x00', b[:8].hex()
    flags=struct.unpack_from('<I',b,4)[0]; bpp=flags&3; has_clut=bool(flags&8)
    p=8; clut=None
    if has_clut:
        tot,x,y,w,h=struct.unpack_from('<IHHHH',b,p)
        raw=np.frombuffer(b,'<u2',w*h,p+12); p+=tot
        clut=raw
    tot,x,y,w,h=struct.unpack_from('<IHHHH',b,p)
    data=b[p+12:p+tot]
    data=data+b'\0'*(w*h*2-len(data))
    if bpp==2:
        c=np.frombuffer(data,'<u2',w*h).reshape(h,w)
        return c16_to_rgba(c)
    if bpp==3: raise NotImplementedError('24bpp')
    a=np.frombuffer(data,np.uint8)
    if bpp==0:
        idx=np.stack([a&15,a>>4],-1).reshape(-1); W=w*4
    else:
        idx=a; W=w*2
    idx=idx.reshape(h,W)
    pal=clut.copy()
    lut=c16_to_rgba(pal[None,:])[0]
    if ddw3_quirk:
        seen={}
        for i,c in enumerate(pal.tolist()):
            k=seen.get(c,0); seen[c]=k+1
            if k>0 and (c&0x7fff)==0 or (k>0 and (c&0x7fff)!=0):
                pass
            if k>0:
                r=((c&31)<<3); # low bits of occurrence don't survive 8-bit truncation unless >=256 (ignored)
                lut[i]=((c&31)<<3,((c>>5)&31)<<3,((c>>10)&31)<<3,255 if not (c>>15) else 127)
    if idx.max()>=len(lut): lut=np.concatenate([lut,np.zeros((idx.max()+1-len(lut),4),np.uint8)])
    return lut[idx]

_LUT=None
def c16_to_rgba(c):
    c=c.astype(np.uint32)
    r=c&31; g=(c>>5)&31; bl=(c>>10)&31; stp=c>>15
    black=(r==0)&(g==0)&(bl==0)
    a=np.where(black,np.where(stp==1,255,0),np.where(stp==1,127,255))
    return np.stack([r<<3,g<<3,bl<<3,a],-1).astype(np.uint8)

def read_map(data):
    n,w,h,step=struct.unpack_from('<4I',data,0)
    first=((4*n)+0x7ff)//0x800*0x800
    actual=(len(data)-first)//step
    cnt=actual
    lens=struct.unpack_from('<%dH'%cnt,data,16)
    ents=[]
    for i in range(cnt):
        pos=first+step*i
        ents.append(data[pos:pos+lens[i]])
    return w,h,ents

def stitch(data, ddw3_quirk=False):
    w,h,ents=read_map(data)
    W,H=128*w,128*h
    canvas=Image.new('RGBA',(W,H))
    info={'w':w,'h':h,'empty':0,'tiles':0,'errors':[]}
    for i in range(w*h):
        if i>=len(ents): info['errors'].append('missing entry %d'%i); continue
        e=ents[i]
        try:
            if e[:4]==b'RLEN': e=unrlen(e)
            if len(e)<8: info['empty']+=1; continue
            layers,tim=parse_tile(e)
            img=parse_tim(tim,ddw3_quirk)
            tile=Image.fromarray(img,'RGBA')
            ox,oy=128*(i%w),128*(i//w)
            for objs in layers:
                for wx,wy,ix,iy,ow,oh in objs:
                    canvas.alpha_composite(tile.crop((ix,iy,ix+ow,iy+oh)),(ox+wx,oy+wy))
            info['tiles']+=1
        except Exception as ex:
            info['errors'].append('%d: %r'%(i,ex))
    return canvas,info

def get_pack(iso, files, num):
    e,s=files['AAA/DAT/Z_STAGE/S%03dPACK.BIN'%num]
    return iso.read(e,s)

if __name__=='__main__':
    iso=Iso(); files={n:(e,s) for n,e,s in all_files(iso)}
    for num in map(int,sys.argv[2:]):
        img,info=stitch(get_pack(iso,files,num))
        img.save('%s/S%03dPACK.png'%(sys.argv[1],num)); print(num,img.size,info)
