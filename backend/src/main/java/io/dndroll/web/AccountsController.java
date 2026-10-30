package io.dndroll.web;

import io.dndroll.application.AccountsService;
import io.dndroll.application.AccountsService.*;
import io.dndroll.domain.DomainException;
import java.security.*;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountsController {
    private final AccountsService service;
    public AccountsController(AccountsService service) { this.service = service; }
    @Configuration static class Wiring {
        @Bean Clock accountClock() { return Clock.systemUTC(); }
        @Bean AccountsService accountsService(Store store, PasswordEncoder passwords, Clock clock, AccountEvents events) {
            var random = new SecureRandom();
            return new AccountsService(store, passwords::encode, clock, () -> {
                byte[] bytes = new byte[32]; random.nextBytes(bytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            }, events);
        }
    }
    public record CreateRequest(UUID requestId, String name) {}
    public record ManagementRequest(UUID requestId, Long expectedVersion) {
        long version() {
            if (requestId == null || expectedVersion == null || expectedVersion < 0) throw new DomainException("INVALID_INPUT", 400, "Request ID and nonnegative expectedVersion required");
            return expectedVersion;
        }
    }
    public record TokenRequest(String token) {}
    public record RegisterRequest(String token, String username, String password) {}
    @PostMapping("/campaigns") ResponseEntity<Created> create(@RequestBody CreateRequest request, Principal actor) {
        return ResponseEntity.status(201).body(service.create(actor.getName(), request.requestId(), request.name()));
    }
    @GetMapping("/campaigns/{campaignId}/members") Members members(@PathVariable UUID campaignId, Principal actor) { return service.members(campaignId, actor.getName()); }
    @GetMapping("/campaigns/{campaignId}/invitations") Invitations invitations(@PathVariable UUID campaignId, Principal actor) { return service.invitations(campaignId, actor.getName()); }
    @PostMapping("/campaigns/{campaignId}/invitations") ResponseEntity<Issued> issue(@PathVariable UUID campaignId, @RequestBody ManagementRequest request, Principal actor) {
        var result = service.issue(campaignId, actor.getName(), request.requestId(), request.version());
        return ResponseEntity.status(result.token() == null ? 200 : 201).body(result);
    }
    @PostMapping("/campaigns/{campaignId}/invitations/{invitationId}/revoke") ResponseEntity<Void> revoke(@PathVariable UUID campaignId, @PathVariable UUID invitationId, @RequestBody ManagementRequest request, Principal actor) {
        service.revoke(campaignId, invitationId, actor.getName(), request.requestId(), request.version()); return ResponseEntity.noContent().build();
    }
    @PostMapping("/campaigns/{campaignId}/members/{username}/remove") ResponseEntity<Void> remove(@PathVariable UUID campaignId, @PathVariable String username, @RequestBody ManagementRequest request, Principal actor) {
        service.remove(campaignId, username, actor.getName(), request.requestId(), request.version()); return ResponseEntity.noContent().build();
    }
    @PostMapping("/invitations/preview") Preview preview(@RequestBody TokenRequest request) { return service.preview(request.token()); }
    @PostMapping("/invitations/accept") Joined accept(@RequestBody TokenRequest request, Principal actor) { return service.accept(request.token(), actor.getName()); }
    @PostMapping("/invitations/register") ResponseEntity<Registered> register(@RequestBody RegisterRequest request, Principal actor) {
        if (actor != null) throw new DomainException("INVALID_INPUT", 400, "Authenticated accounts must accept the invitation");
        return ResponseEntity.status(201).body(service.register(request.token(), request.username(), request.password()));
    }
}
