package io.dndroll.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DiceTest {
    Dice.Entropy sequence(int... values){var q=new ArrayDeque<Integer>();for(int v:values)q.add(v);return sides->{int v=q.remove();assertTrue(v>=1&&v<=sides);return v;};}
    @Test void signedSumsPreserveEveryDieAndConstants(){
        var r=Dice.roll(" 2D6 + 3 - d4 ",List.of(),List.of(),sequence(2,6,4));
        assertEquals("2d6+3-1d4",r.expression());assertEquals(7,r.total());assertEquals(3,r.constant());
        assertEquals(List.of(new Dice.Die(6,2,1,true),new Dice.Die(6,6,1,true),new Dice.Die(4,4,-1,true)),r.dice());
    }
    @Test void advantageAndSurpriseDisadvantageKeepOnlyOneAndNeverStack(){
        var a=Dice.roll("d20+4",List.of("DM ruling","Feature"),List.of(),sequence(3,18));
        assertEquals(22,a.total());assertEquals("ADVANTAGE",a.mode());assertFalse(a.dice().getFirst().kept());
        var d=Dice.roll("d20-2",List.of(),List.of("Surprise"),sequence(18,3));
        assertEquals(1,d.total());assertFalse(d.dice().getFirst().kept());
        var cancel=Dice.roll("d20",List.of("Feature"),List.of("Surprise","Other"),sequence(8));assertEquals(8,cancel.total());assertEquals(1,cancel.dice().size());assertEquals("NORMAL",cancel.mode());
    }
    @Test void tiesKeepFirstDieAndConstantsNeedNoEntropy(){
        var r=Dice.roll("1d20",List.of("Advantage"),List.of(),sequence(10,10));assertTrue(r.dice().getFirst().kept());assertFalse(r.dice().getLast().kept());
        assertEquals(-3,Dice.roll("-3",List.of(),List.of(),sequence()).total());
    }
    @Test void boundsAndUnsupportedSyntaxFailBeforeDrawingEntropy(){
        for(String bad:List.of("","0d6","101d6","d1","d1001","1d6*2","1d20kh1","1d6+-2","10001","1d6+","-101d6","1d6+".repeat(21)+"1","1\n+2")){
            assertThrows(DomainException.class,()->Dice.roll(bad,List.of(),List.of(),s->{fail("Drew for invalid expression "+bad);return 1;}),bad);
        }
        assertThrows(DomainException.class,()->Dice.roll("2d20",List.of("adv"),List.of(),sequence()));
        assertThrows(DomainException.class,()->Dice.roll("d6",List.of("adv"),List.of(),sequence()));
        assertThrows(DomainException.class,()->Dice.roll("-d20",List.of("adv"),List.of(),sequence()));
        assertThrows(DomainException.class,()->Dice.roll("d20",List.of(""),List.of(),sequence()));
    }
}
