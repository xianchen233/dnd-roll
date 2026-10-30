import {act,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {describe,it,expect,vi} from 'vitest';
import {ApiError,type Api} from './api';
import {ModuleUpdatePreview} from './ModuleUpdatePreview';
const result={version:3,packageId:'new',sha256:'hash',missingDependencies:[],changes:[{path:'/nodes/id/text',baseline:'原始',current:'本地',incoming:'新版',conflict:true}]};
function setup(mock=vi.fn().mockResolvedValue(result)){
 const props={api:{previewModuleUpdate:mock} as unknown as Api,campaignId:'c',copyId:'copy',version:3,packageIds:[{id:'new',name:'新版模板'}],onDenied:vi.fn()};
 return {...render(<ModuleUpdatePreview {...props}/>),props,mock};
}
async function preview(){fireEvent.change(screen.getByLabelText('更新候选模板'),{target:{value:'new'}});fireEvent.click(screen.getByRole('button',{name:'比较模板更新'}));await act(async()=>{});}
describe('three-way module preview',()=>{
 it('shows all three values and conflicts without saving any data',async()=>{const v=setup();await preview();expect(v.mock).toHaveBeenCalledWith('c','copy',{expectedVersion:3,packageId:'new'});const conflict=await screen.findByText('双方修改冲突');expect(conflict).toBeVisible();fireEvent.click(conflict.closest('summary')!);for(const text of ['原始','本地','新版'])expect(screen.getByText(text)).toBeVisible();expect(screen.queryByRole('button',{name:'应用更新'})).not.toBeInTheDocument();});
 it('discards late private results on copy or campaign change',async()=>{let resolve!:(v:unknown)=>void;const v=setup(vi.fn().mockReturnValue(new Promise(r=>resolve=r)));await preview();v.rerender(<ModuleUpdatePreview {...v.props} campaignId="other"/>);await act(async()=>resolve(result));expect(screen.queryByText('本地')).not.toBeInTheDocument();});
 it('clears old comparison when the saved copy version changes',async()=>{const v=setup();await preview();await screen.findByText('本地');v.rerender(<ModuleUpdatePreview {...v.props} version={4}/>);expect(screen.queryByText('本地')).not.toBeInTheDocument();});
 it('clears private data and reports denial after current permission is revoked',async()=>{const v=setup();await preview();await screen.findByText('本地');v.mock.mockRejectedValueOnce(new ApiError(403,'revoked'));fireEvent.click(screen.getByRole('button',{name:'比较模板更新'}));await waitFor(()=>expect(v.props.onDenied).toHaveBeenCalledOnce());expect(screen.queryByText('本地')).not.toBeInTheDocument();});
 it('keeps version conflicts visible and offers a fresh comparison after a transport error',async()=>{const v=setup(vi.fn().mockRejectedValueOnce(new ApiError(409,'stale')).mockRejectedValueOnce(Error('network')).mockResolvedValue(result));await preview();expect(screen.getByRole('status')).toHaveTextContent('版本');fireEvent.click(screen.getByRole('button',{name:'比较模板更新'}));await waitFor(()=>expect(screen.getByRole('status')).toHaveTextContent('读取失败'));fireEvent.click(screen.getByRole('button',{name:'比较模板更新'}));await screen.findByText('本地');});
});
