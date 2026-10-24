package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.modules.*;
import io.dndroll.modules.ModuleOperations.*;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/module-operations")
public class ModuleOperationsController {
    private final ModuleOperations service;
    public ModuleOperationsController(ModuleOperations service){this.service=service;}
    @Configuration static class Wiring {
        @Bean ModuleOperations moduleOperations(Store store,CharacterService.Store campaigns,ModuleLifecycle lifecycle,TokenService tokens,CharacterService characters,NotesService notes,InitiativeService initiative,TurnService turns,ContentService content,JsonMapper json,Clock clock){return new ModuleOperations(store,campaigns,lifecycle,tokens,characters,notes,initiative,turns,content,json,clock);}
    }
    @GetMapping List<Definition> list(@PathVariable UUID campaignId,Principal p){return service.list(campaignId,p.getName());}
    @PostMapping("/{id}") Definition save(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode body){return service.save(campaignId,id,p.getName(),ModuleOperations.parseSave(body));}
    @PostMapping("/{id}/impact") Impact impact(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode body){return service.impact(campaignId,id,p.getName(),ModuleBlueprint.command(body,Review.class));}
    @PostMapping("/{id}/executions") Execution execute(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode body){return service.execute(campaignId,id,p.getName(),ModuleBlueprint.command(body,Execute.class));}
    @GetMapping("/{id}/executions") List<Execution> executions(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.executions(campaignId,id,p.getName());}
}
