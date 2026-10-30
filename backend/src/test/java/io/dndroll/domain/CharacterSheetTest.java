package io.dndroll.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class CharacterSheetTest {
    final JsonMapper json=JsonMapper.builder().build();
    CharacterSheet sheet(String patches) {
        ObjectNode node=(ObjectNode)json.valueToTree(CharacterSheet.empty("Hero"));
        json.readTree(patches).properties().forEach(e->node.set(e.getKey(),e.getValue()));
        return json.treeToValue(node,CharacterSheet.class);
    }
    Integer value(CharacterSheet s,String key){var v=s.calculate().get(key);return v==null?null:v.value();}
    @Test void oddNegativeAbilityUsesFloorAndLevelUsesAllClasses() {
        var s=sheet("""
{"abilities":{"STR":9,"DEX":15,"CON":12,"INT":18,"WIS":14,"CHA":10},"classes":[{"id":"a","name":"A","subclass":"","level":3,"source":{"book":"x","revision":"1","rules":"5R","detail":""}},{"id":"b","name":"B","subclass":"","level":2,"source":{"book":"x","revision":"1","rules":"5R","detail":""}}]}
""");
        assertEquals(-1,value(s,"ability:STR"));assertEquals(3,value(s,"proficiency"));assertEquals(12,value(s,"ac"));assertEquals(2,value(s,"initiative"));assertEquals(12,value(s,"passivePerception"));
    }
    @Test void checksCastingAttacksAndSourcesAreIndependent() {
        var s=sheet("""
{"abilities":{"STR":16,"DEX":10,"CON":10,"INT":18,"WIS":10,"CHA":10},"attacks":[{"id":"sword","name":"Sword","ability":"STR","proficient":true,"hitBonus":1,"damageBonus":2,"dice":"1d8","damageType":"slashing","mastery":"chosen","source":{"book":"x","revision":"1","rules":"5R","detail":""}}],"casting":[{"id":"wizard","name":"Wizard","ability":"INT","attackBonus":1,"dcBonus":2,"source":{"book":"x","revision":"1","rules":"5R","detail":""}}]}
""");
        assertEquals(6,value(s,"attack:sword"));assertEquals(5,value(s,"damage:sword"));assertEquals(7,value(s,"spellAttack:wizard"));assertEquals(16,value(s,"spellDc:wizard"));
        assertNotNull(s.calculate().get("attack:sword"));
        assertTrue(s.calculate().get("attack:sword").components().stream().allMatch(c->c.source()!=null));
    }
    @Test void modifiersAndOverridesKeepUnmodifiedExplanation() {
        var s=sheet("""
{"modifiers":[{"target":"initiative","amount":3,"source":{"book":"feat","revision":"1","rules":"5R","detail":"chosen"}}],"overrides":[{"target":"initiative","value":9,"reason":"DM ruling"}]}
""");
        assertEquals(9,value(s,"initiative"));assertNotNull(s.calculate().get("initiative"));assertEquals(3,s.calculate().get("initiative").baseValue());assertEquals("DM ruling",s.calculate().get("initiative").overrideReason());
    }
    @Test void invalidAbilityResourceAndFormulaTargetsAreRejected() {
        assertThrows(DomainException.class,()->sheet("""
{"abilities":{"STR":0}}
""").validate());
        assertThrows(DomainException.class,()->sheet("""
{"resources":[{"id":"slot","name":"slot","kind":"SPELL_SLOT","current":3,"max":2,"recovery":"long rest","source":{"book":"x","revision":"1","rules":"5R","detail":""}}]}
""").validate());
        assertThrows(DomainException.class,()->sheet("""
{"overrides":[{"target":"invented","value":2,"reason":"ruling"}]}
""").validate());
    }
}
