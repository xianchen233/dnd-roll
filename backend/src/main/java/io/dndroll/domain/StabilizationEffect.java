package io.dndroll.domain;

import java.util.*;

/** Internal ordinary 0HP stabilization calculator; no transport or state writes. */
public final class StabilizationEffect {
    private StabilizationEffect() {}
    public static final String PROFILE = "5R-SRD-5.2.1-STABILIZATION-V1";
    public record Plan(String profile, String status, List<String> reasons, int dc,
                       Integer total, Integer hp, Integer tempHp, Integer deathSuccesses,
                       Integer deathFailures, List<CharacterSheet.Condition> annotations) {
        public Plan { reasons = List.copyOf(reasons); annotations = List.copyOf(annotations); }
    }
    public static boolean helperSupported(CharacterSheet helper) {
        if (helper == null) return false;
        helper.validate();
        var b = helper.combat();
        return b.hp() > 0 && b.deathSuccesses() == 0 && b.deathFailures() == 0
            && helper.conditions().isEmpty() && helper.modifiers().isEmpty() && helper.overrides().isEmpty()
            && helper.skills().stream().anyMatch(c -> c.id().equals("medicine") && c.ability().equals("WIS"));
    }
    public static Plan plan(CharacterSheet target, CharacterSheet helper, UUID attemptId,
                            boolean ordinaryZeroReviewed, boolean distinct, boolean boundCheck, Integer total) {
        if (target == null || attemptId == null) throw CharacterSheet.invalid();
        target.validate();
        var b = target.combat();
        var reasons = new ArrayList<String>();
        if (b.maxHp() == 0 || b.hp() != 0) reasons.add("ORDINARY_ZERO_HP_REQUIRED");
        if (b.deathSuccesses() >= 3 || b.deathFailures() >= 3) reasons.add("UNRESOLVED_DEATH_SAVES_REQUIRED");
        if (!target.conditions().isEmpty()) reasons.add("TARGET_CONDITIONS_REQUIRE_MANUAL");
        if (!target.modifiers().isEmpty() || !target.overrides().isEmpty()) reasons.add("TARGET_ADJUSTMENTS_REQUIRE_MANUAL");
        if (!ordinaryZeroReviewed) reasons.add("ORDINARY_ZERO_REVIEW_REQUIRED");
        if (!distinct) reasons.add("DISTINCT_HELPER_REQUIRED");
        if (!helperSupported(helper)) reasons.add("SUPPORTED_STABILIZATION_HELPER_REQUIRED");
        if (!boundCheck || total == null) reasons.add("BOUND_STABILIZATION_CHECK_REQUIRED");
        if (total != null && total < 10) reasons.add("STABILIZATION_CHECK_FAILED");
        if (!reasons.isEmpty()) return new Plan(PROFILE, "MANUAL_REQUIRED", reasons, 10, total, null, null, null, null, List.of());
        var source = new CharacterSheet.Source("SRD 5.2.1", "5.2.1", "5R",
            "Stabilizing a Character p18; Death Saving Throws pp17–18; " + PROFILE);
        var stable = new CharacterSheet.Condition("a16-stable:" + attemptId, "伤势稳定（0HP记录）", "",
            "仍为0HP且昏迷，暂不进行死亡豁免；受到伤害会失去稳定。后续伤害、治疗及1d4小时恢复由DM处理，无自动计时或执行。", source, null, null);
        var unconscious = new CharacterSheet.Condition("a16-unconscious:" + attemptId, "昏迷（0HP记录）", "",
            "普通0HP昏迷；稳定不代表苏醒。失能、倒地、掉落、速度/攻防/专注及后续解除由DM处理。", source, null, null);
        return new Plan(PROFILE, "CANDIDATE", List.of(), 10, total, 0, b.tempHp(), 0, 0, List.of(stable, unconscious));
    }
}
