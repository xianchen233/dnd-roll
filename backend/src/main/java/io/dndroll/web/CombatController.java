package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.CombatService.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/actions")
public class CombatController {
    private final CombatService service;private final JsonMapper json;
    public CombatController(CombatService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {@Bean CombatService combatService(Store store,CharacterService.Store cards,DiceService dice,SceneEvents events,Clock clock,JsonMapper json){return new CombatService(store,cards,dice,events::diceChanged,clock,json);}}
    @GetMapping Listing list(@PathVariable UUID campaignId,Principal p,@RequestParam(required=false) UUID before){return service.list(campaignId,p.getName(),before);}
    @GetMapping("/{id}") Object detail(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.detail(campaignId,id,p.getName());}
    @PostMapping Ack declare(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){return service.declare(campaignId,p.getName(),parse(n,Declare.class));}
    @PostMapping("/{id}/events") Ack append(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.append(campaignId,id,p.getName(),parse(n,Command.class));}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
