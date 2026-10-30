package io.dndroll.domain;
import java.util.*;
/** A new Help attempt against a durably bound, unchanged A18 outcome. */
public final class RestabilizationEffect {
    private RestabilizationEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-RESTABILIZATION-V1";
    public record Plan(String profile,String status,List<String> reasons,int dc,Integer total,Integer hp,Integer tempHp,Integer deathSuccesses,Integer deathFailures,List<CharacterSheet.Condition> annotations){public Plan{reasons=List.copyOf(reasons);annotations=List.copyOf(annotations);}}
    public static Plan plan(CharacterSheet target,CharacterSheet helper,CharacterSheet.Condition unconscious,UUID attempt,boolean original,boolean reviewed,boolean distinct,boolean bound,Integer total){
        if(target==null||attempt==null)throw CharacterSheet.invalid();target.validate();var b=target.combat();var reasons=new ArrayList<String>();
        if(!original)reasons.add("UNCHANGED_DAMAGE_ORIGIN_REQUIRED");if(!reviewed)reasons.add("ORDINARY_HELP_REVIEW_REQUIRED");if(!distinct)reasons.add("DISTINCT_HELPER_REQUIRED");
        if(unconscious==null||!target.conditions().equals(List.of(unconscious)))reasons.add("ORIGINAL_UNCONSCIOUS_ONLY_REQUIRED");
        if(b.maxHp()==0||b.hp()!=0||b.deathSuccesses()!=0||b.deathFailures()<1||b.deathFailures()>2)reasons.add("UNSTABLE_VALUES_REQUIRED");
        if(!target.modifiers().isEmpty()||!target.overrides().isEmpty())reasons.add("TARGET_ADJUSTMENTS_REQUIRE_MANUAL");
        if(!StabilizationEffect.helperSupported(helper))reasons.add("SUPPORTED_RESTABILIZATION_HELPER_REQUIRED");if(!bound||total==null)reasons.add("BOUND_RESTABILIZATION_CHECK_REQUIRED");if(total!=null&&total<10)reasons.add("RESTABILIZATION_CHECK_FAILED");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,10,total,null,null,null,null,List.of());
        var source=new CharacterSheet.Source("SRD 5.2.1","5.2.1","5R","Stabilizing a Character p18; Death Saving Throws pp17–18; "+PROFILE);
        var stable=new CharacterSheet.Condition("a19-stable:"+attempt,"伤势稳定（再次稳定记录）","","新Help医药检定后再次稳定；仍0HP且昏迷，死豁归零。后续受伤、治疗与1d4小时恢复由DM处理，无自动计时或执行。",source,null,null);
        return new Plan(PROFILE,"CANDIDATE",List.of(),10,total,0,b.tempHp(),0,0,List.of(stable,unconscious));
    }
}
