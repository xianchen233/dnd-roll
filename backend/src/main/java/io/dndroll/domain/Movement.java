package io.dndroll.domain;

import java.util.Set;
import java.util.UUID;

public final class Movement {
    private Movement() {}
    public enum Role { DM, PLAYER }
    public record Token(UUID id, String name, int x, int y, long version, boolean hidden, Set<String> controllers) {}
    public record TokenView(UUID id, String name, int x, int y, long version, boolean canMove) {}
    public record Command(UUID requestId, long expectedVersion, int x, int y) {}
    public static TokenView view(Token token, Role role, String actor) {
        if (token.hidden() && role != Role.DM) return null;
        return new TokenView(token.id(), token.name(), token.x(), token.y(), token.version(), role == Role.DM || token.controllers().contains(actor));
    }
    public static Token move(Token token, Role role, String actor, Command command, int width, int height) {
        var visible = view(token, role, actor);
        if (visible == null) throw new DomainException("NOT_FOUND", 404, "Token not found");
        if (!visible.canMove()) throw new DomainException("FORBIDDEN", 403, "You cannot move this token");
        if (command.requestId() == null || command.expectedVersion() < 0 || command.x() < 0 || command.y() < 0 || command.x() >= width || command.y() >= height)
            throw new DomainException("INVALID_INPUT", 400, "Move must contain a request ID, a nonnegative version and coordinates inside the scene");
        if (command.expectedVersion() != token.version()) throw new DomainException("VERSION_CONFLICT", 409, "The token changed; refresh the scene");
        return new Token(token.id(), token.name(), command.x(), command.y(), token.version() + 1, token.hidden(), token.controllers());
    }
}
