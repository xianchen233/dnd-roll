import {expect,it,vi} from 'vitest';
import {Api} from './api';
it('encodes movement routes and preserves exact context, steps and DM adjustments',async()=>{
 const request=vi.fn().mockResolvedValueOnce(Response.json({token:'csrf',headerName:'X-CSRF-TOKEN'})).mockImplementation(()=>Promise.resolve(Response.json({})));const api=new Api(request);
 const body={requestId:'r',expectedVersion:1,sceneVersion:2,characterVersion:3,budgetVersion:'b',turnKey:4,reviewed:true,path:[{x:1,y:2,mode:'WALK' as const,difficult:true}],override:false,cost:0,reason:''};
 await api.previewMovement('c/x','t/x',body);await api.movePath('c/x','t/x',body);await api.movement('c/x','t/x');
 expect(request.mock.calls.map(c=>c[0])).toEqual(['/api/auth/csrf','/api/campaigns/c%2Fx/tokens/t%2Fx/movement/preview','/api/campaigns/c%2Fx/tokens/t%2Fx/movement/paths','/api/campaigns/c%2Fx/tokens/t%2Fx/movement']);
 expect(request.mock.calls[1][1]).toMatchObject({method:'POST',credentials:'include',headers:{'X-CSRF-TOKEN':'csrf'},body:JSON.stringify(body)});expect(request.mock.calls[2][1].body).toEqual(JSON.stringify(body));
});
