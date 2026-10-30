import {act,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {describe,it,expect,vi} from 'vitest';
import {ApiError,type Api} from './api';
import {BackupWorkspace} from './BackupWorkspace';
const status={version:4,retention:7,paused:false,jobs:[{id:'b1',actor:'operator',expectedVersion:3,kind:'MANUAL',state:'COMPLETE',at:'2026-10-21T00:00:00Z',finishedAt:'2026-10-21T00:01:00Z',bytes:4000}]};
function api(extra={}){return {backups:vi.fn().mockResolvedValue(status),createBackup:vi.fn().mockResolvedValue({id:'new',version:5}),...extra};}
function deferred(){let resolve!:(v:any)=>void;const promise=new Promise<any>(r=>resolve=r);return {promise,resolve};}
describe('deployment backup management',()=>{
 it('shows persistent tasks, failures and authenticated archive links',async()=>{
  const mock=api();render(<BackupWorkspace api={mock as unknown as Api}/>);
  expect(await screen.findByRole('link',{name:'下载备份 b1'})).toHaveAttribute('href','/api/admin/backups/b1/content');
  expect(screen.getByText(/自动备份保留最近 7 份/)).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button',{name:'生成手动备份'}));await waitFor(()=>expect(mock.createBackup).toHaveBeenCalledWith(expect.objectContaining({expectedVersion:4}),false));
 });
 it('retains the original request after an uncertain result and requires a new status read',async()=>{
  const mock=api({createBackup:vi.fn().mockRejectedValueOnce(Error('lost ack')).mockResolvedValue({id:'new',version:5})});render(<BackupWorkspace api={mock as unknown as Api}/>);
  const button=await screen.findByRole('button',{name:'生成手动备份'});await waitFor(()=>expect(button).toBeEnabled());fireEvent.click(button);
  fireEvent.click(await screen.findByRole('button',{name:'重试同一备份请求'}));await waitFor(()=>expect(mock.createBackup).toHaveBeenCalledTimes(2));
  expect(mock.createBackup.mock.calls[0]).toEqual(mock.createBackup.mock.calls[1]);expect(mock.backups).toHaveBeenCalledTimes(2);
 });
 it('clears private status on denial and ignores a late response from an old API',async()=>{
  const late=deferred(),old=api({backups:vi.fn().mockReturnValue(late.promise)}),next=api({backups:vi.fn().mockRejectedValue(new ApiError(403,'other DM'))});
  const view=render(<BackupWorkspace api={old as unknown as Api}/>);view.rerender(<BackupWorkspace api={next as unknown as Api}/>);
  await screen.findByText(/当前账号无部署备份权限/);await act(async()=>late.resolve(status));expect(screen.queryByRole('link')).not.toBeInTheDocument();
 });
 it('requires an explicit migration choice before requesting a persistent source write fence',async()=>{
  const mock=api();render(<BackupWorkspace api={mock as unknown as Api}/>);await screen.findByRole('link');
  const button=screen.getByRole('button',{name:'生成迁移备份并保持旧实例停写'});expect(button).toBeDisabled();
  fireEvent.click(screen.getByLabelText('确认迁移期间旧实例持续停止写入'));fireEvent.click(button);
  await waitFor(()=>expect(mock.createBackup).toHaveBeenCalledWith(expect.objectContaining({expectedVersion:4}),true));
 });
});
