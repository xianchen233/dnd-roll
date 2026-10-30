package io.dndroll.domain;

import java.util.*;

/** DM-adjudicated continuous game time; no clock or resource recovery. */
public final class KnockoutRestEffect {
    private KnockoutRestEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-KNOCKOUT-REST-V1";
    public record Plan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,String conditionId) {
        public Plan { reasons=List.copyOf(reasons); }
    }
    public static Plan plan(CharacterSheet target,CharacterSheet.Condition annotation,boolean originalVersion,int minutes,boolean interrupted,boolean reviewed){
        if(target==null||minutes<0||minutes>100000)throw CharacterSheet.invalid();target.validate();
        var b=target.combat();var reasons=new ArrayList<String>();
        if(annotation==null||!originalVersion)reasons.add("UNCHANGED_KNOCKOUT_ORIGIN_REQUIRED");
        if(annotation==null||!target.conditions().equals(List.of(annotation)))reasons.add("CANONICAL_KNOCKOUT_ONLY_REQUIRED");
        if(b.hp()!=1||b.tempHp()!=0||b.deathSuccesses()!=0||b.deathFailures()!=0)reasons.add("KNOCKOUT_VALUES_REQUIRED");
        if(minutes<60)reasons.add("SHORT_REST_DURATION_REQUIRED");
        if(interrupted)reasons.add("UNINTERRUPTED_REST_REQUIRED");
        if(!reviewed)reasons.add("REST_REVIEW_REQUIRED");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,null,null,null);
        return new Plan(PROFILE,"CANDIDATE",List.of(),b.hp(),b.tempHp(),annotation.id());
    }
}
