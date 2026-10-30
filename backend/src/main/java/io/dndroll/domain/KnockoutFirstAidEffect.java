package io.dndroll.domain;
import java.util.*;

/** Ordinary Wisdom (Medicine) aid only; action availability/contact are DM-reviewed. */
public final class KnockoutFirstAidEffect {
    private KnockoutFirstAidEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-KNOCKOUT-FIRST-AID-V1";
    public record Plan(String profile,String status,List<String> reasons,int dc,Integer total,Integer hp,Integer tempHp,String conditionId){public Plan{reasons=List.copyOf(reasons);}}
    public static boolean helperSupported(CharacterSheet s){
        if(s==null)return false;s.validate();var b=s.combat();
        return b.hp()>0&&b.deathSuccesses()==0&&b.deathFailures()==0&&s.conditions().isEmpty()&&s.modifiers().isEmpty()&&s.overrides().isEmpty()
            &&s.skills().stream().anyMatch(v->v.id().equals("medicine")&&v.ability().equals("WIS"));
    }
    public static List<String> eligibility(CharacterSheet target,CharacterSheet.Condition annotation,boolean originalVersion,boolean helperSupported,boolean distinct,boolean reviewed){
        if(target==null)throw CharacterSheet.invalid();target.validate();var b=target.combat();var reasons=new ArrayList<String>();
        if(annotation==null||!originalVersion)reasons.add("UNCHANGED_KNOCKOUT_ORIGIN_REQUIRED");
        if(annotation==null||!target.conditions().equals(List.of(annotation)))reasons.add("CANONICAL_KNOCKOUT_ONLY_REQUIRED");
        if(b.hp()!=1||b.tempHp()!=0||b.deathSuccesses()!=0||b.deathFailures()!=0)reasons.add("KNOCKOUT_VALUES_REQUIRED");
        if(!helperSupported)reasons.add("SUPPORTED_AID_HELPER_REQUIRED");if(!distinct)reasons.add("DISTINCT_HELPER_REQUIRED");if(!reviewed)reasons.add("FIRST_AID_REVIEW_REQUIRED");
        return List.copyOf(reasons);
    }
    public static Plan plan(CharacterSheet target,CharacterSheet.Condition annotation,boolean originalVersion,boolean helperSupported,boolean distinct,boolean reviewed,boolean bound,Integer total){
        var reasons=new ArrayList<>(eligibility(target,annotation,originalVersion,helperSupported,distinct,reviewed));
        if(!bound||total==null)reasons.add("LATEST_BOUND_AID_CHECK_REQUIRED");if(total!=null&&total<10)reasons.add("FIRST_AID_CHECK_FAILED");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,10,total,null,null,null);
        return new Plan(PROFILE,"CANDIDATE",List.of(),10,total,target.combat().hp(),target.combat().tempHp(),annotation.id());
    }
}
