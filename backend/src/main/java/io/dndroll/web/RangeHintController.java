package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/range-hints")
public class RangeHintController {
    private final RangeHintService service;
    private final JsonMapper json;
    public RangeHintController(RangeHintService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {
        @Bean RangeHintService rangeHintService(SceneService.Store scenes,MovementService.Store movement){return new RangeHintService(scenes,movement::fiveFoot);}
    }
    @PostMapping("/preview") RangeHintService.Result preview(@PathVariable UUID campaignId,Principal actor,@RequestBody JsonNode body){
        if(body==null||body.toString().length()>4096)throw CharacterSheet.invalid();
        CharacterController.shape(body,RangeHintService.Request.class);
        return service.preview(campaignId,actor.getName(),json.treeToValue(body,RangeHintService.Request.class));
    }
}
