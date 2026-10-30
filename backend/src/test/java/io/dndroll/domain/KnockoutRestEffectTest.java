package io.dndroll.domain;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class KnockoutRestEffectTest {
    final CharacterSheet.Condition marker=KnockoutEffect.annotation(UUID.randomUUID());
    CharacterSheet sheet(int max,int hp,int temp,int failures,List<CharacterSheet.Condition> conditions){var s=CharacterSheet.empty("Target");var b=s.combat();return s.change(s.name(),new CharacterSheet.Combat(max,hp,temp,b.speed(),0,failures,b.acMode(),b.manualAc(),b.source()),s.resources(),conditions,s.overrides());}
    @Test void oneHourBoundaryIsAdjudicatedGameTimeAndDoesNotHealEvenAtFullHp(){
        var s=sheet(1,1,0,0,List.of(marker));for(int minutes=0;minutes<=120;minutes++){var p=KnockoutRestEffect.plan(s,marker,true,minutes,false,true);assertEquals(minutes<60?"MANUAL_REQUIRED":"CANDIDATE",p.status());if(minutes<60)assertNull(p.hp());else{assertEquals(1,p.hp());assertEquals(0,p.tempHp());assertEquals(marker.id(),p.conditionId());}}assertEquals(1,s.combat().hp());assertEquals(List.of(marker),s.conditions());
    }
    @Test void interruptedUnreviewedAlteredOrNonCanonicalTargetsNeverReturnAnExecutableRemoval(){
        var s=sheet(20,1,0,0,List.of(marker));for(var p:List.of(KnockoutRestEffect.plan(s,marker,true,60,true,true),KnockoutRestEffect.plan(s,marker,true,60,false,false),KnockoutRestEffect.plan(s,marker,false,60,false,true),KnockoutRestEffect.plan(s,null,true,60,false,true),KnockoutRestEffect.plan(sheet(20,0,0,0,List.of(marker)),marker,true,60,false,true),KnockoutRestEffect.plan(sheet(20,1,5,0,List.of(marker)),marker,true,60,false,true),KnockoutRestEffect.plan(sheet(20,1,0,1,List.of(marker)),marker,true,60,false,true),KnockoutRestEffect.plan(sheet(20,1,0,0,List.of()),marker,true,60,false,true))){assertEquals("MANUAL_REQUIRED",p.status());assertNull(p.hp());assertNull(p.tempHp());assertNull(p.conditionId());assertThrows(UnsupportedOperationException.class,()->p.reasons().add("bad"));}
    }
    @Test void boundedInputsRejectBeforePlanning(){var s=sheet(20,1,0,0,List.of(marker));for(int minutes:List.of(-1,100001,Integer.MAX_VALUE))assertThrows(DomainException.class,()->KnockoutRestEffect.plan(s,marker,true,minutes,false,true));assertThrows(DomainException.class,()->KnockoutRestEffect.plan(null,marker,true,60,false,true));assertEquals("CANDIDATE",KnockoutRestEffect.plan(s,marker,true,100000,false,true).status());}
}
