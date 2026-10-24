package io.dndroll.web;

import io.dndroll.application.AssetService;
import io.dndroll.application.AssetService.*;
import io.dndroll.application.SceneService;
import io.dndroll.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}")
public class AssetController {
    private final AssetService service;
    private final JsonMapper json;
    public AssetController(AssetService service, JsonMapper json) { this.service = service; this.json = json; }
    @Configuration static class Wiring {
        @Bean AssetService assetService(Store store, Files files, SceneService.Events events) { return new AssetService(store, files, events); }
    }
    public record UploadRequest(UUID requestId, Long expectedVersion) {}
    public record MapRequest(UUID requestId, Long expectedVersion, JsonNode map) {}
    @GetMapping("/assets") Listing list(@PathVariable UUID campaignId, Principal actor) { return service.list(campaignId, actor.getName()); }
    @PostMapping(value="/assets", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    UploadResult upload(@PathVariable UUID campaignId, @RequestPart("command") UploadRequest command,
        @RequestPart("file") MultipartFile file, HttpServletRequest request, Principal actor) throws Exception {
        var parts = request.getParts();
        if (parts.size() != 2 || parts.stream().filter(p -> p.getName().equals("command")).count() != 1
            || parts.stream().filter(p -> p.getName().equals("file")).count() != 1) throw invalid();
        return service.upload(campaignId, actor.getName(), command.requestId(), version(command.expectedVersion()), file.getBytes(), file.getContentType());
    }
    @GetMapping("/assets/{assetId}/content") ResponseEntity<byte[]> content(@PathVariable UUID campaignId, @PathVariable UUID assetId, Principal actor) {
        var content = service.content(campaignId, assetId, actor.getName());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, content.mediaType())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment").body(content.bytes());
    }
    @GetMapping("/scenes/{sceneId}/map") MapResult map(@PathVariable UUID campaignId, @PathVariable UUID sceneId, Principal actor) { return service.map(campaignId, sceneId, actor.getName()); }
    @PostMapping("/scenes/{sceneId}/map") MapResult configure(@PathVariable UUID campaignId, @PathVariable UUID sceneId,
        @RequestBody MapRequest request, Principal actor) {
        if (request.map() == null) throw invalid();
        MapConfig map = request.map().isNull() ? null : json.treeToValue(request.map(), MapConfig.class);
        return service.configure(campaignId, sceneId, actor.getName(), request.requestId(), version(request.expectedVersion()), map);
    }
    private static long version(Long value) { if (value == null) throw invalid(); return value; }
    private static DomainException invalid() { return new DomainException("INVALID_INPUT", 400, "Invalid asset request"); }
}
