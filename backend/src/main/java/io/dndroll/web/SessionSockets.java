package io.dndroll.web;

import java.util.*;

public final class SessionSockets {
    private final Map<String, Map<String, Runnable>> sessions = new HashMap<>();
    private final Map<String, String> usernames = new HashMap<>();
    public synchronized void register(String sessionId, String socketId, String username, Runnable close) {
        usernames.put(sessionId, username);
        register(sessionId, socketId, close);
    }
    public void closeUser(String username) {
        List<Runnable> closing = new ArrayList<>();
        synchronized (this) {
            for (var sessionId : usernames.entrySet().stream().filter(e -> e.getValue().equals(username)).map(Map.Entry::getKey).toList()) {
                var sockets = sessions.remove(sessionId);
                usernames.remove(sessionId);
                if (sockets != null) closing.addAll(sockets.values());
            }
        }
        closing.forEach(Runnable::run);
    }
    public synchronized void register(String sessionId, String socketId, Runnable close) {
        sessions.computeIfAbsent(sessionId, ignored -> new HashMap<>()).put(socketId, close);
    }
    public synchronized void remove(String sessionId, String socketId) {
        var sockets = sessions.get(sessionId);
        if (sockets != null) { sockets.remove(socketId); if (sockets.isEmpty()) { sessions.remove(sessionId); usernames.remove(sessionId); } }
    }
    public void close(String sessionId) {
        Map<String, Runnable> sockets;
        synchronized (this) { sockets = sessions.remove(sessionId); usernames.remove(sessionId); }
        if (sockets != null) sockets.values().forEach(Runnable::run);
    }
}
