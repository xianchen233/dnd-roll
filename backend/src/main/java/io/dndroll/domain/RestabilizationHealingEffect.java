package io.dndroll.domain;
import java.util.*;
/** Adjudicated healing; the application establishes durable A19 provenance. */
public final class RestabilizationHealingEffect {
    private RestabilizationHealingEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-RESTABILIZATION-HEALING-V1";
    public record Plan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,Integer healed,Integer deathSuccesses,Integer deathFailures,List<String> removed) {
        public Plan { reasons=List.copyOf(reasons);removed=List.copyOf(removed); }
    }
    public static Plan plan(CharacterSheet target,List<CharacterSheet.Condition> canonical,boolean original,int amount,boolean reviewed){
        if(target==null||amount<1||amount>100000)throw CharacterSheet.invalid();target.validate();
        var b=target.combat();var reasons=new ArrayList<String>();
        if(!original)reasons.add("UNCHANGED_RESTABILIZATION_ORIGIN_REQUIRED");
        if(!reviewed)reasons.add("HEALING_REVIEW_REQUIRED");
        if(canonical==null||canonical.size()!=2||!canonical.get(0).id().startsWith("a19-stable:")||!canonical.get(1).id().startsWith("a16-unconscious:")||!target.conditions().equals(canonical))reasons.add("CANONICAL_RESTABILIZATION_PAIR_REQUIRED");
        if(b.maxHp()==0||b.hp()!=0||b.deathSuccesses()!=0||b.deathFailures()!=0)reasons.add("RESTABILIZATION_VALUES_REQUIRED");
        if(!target.modifiers().isEmpty()||!target.overrides().isEmpty())reasons.add("TARGET_ADJUSTMENTS_REQUIRE_MANUAL");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,null,null,null,null,null,List.of());
        int healed=Math.min(amount,b.maxHp());return new Plan(PROFILE,"CANDIDATE",List.of(),healed,b.tempHp(),healed,0,0,canonical.stream().map(CharacterSheet.Condition::id).toList());
    }
}
