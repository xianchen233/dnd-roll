import {it,expect,vi} from 'vitest';
import {Api} from './api';
it('uses encoded character scope, session CSRF and exact unchanged request bodies',async()=>{
 const request=vi.fn().mockResolvedValueOnce(Response.json({token:'csrf',headerName:'X-CSRF-TOKEN'})).mockImplementation(()=>Promise.resolve(Response.json({id:'t',version:1})));const api=new Api(request);
 const input={requestId:'r',expectedVersion:0,targetVersion:2,helperId:'h',helperVersion:3,reviewed:true,reason:'Reviewed'},review={expectedVersion:1,targetVersion:2,helperId:'h',helperVersion:3,rollId:'roll',reviewed:true},apply={...review,requestId:'apply',planHash:'a'.repeat(64),reason:'Apply'};
 await api.rollStabilization('c/x','t/x',input);await api.previewStabilization('c/x','t/x',review);await api.applyStabilization('c/x','t/x',apply);
 expect(request.mock.calls.map(c=>c[0])).toEqual(['/api/auth/csrf',...['rolls','preview','applications'].map(s=>'/api/campaigns/c%2Fx/characters/t%2Fx/stabilization/'+s)]);
 for(const [i,body]of [input,review,apply].entries()){const init=request.mock.calls[i+1][1];expect(init.method).toBe('POST');expect(init.credentials).toBe('include');expect(init.headers['X-CSRF-TOKEN']).toBe('csrf');expect(JSON.parse(init.body)).toEqual(body);}
});
