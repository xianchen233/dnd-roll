import {expect,it,vi} from 'vitest';
import {Api} from './api';
it('keeps preview identity, full command/hash and same-origin encoded routes intact',async()=>{
 const request=vi.fn().mockResolvedValueOnce(Response.json([])).mockResolvedValueOnce(Response.json({token:'csrf',headerName:'X-CSRF-TOKEN'})).mockImplementation(()=>Promise.resolve(Response.json({id:'p',version:1})));const api=new Api(request);
 const create={requestId:'r',characterId:'a',sourceVersion:3,targetId:'b',targetVersion:5,attackId:'trusted',reviewed:true,exceptions:[],advantage:[],disadvantage:[],immunities:[],resistances:[],vulnerabilities:[]};
 const command={requestId:'same',expectedVersion:1,kind:'APPLY' as const,previewHash:'a'.repeat(64),changes:[],correctionOf:'',reason:'Reviewed',summary:''};
 await api.attackPreviews('c/x');await api.createAttackPreview('c/x',create);await api.attackPreview('c/x','p/x');await api.attackPreviewCommand('c/x','p/x',command);await api.attackResults('c/x');
 expect(request.mock.calls.map(c=>c[0])).toEqual(['/api/campaigns/c%2Fx/attack-previews','/api/auth/csrf','/api/campaigns/c%2Fx/attack-previews','/api/campaigns/c%2Fx/attack-previews/p%2Fx','/api/campaigns/c%2Fx/attack-previews/p%2Fx/events','/api/campaigns/c%2Fx/attack-results']);
 expect(request.mock.calls[2][1]).toMatchObject({method:'POST',credentials:'include',body:JSON.stringify(create)});expect(request.mock.calls[4][1]).toMatchObject({method:'POST',credentials:'include',headers:{'X-CSRF-TOKEN':'csrf'},body:JSON.stringify(command)});
});
