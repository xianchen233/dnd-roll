package io.dndroll.domain;

import java.util.*;

/** Separate dagger profile: Finesse is supported, Thrown never qualifies as melee. */
public final class DaggerKnockoutEffect {
    private DaggerKnockoutEffect() {}
    public static final UUID DAGGER=UUID.fromString("e1086d6d-e523-53d9-93b9-7aa24c718e39");
    public static final String PROFILE="5R-SRD-5.2.1-DAGGER-KNOCKOUT-V1";
    public static KnockoutEffect.Plan plan(CharacterSheet target,int damage,boolean supportedDagger,boolean distinct,String treatment,boolean reviewed){
        if(treatment==null||!Set.of("KNOCK_OUT_MELEE","THROWN","MANUAL").contains(treatment))throw CharacterSheet.invalid();
        var base=KnockoutEffect.plan(target,damage,supportedDagger,distinct,treatment.equals("THROWN")?"MANUAL":treatment,reviewed);
        var reasons=base.reasons().stream().map(v->v.equals("SUPPORTED_MELEE_CLUB_REQUIRED")?"SUPPORTED_MELEE_DAGGER_REQUIRED":v).toList();
        return new KnockoutEffect.Plan(PROFILE,base.status(),reasons,base.hp(),base.tempHp(),base.absorbed(),base.hpLoss());
    }
    public static CharacterSheet.Condition annotation(UUID preview){
        var base=KnockoutEffect.annotation(preview);
        return new CharacterSheet.Condition("a12-dagger-knockout:"+preview,base.name(),base.icon(),base.description(),
            new CharacterSheet.Source("SRD 5.2.1","5.2.1","5R","Knocking Out a Creature p17; Finesse p89; Thrown p90; Dagger p91; "+PROFILE),null,null);
    }
}
