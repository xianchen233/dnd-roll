package io.dndroll.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StabilizationEffectTest {
    final UUID attempt = UUID.fromString("20e22936-e200-4ba5-90db-4f27b0eb9737");
    final CharacterSheet helper = CharacterSheet.empty("Helper");
    CharacterSheet target(int hp, int temp, int successes, int failures) {
        var s = CharacterSheet.empty("Target"); var b = s.combat();
        return s.change(s.name(), new CharacterSheet.Combat(12, hp, temp, b.speed(), successes, failures,
            b.acMode(), b.manualAc(), b.source()), s.resources(), s.conditions(), s.overrides());
    }
    StabilizationEffect.Plan plan(CharacterSheet s, Integer total) {
        return StabilizationEffect.plan(s, helper, attempt, true, true, true, total);
    }
    void manual(StabilizationEffect.Plan p, String reason) {
        assertEquals("MANUAL_REQUIRED", p.status()); assertTrue(p.reasons().contains(reason), p.reasons().toString());
        assertNull(p.hp()); assertNull(p.tempHp()); assertNull(p.deathSuccesses()); assertNull(p.deathFailures());
        assertTrue(p.annotations().isEmpty());
    }
    @Test void successfulHelpResetsBothCountersButDoesNotHealOrWakeAndKeepsTemporaryHp() {
        for (int successes=0; successes<=2; successes++) for (int failures=0; failures<=2; failures++) {
            var s=target(0, 7, successes, failures); var p=plan(s, 10);
            assertEquals("CANDIDATE", p.status()); assertEquals(StabilizationEffect.PROFILE, p.profile());
            assertEquals(10,p.dc()); assertEquals(0,p.hp()); assertEquals(7,p.tempHp());
            assertEquals(0,p.deathSuccesses()); assertEquals(0,p.deathFailures()); assertTrue(p.reasons().isEmpty());
            assertEquals(2,p.annotations().size());
            assertTrue(p.annotations().stream().anyMatch(c->c.id().equals("a16-stable:"+attempt)));
            assertTrue(p.annotations().stream().anyMatch(c->c.id().equals("a16-unconscious:"+attempt)));
            for(var c:p.annotations()) { assertEquals("5R",c.source().rules()); assertEquals("5.2.1",c.source().revision()); assertNull(c.expiresAt()); assertNull(c.remainingRounds()); }
            assertEquals(s, target(0,7,successes,failures)); assertTrue(helper.conditions().isEmpty());
            var b=s.combat(); var next=s.change(s.name(),new CharacterSheet.Combat(b.maxHp(),p.hp(),p.tempHp(),b.speed(),p.deathSuccesses(),p.deathFailures(),b.acMode(),b.manualAc(),b.source()),s.resources(),p.annotations(),s.overrides());
            assertDoesNotThrow(next::validate); assertEquals(s.resources(),next.resources());
        }
    }
    @Test void ordinaryMedicineUsesTotalAtDcTenRatherThanDeathSaveOrAttackNaturalRollRules() {
        for(int total=-20;total<=40;total++) { var p=plan(target(0,0,1,2),total); assertEquals(total,p.total());
            if(total<10) manual(p,"STABILIZATION_CHECK_FAILED"); else assertEquals("CANDIDATE",p.status()); }
    }
    @Test void deadAlreadyStableAndNonzeroHpAreNeverRescuedByReviewOrHighTotal() {
        manual(plan(target(1,0,0,0),200),"ORDINARY_ZERO_HP_REQUIRED");
        manual(plan(target(0,0,3,0),200),"UNRESOLVED_DEATH_SAVES_REQUIRED");
        manual(plan(target(0,0,0,3),200),"UNRESOLVED_DEATH_SAVES_REQUIRED");
        var s=target(0,0,0,0);var b=s.combat();
        manual(plan(s.change(s.name(),new CharacterSheet.Combat(0,0,0,b.speed(),0,0,b.acMode(),b.manualAc(),b.source()),s.resources(),s.conditions(),s.overrides()),200),"ORDINARY_ZERO_HP_REQUIRED");
    }
    @Test void existingKnockoutFreeTextOrStableRecordsCannotBeOverwritten() {
        var s=target(0,0,0,0);
        for(var marker:List.of(KnockoutEffect.annotation(attempt),new CharacterSheet.Condition("manual","昏迷/稳定/死亡","","",CharacterSheet.manual(),null,null))) {
            manual(plan(s.change(s.name(),s.combat(),s.resources(),List.of(marker),s.overrides()),20),"TARGET_CONDITIONS_REQUIRE_MANUAL");
        }
        var stable=plan(s,20);
        manual(plan(s.change(s.name(),s.combat(),s.resources(),stable.annotations(),s.overrides()),20),"TARGET_CONDITIONS_REQUIRE_MANUAL");
    }
    @Test void everyEvidenceGateMustHoldAndManualPlansHaveNoExecutableChanges() {
        var s=target(0,0,0,0);
        manual(StabilizationEffect.plan(s,helper,attempt,false,true,true,20),"ORDINARY_ZERO_REVIEW_REQUIRED");
        manual(StabilizationEffect.plan(s,helper,attempt,true,false,true,20),"DISTINCT_HELPER_REQUIRED");
        manual(StabilizationEffect.plan(s,helper,attempt,true,true,false,20),"BOUND_STABILIZATION_CHECK_REQUIRED");
        manual(plan(s,null),"BOUND_STABILIZATION_CHECK_REQUIRED");
        manual(StabilizationEffect.plan(s,null,attempt,true,true,true,20),"SUPPORTED_STABILIZATION_HELPER_REQUIRED");
    }
    @Test void helperNeedNotBeProficientButMustUseOrdinaryWisdomMedicineAndBeConscious() {
        assertTrue(StabilizationEffect.helperSupported(helper)); assertFalse(StabilizationEffect.helperSupported(null));
        assertFalse(StabilizationEffect.helperSupported(target(0,0,0,0)));
        assertFalse(StabilizationEffect.helperSupported(target(1,0,1,0)));
        assertFalse(StabilizationEffect.helperSupported(helper.change(helper.name(),helper.combat(),helper.resources(),List.of(KnockoutEffect.annotation(attempt)),helper.overrides())));
        for(String proficiency:List.of("NONE","PROFICIENT","EXPERT")) {
            var skills=helper.skills().stream().map(c->c.id().equals("medicine")?new CharacterSheet.Check(c.id(),c.name(),"WIS",proficiency,5,c.source()):c).toList();
            assertTrue(StabilizationEffect.helperSupported(withSkills(skills)));
        }
        var skills=helper.skills().stream().map(c->c.id().equals("medicine")?new CharacterSheet.Check(c.id(),c.name(),"INT",c.proficiency(),c.bonus(),c.source()):c).toList();
        assertFalse(StabilizationEffect.helperSupported(withSkills(skills)));
    }
    CharacterSheet withSkills(List<CharacterSheet.Check> skills) {
        var s=helper;return new CharacterSheet(s.name(),s.avatar(),s.species(),s.background(),s.classes(),s.abilities(),skills,s.saves(),s.combat(),s.attacks(),s.casting(),s.spells(),s.features(),s.resources(),s.equipment(),s.money(),s.conditions(),s.modifiers(),s.overrides(),s.notes());
    }
    @Test void modifiersAndOverridesOnEitherSideFallBackRatherThanInferringSpecialRules() {
        for(var s:List.of(target(0,0,0,0),helper)) {
            var overridden=s.change(s.name(),s.combat(),s.resources(),s.conditions(),List.of(new CharacterSheet.Override("skill:medicine",30,"manual")));
            if(s.combat().hp()==0) manual(plan(overridden,20),"TARGET_ADJUSTMENTS_REQUIRE_MANUAL");
            else assertFalse(StabilizationEffect.helperSupported(overridden));
        }
        var s=target(0,0,0,0);var modified=new CharacterSheet(s.name(),s.avatar(),s.species(),s.background(),s.classes(),s.abilities(),s.skills(),s.saves(),s.combat(),s.attacks(),s.casting(),s.spells(),s.features(),s.resources(),s.equipment(),s.money(),s.conditions(),List.of(new CharacterSheet.Modifier("ac",1,CharacterSheet.manual())),s.overrides(),s.notes());
        manual(plan(modified,20),"TARGET_ADJUSTMENTS_REQUIRE_MANUAL");
        var consciousModified=modified.change(modified.name(),helper.combat(),modified.resources(),modified.conditions(),modified.overrides());
        assertFalse(StabilizationEffect.helperSupported(consciousModified));
    }
    @Test void repeatedPlansAreDeterministicImmutableAndAttemptScopedAndInvalidInputIsRejected() {
        var s=target(0,0,1,2);var p=plan(s,20);assertEquals(p,plan(s,20));
        assertThrows(UnsupportedOperationException.class,()->p.reasons().add("x"));
        assertThrows(UnsupportedOperationException.class,()->p.annotations().clear());
        assertNotEquals(p.annotations(),StabilizationEffect.plan(s,helper,UUID.randomUUID(),true,true,true,20).annotations());
        assertThrows(DomainException.class,()->StabilizationEffect.plan(null,helper,attempt,true,true,true,20));
        assertThrows(DomainException.class,()->StabilizationEffect.plan(s,helper,null,true,true,true,20));
        assertThrows(DomainException.class,()->plan(target(0,0,4,0),20));
    }
}
