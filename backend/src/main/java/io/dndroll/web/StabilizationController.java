package io.dndroll.web;
import io.dndroll.application.*;
import io.dndroll.application.StabilizationService.*;
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
public class StabilizationController {
    private final StabilizationService service;private final JsonMapper json;
    public StabilizationController(StabilizationService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {@Bean StabilizationService stabilizationService(Store store,CharacterService.Store cards,DiceService dice,SceneEvents events,Clock clock,JsonMapper json){return new StabilizationService(store,cards,dice,events::diceChanged,clock,json);}}
    private <T>T parse(JsonNode node,Class<T> type){if(node==null||node.toString().length()>4096)throw CharacterSheet.invalid();CharacterController.shape(node,type);return json.treeToValue(node,type);}
    @GetMapping State state(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor){return service.state(campaignId,id,actor.getName());}
    @GetMapping("/history") List<State> history(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor){return service.history(campaignId,id,actor.getName());}
    @PostMapping("/rolls") CharacterService.Ack roll(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor,@RequestBody JsonNode body){return service.roll(campaignId,id,actor.getName(),parse(body,RollInput.class));}
    @PostMapping("/preview") Plan preview(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor,@RequestBody JsonNode body){return service.preview(campaignId,id,actor.getName(),parse(body,Review.class));}
    @PostMapping("/applications") CharacterService.Ack apply(@PathVariable UUID campaignId,@PathVariable UUID id,Principal actor,@RequestBody JsonNode body){return service.apply(campaignId,id,actor.getName(),parse(body,Apply.class));}
}
