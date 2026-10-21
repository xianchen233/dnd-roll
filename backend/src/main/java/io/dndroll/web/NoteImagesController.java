package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/note-attachments")
public class NoteImagesController {
    public record Upload(UUID requestId,long expectedVersion,String visibility) {}
    private final NoteImages images;private final JsonMapper json;
    public NoteImagesController(NoteImages images,JsonMapper json){this.images=images;this.json=json;}
    @Configuration static class Wiring {
        @Bean NoteImages noteImages(NoteImages.Store store,CharacterService.Store campaigns,AssetService.Files files,JsonMapper json){return new NoteImages(store,campaigns,files,json);}
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    AssetService.Asset upload(@PathVariable UUID campaignId,Principal p,@RequestPart("command") JsonNode n,@RequestPart("file") MultipartFile file,HttpServletRequest request)throws Exception{
        var parts=request.getParts();if(parts.size()!=2||parts.stream().filter(part->part.getName().equals("command")).count()!=1||parts.stream().filter(part->part.getName().equals("file")).count()!=1||n==null||n.toString().length()>1024)throw CharacterSheet.invalid();CharacterController.shape(n,Upload.class);var cmd=json.treeToValue(n,Upload.class);return images.upload(campaignId,p.getName(),cmd.requestId(),cmd.expectedVersion(),cmd.visibility(),file.getBytes(),file.getContentType());
    }
    @GetMapping("/{id}/content") ResponseEntity<byte[]> content(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){var content=images.content(campaignId,id,p.getName());return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment").contentType(MediaType.parseMediaType(content.mediaType())).body(content.bytes());}
}
