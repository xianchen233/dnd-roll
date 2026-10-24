package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.TurnService.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/combat")
public class TurnController {
    private final TurnService service;private final JsonMapper json;
    public TurnController(TurnService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {@Bean TurnService turnService(Store store,CharacterService.Store cards,InitiativeService.Store initiative,CombatService.Store receipts,SceneEvents events,Clock clock,JsonMapper json){return new TurnService(store,cards,initiative,receipts,events::diceChanged,clock,json);}}
    @GetMapping Object view(@PathVariable UUID campaignId,Principal p){return service.view(campaignId,p.getName());}
    @GetMapping("/history") List<History> history(@PathVariable UUID campaignId,Principal p){return service.history(campaignId,p.getName());}
    @PostMapping CombatService.Ack command(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){if(n==null||n.toString().length()>32768)throw CharacterSheet.invalid();CharacterController.shape(n,Command.class);return service.command(campaignId,p.getName(),json.treeToValue(n,Command.class));}
}
