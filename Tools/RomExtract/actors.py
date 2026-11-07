import os
import re, sys, struct, tomllib
sys.path.insert(0, __file__.rsplit('/',1)[0])
from sprite import *
DECOMP=os.environ["DW3_DECOMP"]
DDW3=os.environ["DW3_DDW3"]
LBAS=tomllib.load(open(DDW3+'/iso/dw2003-bootstrap/lbas.toml','rb'))['lbas']
def eu_file_entries():
    t=open(DECOMP+'/src/field/field_mode/data/fieldstg.c').read()
    i=t.index('#elif VERSION_EU\ns32 FIELDSTG_fileEntries[]')
    j=t.index('};',i)
    body=t[i:j]
    return [int(x,16) for x in re.findall(r'0x[0-9A-Fa-f]+|\b0\b',body.split('{',1)[1])]
def eu_widths():
    t=open(DECOMP+'/src/field/field_mode/data/fieldstg.c').read()
    i=t.index('#elif VERSION_EU || VERSION_JP\nu8 FIELDSTG_actorWidths[]'); j=t.index('};',i)
    return [int(x,16) for x in re.findall(r'0x[0-9A-Fa-f]+',t[i:j].split('{',1)[1])]
class Sprites:
    def __init__(self):
        self.iso=Iso(); self.F={n:(e,s) for n,e,s in all_files(self.iso)}
        self.fe=eu_file_entries(); self.cache={}
    def pack(self,lba_idx):
        if lba_idx not in self.cache:
            name=LBAS[lba_idx].replace('_BIN','.BIN')
            e,s=self.F['AAA/DAT/FIELD/SPRT/'+name]
            self.cache[lba_idx]=(name,pack_entries(self.iso.read(e,s)))
        return self.cache[lba_idx]
    def sets(self,lba_idx,tab_idx):
        name,ents=self.pack(lba_idx); tab=ents[tab_idx]
        out={}
        for k in range(0,len(tab)-11,12):
            r=struct.unpack_from('<6h',tab,k)
            if r[0]==0: break
            out.setdefault(r[0],r[1:])
        return out
    def frames(self,lba_idx,anim_idx):
        name,ents=self.pack(lba_idx); b=ents[anim_idx]; fr=[]
        for k in range(0,len(b)-3,16):
            w=struct.unpack_from('<i',b,k)[0]
            if w==-1 or w==0: break
            fr.append(struct.unpack_from('<4i',b,k))
        return fr
    def stand(self,char,lba_idx=None,tab_idx=None,set_id=1,direction=0):
        if lba_idx is None:
            v=self.fe[char]; lba_idx,tab_idx=v>>16,v&0xffff
        s=self.sets(lba_idx,tab_idx)
        anim=s[set_id][direction]
        fr=self.frames(lba_idx,anim)
        name,ents=self.pack(lba_idx)
        img,info=tim_to_rgba(ents[fr[0][1]])
        return img,dict(pack=name,lba_index=lba_idx,table_entry=tab_idx,set=set_id,direction=direction,anim_entry=anim,frame_entry=fr[0][1],offset=(fr[0][2],fr[0][3]),frames_in_anim=len(fr),size=img.shape[:2][::-1])
if __name__=='__main__':
    sp=Sprites()
    for ch in (2,0x9D):
        v=sp.fe[ch]; print(ch,hex(v),LBAS[v>>16],sp.sets(v>>16,v&0xffff))
