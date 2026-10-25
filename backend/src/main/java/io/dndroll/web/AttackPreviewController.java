package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.AttackPreviewService.*;
import io.dndroll.domain.*;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}")
public class AttackPreviewController {
    private final AttackPreviewService service;private final JsonMapper json;
    public AttackPreviewController(AttackPreviewService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {
        @Bean AttackPreviewService attackPreviewService(Store store,CharacterService.Store cards,DiceService dice,CombatService combat,SceneEvents events,Clock clock,JsonMapper json)throws java.io.IOException{
            try(var in=new ClassPathResource("content/srd-selection.json").getInputStream()){return new AttackPreviewService(store,cards,dice,combat,Arrays.asList(json.readValue(in,ContentEntry[].class)),events::diceChanged,clock,json);}
        }
    }
    @GetMapping("/attack-previews") List<Preview> list(@PathVariable UUID campaignId,Principal p){return service.list(campaignId,p.getName());}
    @GetMapping("/attack-previews/{id}") Preview detail(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.detail(campaignId,id,p.getName());}
    @PostMapping("/attack-previews") CombatService.Ack create(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){return service.create(campaignId,p.getName(),parse(n,Create.class));}
    @PostMapping("/attack-previews/{id}/events") CombatService.Ack command(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.command(campaignId,id,p.getName(),parse(n,Command.class));}
    @GetMapping("/attack-results") List<PublicResult> results(@PathVariable UUID campaignId,Principal p){return service.results(campaignId,p.getName());}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
