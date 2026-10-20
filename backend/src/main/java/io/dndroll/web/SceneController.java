package io.dndroll.web;

import io.dndroll.application.SceneService;
import io.dndroll.application.SceneService.*;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.*;
import java.util.*;
import java.security.Principal;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campaigns")
public class SceneController {
    private final SceneService service;
    public SceneController(SceneService service) { this.service = service; }
    @Configuration static class Wiring {
        @Bean SceneService sceneService(Store store, Events events,SceneService.MovementPermission permission) { return new SceneService(store, events,permission); }
    }
    public record MoveRequest(UUID requestId, Long expectedVersion, Integer x, Integer y) {
        Command command() {
            if (requestId == null || expectedVersion == null || x == null || y == null) throw new DomainException("INVALID_INPUT", 400, "All move fields are required");
            return new Command(requestId, expectedVersion, x, y);
        }
    }
    @GetMapping List<CampaignView> campaigns(Principal actor) { return service.campaigns(actor.getName()); }
    @GetMapping("/{campaignId}/scene") Snapshot scene(@PathVariable UUID campaignId, Principal actor) { return service.snapshot(campaignId, actor.getName()); }
    @PostMapping("/{campaignId}/tokens/{tokenId}/moves") TokenView move(@PathVariable UUID campaignId, @PathVariable UUID tokenId, @RequestBody MoveRequest request, Principal actor) {
        return service.move(campaignId, tokenId, actor.getName(), request.command());
    }
}
