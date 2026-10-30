import assert from 'node:assert/strict';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required} from './acceptance-support.mjs';
import {prepareBinding,bindingCommand,finishBinding,verifyBinding,resumeBinding} from './binding-evidence.mjs';
const [mode,file]=process.argv.slice(2);assert.ok(['seed','verify','resume'].includes(mode)&&file);const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
if(mode==='seed'){const e=await prepareBinding(dm,player),command=await bindingCommand(dm,e,e.prepareNode),ack=await dm.post(e.root+'/module-operation-bindings/'+e.prepareNode+'/bind',command);await finishBinding(dm,player,e,command,ack);await writeFile(file,JSON.stringify({entries:[e]},null,2),{flag:'wx'});await verifyBinding(dm,player,e);}else for(const e of JSON.parse(await readFile(file,'utf8')).entries){if(mode==='verify')await verifyBinding(dm,player,e);else await writeFile(file+'.resumed.json',JSON.stringify(await resumeBinding(dm,player,e),null,2),{flag:'wx'});}
