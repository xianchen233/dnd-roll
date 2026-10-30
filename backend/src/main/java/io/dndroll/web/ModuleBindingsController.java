package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.modules.*;
import io.dndroll.modules.ModuleBindings.*;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/module-operation-bindings")
public class ModuleBindingsController {
    private final ModuleBindings service;public ModuleBindingsController(ModuleBindings service){this.service=service;}
    @Configuration static class Wiring {@Bean ModuleBindings moduleBindings(Store store,CharacterService.Store campaigns,ModuleLifecycle lifecycle,ModuleOperations operations,ModuleOperations.Store definitions,CharacterService characters,TokenService tokens,NotesService notes,InitiativeService initiative,TurnService turns,JsonMapper json,Clock clock,SceneService.Events events){return new ModuleBindings(store,campaigns,lifecycle,operations,definitions,characters,tokens,notes,initiative,turns,json,clock,events);}}
    @GetMapping Listing list(@PathVariable UUID campaignId,Principal p){return service.list(campaignId,p.getName());}
    @PostMapping("/{nodeId}/preview") Preview preview(@PathVariable UUID campaignId,@PathVariable UUID nodeId,Principal p,@RequestBody JsonNode body){return service.preview(campaignId,nodeId,p.getName(),ModuleBlueprint.command(body,Review.class));}
    @PostMapping("/{nodeId}/bind") Ack bind(@PathVariable UUID campaignId,@PathVariable UUID nodeId,Principal p,@RequestBody JsonNode body){return service.bind(campaignId,nodeId,p.getName(),ModuleBlueprint.command(body,Bind.class));}
    @GetMapping("/{nodeId}/history") List<Evidence> history(@PathVariable UUID campaignId,@PathVariable UUID nodeId,Principal p){return service.history(campaignId,nodeId,p.getName());}
}
