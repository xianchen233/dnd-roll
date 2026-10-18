package io.dndroll.web;

public final class StompPolicy {
    private StompPolicy() {}
    public static boolean allowed(String command, boolean authenticated, String destination) {
        if (!authenticated) return false;
        return switch (command) {
            case "CONNECT", "STOMP", "DISCONNECT", "UNSUBSCRIBE", "HEARTBEAT" -> true;
            case "SUBSCRIBE" -> "/user/queue/scene-changes".equals(destination);
            default -> false;
        };
    }
}
