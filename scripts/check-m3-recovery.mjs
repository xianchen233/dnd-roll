import assert from 'node:assert/strict';
import {readFile,writeFile} from 'node:fs/promises';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {Session,required} from './acceptance-support.mjs';

const names=['damage-effects','ammunition-effects','monster-death','knockout','dagger-knockout','knockout-healing','knockout-rest','knockout-first-aid','stabilization','stabilization-healing','stabilization-damage','restabilization','restabilization-healing','restabilization-damage','instability-healing','initial-instability-healing'];
const [mode,folder]=process.argv.slice(2);assert.ok(['verify','resume','capture','compare'].includes(mode)&&folder);
const cases=await Promise.all(names.map(async(name,i)=>{const file=`${folder}/a${String(i+8).padStart(2,'0')}.json`;return {name,file,data:JSON.parse(await readFile(file,'utf8'))};}));
assert.equal(new Set(cases.map(e=>e.data.root)).size,16,'Independent campaign per effect fixture');
if(mode==='verify'||mode==='resume'){
 for(const c of cases){const script=fileURLToPath(new URL(`check-${c.name}-restart.mjs`,import.meta.url));const output=execFileSync(process.execPath,[script,mode,c.file],{encoding:'utf8',env:process.env,timeout:120000});console.log(`A${names.indexOf(c.name)+8}: ${output.trim().split('\n').at(-1)}`);}
 console.log(`M3 ${mode.toUpperCase()} VERIFIED: all 16 A08–A23 cases; original receipts, private evidence, saved plans and bounded continuations checked`);
}else{
 const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD'));
 const snapshot={};
 for(const {data:e} of cases){
  const urls=['/characters','/rolls','/actions','/combat','/initiative','/attack-previews','/attack-results'];const listing=await dm.get(e.root+'/characters');
  for(const card of listing.characters)for(const suffix of ['','/history','/stabilization','/stabilization/history','/stabilization/healing-history','/stabilization/damage-history','/stabilization/restabilization','/stabilization/restabilization/history','/stabilization/restabilization/healing-history','/stabilization/restabilization/damage-history','/stabilization/restabilization/instability/healing-history','/stabilization/instability/healing-history'])urls.push('/characters/'+card.id+suffix);
  snapshot[e.root]={};for(const url of urls)snapshot[e.root][url]=await dm.get(e.root+url);
 }
 const file=folder+'/post-resume.json';if(mode==='capture')await writeFile(file,JSON.stringify(snapshot,null,2),{flag:'wx'});else assert.deepEqual(snapshot,JSON.parse(await readFile(file,'utf8')));
 console.log(`M3 ${mode.toUpperCase()} VERIFIED: full post-continuation cards/histories/ledgers/dice/actions/initiative and previews for 16 independent campaigns`);
}
