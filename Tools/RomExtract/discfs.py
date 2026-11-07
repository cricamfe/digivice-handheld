"""Walk every directory in the ISO9660 path table of game.iso (2048-byte user data)."""
import struct, os
from iso import Iso
def all_files(iso):
    pvd=iso.sector(16)
    pt_size=struct.unpack('<I',pvd[132:136])[0]; pt_lba=struct.unpack('<I',pvd[140:144])[0]
    pt=iso.sector(pt_lba,(pt_size+2047)//2048)[:pt_size]
    dirs=[];i=0
    while i<len(pt):
        nl=pt[i]; ext=struct.unpack('<I',pt[i+2:i+6])[0]; parent=struct.unpack('<H',pt[i+6:i+8])[0]
        name=pt[i+8:i+8+nl].decode('ascii'); dirs.append((name,ext,parent)); i+=8+nl+(nl&1)
    def path(k):
        n,_,p=dirs[k]; return '' if k==0 else (path(p-1)+'/'+n).lstrip('/')
    out=[]
    for k,(_,ext,_) in enumerate(dirs):
        first=iso.sector(ext); size=struct.unpack('<I',first[10:14])[0]
        data=iso.sector(ext,(size+2047)//2048); j=0
        while j<len(data):
            rl=data[j]
            if rl==0: j=(j//2048+1)*2048; continue
            rec=data[j:j+rl]; j+=rl
            if rec[25]&2 or rec[33:33+rec[32]] in (b'\x00',b'\x01'): continue
            name=rec[33:33+rec[32]].decode('ascii').split(';')[0]
            out.append(((path(k)+'/'+name).lstrip('/'),struct.unpack('<I',rec[2:6])[0],struct.unpack('<I',rec[10:14])[0]))
    return out
if __name__=='__main__':
    iso=Iso(); L=all_files(iso); print(len(L))
    z=[x for x in L if 'STAGE' in x[0].upper()]
    print(len(z)); print(z[:5]); print([x for x in L if 'S331' in x[0] or 'S206' in x[0]])
    dirs=sorted({os.path.dirname(x[0]) for x in L}); print(dirs)
