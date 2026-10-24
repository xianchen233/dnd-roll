package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.DiceService.*;
import io.dndroll.domain.*;
import java.security.*;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}")
public class DiceController {
    private final DiceService service;private final JsonMapper json;
    public DiceController(DiceService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {
        @Bean Dice.Entropy diceEntropy(){var random=new SecureRandom();return sides->random.nextInt(sides)+1;}
        @Bean DiceService diceService(Store store,CharacterService.Store cards,Dice.Entropy entropy,SceneEvents events,Clock clock,JsonMapper json){return new DiceService(store,cards,entropy,events::diceChanged,clock,json);}
    }
    @GetMapping("/rolls") Listing list(@PathVariable UUID campaignId,Principal p,@RequestParam(required=false) UUID before){return service.list(campaignId,p.getName(),before);}
    @GetMapping("/rolls/{id}") Roll detail(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.detail(campaignId,id,p.getName());}
    @PostMapping("/rolls") Roll roll(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode body){return service.roll(campaignId,p.getName(),parse(body,Command.class));}
    @PostMapping("/roll-commands") Roll text(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode body){return service.text(campaignId,p.getName(),parse(body,TextCommand.class));}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>32768)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
