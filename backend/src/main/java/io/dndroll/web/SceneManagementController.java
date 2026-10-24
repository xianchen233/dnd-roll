package io.dndroll.web;

import io.dndroll.application.SceneManagementService;
import io.dndroll.application.SceneManagementService.*;
import io.dndroll.application.SceneService;
import io.dndroll.domain.DomainException;
import java.security.Principal;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/scenes")
public class SceneManagementController {
    private final SceneManagementService service;
    public SceneManagementController(SceneManagementService service) { this.service = service; }
    @Configuration static class Wiring {
        @Bean SceneManagementService sceneManagementService(Store store, SceneService.Events events) { return new SceneManagementService(store, events); }
    }
    public record CreateRequest(UUID requestId, Long expectedVersion, String name) {}
    public record ActivateRequest(UUID requestId, Long expectedVersion) {}
    private static long version(Long version) {
        if (version == null) throw new DomainException("INVALID_INPUT", 400, "All scene command fields are required");
        return version;
    }
    @GetMapping Listing list(@PathVariable UUID campaignId, Principal actor) { return service.list(campaignId, actor.getName()); }
    @PostMapping Result create(@PathVariable UUID campaignId, @RequestBody CreateRequest request, Principal actor) {
        return service.create(campaignId, actor.getName(), request.requestId(), version(request.expectedVersion()), request.name());
    }
    @PostMapping("/{sceneId}/activate") Result activate(@PathVariable UUID campaignId, @PathVariable UUID sceneId, @RequestBody ActivateRequest request, Principal actor) {
        return service.activate(campaignId, sceneId, actor.getName(), request.requestId(), version(request.expectedVersion()));
    }
}
