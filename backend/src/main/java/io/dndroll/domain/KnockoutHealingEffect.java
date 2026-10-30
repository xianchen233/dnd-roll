package io.dndroll.domain;

import java.util.*;

/** Adjudicated healing only; provenance is established by the durable service. */
public final class KnockoutHealingEffect {
    private KnockoutHealingEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-KNOCKOUT-HEALING-V1";
    public record Plan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,Integer healed,String conditionId) {
        public Plan { reasons=List.copyOf(reasons); }
    }
    public static Plan plan(CharacterSheet target,CharacterSheet.Condition annotation,boolean originalVersion,int amount,boolean reviewed){
        if(target==null||amount<1||amount>100000)throw CharacterSheet.invalid();target.validate();
        var b=target.combat();var reasons=new ArrayList<String>();
        if(annotation==null||!originalVersion)reasons.add("UNCHANGED_KNOCKOUT_ORIGIN_REQUIRED");
        if(!reviewed)reasons.add("HEALING_REVIEW_REQUIRED");
        if(annotation==null||!target.conditions().equals(List.of(annotation)))reasons.add("CANONICAL_KNOCKOUT_ONLY_REQUIRED");
        if(b.hp()!=1||b.tempHp()!=0||b.deathSuccesses()!=0||b.deathFailures()!=0)reasons.add("KNOCKOUT_VALUES_REQUIRED");
        int healed=Math.min(amount,b.maxHp()-b.hp());if(healed<=0)reasons.add("NO_ACTUAL_HEALING");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,null,null,null,null);
        return new Plan(PROFILE,"CANDIDATE",List.of(),b.hp()+healed,b.tempHp(),healed,annotation.id());
    }
}
