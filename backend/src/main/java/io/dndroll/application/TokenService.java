package io.dndroll.application;

import io.dndroll.domain.AccountPolicy;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;

/** Private DM authoring. Historical receipts never mutate current configuration. */
public final class TokenService {
    public record Token(UUID id, String name, int x, int y, long version, boolean hidden, List<String> controllers) {}
    public record Scene(UUID id, int width, int height, long version, List<Token> tokens) {}
    public record Listing(UUID sceneId, long sceneVersion, List<Token> tokens) {}
    public record Result(UUID sceneId, long sceneVersion, Token token) {}
    public record Command(UUID requestId, long expectedVersion, String name, int x, int y, boolean hidden, List<String> controllers) {}
    public record Intent(String kind, UUID campaignId, UUID sceneId, UUID tokenId, long expectedVersion, String name, int x, int y, boolean hidden, List<String> controllers) {}
    public record Receipt(Intent intent, Result result) {}
    public interface Store {
        <T> T actorLocked(String actor, Supplier<T> work);
        <T> T locked(UUID campaign, Supplier<T> work);
        Role role(UUID campaign, String actor);
        Scene scene(UUID campaign, UUID scene);
        Receipt receipt(String actor, UUID request);
        void receipt(String actor, UUID request, Receipt receipt);
        void save(UUID campaign, UUID scene, Token token, boolean create);
        long advance(UUID scene);
    }
    private final Store store;
    private final SceneService.Events events;
    public TokenService(Store store, SceneService.Events events) { this.store = store; this.events = events; }
    public Listing list(UUID campaign, UUID scene, String actor) {
        return store.locked(campaign, () -> {
            AccountPolicy.dm(store.role(campaign, actor));
            var target = requireScene(campaign, scene);
            return new Listing(scene, target.version(), target.tokens());
        });
    }
    public Result create(UUID campaign, UUID scene, String actor, Command command) { return command(campaign, scene, null, actor, command); }
    public Result update(UUID campaign, UUID scene, UUID token, String actor, Command command) {
        if (token == null) throw invalid();
        return command(campaign, scene, token, actor, command);
    }
    private Result command(UUID campaign, UUID scene, UUID tokenId, String actor, Command command) {
        if (command == null || command.requestId() == null || command.expectedVersion() < 0 || command.x() < 0 || command.y() < 0) throw invalid();
        String name = command.name() == null ? "" : command.name().strip();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > 100 || command.controllers() == null || command.controllers().size() > 100) throw invalid();
        var controllers = new TreeSet<String>();
        for (String controller : command.controllers()) {
            if (controller == null || controller.isEmpty() || controller.codePointCount(0, controller.length()) > 80 || !controllers.add(controller)) throw invalid();
        }
        var intent = new Intent(tokenId == null ? "CREATE" : "UPDATE", campaign, scene, tokenId, command.expectedVersion(), name, command.x(), command.y(), command.hidden(), List.copyOf(controllers));
        return store.actorLocked(actor, () -> store.locked(campaign, () -> {
            AccountPolicy.dm(store.role(campaign, actor));
            var target = requireScene(campaign, scene);
            Token previous = tokenId == null ? null : target.tokens().stream().filter(t -> t.id().equals(tokenId)).findFirst().orElseThrow(TokenService::missing);
            var existing = store.receipt(actor, command.requestId());
            if (existing != null) {
                if (!existing.intent().equals(intent)) throw new DomainException("IDEMPOTENCY_CONFLICT", 409, "Request ID was already used with different parameters");
                return existing.result();
            }
            if (command.x() >= target.width() || command.y() >= target.height()) throw invalid();
            for (String controller : controllers) if (store.role(campaign, controller) == null) throw invalid();
            AccountPolicy.version(previous == null ? target.version() : previous.version(), command.expectedVersion());
            var token = new Token(previous == null ? UUID.randomUUID() : tokenId, name, command.x(), command.y(), previous == null ? 0 : previous.version() + 1, command.hidden(), intent.controllers());
            store.save(campaign, scene, token, previous == null);
            var result = new Result(scene, store.advance(scene), token);
            store.receipt(actor, command.requestId(), new Receipt(intent, result));
            events.changed(campaign);
            return result;
        }));
    }
    private Scene requireScene(UUID campaign, UUID scene) { var target = store.scene(campaign, scene); if (target == null) throw missing(); return target; }
    private static DomainException missing() { return new DomainException("NOT_FOUND", 404, "Target not found"); }
    private static DomainException invalid() { return new DomainException("INVALID_INPUT", 400, "Invalid token command"); }
}
