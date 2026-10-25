package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.MovementService.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/tokens/{tokenId}/movement")
public class MovementController {
    private final MovementService service;private final JsonMapper json;
    public MovementController(MovementService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {@Bean MovementService movementService(Store store,CharacterService.Store cards,TurnService.Store turns,SceneService scenes,SceneService.MovementPermission permission,SceneService.Events events,Clock clock,JsonMapper json){return new MovementService(store,cards,turns,scenes,permission,events,clock,json);}}
    @GetMapping Object view(@PathVariable UUID campaignId,@PathVariable UUID tokenId,Principal p){return service.view(campaignId,tokenId,p.getName());}
    @PostMapping("/preview") Preview preview(@PathVariable UUID campaignId,@PathVariable UUID tokenId,Principal p,@RequestBody JsonNode n){return service.preview(campaignId,tokenId,p.getName(),parse(n,Path.class));}
    @PostMapping("/paths") Result move(@PathVariable UUID campaignId,@PathVariable UUID tokenId,Principal p,@RequestBody JsonNode n){return service.move(campaignId,tokenId,p.getName(),parse(n,Path.class));}
    @PostMapping("/adjustments") Result adjust(@PathVariable UUID campaignId,@PathVariable UUID tokenId,Principal p,@RequestBody JsonNode n){return service.adjust(campaignId,tokenId,p.getName(),parse(n,Adjustment.class));}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>32768)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
