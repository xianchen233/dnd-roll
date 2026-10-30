package io.dndroll.web;
import io.dndroll.application.*;
import io.dndroll.application.RestabilizationDamageService.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
@RestController
@RequestMapping("/api/campaigns/{campaignId}/characters/{id}/stabilization/restabilization")
public class RestabilizationDamageController {
    private final RestabilizationDamageService service;private final JsonMapper json;
    public RestabilizationDamageController(RestabilizationDamageService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {@Bean RestabilizationDamageService restabilizationDamageService(Store store,RestabilizationService.Store restabilization,StabilizationService.Store stabilization,StabilizationDamageService.Store damage,CharacterService.Store cards,DiceService dice,SceneEvents events,Clock clock,JsonMapper json){return new RestabilizationDamageService(store,restabilization,stabilization,damage,cards,dice,events::diceChanged,clock,json);}}
    private <T>T parse(JsonNode node,Class<T> type){if(node==null||node.toString().length()>4096)throw CharacterSheet.invalid();CharacterController.shape(node,type);return json.treeToValue(node,type);}
    @GetMapping("/damage-history") List<Entry> history(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor){return service.history(campaignId,id,actor.getName());}
    @PostMapping("/damage-preview") Plan preview(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor,@RequestBody JsonNode body){return service.preview(campaignId,id,actor.getName(),parse(body,Review.class));}
    @PostMapping("/damages") CharacterService.Ack apply(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor,@RequestBody JsonNode body){return service.apply(campaignId,id,actor.getName(),parse(body,Apply.class));}
}
