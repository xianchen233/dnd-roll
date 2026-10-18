package io.dndroll.application;

import io.dndroll.domain.Movement.*;
import io.dndroll.domain.Movement;
import io.dndroll.domain.DomainException;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class SceneService {
    public record CampaignView(UUID id, String name, Role role) {}
    public record Scene(UUID id, UUID campaignId, String name, int width, int height, int gridSize, long version, List<Token> tokens) {}
    public record Snapshot(UUID id, UUID campaignId, String name, int width, int height, int gridSize, long version, List<TokenView> tokens) {}
    public record Receipt(UUID tokenId, Command command, TokenView response) {}
    public interface Store {
        List<CampaignView> campaigns(String actor);
        Role role(UUID campaignId, String actor);
        Scene scene(UUID campaignId);
        Receipt receipt(UUID campaignId, String actor, UUID requestId);
        void save(UUID campaignId, String actor, Token token, Command command, TokenView response);
        <T> T locked(UUID campaignId, Supplier<T> action);
    }
    public interface Events { void changed(UUID campaignId); }
    private final Store store;
    private final Events events;
    public SceneService(Store store, Events events) { this.store = store; this.events = events; }
    public List<CampaignView> campaigns(String actor) { return store.campaigns(actor); }
    public Snapshot snapshot(UUID campaignId, String actor) {
        return store.locked(campaignId, () -> {
            var role = requireMember(campaignId, actor);
            var scene = store.scene(campaignId);
            var tokens = scene.tokens().stream().map(t -> Movement.view(t, role, actor)).filter(java.util.Objects::nonNull).toList();
            return new Snapshot(scene.id(), scene.campaignId(), scene.name(), scene.width(), scene.height(), scene.gridSize(), scene.version(), tokens);
        });
    }
    public TokenView move(UUID campaignId, UUID tokenId, String actor, Command command) {
        return store.locked(campaignId, () -> {
            var role = requireMember(campaignId, actor);
            var scene = store.scene(campaignId);
            // Current scene visibility is checked before disclosing any receipt state.
            var token = findToken(scene, tokenId);
            var visible = Movement.view(token, role, actor);
            if (visible == null) throw new DomainException("NOT_FOUND", 404, "Token not found");
            if (!visible.canMove()) throw new DomainException("FORBIDDEN", 403, "You cannot move this token");
            var existing = store.receipt(campaignId, actor, command.requestId());
            if (existing != null) {
                if (!existing.tokenId().equals(tokenId) || !existing.command().equals(command))
                    throw new DomainException("IDEMPOTENCY_CONFLICT", 409, "Request ID was already used with different parameters");
                return existing.response();
            }
            var moved = Movement.move(token, role, actor, command, scene.width(), scene.height());
            var response = Movement.view(moved, role, actor);
            store.save(campaignId, actor, moved, command, response);
            events.changed(campaignId);
            return response;
        });
    }
    private Role requireMember(UUID campaignId, String actor) {
        var role = store.role(campaignId, actor);
        if (role == null) throw new DomainException("FORBIDDEN", 403, "Campaign membership required");
        return role;
    }
    private Token findToken(Scene scene, UUID tokenId) {
        return scene.tokens().stream().filter(t -> t.id().equals(tokenId)).findFirst().orElseThrow(() -> new DomainException("NOT_FOUND", 404, "Token not found"));
    }
}
