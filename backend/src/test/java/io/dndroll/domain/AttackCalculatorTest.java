package io.dndroll.domain;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.dndroll.domain.AttackCalculator.*;

class AttackCalculatorTest {
    static final CharacterSheet.Source CORE = new CharacterSheet.Source("SRD", "5.2.1", "5R", "Longsword; DM reviewed");
    Factor factor(String reason) { return new Factor(reason, CORE); }
    Damage damage(int modifier, List<Factor> immune, List<Factor> resist, List<Factor> vulnerable) {
        return new Damage(1,8,modifier,DamageType.SLASHING,immune,resist,vulnerable);
    }
    Request request(Roll roll, int bonus, int ac, Damage damage) {
        return new Request(PROFILE,"WEAPON_ATTACK",CORE,new Context(true,List.of()),roll,bonus,ac,damage);
    }
    Request simple(int die, int bonus, int ac) {
        return request(new Roll(List.of(die),List.of(),List.of()),bonus,ac,damage(3,List.of(),List.of(),List.of()));
    }
    @Test void equalityHitsAndOneBelowMissesWithoutRollingDamage() {
        var hit=plan(simple(10,5,15));
        assertEquals("CANDIDATE",hit.status());assertEquals("HIT",hit.candidate().verdict());
        var miss=resolve(simple(9,5,15),List.of());
        assertEquals("MISS",miss.review().candidate().verdict());assertEquals(0,miss.damage());
        assertEquals(0,miss.review().candidate().damageCount());
    }
    @Test void naturalOneAndTwentyOverrideTotalsButTotalTwentyIsNotCritical() {
        assertEquals("MISS",plan(simple(1,100,10)).candidate().verdict());
        assertEquals("CRITICAL",plan(simple(20,-100,1000)).candidate().verdict());
        assertEquals("HIT",plan(simple(15,5,20)).candidate().verdict());
        assertEquals("MISS",plan(simple(19,0,20)).candidate().verdict());
    }
    @Test void advantageUsesOnlySelectedNaturalAndNonstackingDisadvantageCanDiscardTwenty() {
        var a=plan(request(new Roll(List.of(1,20),List.of(factor("a"),factor("b")),List.of()),0,1000,damage(3,List.of(),List.of(),List.of())));
        assertEquals("ADVANTAGE",a.candidate().mode());assertEquals(1,a.candidate().keptIndex());assertEquals("CRITICAL",a.candidate().verdict());
        var d=plan(request(new Roll(List.of(20,1),List.of(),List.of(factor("a"),factor("b"))),100,0,damage(3,List.of(),List.of(),List.of())));
        assertEquals("DISADVANTAGE",d.candidate().mode());assertEquals("MISS",d.candidate().verdict());
    }
    @Test void anyAdvantageAndDisadvantageCancelToOneDieAndTiesKeepFirst() {
        var r=request(new Roll(List.of(10),List.of(factor("a")),List.of(factor("b"),factor("c"))),5,15,damage(3,List.of(),List.of(),List.of()));
        assertEquals("NORMAL",plan(r).candidate().mode());assertEquals("HIT",plan(r).candidate().verdict());
        var tie=request(new Roll(List.of(10,10),List.of(factor("a")),List.of()),5,15,r.damage());
        assertEquals(0,plan(tie).candidate().keptIndex());
    }
    @Test void criticalNeedsIndependentExtraDiceAndAddsModifierOnce() {
        var r=simple(20,0,99);var result=resolve(r,List.of(2,7));
        assertEquals(2,result.review().candidate().damageCount());assertEquals(12,result.damage());
        assertEquals(12,result.rawDamage());
        assertThrows(DomainException.class,()->resolve(r,List.of(2)));
    }
    @Test void resistanceThenVulnerabilityRoundsBeforeDoublingAndNeverStacks() {
        var r=request(new Roll(List.of(15),List.of(),List.of()),5,15,
                damage(0,List.of(),List.of(factor("slashing"),factor("all")),List.of(factor("slashing"),factor("all"))));
        var result=resolve(r,List.of(7));assertEquals(7,result.rawDamage());assertEquals(6,result.damage());
        assertTrue(result.explanation().stream().anyMatch(e->e.rule().equals("RESISTANCE")&&e.calculation().contains("7")));
    }
    @Test void immunityWinsAndNegativeDamageStopsAtZero() {
        var immune=request(new Roll(List.of(15),List.of(),List.of()),5,15,
                damage(0,List.of(factor("slashing")),List.of(factor("all")),List.of(factor("all"))));
        assertEquals(0,resolve(immune,List.of(7)).damage());
        var negative=request(immune.roll(),5,15,damage(-10,List.of(),List.of(),List.of(factor("all"))));
        assertEquals(0,resolve(negative,List.of(1)).damage());
    }
    @Test void unknownProfilesActionsSourcesUnreviewedContextAndExceptionsRequireManualWithNoPartialResult() {
        var r=simple(20,100,0);
        var legacy=new CharacterSheet.Source("Expansion", "2014", "2014", "DM enabled");
        var cases=List.of(
            new Request("unknown",r.action(),CORE,r.context(),r.roll(),100,0,r.damage()),
            new Request(PROFILE,"SPELL_ATTACK",CORE,r.context(),r.roll(),100,0,r.damage()),
            new Request(PROFILE,r.action(),legacy,r.context(),r.roll(),100,0,r.damage()),
            new Request(PROFILE,r.action(),CORE,new Context(false,List.of()),r.roll(),100,0,r.damage()),
            new Request(PROFILE,r.action(),CORE,new Context(true,List.of(factor("automatic critical"))),r.roll(),100,0,r.damage()),
            new Request(PROFILE,r.action(),CORE,r.context(),new Roll(List.of(20,10),List.of(new Factor("extension",legacy)),List.of()),100,0,r.damage()));
        for(var c:cases){var result=resolve(c,List.of());assertEquals("MANUAL_REQUIRED",result.review().status());
            assertFalse(result.review().reasons().isEmpty());assertNull(result.review().candidate());assertNull(result.damage());assertNull(result.rawDamage());}
    }
    @Test void invalidDiceCannotBeSilentlyDroppedOrClamped() {
        for(var dice:List.of(List.of(0),List.of(21),List.of(10,11),List.<Integer>of())) {
            assertThrows(DomainException.class,()->plan(request(new Roll(dice,List.of(),List.of()),5,15,damage(3,List.of(),List.of(),List.of()))));
        }
        for(var dice:List.of(List.of(0),List.of(9),List.of(1,2),List.<Integer>of()))
            assertThrows(DomainException.class,()->resolve(simple(15,5,15),dice));
        assertThrows(DomainException.class,()->resolve(simple(1,100,0),List.of(1)));
    }
    @Test void inputsAndEvidenceAreImmutableAndReevaluationIsIdentical() {
        var dice=new ArrayList<>(List.of(15));var factors=new ArrayList<>(List.of(factor("confirmed")));
        var r=request(new Roll(dice,List.of(),List.of()),5,15,damage(3,List.of(),factors,List.of()));
        var actual=new ArrayList<>(List.of(8));var result=resolve(r,actual);
        dice.set(0,1);factors.clear();actual.set(0,1);
        assertEquals(result,resolve(r,List.of(8)));assertEquals(5,result.damage());
        assertThrows(UnsupportedOperationException.class,()->result.dice().add(2));
        assertThrows(UnsupportedOperationException.class,()->result.review().explanation().clear());
        assertTrue(result.explanation().stream().allMatch(e->e.source().revision().equals("5.2.1")));
    }
    @Test void criticalDoublesAllBaseDiceButNeverCopiesTheFirstRollOrModifier() {
        var d=new Damage(2,6,-2,DamageType.BLUDGEONING,List.of(),List.of(),List.of());
        var r=request(new Roll(List.of(20),List.of(),List.of()),0,10,d);
        var result=resolve(r,List.of(1,2,5,6));
        assertEquals(4,result.review().candidate().damageCount());assertEquals(12,result.damage());
        assertEquals(List.of(1,2,5,6),result.dice());
    }
    @Test void ordinaryDamageAndEachDefenseAreIndependent() {
        var roll=new Roll(List.of(15),List.of(),List.of());
        assertEquals(10,resolve(request(roll,5,15,damage(3,List.of(),List.of(),List.of())),List.of(7)).damage());
        assertEquals(3,resolve(request(roll,5,15,damage(0,List.of(),List.of(factor("r")),List.of())),List.of(7)).damage());
        assertEquals(14,resolve(request(roll,5,15,damage(0,List.of(),List.of(),List.of(factor("v")))),List.of(7)).damage());
    }
    @Test void unverifiedDefenseSourceCannotSlipThroughEvenOnANaturalTwenty() {
        var other=new Factor("conditional defense",new CharacterSheet.Source("DM expansion","2025","5R","Unverified"));
        for(var d:List.of(damage(0,List.of(other),List.of(),List.of()),damage(0,List.of(),List.of(other),List.of()),damage(0,List.of(),List.of(),List.of(other)))) {
            var r=request(new Roll(List.of(20),List.of(),List.of()),100,0,d);
            var result=resolve(r,List.of());assertNull(result.damage());assertNull(result.review().candidate());
            assertTrue(result.review().reasons().getFirst().startsWith("UNSUPPORTED_FACTOR_SOURCE"));
        }
    }
    @Test void allManualReasonsRemainAvailableAndManualDoesNotAcceptDamageDice() {
        var custom=new CharacterSheet.Source("Homebrew","v1","CUSTOM","");var r=simple(20,0,0);
        var unsupported=new Request("unknown","SAVE",custom,new Context(false,List.of(factor("reroll"))),r.roll(),0,0,r.damage());
        var result=plan(unsupported);assertEquals(5,result.reasons().size());assertTrue(result.explanation().isEmpty());
        assertThrows(DomainException.class,()->resolve(unsupported,List.of(8)));
    }
    @Test void sourceEditionMustMatchExactlyAndIsNotInferredFromYear() {
        var r=simple(20,0,0);
        for(var source:List.of(new CharacterSheet.Source("SRD","5.2","5R",""),
                new CharacterSheet.Source("SRD","5.2.1","2014",""),
                new CharacterSheet.Source("Players Handbook","2024","5R",""),
                new CharacterSheet.Source("SRD","5.2.1","CUSTOM",""))) {
            assertEquals("MANUAL_REQUIRED",plan(new Request(PROFILE,r.action(),source,r.context(),r.roll(),0,0,r.damage())).status());
        }
    }
    @Test void malformedAndOversizedInputsFailWithDomainErrors() {
        assertThrows(DomainException.class,()->plan(null));assertThrows(DomainException.class,()->resolve(simple(15,0,0),null));
        assertThrows(DomainException.class,()->new Roll(Arrays.asList((Integer)null),List.of(),List.of()));
        assertThrows(DomainException.class,()->new Context(true,null));
        assertThrows(DomainException.class,()->new Factor("",CORE));assertThrows(DomainException.class,()->new Factor("x",null));
        assertThrows(DomainException.class,()->new Roll(List.of(1),Collections.nCopies(101,factor("a")),List.of()));
        assertThrows(DomainException.class,()->new Damage(0,8,0,DamageType.FIRE,List.of(),List.of(),List.of()));
        assertThrows(DomainException.class,()->new Damage(51,8,0,DamageType.FIRE,List.of(),List.of(),List.of()));
        assertThrows(DomainException.class,()->new Damage(1,1001,0,DamageType.FIRE,List.of(),List.of(),List.of()));
        assertThrows(DomainException.class,()->new Damage(1,8,10001,DamageType.FIRE,List.of(),List.of(),List.of()));
        assertThrows(DomainException.class,()->new Damage(1,8,0,null,List.of(),List.of(),List.of()));
        assertThrows(DomainException.class,()->simple(15,10001,0));assertThrows(DomainException.class,()->simple(15,0,1001));
        assertThrows(DomainException.class,()->resolve(simple(15,0,0),Arrays.asList((Integer)null)));
        var error=assertThrows(DomainException.class,()->simple(15,0,-1));assertEquals(400,error.status());assertEquals("INVALID_INPUT",error.code());
    }
    @Test void largestSupportedCriticalRemainsBoundedWithoutIntegerOverflow() {
        var d=new Damage(50,1000,10000,DamageType.FIRE,List.of(),List.of(),List.of(factor("fire")));
        var r=request(new Roll(List.of(20),List.of(),List.of()),10000,1000,d);
        assertEquals(220000,resolve(r,Collections.nCopies(100,1000)).damage());
    }
}
