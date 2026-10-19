package io.dndroll.web;

import io.dndroll.application.MapVisibilityService;
import io.dndroll.application.MapVisibilityService.*;
import io.dndroll.application.SceneService;
import io.dndroll.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campaigns/{campaignId}")
public class MapVisibilityController {
    private final MapVisibilityService service;
    public MapVisibilityController(MapVisibilityService service){this.service=service;}
    @Configuration static class Wiring {
        @Bean MapVisibilityService mapVisibilityService(Store store,SceneService.Events events){return new MapVisibilityService(store,events);}
    }
    public record FogRequest(UUID requestId,Long expectedVersion,List<Cell> revealed) {}
    @GetMapping("/scene/map-view") View view(@PathVariable UUID campaignId,Principal actor){return service.view(campaignId,actor.getName());}
    @GetMapping("/scene/map-image") ResponseEntity<byte[]> image(@PathVariable UUID campaignId,HttpServletRequest request,Principal actor){
        service.authorizeImageRequest(campaignId,actor.getName());
        var params=request.getParameterMap();
        if(!params.keySet().equals(Set.of("sceneId","version"))||params.values().stream().anyMatch(a->a.length!=1))throw invalid();
        UUID scene;long version;
        try {String raw=request.getParameter("sceneId");scene=UUID.fromString(raw);if(!scene.toString().equalsIgnoreCase(raw))throw invalid();String value=request.getParameter("version");if(!value.matches("[0-9]+"))throw invalid();version=Long.parseLong(value);}
        catch(IllegalArgumentException failure){throw invalid();}
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(service.image(campaignId,actor.getName(),scene,version));
    }
    @GetMapping("/scenes/{sceneId}/fog") Fog fog(@PathVariable UUID campaignId,@PathVariable UUID sceneId,Principal actor){return service.fog(campaignId,sceneId,actor.getName());}
    @PostMapping("/scenes/{sceneId}/fog") Fog configure(@PathVariable UUID campaignId,@PathVariable UUID sceneId,@RequestBody FogRequest request,Principal actor){
        if(request.expectedVersion()==null)throw invalid();return service.configure(campaignId,sceneId,actor.getName(),request.requestId(),request.expectedVersion(),request.revealed());
    }
    private static DomainException invalid(){return new DomainException("INVALID_INPUT",400,"Invalid map visibility request");}
}
