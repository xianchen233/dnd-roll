package io.dndroll.web;

import io.dndroll.application.TokenService;
import io.dndroll.application.TokenService.*;
import io.dndroll.application.SceneService;
import io.dndroll.domain.DomainException;
import java.security.Principal;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/scenes/{sceneId}/tokens")
public class TokenController {
    private final TokenService service;
    public TokenController(TokenService service) { this.service = service; }
    @Configuration static class Wiring {
        @Bean TokenService tokenService(Store store, SceneService.Events events) { return new TokenService(store, events); }
    }
    public record Request(UUID requestId, Long expectedVersion, String name, Integer x, Integer y, Boolean hidden, List<String> controllers) {
        Command command() {
            if (requestId == null || expectedVersion == null || name == null || x == null || y == null || hidden == null || controllers == null)
                throw new DomainException("INVALID_INPUT",400,"All token command fields are required");
            return new Command(requestId,expectedVersion,name,x,y,hidden,controllers);
        }
    }
    @GetMapping Listing list(@PathVariable UUID campaignId, @PathVariable UUID sceneId, Principal actor) { return service.list(campaignId,sceneId,actor.getName()); }
    @PostMapping Result create(@PathVariable UUID campaignId, @PathVariable UUID sceneId, @RequestBody Request body, Principal actor) { return service.create(campaignId,sceneId,actor.getName(),body.command()); }
    @PostMapping("/{tokenId}") Result update(@PathVariable UUID campaignId, @PathVariable UUID sceneId, @PathVariable UUID tokenId, @RequestBody Request body, Principal actor) { return service.update(campaignId,sceneId,tokenId,actor.getName(),body.command()); }
}
