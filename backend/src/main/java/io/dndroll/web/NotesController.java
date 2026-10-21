package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.NotesService.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/notes")
public class NotesController {
    private final NotesService service;private final JsonMapper json;
    public NotesController(NotesService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {
        @Bean NotesService notesService(Store store,CharacterService.Store campaigns,SceneEvents events,Clock clock,JsonMapper json,NoteImages images){return new NotesService(store,campaigns,events::diceChanged,clock,json,images);}
    }
    private Filter filter(Map<String,String> q){return new Filter(q.getOrDefault("q",""),q.getOrDefault("visibility",""));}
    @GetMapping Listing list(@PathVariable UUID campaignId,Principal p,@RequestParam(required=false) UUID before,@RequestParam Map<String,String> query){return service.list(campaignId,p.getName(),before,filter(query));}
    @PostMapping Ack create(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){return service.create(campaignId,p.getName(),parse(n,Create.class));}
    @GetMapping("/{id}") View detail(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.detail(campaignId,id,p.getName());}
    @PostMapping("/{id}") Ack edit(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.edit(campaignId,id,p.getName(),parse(n,Edit.class));}
    @GetMapping("/{id}/history") List<Revision> history(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.history(campaignId,id,p.getName());}
    @GetMapping("/export") ResponseEntity<?> export(@PathVariable UUID campaignId,Principal p,@RequestParam Map<String,String> query){var format=query.getOrDefault("format","markdown");if(!Set.of("json","markdown").contains(format))throw CharacterSheet.invalid();var data=service.export(campaignId,p.getName(),filter(query));return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"notes."+(format.equals("json")?"json":"md")+"\"").contentType(MediaType.parseMediaType(format.equals("json")?"application/json":"text/markdown;charset=UTF-8")).body(format.equals("json")?data:service.markdown(data));}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>131072)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
