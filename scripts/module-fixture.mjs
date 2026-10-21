// Synthetic original content only. Shared by browser and persistence acceptance.
import {randomUUID,createHash} from 'node:crypto';
import {sourcePng} from './acceptance-support.mjs';
const hash=b=>createHash('sha256').update(b).digest('hex');
function crc(bytes){let c=0xffffffff;for(const b of bytes){c^=b;for(let bit=0;bit<8;bit++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;}
export function zipEntries(entries){
 const local=[],central=[];let offset=0;
 for(const [name,value]of Object.entries(entries)){
  const filename=Buffer.from(name),bytes=Buffer.from(value),sum=crc(bytes),l=Buffer.alloc(30),c=Buffer.alloc(46);
  l.writeUInt32LE(0x04034b50);l.writeUInt16LE(20,4);l.writeUInt32LE(sum,14);l.writeUInt32LE(bytes.length,18);l.writeUInt32LE(bytes.length,22);l.writeUInt16LE(filename.length,26);
  c.writeUInt32LE(0x02014b50);c.writeUInt16LE(20,4);c.writeUInt16LE(20,6);c.writeUInt32LE(sum,16);c.writeUInt32LE(bytes.length,20);c.writeUInt32LE(bytes.length,24);c.writeUInt16LE(filename.length,28);c.writeUInt32LE(offset,42);
  local.push(l,filename,bytes);central.push(c,filename);offset+=l.length+filename.length+bytes.length;
 }
 const directory=Buffer.concat(central),end=Buffer.alloc(22);end.writeUInt32LE(0x06054b50);end.writeUInt16LE(Object.keys(entries).length,8);end.writeUInt16LE(Object.keys(entries).length,10);end.writeUInt32LE(directory.length,12);end.writeUInt32LE(offset,16);return Buffer.concat([...local,directory,end]);
}
export function moduleFixture({moduleId=randomUUID(),version='1.0.0',rules='5R',name='Synthetic adventure'}={}){
 const imageId=randomUUID(),dependencyId=randomUUID(),nodeId=randomUUID(),image=sourcePng(),content=Buffer.from(JSON.stringify({nodes:[{id:nodeId,kind:'SCENE',name:'Private tower',text:'<script>literal module text</script>',visibility:'DM',nodeIds:[],assetIds:[imageId],dependencyIds:[dependencyId]}]}));
 const manifest={format:'dnd-roll.module',formatVersion:1,moduleId,moduleVersion:version,name,rules,rights:'Synthetic original acceptance fixture; permitted sharing',dependencies:[{id:dependencyId,version:'expansion-2',rules:'2014'}],content:{size:content.length,sha256:hash(content)},assets:[{id:imageId,mediaType:'image/png',size:image.length,sha256:hash(image)}]};
 return {manifest,bytes:zipEntries({'manifest.json':JSON.stringify(manifest),'content.json':content,['assets/'+imageId]:image})};
}
