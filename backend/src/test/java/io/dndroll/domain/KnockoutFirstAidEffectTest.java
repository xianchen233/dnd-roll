package io.dndroll.domain;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class KnockoutFirstAidEffectTest {
    final CharacterSheet.Condition marker=KnockoutEffect.annotation(UUID.randomUUID());
    CharacterSheet target(){var s=CharacterSheet.empty("Target");return s.change(s.name(),s.combat(),s.resources(),List.of(marker),s.overrides());}
    @Test void ordinarySkillTotalsCompareAtTenWithoutHealingOrAttackCriticalRules(){
        for(int total=-10;total<=30;total++){var p=KnockoutFirstAidEffect.plan(target(),marker,true,true,true,true,true,total);assertEquals(total<10?"MANUAL_REQUIRED":"CANDIDATE",p.status());assertEquals(total,p.total());assertEquals(10,p.dc());if(total<10)assertNull(p.conditionId());else{assertEquals(marker.id(),p.conditionId());assertEquals(1,p.hp());assertEquals(0,p.tempHp());}}
    }
    @Test void allManualPredicatesHaveNoExecutableRemovalAndNoMutableReasons(){
        for(var p:List.of(KnockoutFirstAidEffect.plan(target(),marker,false,true,true,true,true,20),KnockoutFirstAidEffect.plan(target(),marker,true,false,true,true,true,20),KnockoutFirstAidEffect.plan(target(),marker,true,true,false,true,true,20),KnockoutFirstAidEffect.plan(target(),marker,true,true,true,false,true,20),KnockoutFirstAidEffect.plan(target(),marker,true,true,true,true,false,20),KnockoutFirstAidEffect.plan(target(),marker,true,true,true,true,true,null),KnockoutFirstAidEffect.plan(CharacterSheet.empty("No record"),marker,true,true,true,true,true,20))){assertEquals("MANUAL_REQUIRED",p.status());assertNull(p.hp());assertNull(p.tempHp());assertNull(p.conditionId());assertThrows(UnsupportedOperationException.class,()->p.reasons().add("bad"));}
    }
    @Test void helpersMustBeConsciousAndHaveAnOrdinaryWisdomMedicineCalculation(){
        var s=CharacterSheet.empty("Helper");assertTrue(KnockoutFirstAidEffect.helperSupported(s));assertFalse(KnockoutFirstAidEffect.helperSupported(null));var b=s.combat();assertFalse(KnockoutFirstAidEffect.helperSupported(s.change(s.name(),new CharacterSheet.Combat(1,0,0,b.speed(),0,0,b.acMode(),b.manualAc(),b.source()),s.resources(),List.of(),s.overrides())));assertFalse(KnockoutFirstAidEffect.helperSupported(s.change(s.name(),b,s.resources(),List.of(marker),s.overrides())));assertFalse(KnockoutFirstAidEffect.helperSupported(s.change(s.name(),b,s.resources(),List.of(),List.of(new CharacterSheet.Override("skill:medicine",20,"Unsupported override")))));
    }
}
