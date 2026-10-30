package io.dndroll.domain;

import java.util.*;

/** Explicit ordinary-monster treatment, not an inference from card names or control. */
public final class MonsterDeathEffect {
    private MonsterDeathEffect() {}
    public static final String PROFILE="5R-SRD-5.2.1-MONSTER-DEATH-V1";
    public record Plan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,Integer absorbed,Integer hpLoss) {
        public Plan { reasons=List.copyOf(reasons); }
    }
    public static Plan plan(CharacterSheet target,int damage,boolean distinct,String treatment,boolean reviewed){
        if(target==null||damage<0||damage>220000||!Set.of("MONSTER_DIES_AT_ZERO","MANUAL").contains(treatment))throw CharacterSheet.invalid();
        target.validate();var b=target.combat();var reasons=new ArrayList<String>();int absorbed=Math.min(b.tempHp(),damage);
        if(!distinct)reasons.add("DISTINCT_TARGET_REQUIRED");
        if(!treatment.equals("MONSTER_DIES_AT_ZERO"))reasons.add("MONSTER_TREATMENT_REQUIRED");
        if(!reviewed)reasons.add("DEATH_REVIEW_REQUIRED");
        if(b.hp()==0)reasons.add("ALREADY_ZERO_REQUIRES_MANUAL");
        if(damage-absorbed<b.hp())reasons.add("LETHAL_DAMAGE_REQUIRED");
        if(!target.conditions().isEmpty())reasons.add("CONDITIONS_REQUIRE_MANUAL");
        if(b.deathSuccesses()!=0||b.deathFailures()!=0)reasons.add("DEATH_SAVES_REQUIRE_MANUAL");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,null,null,null,null);
        return new Plan(PROFILE,"CANDIDATE",List.of(),0,0,absorbed,b.hp());
    }
    public static CharacterSheet.Condition annotation(UUID preview){
        return new CharacterSheet.Condition("a10-death:"+preview,"死亡（记录）","","DM明确采用普通怪物0HP死亡；这是死亡注记，不自动执行全部死亡规则。复活或误判需DM独立移除。",
            new CharacterSheet.Source("SRD 5.2.1","5.2.1","5R","Monster Death p17; "+PROFILE),null,null);
    }
}
