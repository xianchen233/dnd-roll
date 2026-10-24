import assert from 'node:assert/strict';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required} from './acceptance-support.mjs';
const [mode,file]=process.argv.slice(2);assert.ok(['capture','verify'].includes(mode)&&file);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD'));
if(mode==='capture'){
 const campaigns=(await dm.get('/campaigns')).filter(c=>c.role==='DM').sort((a,b)=>Number(/combat|P06/i.test(b.name))-Number(/combat|P06/i.test(a.name)));
 let saved;
 for(const campaign of campaigns){
  const root='/campaigns/'+campaign.id,listing=await dm.get(root+'/actions');
  const action=listing.actions.find(a=>!a.events.some(e=>e.kind==='APPLY'));
  if(action){saved={root,action:await dm.get(root+'/actions/'+action.id),source:await dm.get(root+'/characters/'+action.characterId),target:await dm.get(root+'/characters/'+action.targetId),combat:await dm.get(root+'/combat')};break;}
 }
 assert.ok(saved,'acceptance database must contain an unapplied manual decision');
 await writeFile(file,JSON.stringify(saved,null,2),{flag:'wx'});
 console.log('PASS: unapplied manual action, original rolls/decision, source/target resources and current combat captured without changing frozen source');
}else{
 const s=JSON.parse(await readFile(file,'utf8'));
 assert.deepEqual(await dm.get(s.root+'/actions/'+s.action.id),s.action);
 assert.deepEqual(await dm.get(s.root+'/characters/'+s.action.characterId),s.source);
 assert.deepEqual(await dm.get(s.root+'/characters/'+s.action.targetId),s.target);
 assert.deepEqual(await dm.get(s.root+'/combat'),s.combat);
 assert.ok(!s.action.events.some(e=>e.kind==='APPLY'));
 console.log('PASS: isolated restore retains the pending manual result and original rolls/versions/resources; startup did not apply it');
}
