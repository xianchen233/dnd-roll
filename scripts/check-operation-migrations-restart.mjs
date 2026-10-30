import assert from 'node:assert/strict';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required} from './acceptance-support.mjs';
import {prepareSelected,selectedCommand,finishSelected,verifySelected,resumeSelected} from './operation-migration-evidence.mjs';
const [mode,file]=process.argv.slice(2);assert.ok(['seed','verify','resume'].includes(mode)&&file);const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
if(mode==='seed'){const e=await prepareSelected(dm,player),command=await selectedCommand(dm,e),ack=await dm.post(e.root+'/module-instance/operation-migrations',command);await finishSelected(dm,player,e,command,ack);await writeFile(file,JSON.stringify({entries:[e]},null,2),{flag:'wx'});await verifySelected(dm,player,e);}else for(const e of JSON.parse(await readFile(file,'utf8')).entries){if(mode==='verify')await verifySelected(dm,player,e);else await writeFile(file+'.resumed.json',JSON.stringify(await resumeSelected(dm,player,e),null,2),{flag:'wx'});}
