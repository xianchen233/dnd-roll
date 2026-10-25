package io.dndroll.domain;

import java.util.*;

/** Pure A04 calculation boundary. Authenticated assembly and application belong to A05. */
public final class AttackCalculator {
    private AttackCalculator() {}
    public static final String PROFILE = "5R-SRD-5.2.1-WEAPON-V1";
    public enum DamageType { ACID, BLUDGEONING, COLD, FIRE, FORCE, LIGHTNING, NECROTIC,
        PIERCING, POISON, PSYCHIC, RADIANT, SLASHING, THUNDER }
    public record Factor(String reason, CharacterSheet.Source source) {
        public Factor { CharacterSheet.text(reason,1000,true); validateSource(source); }
    }
    public record Context(boolean reviewed, List<Factor> exceptions) {
        public Context { exceptions=copy(exceptions,100); }
    }
    public record Roll(List<Integer> d20, List<Factor> advantages, List<Factor> disadvantages) {
        public Roll {
            d20=copy(d20,2); advantages=copy(advantages,100); disadvantages=copy(disadvantages,100);
            int required=advantages.isEmpty()==disadvantages.isEmpty()?1:2;
            if(d20.size()!=required)throw invalid();
            d20.forEach(n->CharacterSheet.number(n,1,20));
        }
    }
    public record Damage(int count, int sides, int modifier, DamageType type,
                         List<Factor> immunities, List<Factor> resistances, List<Factor> vulnerabilities) {
        public Damage {
            CharacterSheet.number(count,1,50); CharacterSheet.number(sides,2,1000);
            CharacterSheet.number(modifier,-10000,10000); if(type==null)throw invalid();
            immunities=copy(immunities,100); resistances=copy(resistances,100); vulnerabilities=copy(vulnerabilities,100);
        }
    }
    public record Request(String profile, String action, CharacterSheet.Source source, Context context,
                          Roll roll, int hitModifier, int armorClass, Damage damage) {
        public Request {
            CharacterSheet.text(profile,100,true); CharacterSheet.text(action,100,true); validateSource(source);
            if(context==null||roll==null||damage==null)throw invalid();
            CharacterSheet.number(hitModifier,-10000,10000); CharacterSheet.number(armorClass,0,1000);
        }
    }
    public record Explanation(String rule, String calculation, CharacterSheet.Source source) {}
    public record Candidate(String mode, int keptIndex, int natural, int total, String verdict,
                            int damageCount, int damageSides) {}
    public record Review(Request request, String status, List<String> reasons, Candidate candidate,
                         List<Explanation> explanation) {
        public Review { reasons=copy(reasons,1000); explanation=copy(explanation,1000); }
    }
    public record Resolution(Review review, List<Integer> dice, Integer rawDamage, Integer damage,
                             List<Explanation> explanation) {
        public Resolution { dice=copy(dice,100); explanation=copy(explanation,1000); }
    }
    public static Review plan(Request request) {
        if(request==null)throw invalid();
        var reasons=new ArrayList<String>();
        if(!PROFILE.equals(request.profile()))reasons.add("UNSUPPORTED_PROFILE: "+request.profile());
        if(!"WEAPON_ATTACK".equals(request.action()))reasons.add("UNSUPPORTED_ACTION: "+request.action());
        if(!supported(request.source()))reasons.add("UNSUPPORTED_ATTACK_SOURCE");
        if(!request.context().reviewed())reasons.add("CONTEXT_REVIEW_REQUIRED");
        request.context().exceptions().forEach(f->reasons.add("UNSUPPORTED_EXCEPTION: "+f.reason()));
        var factors=new ArrayList<Factor>();
        factors.addAll(request.roll().advantages()); factors.addAll(request.roll().disadvantages());
        factors.addAll(request.damage().immunities()); factors.addAll(request.damage().resistances());
        factors.addAll(request.damage().vulnerabilities());
        factors.stream().filter(f->!supported(f.source())).forEach(f->reasons.add("UNSUPPORTED_FACTOR_SOURCE: "+f.reason()));
        if(!reasons.isEmpty())return new Review(request,"MANUAL_REQUIRED",reasons,null,List.of());

        var roll=request.roll(); int kept=0; String mode="NORMAL";
        if(roll.d20().size()==2){
            boolean advantage=!roll.advantages().isEmpty(); mode=advantage?"ADVANTAGE":"DISADVANTAGE";
            int first=roll.d20().getFirst(), second=roll.d20().getLast();
            if(advantage?second>first:second<first)kept=1;
        }
        int natural=roll.d20().get(kept), total=natural+request.hitModifier();
        String verdict=natural==1?"MISS":natural==20?"CRITICAL":total>=request.armorClass()?"HIT":"MISS";
        int count=verdict.equals("MISS")?0:request.damage().count()*(verdict.equals("CRITICAL")?2:1);
        var candidate=new Candidate(mode,kept,natural,total,verdict,count,request.damage().sides());
        return new Review(request,"CANDIDATE",List.of(),candidate,List.of(
            explain("D20_SELECTION",mode+" "+roll.d20()+" -> index "+kept+" = "+natural,"Advantage/Disadvantage pp7-8"),
            explain("ATTACK",natural+" + "+request.hitModifier()+" = "+total+" vs AC "+request.armorClass()+" -> "+verdict,"Attack Rolls p7"),
            explain("DAMAGE_PLAN",count+"d"+request.damage().sides()+" + "+request.damage().modifier(),"Critical Hits p16")));
    }
    public static Resolution resolve(Request request, List<Integer> damageDice) {
        var dice=copy(damageDice,100); var review=plan(request);
        if(review.candidate()==null){
            if(!dice.isEmpty())throw invalid();
            return new Resolution(review,List.of(),null,null,List.of());
        }
        var candidate=review.candidate(); if(dice.size()!=candidate.damageCount())throw invalid();
        dice.forEach(n->CharacterSheet.number(n,1,candidate.damageSides()));
        var explanation=new ArrayList<>(review.explanation());
        if(candidate.verdict().equals("MISS"))return new Resolution(review,dice,0,0,explanation);
        var damage=request.damage();
        int raw=Math.max(0,dice.stream().mapToInt(Integer::intValue).sum()+damage.modifier()), result=raw;
        explanation.add(explain("DAMAGE", "max(0, "+dice+" + "+damage.modifier()+") = "+raw,"Damage Rolls p16"));
        if(!damage.resistances().isEmpty()){
            result/=2;
            explanation.add(explain("RESISTANCE","floor("+raw+" / 2) = "+result,"Resistance and Vulnerability p17"));
        }
        if(!damage.vulnerabilities().isEmpty()){
            int before=result; result*=2;
            explanation.add(explain("VULNERABILITY",before+" * 2 = "+result,"Resistance and Vulnerability p17"));
        }
        if(!damage.immunities().isEmpty()){
            result=0; explanation.add(explain("IMMUNITY","damage = 0","Immunity p17"));
        }
        return new Resolution(review,dice,raw,result,explanation);
    }
    private static Explanation explain(String rule,String calculation,String detail){
        return new Explanation(rule,calculation,new CharacterSheet.Source("SRD","5.2.1","5R",detail));
    }
    private static boolean supported(CharacterSheet.Source source){
        return source.book().equals("SRD")&&source.revision().equals("5.2.1")&&source.rules().equals("5R");
    }
    private static void validateSource(CharacterSheet.Source source){
        if(source==null)throw invalid();
        CharacterSheet.text(source.book(),200,true); CharacterSheet.text(source.revision(),100,true);
        CharacterSheet.text(source.rules(),100,true); CharacterSheet.text(source.detail(),1000,false);
    }
    private static <T> List<T> copy(List<T> values,int max){
        if(values==null||values.size()>max||values.stream().anyMatch(Objects::isNull))throw invalid();
        return List.copyOf(values);
    }
    private static DomainException invalid(){return new DomainException("INVALID_INPUT",400,"Invalid automatic attack input");}
}
