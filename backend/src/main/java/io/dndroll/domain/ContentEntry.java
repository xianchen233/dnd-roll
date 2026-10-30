package io.dndroll.domain;

import io.dndroll.domain.CharacterSheet.*;
import java.util.*;

/** Immutable catalog data. Fill never executes rules or confers proficiency. */
public record ContentEntry(UUID id, Data data, boolean builtin, String license, String attribution, String changes) {
    public record Data(String name,List<String> aliases,String type,Source source,String description,int spellLevel,String dice,String damageType,String rights) {
        public void validate(){
            CharacterSheet.text(name,100,true);CharacterSheet.text(description,5000,false);CharacterSheet.text(rights,1000,true);
            if(aliases==null||aliases.size()>20)throw CharacterSheet.invalid();aliases.forEach(a->CharacterSheet.text(a,100,true));
            if(!TYPES.contains(type)||source==null||!Set.of("5R","2014","CUSTOM").contains(source.rules()))throw CharacterSheet.invalid();
            CharacterSheet.text(source.book(),200,true);CharacterSheet.text(source.revision(),100,true);CharacterSheet.text(source.detail(),500,false);
            CharacterSheet.number(spellLevel,0,9);CharacterSheet.text(dice,100,false);CharacterSheet.text(damageType,100,false);
        }
    }
    public static final Set<String> TYPES=Set.of("SPECIES","BACKGROUND","FEATURE","SPELL","ITEM","ATTACK","RULE");
    public CharacterSheet fill(CharacterSheet s,String ability,String casting){
        if(!CharacterSheet.ABILITIES.contains(ability)||casting==null)throw CharacterSheet.invalid();
        var d=data;String key="content:"+id;
        var source=new Source(d.source().book(),d.source().revision(),d.source().rules(),d.source().detail()+"; entry="+id+"; "+license+(builtin?"; "+attribution+"; "+changes:""));
        var species=s.species();var background=s.background();var attacks=new ArrayList<>(s.attacks());var spells=new ArrayList<>(s.spells());var features=new ArrayList<>(s.features());var items=new ArrayList<>(s.equipment());
        if(!d.type().equals("SPELL")&&!casting.isEmpty())throw CharacterSheet.invalid();
        switch(d.type()){
            case "SPECIES" -> species=new Entry(key,d.name(),d.description(),source);
            case "BACKGROUND" -> background=new Background(new Entry(key,d.name(),d.description(),source),background.increases(),background.originFeat(),background.proficiencies(),background.equipment());
            case "ATTACK" -> attacks.add(new Attack(key,d.name(),ability,false,0,0,d.dice(),d.damageType(),"",source));
            case "SPELL" -> {if(s.casting().stream().noneMatch(c->c.id().equals(casting)))throw CharacterSheet.invalid();spells.add(new Spell(key,d.name(),d.spellLevel(),false,casting,d.description(),source));}
            case "ITEM" -> items.add(new Item(key,d.name(),1,false,d.description(),source));
            default -> features.add(new Feature(key,d.name(),d.type(),"",d.description(),source));
        }
        var out=new CharacterSheet(s.name(),s.avatar(),species,background,s.classes(),s.abilities(),s.skills(),s.saves(),s.combat(),attacks,s.casting(),spells,features,s.resources(),items,s.money(),s.conditions(),s.modifiers(),s.overrides(),s.notes());out.validate();return out;
    }
}
