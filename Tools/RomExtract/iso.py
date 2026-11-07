import os
import struct
ISO=os.environ["DW3_ISO"]
class Iso:
    def __init__(self, path=ISO):
        self.f=open(path,'rb')
    def sector(self,lba,n=1):
        self.f.seek(lba*2048); return self.f.read(2048*n)
    def listdir(self,lba,size):
        data=self.sector(lba,(size+2047)//2048)
        out=[];p=0
        while p<len(data):
            l=data[p]
            if l==0:
                p=(p//2048+1)*2048; 
                if p>=len(data):break
                continue
            ext=struct.unpack_from('<I',data,p+2)[0]; sz=struct.unpack_from('<I',data,p+10)[0]
            flags=data[p+25]; nl=data[p+32]; name=data[p+33:p+33+nl].decode('ascii','replace')
            out.append((name,ext,sz,bool(flags&2))); p+=l
        return out
    def root(self):
        pvd=self.sector(16)
        assert pvd[1:6]==b'CD001'
        ext=struct.unpack_from('<I',pvd,156+2)[0]; sz=struct.unpack_from('<I',pvd,156+10)[0]
        return ext,sz
    def walk(self,lba=None,size=None,prefix=''):
        if lba is None: lba,size=self.root()
        for name,ext,sz,isdir in self.listdir(lba,size):
            if name in('\x00','\x01'):continue
            if isdir: yield from self.walk(ext,sz,prefix+name+'/')
            else: yield prefix+name,ext,sz
    def read(self,lba,size): return self.sector(lba,(size+2047)//2048)[:size]
if __name__=='__main__':
    iso=Iso(); L=list(iso.walk())
    print(len(L))
    import sys
    for n,e,s in L[:60]: print(n,e,s)
    print([x for x in L if 'S331' in x[0] or 'S206' in x[0]])
