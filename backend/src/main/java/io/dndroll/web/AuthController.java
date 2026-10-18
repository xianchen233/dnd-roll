package io.dndroll.web;

import io.dndroll.domain.DomainException;
import jakarta.servlet.http.*;
import java.security.Principal;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final HttpSessionSecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;
    public AuthController(AuthenticationManager manager, HttpSessionSecurityContextRepository contexts, CsrfTokenRepository csrfTokens) { this.manager = manager; this.contexts = contexts; this.csrfTokens = csrfTokens; }
    public record Credentials(String username, String password) {}
    public record Identity(String username) {}
    public record Csrf(String token, String headerName) {}
    @GetMapping("/csrf") Csrf csrf(CsrfToken token) { return new Csrf(token.getToken(), token.getHeaderName()); }
    @GetMapping("/me") Identity me(Principal principal) { return new Identity(principal.getName()); }
    @PostMapping("/login") Identity login(@RequestBody Credentials credentials, HttpServletRequest request, HttpServletResponse response) {
        if (credentials.username() == null || credentials.password() == null || credentials.username().isBlank() || credentials.password().isBlank())
            throw new DomainException("INVALID_INPUT", 400, "Username and password are required");
        var authentication = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(credentials.username(), credentials.password()));
        if (request.getSession(false) != null) request.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication); SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response); csrfTokens.saveToken(null, request, response);
        return new Identity(authentication.getName());
    }
}
