package io.dndroll.web;
import io.dndroll.application.*;
import io.dndroll.application.StabilizationHealingService.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
@RestController
@RequestMapping("/api/campaigns/{campaignId}/characters/{id}/stabilization")
public class StabilizationHealingController {
    private final StabilizationHealingService service;private final JsonMapper json;
    public StabilizationHealingController(StabilizationHealingService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {@Bean StabilizationHealingService stabilizationHealingService(Store store,StabilizationService.Store stabilization,CharacterService.Store cards,DiceService dice,SceneEvents events,Clock clock,JsonMapper json){return new StabilizationHealingService(store,stabilization,cards,dice,events::diceChanged,clock,json);}}
    private <T>T parse(JsonNode node,Class<T> type){if(node==null||node.toString().length()>4096)throw CharacterSheet.invalid();CharacterController.shape(node,type);return json.treeToValue(node,type);}
    @GetMapping("/healing-history") List<Entry> history(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor){return service.history(campaignId,id,actor.getName());}
    @PostMapping("/healing-preview") Plan preview(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor,@RequestBody JsonNode body){return service.preview(campaignId,id,actor.getName(),parse(body,Review.class));}
    @PostMapping("/healings") CharacterService.Ack apply(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor,@RequestBody JsonNode body){return service.apply(campaignId,id,actor.getName(),parse(body,Apply.class));}
}
