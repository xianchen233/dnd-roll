package io.dndroll.domain;

import java.util.List;

/** Explicit declared 5-foot grid profile; not a terrain or collision engine. */
public final class MovementPath {
    private MovementPath() {}
    public record Step(int x,int y,String mode,boolean difficult) {}
    public static int cost(int x,int y,int width,int height,List<Step> steps,boolean override,int manualCost) {
        if(steps==null||steps.isEmpty()||steps.size()>200||manualCost<0||manualCost>100000||!override&&manualCost!=0)throw CharacterSheet.invalid();
        int cost=0;
        for(var step:steps){
            if(step==null||step.mode()==null||!java.util.Set.of("WALK","CRAWL","CLIMB","SWIM","MANUAL").contains(step.mode())||!override&&step.mode().equals("MANUAL")||step.x()<0||step.y()<0||step.x()>=width||step.y()>=height)throw CharacterSheet.invalid();
            if(!override&&Math.max(Math.abs(step.x()-x),Math.abs(step.y()-y))!=1)throw CharacterSheet.invalid();
            cost+=5*(1+(step.mode().equals("WALK")?0:1)+(step.difficult()?1:0));x=step.x();y=step.y();
        }
        return override?manualCost:cost;
    }
}
