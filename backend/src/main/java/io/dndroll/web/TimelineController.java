package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.TimelineService.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.time.Clock;
import org.springframework.http.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}")
public class TimelineController {
    private final TimelineService service;private final JsonMapper json;private final TimelineImages images;
    public TimelineController(TimelineService service,JsonMapper json,TimelineImages images){this.service=service;this.json=json;this.images=images;}
    @Configuration static class Wiring {
        @Bean TimelineImages timelineImages(TimelineImages.Store store,CharacterService.Store cards,AssetService.Files files){return new TimelineImages(store,cards,files);}
        @Bean TimelineService timelineService(Store store,CharacterService.Store cards,SceneEvents events,Clock clock,JsonMapper json,TimelineImages images,DiceService dice,CombatService combat){return new TimelineService(store,cards,events::diceChanged,clock,json,images,dice,combat);}
    }
    private Filter filter(Map<String,String> q){return new Filter(q.getOrDefault("q",""),q.getOrDefault("kind",""),q.getOrDefault("characterId",""),q.getOrDefault("sessionId",""),q.getOrDefault("from",""),q.getOrDefault("to",""));}
    @GetMapping("/timeline") Listing list(@PathVariable UUID campaignId,Principal p,@RequestParam(required=false) UUID before,@RequestParam Map<String,String> query){return service.list(campaignId,p.getName(),before,filter(query));}
    @GetMapping("/timeline/{id}") Entry detail(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.detail(campaignId,id,p.getName());}
    @PostMapping("/messages") Ack send(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){return service.send(campaignId,p.getName(),parse(n,Send.class));}
    @PostMapping("/messages/{id}") Ack edit(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.edit(campaignId,id,p.getName(),parse(n,Edit.class));}
    @GetMapping("/messages/{id}/history") List<Revision> history(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.history(campaignId,id,p.getName());}
    @GetMapping("/timeline/sessions") Sessions sessions(@PathVariable UUID campaignId,Principal p){return service.sessions(campaignId,p.getName());}
    @PostMapping("/timeline/sessions") Ack session(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){return service.session(campaignId,p.getName(),parse(n,SessionCommand.class));}
    @GetMapping("/timeline/export") ResponseEntity<?> export(@PathVariable UUID campaignId,Principal p,@RequestParam Map<String,String> query){String format=query.getOrDefault("format","markdown");if(!Set.of("json","markdown").contains(format))throw CharacterSheet.invalid();var data=service.export(campaignId,p.getName(),filter(query));return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"timeline."+(format.equals("json")?"json":"md")+"\"").contentType(MediaType.parseMediaType(format.equals("json")?"application/json":"text/markdown;charset=UTF-8")).body(format.equals("json")?data:service.markdown(data));}
    public record Upload(UUID requestId,long expectedVersion) {}
    @PostMapping(value="/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    AssetService.Asset upload(@PathVariable UUID campaignId,Principal p,@RequestPart("command") JsonNode n,@RequestPart("file") MultipartFile file,HttpServletRequest request)throws Exception{var parts=request.getParts();if(parts.size()!=2||parts.stream().filter(part->part.getName().equals("command")).count()!=1||parts.stream().filter(part->part.getName().equals("file")).count()!=1)throw CharacterSheet.invalid();var command=parse(n,Upload.class);return images.upload(campaignId,p.getName(),command.requestId(),command.expectedVersion(),file.getBytes(),file.getContentType());}
    @GetMapping("/attachments/{id}/content") ResponseEntity<byte[]> content(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){var content=images.content(campaignId,id,p.getName());return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment").contentType(MediaType.parseMediaType(content.mediaType())).body(content.bytes());}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
