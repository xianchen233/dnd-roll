package io.dndroll.domain;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;
class StabilizationHealingEffectTest {
    final UUID attempt=UUID.fromString("10000000-0000-0000-0000-000000000001");
    CharacterSheet zero(){var s=CharacterSheet.empty("Target");var b=s.combat();return s.change(s.name(),new CharacterSheet.Combat(12,0,7,b.speed(),1,2,b.acMode(),b.manualAc(),b.source()),s.resources(),List.of(),s.overrides());}
    List<CharacterSheet.Condition> canonical(){return StabilizationEffect.plan(zero(),CharacterSheet.empty("Helper"),attempt,true,true,true,20).annotations();}
    CharacterSheet stable(){var s=zero();var b=s.combat();return s.change(s.name(),new CharacterSheet.Combat(12,0,7,b.speed(),0,0,b.acMode(),b.manualAc(),b.source()),s.resources(),canonical(),s.overrides());}
    StabilizationHealingEffect.Plan plan(CharacterSheet s,int n){return StabilizationHealingEffect.plan(s,canonical(),true,n,true);}
    void manual(StabilizationHealingEffect.Plan p,String reason){assertEquals("MANUAL_REQUIRED",p.status());assertTrue(p.reasons().contains(reason),p.reasons().toString());assertNull(p.hp());assertNull(p.healed());assertTrue(p.removed().isEmpty());}
    @Test void restoresOneRealHpAndRemovesExactlyOriginalPairWhileKeepingTempHp(){var s=stable();var p=plan(s,1);assertEquals("CANDIDATE",p.status());assertEquals(1,p.hp());assertEquals(1,p.healed());assertEquals(7,p.tempHp());assertEquals(0,p.deathSuccesses());assertEquals(0,p.deathFailures());assertEquals(canonical().stream().map(CharacterSheet.Condition::id).toList(),p.removed());assertEquals(stable(),s);}
    @Test void capsRealHealingAtMaximumWithoutUsingTemporaryHp(){for(int n:List.of(12,13,100000)){var p=plan(stable(),n);assertEquals("CANDIDATE",p.status());assertEquals(12,p.hp());assertEquals(12,p.healed());assertEquals(7,p.tempHp());}}
    @Test void needsAuthoritativeOriginalAndReview(){manual(StabilizationHealingEffect.plan(stable(),canonical(),false,1,true),"UNCHANGED_STABILIZATION_ORIGIN_REQUIRED");manual(StabilizationHealingEffect.plan(stable(),canonical(),true,1,false),"HEALING_REVIEW_REQUIRED");}
    @Test void rejectsMissingExtraAndAlteredCanonicalRecords(){var s=stable();manual(StabilizationHealingEffect.plan(s,null,true,1,true),"CANONICAL_STABILIZATION_PAIR_REQUIRED");for(var list:List.of(List.<CharacterSheet.Condition>of(),canonical().subList(0,1))){manual(plan(s.change(s.name(),s.combat(),s.resources(),list,s.overrides()),1),"CANONICAL_STABILIZATION_PAIR_REQUIRED");}var extra=new ArrayList<>(canonical());extra.add(new CharacterSheet.Condition("other","昏迷","","Other source",canonical().getFirst().source(),null,null));manual(plan(s.change(s.name(),s.combat(),s.resources(),extra,s.overrides()),1),"CANONICAL_STABILIZATION_PAIR_REQUIRED");}
    @Test void rejectsNonzeroHpAndDeathCounters(){var s=stable();var b=s.combat();for(var c:List.of(new CharacterSheet.Combat(12,1,7,b.speed(),0,0,b.acMode(),b.manualAc(),b.source()),new CharacterSheet.Combat(12,0,7,b.speed(),1,0,b.acMode(),b.manualAc(),b.source()))){manual(plan(s.change(s.name(),c,s.resources(),s.conditions(),s.overrides()),1),"STABILIZATION_VALUES_REQUIRED");}}
    @Test void rejectsAdjustments(){var s=stable();manual(plan(s.change(s.name(),s.combat(),s.resources(),s.conditions(),List.of(new CharacterSheet.Override("ac",20,"test"))),1),"TARGET_ADJUSTMENTS_REQUIRE_MANUAL");}
    @Test void invalidInputsAreRejected(){assertThrows(DomainException.class,()->plan(null,1));for(int n:List.of(0,-1,100001,Integer.MAX_VALUE))assertThrows(DomainException.class,()->plan(stable(),n));}
    @Test void planIsDeterministicAndImmutable(){var p=plan(stable(),3);assertEquals(p,plan(stable(),3));assertThrows(UnsupportedOperationException.class,()->p.removed().add("x"));assertThrows(UnsupportedOperationException.class,()->p.reasons().add("x"));}
}
