package io.dndroll.web;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.*;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class RealtimeConfiguration implements WebSocketMessageBrokerConfigurer {
    private final SessionSockets sockets = new SessionSockets();
    @Bean ServletListenerRegistrationBean<HttpSessionListener> sessionLifecycle() {
        return new ServletListenerRegistrationBean<>(new HttpSessionListener() {
            public void sessionDestroyed(HttpSessionEvent event) { sockets.close(event.getSession().getId()); }
        });
    }
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
            public void afterConnectionEstablished(WebSocketSession socket) throws Exception {
                var httpSession = (HttpSession) socket.getAttributes().get("httpSession");
                var sessionId = (String) socket.getAttributes().get("httpSessionId");
                sockets.register(sessionId, socket.getId(), () -> {
                    try { socket.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) { /* Transport is already unavailable. */ }
                });
                try { httpSession.getCreationTime(); }
                catch (IllegalStateException expired) { sockets.close(sessionId); return; }
                super.afterConnectionEstablished(socket);
            }
            public void afterConnectionClosed(WebSocketSession socket, CloseStatus status) throws Exception {
                sockets.remove((String) socket.getAttributes().get("httpSessionId"), socket.getId());
                super.afterConnectionClosed(socket, status);
            }
        });
    }
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue"); registry.setUserDestinationPrefix("/user");
    }
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").addInterceptors(new HandshakeInterceptor() {
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Map<String, Object> attributes) {
                if (!(request instanceof ServletServerHttpRequest servlet)) return false;
                var token = servlet.getServletRequest().getAttribute(CsrfToken.class.getName());
                if (!(token instanceof CsrfToken csrf)) return false;
                var session = servlet.getServletRequest().getSession(false);
                if (session == null) return false;
                attributes.put("httpSession", session); attributes.put("httpSessionId", session.getId());
                // Materialize the deferred servlet token before its HTTP request is recycled.
                attributes.put("csrf", new DefaultCsrfToken(csrf.getHeaderName(), csrf.getParameterName(), csrf.getToken())); return true;
            }
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) {}
        });
    }
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) throw new MessagingException("Invalid STOMP frame");
                var principal = accessor.getUser();
                boolean authenticated = principal instanceof Authentication auth && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
                var command = accessor.getCommand();
                String type = command == null ? "HEARTBEAT" : command.name();
                if (!StompPolicy.allowed(type, authenticated, accessor.getDestination())) throw new MessagingException("STOMP operation denied");
                if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
                    var attributes = accessor.getSessionAttributes();
                    var token = attributes == null ? null : attributes.get("csrf");
                    if (!(token instanceof CsrfToken csrf)) throw new MessagingException("CSRF token required");
                    var received = accessor.getFirstNativeHeader(csrf.getHeaderName());
                    if (received == null || !MessageDigest.isEqual(csrf.getToken().getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8)))
                        throw new MessagingException("Invalid CSRF token");
                }
                return message;
            }
        });
    }
}
