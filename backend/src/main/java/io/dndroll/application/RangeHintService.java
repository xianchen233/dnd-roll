package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.*;
import java.util.UUID;
import java.util.function.Predicate;

/** A read-only range aid. It never authorizes an attack or consults other endpoints. */
public final class RangeHintService {
    public record Request(UUID sceneId,long sceneVersion,UUID sourceId,long sourceVersion,
                          UUID targetId,long targetVersion,String profile,boolean reviewed,
                          String mode,int normalFeet,int longFeet) {}
    public record Result(String status,Integer distanceFeet,String band,String reason) {}
    private final SceneService.Store scenes;
    private final Predicate<UUID> fiveFoot;
    public RangeHintService(SceneService.Store scenes,Predicate<UUID> fiveFoot){this.scenes=scenes;this.fiveFoot=fiveFoot;}

    public Result preview(UUID campaign,String actor,Request request){
        return scenes.locked(campaign,()->{
            var role=scenes.role(campaign,actor);
            if(role==null)throw new DomainException("FORBIDDEN",403,"Campaign membership required");
            validate(request);
            var scene=scenes.scene(campaign);
            // Visibility precedes versions, calibration and measurements; foreign IDs are identical.
            var source=visible(scene,request.sourceId(),role,actor);
            var target=visible(scene,request.targetId(),role,actor);
            if(!scene.id().equals(request.sceneId()))throw new DomainException("VERSION_CONFLICT",409,"Scene changed; refresh the scene");
            AccountPolicy.version(scene.version(),request.sceneVersion());
            AccountPolicy.version(source.version(),request.sourceVersion());
            AccountPolicy.version(target.version(),request.targetVersion());
            if(!request.reviewed())return new Result("MANUAL_REQUIRED",null,null,"REVIEW_REQUIRED");
            if(!fiveFoot.test(scene.id()))return new Result("MANUAL_REQUIRED",null,null,"GRID_REQUIRES_MANUAL");
            int feet=5*Math.max(Math.abs(source.x()-target.x()),Math.abs(source.y()-target.y()));
            String band=feet>request.longFeet()?"OUT_OF_RANGE":request.mode().equals("MELEE")?"WITHIN_REACH":feet<=request.normalFeet()?"NORMAL_RANGE":"LONG_RANGE";
            return new Result("CANDIDATE",feet,band,"");
        });
    }
    private static TokenView visible(SceneService.Scene scene,UUID id,Role role,String actor){
        var token=scene.tokens().stream().filter(t->t.id().equals(id)).findFirst().orElse(null);
        var view=token==null?null:Movement.view(token,role,actor);
        if(view==null)throw new DomainException("NOT_FOUND",404,"Token not found");
        return view;
    }
    private static void validate(Request r){
        if(r==null||r.sceneId()==null||r.sourceId()==null||r.targetId()==null||!"GRID_5R_5FT_RANGE_V1".equals(r.profile())||
            !("MELEE".equals(r.mode())||"RANGED".equals(r.mode()))||r.normalFeet()<0||r.longFeet()>100000||r.normalFeet()>r.longFeet()||
            "MELEE".equals(r.mode())&&r.normalFeet()!=r.longFeet())throw CharacterSheet.invalid();
        for(long version:new long[]{r.sceneVersion(),r.sourceVersion(),r.targetVersion()})if(version<0||version>9007199254740991L)throw CharacterSheet.invalid();
    }
}
