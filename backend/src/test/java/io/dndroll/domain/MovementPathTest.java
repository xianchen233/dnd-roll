package io.dndroll.domain;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.dndroll.domain.MovementPath.*;

class MovementPathTest {
    Step s(int x,int y,String mode,boolean difficult){return new Step(x,y,mode,difficult);}
    @Test void loopsAndDiagonalsCountEveryEnteredSquareRatherThanEndpointDistance(){
        assertEquals(15,cost(1,1,20,20,List.of(s(2,2,"WALK",false),s(2,1,"WALK",false),s(1,1,"WALK",false)),false,0));
    }
    @Test void mixedModesAndDifficultTerrainAddCostsRatherThanMultiply(){
        assertEquals(50,cost(0,0,20,20,List.of(s(1,0,"WALK",true),s(2,0,"CRAWL",true),s(3,0,"CLIMB",true),s(4,0,"SWIM",false)),false,0));
    }
    @Test void cannotSkipCellsOrStandStillInAnOrdinaryPath(){
        assertThrows(DomainException.class,()->cost(0,0,20,20,List.of(s(2,0,"WALK",false)),false,0));
        assertThrows(DomainException.class,()->cost(0,0,20,20,List.of(s(0,0,"WALK",false)),false,0));
    }
    @Test void explicitManualCostPermitsRepositionButStillChecksBounds(){
        assertEquals(0,cost(0,0,20,20,List.of(s(19,19,"MANUAL",false)),true,0));
        assertThrows(DomainException.class,()->cost(0,0,20,20,List.of(s(20,19,"MANUAL",false)),true,0));
    }
    @Test void rejectsUnknownAndUnsupportedModesWithoutPartialCost(){
        for(String mode:List.of("FLY","BURROW","walk","UNKNOWN"))assertThrows(DomainException.class,()->cost(0,0,20,20,List.of(s(1,0,"WALK",false),s(2,0,mode,false)),false,0));
        assertThrows(DomainException.class,()->cost(0,0,20,20,List.of(s(1,0,"MANUAL",false)),false,0));
    }
    @Test void boundedPathAndCostAreRequired(){
        assertThrows(DomainException.class,()->cost(0,0,20,20,List.of(),false,0));
        assertThrows(DomainException.class,()->cost(0,0,20,20,Collections.nCopies(201,s(1,0,"WALK",false)),true,1));
        assertThrows(DomainException.class,()->cost(0,0,20,20,List.of(s(1,0,"WALK",false)),true,-1));
        assertThrows(DomainException.class,()->cost(0,0,20,20,List.of(s(1,0,"WALK",false)),false,5));
    }
}
