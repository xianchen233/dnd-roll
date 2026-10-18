package io.dndroll.web;

import java.util.*;

public final class SessionSockets {
    private final Map<String, Map<String, Runnable>> sessions = new HashMap<>();
    public synchronized void register(String sessionId, String socketId, Runnable close) {
        sessions.computeIfAbsent(sessionId, ignored -> new HashMap<>()).put(socketId, close);
    }
    public synchronized void remove(String sessionId, String socketId) {
        var sockets = sessions.get(sessionId);
        if (sockets != null) { sockets.remove(socketId); if (sockets.isEmpty()) sessions.remove(sessionId); }
    }
    public void close(String sessionId) {
        Map<String, Runnable> sockets;
        synchronized (this) { sockets = sessions.remove(sessionId); }
        if (sockets != null) sockets.values().forEach(Runnable::run);
    }
}
