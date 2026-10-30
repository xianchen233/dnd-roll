package io.dndroll.domain;
import java.util.*;
/** First bounded, adjudicated damage event; no dice or automatic death. */
public final class StabilizationDamageEffect {
    private StabilizationDamageEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-STABILIZATION-DAMAGE-V1";
    public record Plan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,Integer absorbed,Integer deathSuccesses,Integer deathFailures,List<String> removed) {
        public Plan { reasons=List.copyOf(reasons);removed=List.copyOf(removed); }
    }
    public static Plan plan(CharacterSheet target,List<CharacterSheet.Condition> canonical,boolean original,int amount,boolean critical,boolean reviewed){
        if(target==null||amount<0||amount>220000)throw CharacterSheet.invalid();target.validate();var b=target.combat();var reasons=new ArrayList<String>();
        if(!original)reasons.add("UNCHANGED_STABILIZATION_ORIGIN_REQUIRED");
        if(!reviewed)reasons.add("DAMAGE_REVIEW_REQUIRED");
        if(canonical==null||canonical.size()!=2||!target.conditions().equals(canonical))reasons.add("CANONICAL_STABILIZATION_PAIR_REQUIRED");
        if(b.maxHp()==0||b.hp()!=0||b.deathSuccesses()!=0||b.deathFailures()!=0)reasons.add("STABILIZATION_VALUES_REQUIRED");
        if(!target.modifiers().isEmpty()||!target.overrides().isEmpty())reasons.add("TARGET_ADJUSTMENTS_REQUIRE_MANUAL");
        if(amount==0)reasons.add("POSITIVE_DAMAGE_REQUIRED");
        if(amount>=b.maxHp())reasons.add("POTENTIALLY_FATAL_DAMAGE_REQUIRES_MANUAL");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,null,null,null,null,null,List.of());
        int absorbed=Math.min(b.tempHp(),amount);return new Plan(PROFILE,"CANDIDATE",List.of(),0,b.tempHp()-absorbed,absorbed,0,critical?2:1,List.of(canonical.getFirst().id()));
    }
}
