// Synthetic UI acceptance state, shared by browser capture and fresh-session
// restart/isolated-restore verification. No database shortcuts or UI mocks.
import {createHash} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
export async function controlsSnapshot(get,bytes,e){
 const result={},save=async path=>result[path]=await get(path);
 await save(e.source);await save(e.source+'/updates');
 for(const h of result[e.source+'/updates'])result[e.source+'/updates/'+h.version+'/content']=createHash('sha256').update(await bytes(e.source+'/updates/'+h.version+'/content')).digest('hex');
 const migrations=await get(e.root+'/module-instance/migrations');
 for(const root of [e.root,...migrations.map(x=>'/api/campaigns/'+x.recoveryCampaignId)]){
  for(const suffix of ['/module-instance','/scene','/characters','/initiative','/initiative/history','/combat','/combat/history','/notes/export?format=json','/timeline/export?format=json','/module-instance/updates','/module-instance/migrations','/module-operations'])await save(root+suffix);
  for(const op of result[root+'/module-operations'])await save(root+'/module-operations/'+op.id+'/executions');
  for(const card of result[root+'/characters'].characters)for(const suffix of ['','/access','/history'])await save(root+'/characters/'+card.id+suffix);
  const scene=result[root+'/scene'];await save(root+'/scenes/'+scene.id+'/tokens');await save(root+'/scenes/'+scene.id+'/fog');
  for(const id of new Set(Object.values(result[root+'/module-instance'].mapping.assets)))result[root+'/assets/'+id+'/content']=createHash('sha256').update(await bytes(root+'/assets/'+id+'/content')).digest('hex');
 }
 return result;
}
export async function appendControlsEvidence(file,entry){let entries=[];try{entries=JSON.parse(await readFile(file,'utf8'));}catch(e){if(e.code!=='ENOENT')throw e;}entries.push(entry);await writeFile(file,JSON.stringify(entries,null,2));}
