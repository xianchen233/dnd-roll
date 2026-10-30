package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.InitiativeService.*;
import io.dndroll.domain.*;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/initiative")
public class InitiativeController {
    private final InitiativeService service;private final JsonMapper json;
    public InitiativeController(InitiativeService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {@Bean InitiativeService initiativeService(Store store,CharacterService.Store cards,Dice.Entropy entropy,SceneEvents events,Clock clock,JsonMapper json){return new InitiativeService(store,cards,entropy,events::diceChanged,clock,json);}}
    @GetMapping Object view(@PathVariable UUID campaignId,Principal p){return service.view(campaignId,p.getName());}
    @GetMapping("/history") List<History> history(@PathVariable UUID campaignId,Principal p){return service.history(campaignId,p.getName());}
    @PostMapping Ack generate(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){return service.generate(campaignId,p.getName(),parse(n,Generate.class));}
    @PostMapping("/confirm") Ack confirm(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){return service.confirm(campaignId,p.getName(),parse(n,Confirm.class));}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>131072)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
