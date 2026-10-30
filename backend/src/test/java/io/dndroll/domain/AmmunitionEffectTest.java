package io.dndroll.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AmmunitionEffectTest {
    CharacterSheet sheet(int arrows,String kind,List<CharacterSheet.Condition> conditions){
        var s=CharacterSheet.empty("Archer");var b=s.combat();return s.change(s.name(),new CharacterSheet.Combat(20,20,2,b.speed(),0,0,b.acMode(),b.manualAc(),b.source()),List.of(new CharacterSheet.Resource("counter","Arbitrary label",kind,arrows,100000,"Manual",CharacterSheet.manual())),conditions,s.overrides());
    }
    @Test void zeroDamageAndCriticalDamageEachSpendOneMappedCounterAndKeepInputImmutable(){
        var source=sheet(100000,"CUSTOM",List.of());var target=sheet(5,"CUSTOM",List.of());
        for(int damage:List.of(0,1,2,4,10)){var p=AmmunitionEffect.plan(source,target,true,damage,"counter",true);assertEquals("CANDIDATE",p.status());assertEquals(100000,p.before());assertEquals(99999,p.after());assertEquals(20-Math.max(0,damage-2),p.hp());assertEquals(100000,source.resources().getFirst().current());}
    }
    @Test void unsupportedReviewWeaponSourceAndTargetEffectsNeverReturnPartialNumbers(){
        var condition=new CharacterSheet.Condition("expired","Expired","","",CharacterSheet.manual(),0,null);var source=sheet(5,"CUSTOM",List.of());
        var plans=List.of(AmmunitionEffect.plan(source,source,false,0,"counter",true),AmmunitionEffect.plan(source,source,true,0,"counter",false),AmmunitionEffect.plan(sheet(0,"CUSTOM",List.of()),source,true,0,"counter",true),AmmunitionEffect.plan(sheet(5,"FEATURE",List.of()),source,true,0,"counter",true),AmmunitionEffect.plan(source,source,true,0,"missing",true),AmmunitionEffect.plan(sheet(5,"CUSTOM",List.of(condition)),source,true,0,"counter",true),AmmunitionEffect.plan(source,source,true,22,"counter",true));
        for(var p:plans){assertEquals("MANUAL_REQUIRED",p.status());assertFalse(p.reasons().isEmpty());assertNull(p.before());assertNull(p.after());assertNull(p.hp());assertNull(p.tempHp());assertThrows(UnsupportedOperationException.class,()->p.reasons().add("changed"));}
    }
    @Test void malformedInputsRejectInsteadOfBecomingAPlan(){
        var s=sheet(1,"CUSTOM",List.of());assertThrows(DomainException.class,()->AmmunitionEffect.plan(null,s,true,0,"counter",true));assertThrows(DomainException.class,()->AmmunitionEffect.plan(s,s,true,-1,"counter",true));assertThrows(DomainException.class,()->AmmunitionEffect.plan(s,s,true,0,"",true));
    }
}
