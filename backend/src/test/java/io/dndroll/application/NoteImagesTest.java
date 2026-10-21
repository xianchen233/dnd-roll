package io.dndroll.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class NoteImagesTest {
    static class Fixture {
        final UUID campaign=UUID.randomUUID(),request=UUID.randomUUID();final CharacterService.Store cards=mock(CharacterService.Store.class);final NoteImages.Store store=mock(NoteImages.Store.class);final Map<String,byte[]> files=new HashMap<>();
        NoteImages.Image saved;NotesService.Receipt receipt;String outcome="";boolean down,readable=true;final NoteImages service;
        Fixture(){
            when(cards.role(campaign,"author")).thenReturn(Role.PLAYER);when(cards.campaignVersion(campaign)).thenReturn(0L);
            when(cards.locked(eq(campaign),any())).thenAnswer(call->{if(down)throw new IllegalStateException("database unavailable");return ((Supplier<?>)call.getArgument(1)).get();});
            when(cards.actorLocked(eq("author"),any())).thenAnswer(call->{var result=((Supplier<?>)call.getArgument(1)).get();if(!outcome.isEmpty()){if(outcome.equals("ROLLBACK")){saved=null;receipt=null;}if(outcome.equals("UNKNOWN"))down=true;throw new IllegalStateException("commit acknowledgment lost");}return result;});
            when(store.image(eq(campaign),any())).thenAnswer(call->saved!=null&&saved.metadata().id().equals(call.getArgument(1))?saved:null);
            doAnswer(call->{saved=call.getArgument(1);return null;}).when(store).saveImage(eq(campaign),any());
            when(store.uploadReceipt("author",request)).thenAnswer(call->receipt);
            doAnswer(call->{receipt=call.getArgument(3);return null;}).when(store).uploadReceipt(eq(campaign),eq("author"),eq(request),any());
            when(store.readableImage(eq(campaign),any(),eq("author"),eq(false))).thenAnswer(call->readable);
            service=new NoteImages(store,cards,new AssetService.Files(){public void publish(String key,byte[] bytes){files.put(key,bytes.clone());}public byte[] read(String key){return files.get(key);}public void discard(String key){files.remove(key);}},tools.jackson.databind.json.JsonMapper.builder().build());
        }
        byte[] png()throws Exception{var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    }
    @Test void committedFileSurvivesLostAcknowledgmentAndRetryReturnsSameUpload()throws Exception{
        var f=new Fixture();f.outcome="COMMITTED";byte[] image=f.png();assertThrows(IllegalStateException.class,()->f.service.upload(f.campaign,"author",f.request,0,"TEAM",image,"image/png"));assertEquals(1,f.files.size());var id=f.saved.metadata().id();f.outcome="";assertEquals(id,f.service.upload(f.campaign,"author",f.request,0,"TEAM",image,"image/png").id());assertArrayEquals(image,f.service.content(f.campaign,id,"author").bytes());verify(f.store,times(1)).saveImage(eq(f.campaign),any());
    }
    @Test void rollbackCleansOnlyNewFileButUnavailableReconciliationPreservesIt()throws Exception{
        var rolledBack=new Fixture();rolledBack.outcome="ROLLBACK";byte[] image=rolledBack.png();assertThrows(IllegalStateException.class,()->rolledBack.service.upload(rolledBack.campaign,"author",rolledBack.request,0,"TEAM",image,"image/png"));assertEquals(0,rolledBack.files.size());
        var unknown=new Fixture();unknown.outcome="UNKNOWN";assertThrows(IllegalStateException.class,()->unknown.service.upload(unknown.campaign,"author",unknown.request,0,"TEAM",image,"image/png"));assertEquals(1,unknown.files.size());
    }
    @Test void currentVisibilityAndMembershipPrecedeOldUploadReceipts()throws Exception{
        var f=new Fixture();byte[] bytes=f.png();var image=f.service.upload(f.campaign,"author",f.request,0,"TEAM",bytes,"image/png");f.readable=false;assertEquals(404,assertThrows(DomainException.class,()->f.service.upload(f.campaign,"author",f.request,0,"TEAM",bytes,"image/png")).status());assertEquals(404,assertThrows(DomainException.class,()->f.service.content(f.campaign,image.id(),"author")).status());when(f.cards.role(f.campaign,"author")).thenReturn(null);assertEquals(403,assertThrows(DomainException.class,()->f.service.upload(f.campaign,"author",f.request,0,"TEAM",bytes,"image/png")).status());
    }
}
