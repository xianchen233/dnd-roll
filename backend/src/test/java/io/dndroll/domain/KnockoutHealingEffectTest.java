package io.dndroll.domain;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class KnockoutHealingEffectTest {
    final CharacterSheet.Condition marker=KnockoutEffect.annotation(UUID.randomUUID());
    CharacterSheet sheet(int max,int hp,int temp,int failures,List<CharacterSheet.Condition> conditions){var s=CharacterSheet.empty("Target");var b=s.combat();return s.change(s.name(),new CharacterSheet.Combat(max,hp,temp,b.speed(),0,failures,b.acMode(),b.manualAc(),b.source()),s.resources(),conditions,s.overrides());}
    @Test void healingIsActualClampedRecoveryAndNeverTemporaryHp(){
        for(int max=1;max<=25;max++)for(int amount=1;amount<=30;amount++){var p=KnockoutHealingEffect.plan(sheet(max,1,0,0,List.of(marker)),marker,true,amount,true);if(max==1){assertEquals("MANUAL_REQUIRED",p.status());assertNull(p.hp());}else{assertEquals(Math.min(max,1+amount),p.hp());assertEquals(Math.min(max-1,amount),p.healed());assertEquals(0,p.tempHp());assertEquals(marker.id(),p.conditionId());}}
    }
    @Test void everyUnsupportedPredicateReturnsNoExecutableNumbers(){
        var s=sheet(20,1,0,0,List.of(marker));for(var p:List.of(KnockoutHealingEffect.plan(s,marker,false,3,true),KnockoutHealingEffect.plan(s,marker,true,3,false),KnockoutHealingEffect.plan(s,null,true,3,true),KnockoutHealingEffect.plan(sheet(20,1,5,0,List.of(marker)),marker,true,3,true),KnockoutHealingEffect.plan(sheet(20,0,0,0,List.of(marker)),marker,true,3,true),KnockoutHealingEffect.plan(sheet(20,1,0,1,List.of(marker)),marker,true,3,true),KnockoutHealingEffect.plan(sheet(20,1,0,0,List.of()),marker,true,3,true))){assertEquals("MANUAL_REQUIRED",p.status());assertNull(p.hp());assertNull(p.healed());assertNull(p.conditionId());assertThrows(UnsupportedOperationException.class,()->p.reasons().add("bad"));}
    }
    @Test void inputBoundsRejectBeforeCalculation(){var s=sheet(100000,1,0,0,List.of(marker));for(int amount:List.of(0,-1,100001,Integer.MAX_VALUE))assertThrows(DomainException.class,()->KnockoutHealingEffect.plan(s,marker,true,amount,true));assertEquals(100000,KnockoutHealingEffect.plan(s,marker,true,100000,true).hp());}
}
