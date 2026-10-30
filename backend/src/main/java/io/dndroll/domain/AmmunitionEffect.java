package io.dndroll.domain;

import java.util.*;

/** Ordinary shortbow arrows are an explicit DM-mapped counter, never inferred from text. */
public final class AmmunitionEffect {
    private AmmunitionEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-SHORTBOW-AMMO-V1";
    public static final UUID SHORTBOW=UUID.fromString("1fd208e1-f443-5090-b83c-363a460245aa");
    public record Plan(String profile,String status,List<String> reasons,String resourceId,Integer before,Integer after,Integer hp,Integer tempHp) {
        public Plan { reasons=List.copyOf(reasons); }
    }
    public static Plan manual(String resourceId,List<String> reasons){return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,resourceId,null,null,null,null);}
    public static Plan plan(CharacterSheet source,CharacterSheet target,boolean shortbow,int damage,String resourceId,boolean reviewed){
        if(source==null)throw CharacterSheet.invalid();source.validate();CharacterSheet.text(resourceId,100,true);
        var targetPlan=DamageEffect.plan(target,damage);var reasons=new ArrayList<>(targetPlan.reasons());
        if(!shortbow)reasons.add("SUPPORTED_SHORTBOW_REQUIRED");
        if(!reviewed)reasons.add("AMMUNITION_REVIEW_REQUIRED");
        var b=source.combat();if(b.hp()==0||b.deathSuccesses()!=0||b.deathFailures()!=0||!source.conditions().isEmpty())reasons.add("SOURCE_EFFECTS_REQUIRE_MANUAL");
        var counter=source.resources().stream().filter(r->r.id().equals(resourceId)&&r.kind().equals("CUSTOM")).findFirst().orElse(null);
        if(counter==null)reasons.add("ARROW_COUNTER_REQUIRED");else if(counter.current()<1)reasons.add("INSUFFICIENT_AMMUNITION");
        if(!reasons.isEmpty())return manual(resourceId,reasons);
        return new Plan(PROFILE,"CANDIDATE",List.of(),resourceId,counter.current(),counter.current()-1,targetPlan.hp(),targetPlan.tempHp());
    }
}
