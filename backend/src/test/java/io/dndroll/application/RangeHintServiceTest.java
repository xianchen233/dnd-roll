package io.dndroll.application;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.RangeHintService.*;
import io.dndroll.domain.*;
import io.dndroll.domain.Movement.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class RangeHintServiceTest {
    final UUID campaign=UUID.randomUUID(),scene=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
    final class Memory implements SceneService.Store {
        Role role=Role.PLAYER; boolean hidden=false; int x=4,y=3; long version=0;
        public List<SceneService.CampaignView> campaigns(String actor){throw new AssertionError("No listing needed");}
        public Role role(UUID c,String actor){return role;}
        public SceneService.Scene scene(UUID c){return new SceneService.Scene(scene,campaign,"Grid",24,16,48,version,List.of(new Token(a,"A",1,1,0,false,Set.of()),new Token(b,"B",x,y,0,hidden,Set.of())));}
        public SceneService.Receipt receipt(UUID c,String actor,UUID request){throw new AssertionError("No saved receipts");}
        public void save(UUID c,String actor,Token t,Command d,TokenView response){throw new AssertionError("No writes");}
        public <T>T locked(UUID c,Supplier<T> action){return action.get();}
    }
    Request request(String mode,int normal,int maximum,boolean reviewed){return new Request(scene,0,a,0,b,0,"GRID_5R_5FT_RANGE_V1",reviewed,mode,normal,maximum);}
    @Test void shortestGridDistanceIncludesDiagonalsAndInclusiveRangeBoundaries(){
        var memory=new Memory();var service=new RangeHintService(memory,s->true);
        var longRange=service.preview(campaign,"viewer",request("RANGED",10,15,true));assertEquals(15,longRange.distanceFeet());assertEquals("LONG_RANGE",longRange.band());
        assertEquals("NORMAL_RANGE",service.preview(campaign,"viewer",request("RANGED",15,15,true)).band());
        assertEquals("OUT_OF_RANGE",service.preview(campaign,"viewer",request("RANGED",10,10,true)).band());
        assertEquals("WITHIN_REACH",service.preview(campaign,"viewer",request("MELEE",15,15,true)).band());
        memory.x=0;memory.y=0;assertEquals(5,service.preview(campaign,"viewer",request("MELEE",5,5,true)).distanceFeet());
        memory.x=1;memory.y=1;assertEquals(0,service.preview(campaign,"viewer",request("MELEE",0,0,true)).distanceFeet());
    }
    @Test void unsupportedCalibrationAndUnreviewedContextHaveNoPartialMeasurement(){
        var service=new RangeHintService(new Memory(),s->false);
        assertEquals(new Result("MANUAL_REQUIRED",null,null,"GRID_REQUIRES_MANUAL"),service.preview(campaign,"viewer",request("RANGED",10,15,true)));
        assertEquals(new Result("MANUAL_REQUIRED",null,null,"REVIEW_REQUIRED"),service.preview(campaign,"viewer",request("RANGED",10,15,false)));
    }
    @Test void denialAndVisibilityPrecedeMeasurementEvenForManualContext(){
        var memory=new Memory();var service=new RangeHintService(memory,s->{throw new AssertionError("Never inspect calibration for denied target");});
        memory.hidden=true;assertEquals(404,assertThrows(DomainException.class,()->service.preview(campaign,"viewer",request("RANGED",10,15,false))).status());
        memory.role=null;assertEquals(403,assertThrows(DomainException.class,()->service.preview(campaign,"viewer",request("RANGED",10,15,true))).status());
    }
    @Test void changedVersionsNeverReuseOldMeasurements(){
        var memory=new Memory();var service=new RangeHintService(memory,s->true);service.preview(campaign,"viewer",request("RANGED",10,15,true));memory.version++;
        assertEquals(409,assertThrows(DomainException.class,()->service.preview(campaign,"viewer",request("RANGED",10,15,true))).status());
    }
}
