package io.dndroll.domain;

import java.util.*;

/** Pure HP/temp HP plan. Damage has already passed the attack calculator's defenses. */
public final class DamageEffect {
    private DamageEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-DAMAGE-HP-V1";
    public record Plan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,Integer absorbed,Integer hpLoss) {
        public Plan { reasons=List.copyOf(reasons); }
    }
    public static Plan plan(CharacterSheet target,int damage) {
        if(target==null||damage<0||damage>220000)throw CharacterSheet.invalid();
        target.validate();var b=target.combat();var reasons=new ArrayList<String>();
        int absorbed=Math.min(b.tempHp(),damage),loss=damage-absorbed;
        if(b.hp()==0||loss>=b.hp())reasons.add("ZERO_HP_REQUIRES_MANUAL");
        if(!target.conditions().isEmpty())reasons.add("CONDITIONS_REQUIRE_MANUAL");
        if(b.deathSuccesses()!=0||b.deathFailures()!=0)reasons.add("DEATH_SAVES_REQUIRE_MANUAL");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,null,null,null,null);
        return new Plan(PROFILE,"CANDIDATE",List.of(),b.hp()-loss,b.tempHp()-absorbed,absorbed,loss);
    }
}
