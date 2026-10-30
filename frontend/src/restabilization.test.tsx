import React from 'react';
import { describe,it,expect,vi } from 'vitest';
import { render,screen,fireEvent,waitFor } from '@testing-library/react';
import { ApiError,type Api } from './api';
import { emptySheet } from './characters';
import { RestabilizationWorkspace } from './RestabilizationWorkspace';
import { StabilizationWorkspace } from './StabilizationWorkspace';
const reviewLabel='我已确认原失稳目标仍存活且未稳定，本次为新的合法Help动作与接触，无未处理例外';
function setup(latest=false,total=20){
 const sheet=emptySheet('Target');sheet.combat.hp=0;const helper=emptySheet('Helper');
 const attempt={id:'r',originVersion:3,status:'ROLLED',target:{id:'t',version:2,sheet},helper:{id:'h',version:3,sheet:helper},roll:{id:'r',result:{total}},reviewed:true};
 let state={id:'t',version:latest?1:0,latest:latest?attempt:null};
 const mocks={stabilization:vi.fn().mockImplementation(async()=>({id:'t',version:state.latest?.status==='APPLIED'?4:3,latest:{status:state.latest?.status==='APPLIED'?'RESTABILIZED':'UNSTABLE'}})),restabilization:vi.fn().mockImplementation(async()=>state),characters:vi.fn().mockResolvedValue({characters:[{id:'t',name:'Target'},{id:'h',name:'Helper'},{id:'x',name:'Other'}]}),character:vi.fn().mockImplementation(async(_c:string,id:string)=>({id,version:id==='t'?2:3,sheet:id==='t'?sheet:helper})),rollRestabilization:vi.fn().mockImplementation(async()=>{state={id:'t',version:1,latest:attempt};return {id:'t',version:1};}),previewRestabilization:vi.fn().mockResolvedValue({id:'t',version:1,targetVersion:2,helperVersion:3,rollId:'r',hash:'a'.repeat(64),effect:{status:total>=10?'CANDIDATE':'MANUAL_REQUIRED',dc:10,total,hp:total>=10?0:null,tempHp:7,deathSuccesses:0,deathFailures:0,reasons:total>=10?[]:['RESTABILIZATION_CHECK_FAILED']}}),applyRestabilization:vi.fn().mockImplementation(async()=>{state={id:'t',version:2,latest:{...attempt,status:'APPLIED'}};return {id:'t',version:2};})};
 const changed=vi.fn(),denied=vi.fn();const props={api:mocks as unknown as Api,campaignId:'c',targetId:'t',revision:0,onChanged:changed,onDenied:denied};return {mocks,props,changed,denied};
}
async function prepare(){await screen.findByRole('option',{name:'Helper'});fireEvent.change(screen.getByLabelText('再次稳定施救者'),{target:{value:'h'}});fireEvent.click(screen.getByLabelText(reviewLabel));fireEvent.change(screen.getByLabelText('再次稳定理由'),{target:{value:'Reviewed Help'}});}
describe('explicit zero HP restabilization',()=>{
 it('saves check then previews then applies, with captured unknown retries and no client final values',async()=>{
  const {mocks,props}=setup();mocks.rollRestabilization.mockRejectedValueOnce(new TypeError('lost roll'));mocks.applyRestabilization.mockRejectedValueOnce(new TypeError('lost apply'));render(<RestabilizationWorkspace {...props}/>);await prepare();
  fireEvent.click(screen.getByRole('button',{name:'进行新的Help医药检定'}));await screen.findByRole('button',{name:'重试同一再次稳定操作'});const body=mocks.rollRestabilization.mock.calls[0][2];expect(body).not.toHaveProperty('total');expect(body).not.toHaveProperty('boundCheck');
  fireEvent.click(screen.getByRole('button',{name:'重试同一再次稳定操作'}));await waitFor(()=>expect(mocks.rollRestabilization).toHaveBeenCalledTimes(2));expect(mocks.rollRestabilization.mock.calls[1][2]).toEqual(body);await screen.findByText(/已保存新医药检定：20/);expect(mocks.applyRestabilization).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button',{name:'预览再次稳定方案'}));await screen.findByText(/方案：再次稳定，仍0HP且昏迷/);fireEvent.click(screen.getByRole('button',{name:'确认再次稳定'}));await screen.findByRole('button',{name:'重试同一再次稳定操作'});const apply=mocks.applyRestabilization.mock.calls[0][2];expect(apply).toMatchObject({rollId:'r',planHash:'a'.repeat(64)});expect(apply).not.toHaveProperty('hp');fireEvent.click(screen.getByRole('button',{name:'重试同一再次稳定操作'}));await screen.findByText(/已再次稳定/);expect(mocks.applyRestabilization.mock.calls[1][2]).toEqual(apply);
 });
 it('loads persisted check without reroll and a failed check cannot be applied',async()=>{
  const {props,mocks}=setup(true,1);render(<RestabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'预览再次稳定方案'}));await screen.findByText(/RESTABILIZATION_CHECK_FAILED/);expect(screen.queryByRole('button',{name:'确认再次稳定'})).not.toBeInTheDocument();expect(mocks.rollRestabilization).not.toHaveBeenCalled();
 });
 it('invalidates plan on review or helper changes',async()=>{
  const {props}=setup(true);render(<RestabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'预览再次稳定方案'}));await screen.findByText(/方案：再次稳定/);fireEvent.click(screen.getByLabelText(reviewLabel));expect(screen.queryByRole('button',{name:'确认再次稳定'})).not.toBeInTheDocument();
 });
 it('clears private state on denied read and calls the scope owner',async()=>{
  const {props,mocks,denied}=setup();mocks.restabilization.mockRejectedValue(new ApiError(403,'revoked'));render(<RestabilizationWorkspace {...props}/>);await waitFor(()=>expect(denied).toHaveBeenCalled());expect(screen.queryByText(/已保存新医药检定/)).not.toBeInTheDocument();
 });
 it('preserves the exact unknown request across revision invalidation',async()=>{
  const {props,mocks}=setup();mocks.rollRestabilization.mockRejectedValueOnce(new TypeError('lost'));const view=render(<RestabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'进行新的Help医药检定'}));await screen.findByRole('button',{name:'重试同一再次稳定操作'});const captured=mocks.rollRestabilization.mock.calls[0][2];view.rerender(<RestabilizationWorkspace {...props} revision={1}/>);fireEvent.click(screen.getByRole('button',{name:'重试同一再次稳定操作'}));await waitFor(()=>expect(mocks.rollRestabilization).toHaveBeenCalledTimes(2));expect(mocks.rollRestabilization.mock.calls[1][2]).toEqual(captured);
 });
 it('late denied preview clears private state even after a newer read',async()=>{
  const {props,mocks,denied}=setup(true);let reject!:()=>void;mocks.previewRestabilization.mockImplementationOnce(()=>new Promise((_yes,no)=>{reject=()=>no(new ApiError(403,'revoked'));}));const view=render(<RestabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'预览再次稳定方案'}));await waitFor(()=>expect(mocks.previewRestabilization).toHaveBeenCalled());view.rerender(<RestabilizationWorkspace {...props} revision={1}/>);reject();await waitFor(()=>expect(denied).toHaveBeenCalled());expect(screen.queryByText(/已保存新医药检定/)).not.toBeInTheDocument();
 });
 it('known conflict recovers by read, without an automatic new attempt',async()=>{
  const {props,mocks}=setup();mocks.rollRestabilization.mockRejectedValueOnce(new ApiError(409,'stale'));render(<RestabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'进行新的Help医药检定'}));await screen.findByRole('alert');expect(screen.queryByRole('button',{name:'重试同一再次稳定操作'})).not.toBeInTheDocument();expect(mocks.rollRestabilization).toHaveBeenCalledTimes(1);await waitFor(()=>expect(mocks.restabilization).toHaveBeenCalledTimes(2));
 });
});
it('old A16 controls remain historical after RESTABILIZED',async()=>{const {mocks,props}=setup();mocks.stabilization.mockResolvedValue({id:'t',version:4,latest:{status:'RESTABILIZED',helper:{id:'h',sheet:emptySheet('Old helper')},roll:{result:{total:20}}}} as never);render(<StabilizationWorkspace {...props}/>);await screen.findByText(/已保存医药检定/);expect(screen.queryByRole('button',{name:'进行Help医药检定'})).not.toBeInTheDocument();expect(screen.queryByRole('button',{name:'确认应用稳定记录'})).not.toBeInTheDocument();await screen.findByText(/原失稳记录已再次稳定/);});
