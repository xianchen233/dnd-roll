package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.ContentService.*;
import io.dndroll.domain.*;
import java.security.Principal;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}")
public class ContentController {
    private final ContentService service;private final JsonMapper json;
    public ContentController(ContentService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {
        @Bean ContentService contentService(Store store,CharacterService.Store cards,CharacterService characters,SceneService.Events events,JsonMapper json)throws java.io.IOException{
            try(var input=new ClassPathResource("content/srd-selection.json").getInputStream()){
                return new ContentService(store,cards,characters,events,json,Arrays.asList(json.readValue(input,ContentEntry[].class)));
            }
        }
    }
    public record Custom(UUID requestId,long expectedVersion,ContentEntry.Data entry) {}
    public record Settings(UUID requestId,long expectedVersion,String mode,List<UUID> approvedIds) {}
    public record Inspect(long expectedVersion,UUID entryId,String ability,String castingId) {}
    public record Fill(UUID requestId,long expectedVersion,long policyVersion,UUID entryId,String ability,String castingId,String reason) {}
    @GetMapping("/content") Listing list(@PathVariable UUID campaignId,Principal p,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String type,@RequestParam(defaultValue="")String source,@RequestParam(defaultValue="")String rules,@RequestParam(defaultValue="")String admitted){return service.list(campaignId,p.getName(),q,type,source,rules,admitted);}
    @PostMapping("/content/custom") Ack custom(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode body){var r=parse(body,Custom.class);return service.publish(campaignId,p.getName(),r.requestId(),r.expectedVersion(),r.entry());}
    @PostMapping("/content/settings") Ack settings(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode body){var r=parse(body,Settings.class);return service.settings(campaignId,p.getName(),r.requestId(),r.expectedVersion(),r.mode(),r.approvedIds());}
    @PostMapping("/characters/{id}/content-preview") Preview preview(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode body){var r=parse(body,Inspect.class);return service.preview(campaignId,id,p.getName(),r.expectedVersion(),r.entryId(),r.ability(),r.castingId());}
    @PostMapping("/characters/{id}/content") CharacterService.Ack fill(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode body){var r=parse(body,Fill.class);return service.fill(campaignId,id,p.getName(),r.requestId(),r.expectedVersion(),r.policyVersion(),r.entryId(),r.ability(),r.castingId(),r.reason());}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>32768)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
