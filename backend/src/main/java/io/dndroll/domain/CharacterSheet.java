package io.dndroll.domain;

import java.time.Instant;
import java.util.*;

/** Typed, portable sheet. Content descriptions never imply executable rules. */
public record CharacterSheet(
    String name, String avatar, Entry species, Background background, List<ClassLevel> classes,
    Map<String,Integer> abilities, List<Check> skills, List<Check> saves, Combat combat,
    List<Attack> attacks, List<Casting> casting, List<Spell> spells, List<Feature> features,
    List<Resource> resources, List<Item> equipment, Money money, List<Condition> conditions,
    List<Modifier> modifiers, List<Override> overrides, String notes
) {
    public record Source(String book, String revision, String rules, String detail) {}
    public record Entry(String id, String name, String description, Source source) {}
    public record Background(Entry entry, Map<String,Integer> increases, String originFeat, List<String> proficiencies, List<String> equipment) {}
    public record ClassLevel(String id, String name, String subclass, int level, Source source) {}
    public record Check(String id, String name, String ability, String proficiency, int bonus, Source source) {}
    public record Combat(int maxHp, int hp, int tempHp, int speed, int deathSuccesses, int deathFailures, String acMode, int manualAc, Source source) {}
    public record Attack(String id, String name, String ability, boolean proficient, int hitBonus, int damageBonus, String dice, String damageType, String mastery, Source source) {}
    public record Casting(String id, String name, String ability, int attackBonus, int dcBonus, Source source) {}
    public record Spell(String id, String name, int level, boolean prepared, String castingId, String description, Source source) {}
    public record Feature(String id, String name, String kind, String choices, String description, Source source) {}
    public record Resource(String id, String name, String kind, int current, int max, String recovery, Source source) {}
    public record Item(String id, String name, int quantity, boolean equipped, String description, Source source) {}
    public record Money(int cp, int sp, int ep, int gp, int pp) {}
    public record Condition(String id, String name, String icon, String description, Source source, Integer remainingRounds, Instant expiresAt) {}
    public record Modifier(String target, int amount, Source source) {}
    public record Override(String target, int value, String reason) {}
    public record Component(String label, int value, Source source) {}
    public record Calculation(int baseValue, int value, List<Component> components, String overrideReason) {}
    public CharacterSheet change(String newName,Combat newCombat,List<Resource> newResources,List<Condition> newConditions,List<Override> newOverrides) {
        return new CharacterSheet(newName,avatar,species,background,classes,abilities,skills,saves,newCombat,attacks,casting,spells,features,newResources,equipment,money,newConditions,modifiers,newOverrides,notes);
    }

    public static final List<String> ABILITIES = List.of("STR","DEX","CON","INT","WIS","CHA");
    public static final Map<String,String> SKILLS = Collections.unmodifiableMap(new LinkedHashMap<>(Map.ofEntries(
        Map.entry("acrobatics","DEX"),Map.entry("animalHandling","WIS"),Map.entry("arcana","INT"),Map.entry("athletics","STR"),
        Map.entry("deception","CHA"),Map.entry("history","INT"),Map.entry("insight","WIS"),Map.entry("intimidation","CHA"),
        Map.entry("investigation","INT"),Map.entry("medicine","WIS"),Map.entry("nature","INT"),Map.entry("perception","WIS"),
        Map.entry("performance","CHA"),Map.entry("persuasion","CHA"),Map.entry("religion","INT"),Map.entry("sleightOfHand","DEX"),
        Map.entry("stealth","DEX"),Map.entry("survival","WIS"))));
    public static Source manual() { return new Source("手动填写","1","CUSTOM",""); }
    public static CharacterSheet empty(String name) {
        var s=manual();var abilities=new LinkedHashMap<String,Integer>();ABILITIES.forEach(a->abilities.put(a,10));
        var skills=SKILLS.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->new Check(e.getKey(),e.getKey(),e.getValue(),"NONE",0,s)).toList();
        var saves=ABILITIES.stream().map(a->new Check(a,a,a,"NONE",0,s)).toList();
        return new CharacterSheet(name,"",new Entry("species","","",s),new Background(new Entry("background","","",s),Map.of(),"",List.of(),List.of()),
            List.of(new ClassLevel("class-1","冒险者","",1,s)),abilities,skills,saves,new Combat(1,1,0,30,0,0,"UNARMORED",10,s),
            List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),new Money(0,0,0,0,0),List.of(),List.of(),List.of(),"");
    }
    public Map<String,Calculation> calculate() {
        var out=new LinkedHashMap<String,Calculation>();
        Source core=new Source("SRD 5.2","5.2","5R","基础公式；见 docs/research/character-sheet-5r.md");
        int level=classes.stream().mapToInt(ClassLevel::level).sum(), pb=2+(level-1)/4;
        for(String a:ABILITIES) put(out,"ability:"+a,List.of(new Component(a+" floor((score-10)/2)",Math.floorDiv(abilities.get(a)-10,2),core)));
        put(out,"proficiency",List.of(new Component("角色总等级 "+level,pb,core)));
        pb=out.get("proficiency").value();
        for(Check c:concat(skills,saves)) {
            int multiplier=c.proficiency().equals("EXPERT")?2:c.proficiency().equals("PROFICIENT")?1:0;
            put(out,(saves.contains(c)?"save:":"skill:")+c.id(),List.of(new Component(c.ability(),out.get("ability:"+c.ability()).value(),core),new Component(c.proficiency(),pb*multiplier,c.source()),new Component("手填修正",c.bonus(),c.source())));
        }
        put(out,"ac",combat.acMode().equals("UNARMORED")?List.of(new Component("无甲基础",10,core),new Component("DEX",out.get("ability:DEX").value(),core)):List.of(new Component("手动AC",combat.manualAc(),combat.source())));
        put(out,"initiative",List.of(new Component("DEX",out.get("ability:DEX").value(),core)));
        put(out,"passivePerception",List.of(new Component("被动基础",10,core),new Component("察觉",out.get("skill:perception").value(),core)));
        for(Attack a:attacks) {
            int mod=out.get("ability:"+a.ability()).value();
            put(out,"attack:"+a.id(),List.of(new Component(a.ability(),mod,core),new Component("武器熟练",a.proficient()?pb:0,a.source()),new Component("命中修正",a.hitBonus(),a.source())));
            put(out,"damage:"+a.id(),List.of(new Component(a.ability(),mod,core),new Component("伤害修正",a.damageBonus(),a.source())));
        }
        for(Casting c:casting) {
            int mod=out.get("ability:"+c.ability()).value();
            put(out,"spellAttack:"+c.id(),List.of(new Component(c.ability(),mod,c.source()),new Component("熟练",pb,core),new Component("法术攻击修正",c.attackBonus(),c.source())));
            put(out,"spellDc:"+c.id(),List.of(new Component("DC基础",8,core),new Component(c.ability(),mod,c.source()),new Component("熟练",pb,core),new Component("DC修正",c.dcBonus(),c.source())));
        }
        return Collections.unmodifiableMap(out);
    }
    private void put(Map<String,Calculation> out,String key,List<Component> components) {
        var all=new ArrayList<>(components);
        modifiers.stream().filter(m->m.target().equals(key)).forEach(m->all.add(new Component("额外修正",m.amount(),m.source())));
        int base=all.stream().mapToInt(Component::value).sum();var o=overrides.stream().filter(v->v.target().equals(key)).findFirst();
        out.put(key,new Calculation(base,o.map(Override::value).orElse(base),List.copyOf(all),o.map(Override::reason).orElse("")));
    }
    private static <T> List<T> concat(List<T>a,List<T>b){var r=new ArrayList<T>(a);r.addAll(b);return r;}
    public void validate() {
        text(name,100,true);text(avatar,16,false);text(notes,10000,false);entry(species);entry(background.entry());
        if(abilities==null||!abilities.keySet().equals(new HashSet<>(ABILITIES)))throw invalid();abilities.values().forEach(v->{if(v==null)throw invalid();number(v,1,30);});
        list(classes,20);if(classes.isEmpty())throw invalid();unique(classes.stream().map(ClassLevel::id).toList());
        classes.forEach(c->{text(c.name(),100,true);text(c.subclass(),100,false);number(c.level(),1,20);source(c.source());});number(classes.stream().mapToInt(ClassLevel::level).sum(),1,20);
        if(background.increases()==null||!ABILITIES.containsAll(background.increases().keySet()))throw invalid();background.increases().values().forEach(v->{if(v==null)throw invalid();number(v,0,2);});
        text(background.originFeat(),80,false);strings(background.proficiencies(),50,200);strings(background.equipment(),100,200);
        checks(skills,false);checks(saves,true);
        if(combat==null||money==null)throw invalid();number(combat.maxHp(),0,100000);number(combat.hp(),0,combat.maxHp());number(combat.tempHp(),0,100000);number(combat.speed(),0,10000);
        number(combat.deathSuccesses(),0,3);number(combat.deathFailures(),0,3);one(combat.acMode(),"UNARMORED","MANUAL");number(combat.manualAc(),0,1000);source(combat.source());
        list(attacks,100);unique(attacks.stream().map(Attack::id).toList());attacks.forEach(a->{text(a.name(),100,true);ability(a.ability());number(a.hitBonus(),-1000,1000);number(a.damageBonus(),-1000,1000);text(a.dice(),100,false);text(a.damageType(),100,false);text(a.mastery(),1000,false);source(a.source());});
        list(casting,20);unique(casting.stream().map(Casting::id).toList());casting.forEach(c->{text(c.name(),100,true);ability(c.ability());number(c.attackBonus(),-1000,1000);number(c.dcBonus(),-1000,1000);source(c.source());});
        list(spells,500);unique(spells.stream().map(Spell::id).toList());spells.forEach(s->{text(s.name(),100,true);number(s.level(),0,9);if(casting.stream().noneMatch(c->c.id().equals(s.castingId())))throw invalid();text(s.description(),5000,false);source(s.source());});
        list(features,200);unique(features.stream().map(Feature::id).toList());features.forEach(f->{text(f.name(),100,true);text(f.kind(),100,true);text(f.choices(),2000,false);text(f.description(),5000,false);source(f.source());});
        list(resources,100);unique(resources.stream().map(Resource::id).toList());resources.forEach(r->{if(Set.of("hp","tempHp").contains(r.id()))throw invalid();text(r.name(),100,true);one(r.kind(),"HIT_DICE","SPELL_SLOT","PACT_SLOT","FEATURE","CUSTOM");number(r.max(),0,100000);number(r.current(),0,r.max());text(r.recovery(),1000,false);source(r.source());});
        list(equipment,300);unique(equipment.stream().map(Item::id).toList());equipment.forEach(i->{text(i.name(),100,true);number(i.quantity(),0,100000);text(i.description(),5000,false);source(i.source());});
        for(int n:List.of(money.cp(),money.sp(),money.ep(),money.gp(),money.pp()))number(n,0,100000000);
        list(conditions,100);unique(conditions.stream().map(Condition::id).toList());conditions.forEach(c->{text(c.name(),100,true);text(c.icon(),16,false);text(c.description(),5000,false);source(c.source());if(c.remainingRounds()!=null)number(c.remainingRounds(),0,100000);});
        list(modifiers,100);modifiers.forEach(m->{text(m.target(),100,true);number(m.amount(),-1000,1000);source(m.source());});
        list(overrides,100);unique(overrides.stream().map(Override::target).toList());overrides.forEach(o->{number(o.value(),-10000,10000);text(o.reason(),1000,true);});
        var keys=calculate().keySet();if(modifiers.stream().anyMatch(m->!keys.contains(m.target()))||overrides.stream().anyMatch(o->!keys.contains(o.target())))throw invalid();
    }
    private void checks(List<Check> checks,boolean saves) {
        list(checks,18);Set<String> expected=saves?new HashSet<>(ABILITIES):SKILLS.keySet();
        if(!new HashSet<>(checks.stream().map(Check::id).toList()).equals(expected)||checks.size()!=expected.size())throw invalid();
        checks.forEach(c->{text(c.name(),100,true);ability(c.ability());if(saves&&!c.ability().equals(c.id()))throw invalid();one(c.proficiency(),saves?new String[]{"NONE","PROFICIENT"}:new String[]{"NONE","PROFICIENT","EXPERT"});number(c.bonus(),-1000,1000);source(c.source());});
    }
    private static void entry(Entry e){if(e==null)throw invalid();text(e.id(),80,true);text(e.name(),100,false);text(e.description(),5000,false);source(e.source());}
    private static void source(Source s){if(s==null)throw invalid();text(s.book(),200,true);text(s.revision(),100,true);one(s.rules(),"5R","2014","CUSTOM");text(s.detail(),1000,false);}
    private static void ability(String a){if(!ABILITIES.contains(a))throw invalid();}
    public static void text(String s,int max,boolean required){
        if(s==null||(required&&s.isBlank())||s.indexOf(0)>=0||s.codePointCount(0,s.length())>max)throw invalid();
        for(int i=0;i<s.length();i++){char c=s.charAt(i);if(Character.isHighSurrogate(c)){if(i+1>=s.length()||!Character.isLowSurrogate(s.charAt(++i)))throw invalid();}else if(Character.isLowSurrogate(c))throw invalid();}
    }
    public static void number(long n,long min,long max){if(n<min||n>max)throw invalid();}
    private static void one(String v,String... options){if(v==null||!Arrays.asList(options).contains(v))throw invalid();}
    private static void list(List<?> v,int max){if(v==null||v.size()>max||v.stream().anyMatch(Objects::isNull))throw invalid();}
    private static void strings(List<String> v,int max,int length){list(v,max);v.forEach(s->text(s,length,false));}
    private static void unique(List<String> ids){ids.forEach(s->text(s,100,true));if(new HashSet<>(ids).size()!=ids.size())throw invalid();}
    public static DomainException invalid(){return new DomainException("INVALID_INPUT",400,"Invalid character data");}
}
