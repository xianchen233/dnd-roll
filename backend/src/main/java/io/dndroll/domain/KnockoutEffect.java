package io.dndroll.domain;

import java.util.*;

/** Explicit club melee knockout record; rest and unconscious consequences stay manual. */
public final class KnockoutEffect {
    private KnockoutEffect() {}
    public static final UUID CLUB=UUID.fromString("916bf8b1-a24e-5dea-be38-2caee47dad54");
    public static final String PROFILE="5R-SRD-5.2.1-KNOCKOUT-V1";
    public record Plan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,Integer absorbed,Integer hpLoss) {
        public Plan { reasons=List.copyOf(reasons); }
    }
    public static Plan plan(CharacterSheet target,int damage,boolean supportedClub,boolean distinct,String treatment,boolean reviewed){
        if(target==null||damage<0||damage>220000||treatment==null||!Set.of("KNOCK_OUT_MELEE","MANUAL").contains(treatment))throw CharacterSheet.invalid();
        target.validate();var b=target.combat();var reasons=new ArrayList<String>();int absorbed=Math.min(b.tempHp(),damage);
        if(!supportedClub)reasons.add("SUPPORTED_MELEE_CLUB_REQUIRED");
        if(!distinct)reasons.add("DISTINCT_TARGET_REQUIRED");
        if(!treatment.equals("KNOCK_OUT_MELEE"))reasons.add("KNOCKOUT_TREATMENT_REQUIRED");
        if(!reviewed)reasons.add("KNOCKOUT_REVIEW_REQUIRED");
        if(b.hp()==0)reasons.add("ALREADY_ZERO_REQUIRES_MANUAL");
        if(damage-absorbed<b.hp())reasons.add("LETHAL_DAMAGE_REQUIRED");
        if(!target.conditions().isEmpty())reasons.add("CONDITIONS_REQUIRE_MANUAL");
        if(b.deathSuccesses()!=0||b.deathFailures()!=0)reasons.add("DEATH_SAVES_REQUIRE_MANUAL");
        if(!reasons.isEmpty())return new Plan(PROFILE,"MANUAL_REQUIRED",reasons,null,null,null,null);
        return new Plan(PROFILE,"CANDIDATE",List.of(),1,0,absorbed,b.hp()-1);
    }
    public static CharacterSheet.Condition annotation(UUID preview){
        return new CharacterSheet.Condition("a11-knockout:"+preview,"昏迷（击昏记录）","","近战击昏留1HP；DM立即开始并跟踪短休，短休结束、恢复任意HP或动作急救通过DC10感知（医药）后独立移除记录。失能、倒地、掉落物、速度/攻防/专注等关联规则由DM处理；醒来仍倒地。无自动计时或解除。",
            new CharacterSheet.Source("SRD 5.2.1","5.2.1","5R","Knocking Out a Creature p17; Unconscious p191; "+PROFILE),null,null);
    }
}
