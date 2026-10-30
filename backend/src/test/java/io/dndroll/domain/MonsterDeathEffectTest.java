package io.dndroll.domain;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class MonsterDeathEffectTest {
    CharacterSheet sheet(int hp,int temp,int failures,List<CharacterSheet.Condition> conditions){var s=CharacterSheet.empty("Target");var b=s.combat();return s.change(s.name(),new CharacterSheet.Combat(100,hp,temp,b.speed(),0,failures,b.acMode(),b.manualAc(),b.source()),s.resources(),conditions,s.overrides());}
    @Test void lethalBoundaryConsumesTemporaryHpFirstAndClampsOnlyExplicitMonsterDeath(){
        for(int hp=1;hp<=10;hp++)for(int temp=0;temp<=10;temp++)for(int damage=0;damage<=30;damage++){
            var s=sheet(hp,temp,0,List.of());var p=MonsterDeathEffect.plan(s,damage,true,"MONSTER_DIES_AT_ZERO",true);
            if(damage>=hp+temp){assertEquals("CANDIDATE",p.status());assertEquals(0,p.hp());assertEquals(0,p.tempHp());assertEquals(temp,p.absorbed());assertEquals(hp,p.hpLoss());assertEquals("MANUAL_REQUIRED",DamageEffect.plan(s,damage).status());}
            else{assertEquals("MANUAL_REQUIRED",p.status());assertNull(p.hp());assertNull(p.absorbed());}assertEquals(hp,s.combat().hp());
        }
    }
    @Test void allManualPredicatesPreserveNullNumbersAndImmutableReasons(){
        for(var p:List.of(MonsterDeathEffect.plan(sheet(1,0,0,List.of()),5,false,"MONSTER_DIES_AT_ZERO",true),MonsterDeathEffect.plan(sheet(1,0,0,List.of()),5,true,"MANUAL",true),MonsterDeathEffect.plan(sheet(1,0,0,List.of()),5,true,"MONSTER_DIES_AT_ZERO",false),MonsterDeathEffect.plan(sheet(0,0,0,List.of()),5,true,"MONSTER_DIES_AT_ZERO",true),MonsterDeathEffect.plan(sheet(1,0,1,List.of()),5,true,"MONSTER_DIES_AT_ZERO",true),MonsterDeathEffect.plan(sheet(1,0,0,List.of(MonsterDeathEffect.annotation(UUID.randomUUID()))),5,true,"MONSTER_DIES_AT_ZERO",true))){assertEquals("MANUAL_REQUIRED",p.status());assertNull(p.hp());assertNull(p.tempHp());assertNull(p.hpLoss());assertNull(p.absorbed());assertThrows(UnsupportedOperationException.class,()->p.reasons().add("bad"));}
    }
    @Test void invalidDamageSheetAndTreatmentRejectAndMarkerIsDeterministic(){
        for(int damage:List.of(-1,220001))assertThrows(DomainException.class,()->MonsterDeathEffect.plan(sheet(1,0,0,List.of()),damage,true,"MONSTER_DIES_AT_ZERO",true));assertThrows(DomainException.class,()->MonsterDeathEffect.plan(null,1,true,"MONSTER_DIES_AT_ZERO",true));assertThrows(DomainException.class,()->MonsterDeathEffect.plan(sheet(1,0,0,List.of()),1,true,"OTHER",true));
        var id=UUID.randomUUID();assertEquals(MonsterDeathEffect.annotation(id),MonsterDeathEffect.annotation(id));assertNull(MonsterDeathEffect.annotation(id).expiresAt());assertNull(MonsterDeathEffect.annotation(id).remainingRounds());
    }
}
