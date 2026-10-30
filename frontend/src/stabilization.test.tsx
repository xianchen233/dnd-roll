import React from 'react';
import { describe,it,expect,vi } from 'vitest';
import { render,screen,fireEvent,waitFor } from '@testing-library/react';
import { ApiError,type Api } from './api';
import { emptySheet } from './characters';
import { StabilizationWorkspace } from './StabilizationWorkspace';
const reviewLabel='我已确认目标为仍存活且未稳定的普通0HP角色，本次Help动作与接触合法，无未处理例外';
function setup(latest=false,total=20){
 const sheet=emptySheet('Target');sheet.combat.hp=0;const helper=emptySheet('Helper');
 const attempt={id:'r',status:'ROLLED',target:{id:'t',version:2,sheet},helper:{id:'h',version:3,sheet:helper},roll:{id:'r',result:{total}},reviewed:true};
 let state={id:'t',version:latest?1:0,latest:latest?attempt:null};
 const mocks={stabilization:vi.fn().mockImplementation(async()=>state),characters:vi.fn().mockResolvedValue({characters:[{id:'t',name:'Target'},{id:'h',name:'Helper'},{id:'x',name:'Other'}]}),character:vi.fn().mockImplementation(async(_c:string,id:string)=>({id,version:id==='t'?2:3,sheet:id==='t'?sheet:helper})),rollStabilization:vi.fn().mockImplementation(async()=>{state={id:'t',version:1,latest:attempt};return {id:'t',version:1};}),previewStabilization:vi.fn().mockResolvedValue({id:'t',version:1,targetVersion:2,helperVersion:3,rollId:'r',hash:'a'.repeat(64),effect:{status:total>=10?'CANDIDATE':'MANUAL_REQUIRED',dc:10,total,hp:total>=10?0:null,tempHp:7,deathSuccesses:0,deathFailures:0,reasons:total>=10?[]:['STABILIZATION_CHECK_FAILED']}}),applyStabilization:vi.fn().mockImplementation(async()=>{state={id:'t',version:2,latest:{...attempt,status:'APPLIED'}};return {id:'t',version:2};})};
 const changed=vi.fn(),denied=vi.fn();const props={api:mocks as unknown as Api,campaignId:'c',targetId:'t',revision:0,onChanged:changed,onDenied:denied};return {mocks,props,changed,denied};
}
async function prepare(){await screen.findByRole('option',{name:'Helper'});fireEvent.change(screen.getByLabelText('稳定施救者'),{target:{value:'h'}});fireEvent.click(screen.getByLabelText(reviewLabel));fireEvent.change(screen.getByLabelText('稳定操作理由'),{target:{value:'Reviewed Help'}});}
describe('explicit zero HP stabilization',()=>{
 it('saves check then previews then applies, with captured unknown retries and no client final values',async()=>{
  const {mocks,props}=setup();mocks.rollStabilization.mockRejectedValueOnce(new TypeError('lost roll'));mocks.applyStabilization.mockRejectedValueOnce(new TypeError('lost apply'));render(<StabilizationWorkspace {...props}/>);await prepare();
  fireEvent.click(screen.getByRole('button',{name:'进行Help医药检定'}));await screen.findByRole('button',{name:'重试同一稳定操作'});const body=mocks.rollStabilization.mock.calls[0][2];expect(body).not.toHaveProperty('total');expect(body).not.toHaveProperty('boundCheck');
  fireEvent.click(screen.getByRole('button',{name:'重试同一稳定操作'}));await waitFor(()=>expect(mocks.rollStabilization).toHaveBeenCalledTimes(2));expect(mocks.rollStabilization.mock.calls[1][2]).toEqual(body);await screen.findByText(/已保存医药检定：20/);expect(mocks.applyStabilization).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button',{name:'预览稳定方案'}));await screen.findByText(/方案：稳定，仍0HP且昏迷/);fireEvent.click(screen.getByRole('button',{name:'确认应用稳定记录'}));await screen.findByRole('button',{name:'重试同一稳定操作'});const apply=mocks.applyStabilization.mock.calls[0][2];expect(apply).toMatchObject({rollId:'r',planHash:'a'.repeat(64)});expect(apply).not.toHaveProperty('hp');fireEvent.click(screen.getByRole('button',{name:'重试同一稳定操作'}));await screen.findByText(/已应用稳定记录/);expect(mocks.applyStabilization.mock.calls[1][2]).toEqual(apply);
 });
 it('loads persisted check without reroll and a failed check cannot be applied',async()=>{
  const {props,mocks}=setup(true,1);render(<StabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'预览稳定方案'}));await screen.findByText(/STABILIZATION_CHECK_FAILED/);expect(screen.queryByRole('button',{name:'确认应用稳定记录'})).not.toBeInTheDocument();expect(mocks.rollStabilization).not.toHaveBeenCalled();
 });
 it('invalidates plan on review or helper changes',async()=>{
  const {props}=setup(true);render(<StabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'预览稳定方案'}));await screen.findByText(/方案：稳定/);fireEvent.click(screen.getByLabelText(reviewLabel));expect(screen.queryByRole('button',{name:'确认应用稳定记录'})).not.toBeInTheDocument();
 });
 it('clears private state on denied read and calls the scope owner',async()=>{
  const {props,mocks,denied}=setup();mocks.stabilization.mockRejectedValue(new ApiError(403,'revoked'));render(<StabilizationWorkspace {...props}/>);await waitFor(()=>expect(denied).toHaveBeenCalled());expect(screen.queryByText(/已保存医药检定/)).not.toBeInTheDocument();
 });
 it('preserves the exact unknown request across revision invalidation',async()=>{
  const {props,mocks}=setup();mocks.rollStabilization.mockRejectedValueOnce(new TypeError('lost'));const view=render(<StabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'进行Help医药检定'}));await screen.findByRole('button',{name:'重试同一稳定操作'});const captured=mocks.rollStabilization.mock.calls[0][2];view.rerender(<StabilizationWorkspace {...props} revision={1}/>);fireEvent.click(screen.getByRole('button',{name:'重试同一稳定操作'}));await waitFor(()=>expect(mocks.rollStabilization).toHaveBeenCalledTimes(2));expect(mocks.rollStabilization.mock.calls[1][2]).toEqual(captured);
 });
 it('late denied preview clears private state even after a newer read',async()=>{
  const {props,mocks,denied}=setup(true);let reject!:()=>void;mocks.previewStabilization.mockImplementationOnce(()=>new Promise((_yes,no)=>{reject=()=>no(new ApiError(403,'revoked'));}));const view=render(<StabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'预览稳定方案'}));await waitFor(()=>expect(mocks.previewStabilization).toHaveBeenCalled());view.rerender(<StabilizationWorkspace {...props} revision={1}/>);reject();await waitFor(()=>expect(denied).toHaveBeenCalled());expect(screen.queryByText(/已保存医药检定/)).not.toBeInTheDocument();
 });
 it('known conflict recovers by read, without an automatic new attempt',async()=>{
  const {props,mocks}=setup();mocks.rollStabilization.mockRejectedValueOnce(new ApiError(409,'stale'));render(<StabilizationWorkspace {...props}/>);await prepare();fireEvent.click(screen.getByRole('button',{name:'进行Help医药检定'}));await screen.findByRole('alert');expect(screen.queryByRole('button',{name:'重试同一稳定操作'})).not.toBeInTheDocument();expect(mocks.rollStabilization).toHaveBeenCalledTimes(1);await waitFor(()=>expect(mocks.stabilization).toHaveBeenCalledTimes(2));
 });
});
