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
describe('dagger knockout controls',()=>{
 it('a saved knockout keeps its wake-up guidance and is never described as monster death',async()=>{
  const {api,mocks,p}=setup();p.status='CONFIRMED';p.version=1;p.events=[{id:'confirm',kind:'CONFIRM_DAGGER_KNOCKOUT',actor:'dm',at:p.at,changes:[{characterId:'b',expectedVersion:5,hp:1,tempHp:0,resources:[]}],reason:'Reviewed',correctionOf:'',summary:'',combatEventId:''}];mocks.attackPreview.mockResolvedValue(p);
  render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();
  expect(screen.queryByText(/此方案采用普通怪物死亡规则/)).toBeNull();
  expect(screen.getByText(/此方案为近战击昏/)).toHaveTextContent('醒来仍倒地');
 });
 it('requires an explicit melee knockout choice and exception review',async()=>{
  const {api}=setup();render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();
  expect(screen.getByRole('combobox',{name:'匕首使用方式'})).toHaveValue('MANUAL');
  expect(screen.getByRole('button',{name:'预览匕首近战击昏'})).toBeDisabled();fireEvent.change(screen.getByLabelText('匕首使用方式'),{target:{value:'THROWN'}});expect(screen.getByRole('button',{name:'预览匕首近战击昏'})).toBeDisabled();
 });
 it('retries the exact uncertain knockout confirmation and never uploads computed values',async()=>{
  const {api,mocks}=setup(),confirm=vi.fn().mockRejectedValueOnce(new TypeError('lost')).mockResolvedValue({id:'p',version:1});Object.assign(mocks,{previewDaggerKnockoutEffect:vi.fn().mockResolvedValue({status:'CANDIDATE',hp:1,tempHp:0,absorbed:0,hpLoss:0}),confirmDaggerKnockoutEffect:confirm});
  render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();fireEvent.change(screen.getByLabelText('匕首使用方式'),{target:{value:'KNOCK_OUT_MELEE'}});fireEvent.click(screen.getByLabelText('我已确认本次匕首近战击昏无免疫或未处理例外，并将人工处理昏迷关联规则、短休与解除'));fireEvent.click(screen.getByRole('button',{name:'预览匕首近战击昏'}));await screen.findByRole('button',{name:'确认匕首近战击昏方案'});fireEvent.change(screen.getByLabelText('结算理由'),{target:{value:'Reviewed'}});fireEvent.click(screen.getByRole('button',{name:'确认匕首近战击昏方案'}));await screen.findByRole('button',{name:'重试同一结算请求'});expect(screen.getByLabelText('匕首使用方式')).toBeDisabled();const first=confirm.mock.calls[0];expect(first[2]).toEqual({requestId:expect.any(String),expectedVersion:0,previewHash:'a'.repeat(64),treatment:'KNOCK_OUT_MELEE',reviewed:true,reason:'Reviewed'});fireEvent.click(screen.getByRole('button',{name:'重试同一结算请求'}));await waitFor(()=>expect(confirm).toHaveBeenCalledTimes(2));expect(confirm.mock.calls[1]).toEqual(first);expect(mocks.attackPreviewCommand).not.toHaveBeenCalled();
 });
 it('clears knockout choice/review on revision and ignores the old plan, but handles late denial',async()=>{
  const {api,mocks}=setup(),late=deferred<unknown>(),denied=vi.fn();Object.assign(mocks,{previewDaggerKnockoutEffect:vi.fn().mockReturnValue(late.promise)});const props={api,campaignId:'c',isDm:true,revision:0,onDenied:denied};const view=render(<AttackPreviewWorkspace {...props}/>);await select();fireEvent.change(screen.getByLabelText('匕首使用方式'),{target:{value:'KNOCK_OUT_MELEE'}});fireEvent.click(screen.getByLabelText('我已确认本次匕首近战击昏无免疫或未处理例外，并将人工处理昏迷关联规则、短休与解除'));fireEvent.click(screen.getByRole('button',{name:'预览匕首近战击昏'}));view.rerender(<AttackPreviewWorkspace {...props} revision={1}/>);await act(async()=>late.resolve({status:'CANDIDATE',hp:1,tempHp:0,absorbed:0,hpLoss:0}));expect(screen.queryByRole('button',{name:'确认匕首近战击昏方案'})).toBeNull();expect(screen.getByLabelText('匕首使用方式')).toHaveValue('MANUAL');
  const next=deferred<unknown>();Object.assign(mocks,{previewDaggerKnockoutEffect:vi.fn().mockReturnValue(next.promise)});fireEvent.change(screen.getByLabelText('匕首使用方式'),{target:{value:'KNOCK_OUT_MELEE'}});fireEvent.click(screen.getByLabelText('我已确认本次匕首近战击昏无免疫或未处理例外，并将人工处理昏迷关联规则、短休与解除'));fireEvent.click(screen.getByRole('button',{name:'预览匕首近战击昏'}));view.rerender(<AttackPreviewWorkspace {...props} revision={2}/>);await act(async()=>next.reject(new ApiError(403,'Forbidden')));expect(denied).toHaveBeenCalledOnce();expect(screen.queryByText(/证据哈希/)).toBeNull();
 });
 it('invalidates a candidate when the explicit knockout treatment or review changes',async()=>{
  const {api,mocks}=setup();Object.assign(mocks,{previewDaggerKnockoutEffect:vi.fn().mockResolvedValue({status:'CANDIDATE',hp:1,tempHp:0,absorbed:0,hpLoss:0})});render(<AttackPreviewWorkspace api={api} campaignId="c" isDm revision={0} onDenied={()=>{}}/>);await select();fireEvent.change(screen.getByLabelText('匕首使用方式'),{target:{value:'KNOCK_OUT_MELEE'}});fireEvent.click(screen.getByLabelText('我已确认本次匕首近战击昏无免疫或未处理例外，并将人工处理昏迷关联规则、短休与解除'));fireEvent.click(screen.getByRole('button',{name:'预览匕首近战击昏'}));await screen.findByRole('button',{name:'确认匕首近战击昏方案'});fireEvent.change(screen.getByLabelText('匕首使用方式'),{target:{value:'MANUAL'}});expect(screen.queryByRole('button',{name:'确认匕首近战击昏方案'})).toBeNull();expect(screen.getByLabelText('我已确认本次匕首近战击昏无免疫或未处理例外，并将人工处理昏迷关联规则、短休与解除')).not.toBeChecked();
 });

});
