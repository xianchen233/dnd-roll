package io.dndroll.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DamageEffectTest {
    CharacterSheet sheet(int hp,int temp,int successes,int failures,List<CharacterSheet.Condition> conditions){
        var s=CharacterSheet.empty("Target");var b=s.combat();
        return s.change(s.name(),new CharacterSheet.Combat(100,hp,temp,b.speed(),successes,failures,b.acMode(),b.manualAc(),b.source()),s.resources(),conditions,s.overrides());
    }
    @Test void preservesEveryPointAcrossBufferAndHpBoundariesWithoutChangingInput(){
        for(int hp=1;hp<=20;hp++)for(int temp=0;temp<=20;temp++)for(int damage=0;damage<hp+temp;damage++){
            var s=sheet(hp,temp,0,0,List.of());var p=DamageEffect.plan(s,damage);
            assertEquals("CANDIDATE",p.status());assertTrue(p.hp()>0);assertEquals(damage,p.absorbed()+p.hpLoss());
            assertEquals(hp+temp-damage,p.hp()+p.tempHp());assertEquals(Math.min(temp,damage),p.absorbed());assertEquals(hp,s.combat().hp());assertEquals(temp,s.combat().tempHp());
        }
    }
    @Test void neverApproximatesZeroHpOrLethalDamageIncludingExactBoundary(){
        for(var input:List.of(List.of(0,10,0),List.of(0,10,1),List.of(5,3,8),List.of(5,3,220000))){
            var p=DamageEffect.plan(sheet(input.get(0),input.get(1),0,0,List.of()),input.get(2));assertEquals("MANUAL_REQUIRED",p.status());assertEquals(List.of("ZERO_HP_REQUIRES_MANUAL"),p.reasons());assertNull(p.hp());assertNull(p.tempHp());assertNull(p.absorbed());assertNull(p.hpLoss());
        }
    }
    @Test void conditionsAndDeathRecordsRequireManualEvenAtZeroDamage(){
        var condition=new CharacterSheet.Condition("expired","Manual","","",CharacterSheet.manual(),0,null);
        var p=DamageEffect.plan(sheet(20,5,1,1,List.of(condition)),0);assertEquals(List.of("CONDITIONS_REQUIRE_MANUAL","DEATH_SAVES_REQUIRE_MANUAL"),p.reasons());assertNull(p.hp());assertThrows(UnsupportedOperationException.class,()->p.reasons().add("changed"));
    }
    @Test void invalidDamageAndInvalidSheetsRejectInsteadOfReturningPartialEffects(){
        for(int damage:List.of(-1,220001))assertEquals(400,assertThrows(DomainException.class,()->DamageEffect.plan(sheet(10,0,0,0,List.of()),damage)).status());
        assertThrows(DomainException.class,()->DamageEffect.plan(sheet(101,0,0,0,List.of()),1));assertThrows(DomainException.class,()->DamageEffect.plan(null,1));
    }
}
