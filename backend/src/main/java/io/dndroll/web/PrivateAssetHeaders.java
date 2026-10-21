package io.dndroll.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Runs before security so anonymous, revoked and malformed download failures remain private. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PrivateAssetHeaders extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().matches("/api/campaigns/[^/]+/(assets/[^/]+/content|scene/map-(view|image)|scenes/[^/]+/fog|characters(?:/.*)?|rolls(?:/.*)?|roll-commands|initiative(?:/.*)?|actions(?:/.*)?|combat(?:/.*)?|timeline(?:/.*)?|messages(?:/.*)?|attachments(?:/.*)?|notes(?:/.*)?|note-attachments(?:/.*)?)")) {
            response.setHeader("Cache-Control", "no-store, private");
            response.setHeader("X-Content-Type-Options", "nosniff");
        }
        chain.doFilter(request, response);
    }
}
