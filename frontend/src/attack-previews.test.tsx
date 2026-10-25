import React from 'react';
import {act,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {describe,it,expect,vi} from 'vitest';
import {Api,ApiError} from './api';
import {AttackPreviewWorkspace} from './AttackPreviewWorkspace';
import {emptySheet} from './characters';
import type {AttackPreview} from './attack-previews';
const deferred=<T,>()=>{let resolve!:(v:T)=>void,reject!:(e:unknown)=>void;const promise=new Promise<T>((a,b)=>{resolve=a;reject=b;});return {promise,resolve,reject};};
const fixture=():AttackPreview=>({id:'p',version:0,status:'PREVIEW',actor:'dm',at:'2026-10-25T00:00:00Z',hash:'a'.repeat(64),events:[],evidence:{review:{} as never,source:{id:'a',version:3,sheet:emptySheet('Hero')},target:{id:'b',version:5,sheet:emptySheet('Enemy')},entry:{},manualReasons:[],attackRoll:null,damageRoll:null,actionId:'action',resolution:{review:{candidate:{verdict:'HIT',natural:15,total:17,damageCount:1,damageSides:4}},dice:[2],rawDamage:2,damage:2,explanation:[]}}});
function setup(){const p=fixture();const api={attackPreviews:vi.fn().mockResolvedValue([p]),attackResults:vi.fn().mockResolvedValue([]),characters:vi.fn().mockResolvedValue({campaignVersion:0,characters:[]}),attackPreview:vi.fn().mockResolvedValue(p),attackPreviewCommand:vi.fn().mockResolvedValue({id:'p',version:1})};return {api:api as unknown as Api,mocks:api,p};}
async function select(){await waitFor(()=>expect(screen.getByRole('combobox',{name:'已保存预览'})).not.toBeDisabled());fireEvent.change(screen.getByRole('combobox',{name:'已保存预览'}),{target:{value:'p'}});await screen.findByText('候选伤害：2');}
describe('durable attack preview controls',()=>{
 it('requires explicit final values and DM confirmation before a separate application',async()=>{
  const {api,mocks,p}=setup();render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();
  expect(screen.getByRole('button',{name:'确认最终值'})).toBeDisabled();expect(screen.queryByRole('button',{name:'原子应用已确认结果'})).toBeNull();
  fireEvent.change(screen.getByLabelText('结算理由'),{target:{value:'Checked'}});fireEvent.click(screen.getByLabelText('我已核对最终值及未支持效果'));
  fireEvent.click(screen.getByRole('button',{name:'确认最终值'}));await waitFor(()=>expect(mocks.attackPreviewCommand).toHaveBeenCalledTimes(1));
  const body=mocks.attackPreviewCommand.mock.calls[0][2];expect(body).toMatchObject({kind:'CONFIRM',expectedVersion:0,previewHash:p.hash,changes:[{characterId:'b',expectedVersion:5,hp:1,tempHp:0}]});
 });
 it('locks uncertain writes and retries the exact body without rebasing from refresh',async()=>{
  const {api,mocks,p}=setup();mocks.attackPreviewCommand.mockRejectedValueOnce(new TypeError('network'));
  render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();fireEvent.change(screen.getByLabelText('结算理由'),{target:{value:'Checked'}});fireEvent.click(screen.getByLabelText('我已核对最终值及未支持效果'));fireEvent.click(screen.getByRole('button',{name:'确认最终值'}));
  await screen.findByRole('button',{name:'重试同一结算请求'});expect(screen.getByLabelText('结算理由')).toBeDisabled();const first=mocks.attackPreviewCommand.mock.calls[0][2];mocks.attackPreviews.mockResolvedValue([{...p,version:99}]);fireEvent.click(screen.getByRole('button',{name:'刷新结算'}));
  fireEvent.click(screen.getByRole('button',{name:'重试同一结算请求'}));await waitFor(()=>expect(mocks.attackPreviewCommand).toHaveBeenCalledTimes(2));expect(mocks.attackPreviewCommand.mock.calls[1][2]).toEqual(first);
 });
 it('does not offer write retry after an ack when the authoritative read fails',async()=>{
  const {api,mocks}=setup();render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();mocks.attackPreview.mockRejectedValue(new TypeError('offline'));fireEvent.change(screen.getByLabelText('结算理由'),{target:{value:'Checked'}});fireEvent.click(screen.getByLabelText('我已核对最终值及未支持效果'));fireEvent.click(screen.getByRole('button',{name:'确认最终值'}));
  await screen.findByText(/请求已确认，但读取失败/);expect(screen.queryByRole('button',{name:'重试同一结算请求'})).toBeNull();expect(mocks.attackPreviewCommand).toHaveBeenCalledTimes(1);
 });
 it('observes late 403 independently after another read fails and ignores stale private success',async()=>{
  const {api,mocks}=setup();const late=deferred<AttackPreview[]>();mocks.attackPreviews.mockReturnValue(late.promise);mocks.attackResults.mockRejectedValue(new TypeError('network'));const denied=vi.fn();
  render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={denied}/>);await screen.findByText(/读取结算失败/);await act(async()=>late.reject(new ApiError(403,'revoked')));await waitFor(()=>expect(denied).toHaveBeenCalledTimes(1));expect(screen.queryByRole('combobox',{name:'已保存预览'})).toBeNull();
 });
 it('never requests private previews for a player and clears old API scope on switch',async()=>{
  const {api,mocks}=setup();const view=render(<AttackPreviewWorkspace api={api} campaignId="c" isDm={false} revision={0} onDenied={()=>{}}/>);await screen.findByText('尚无公开结算摘要。');expect(mocks.attackPreviews).not.toHaveBeenCalled();
  view.rerender(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();const newer=setup();view.rerender(<AttackPreviewWorkspace api={newer.api} campaignId="other" isDm revision={0} onDenied={()=>{}}/>);expect(screen.queryByText('候选伤害：2')).toBeNull();
 });
 it('a late denial from the previous API cannot clear the newly selected scope',async()=>{
  const old=setup(),newer=setup(),late=deferred<AttackPreview[]>();old.mocks.attackPreviews.mockReturnValue(late.promise);const denied=vi.fn();const view=render(<AttackPreviewWorkspace api={old.api} campaignId="c" isDm revision={0} onDenied={denied}/>);
  view.rerender(<AttackPreviewWorkspace api={newer.api} campaignId="c" isDm revision={0} onDenied={denied}/>);await select();await act(async()=>late.reject(new ApiError(403,'old domain')));expect(denied).not.toHaveBeenCalled();expect(screen.getByText('候选伤害：2')).toBeVisible();
 });
 it('an applied preview requires current values before linked correction',async()=>{
  const {api,mocks,p}=setup();p.status='APPLIED';p.version=2;p.events=[{id:'apply',kind:'APPLY',actor:'dm',at:p.at,reason:'original',changes:[],correctionOf:'',summary:'',combatEventId:'event'}];mocks.attackPreviews.mockResolvedValue([p]);mocks.attackPreview.mockResolvedValue(p);
  Object.assign(mocks,{character:vi.fn().mockImplementation((_c:string,id:string)=>Promise.resolve({id,version:9,sheet:emptySheet(id),calculations:{},canEdit:true,canManage:true,expiredConditionIds:[]}))});render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();expect(screen.getByRole('button',{name:'追加当前值更正'})).toBeDisabled();
  fireEvent.click(screen.getByRole('button',{name:'读取当前值准备更正'}));await screen.findByLabelText('最终生命值 b');fireEvent.change(screen.getByLabelText('最终生命值 b'),{target:{value:'0'}});fireEvent.change(screen.getByLabelText('结算理由'),{target:{value:'Preserve later play'}});fireEvent.change(screen.getByLabelText('更正所关联的应用'),{target:{value:'apply'}});fireEvent.click(screen.getByLabelText('我已核对最终值及未支持效果'));fireEvent.click(screen.getByRole('button',{name:'追加当前值更正'}));await waitFor(()=>expect(mocks.attackPreviewCommand).toHaveBeenCalled());expect(mocks.attackPreviewCommand.mock.calls[0][2]).toMatchObject({kind:'CORRECT',expectedVersion:2,previewHash:p.hash,correctionOf:'apply',changes:expect.arrayContaining([{characterId:'b',expectedVersion:9,hp:0,tempHp:0,resources:[]}])});
 });
});
