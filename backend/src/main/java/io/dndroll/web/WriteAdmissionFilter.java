package io.dndroll.web;

import io.dndroll.backup.WriteAdmission;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** All application writes are synchronous HTTP; client STOMP SEND remains denied. */
@Component @Order(0)
public class WriteAdmissionFilter extends OncePerRequestFilter {
    private final WriteAdmission gate;
    public WriteAdmissionFilter(WriteAdmission gate){this.gate=gate;}
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        if(!request.getRequestURI().startsWith("/api/")||request.getRequestURI().matches("/api/admin/backups(?:/.*)?")||request.getRequestURI().matches("/api/auth/(login|logout)")||SetHolder.READ.contains(request.getMethod())){chain.doFilter(request,response);return;}
        WriteAdmission.Lease lease;
        try{lease=gate.enter();}catch(IllegalStateException paused){response.setHeader("Cache-Control","no-store, private");response.setHeader("Retry-After","5");ApiErrors.write(response,503,"WRITE_PAUSED","Writes paused for deployment backup; retry later");return;}
        try(lease){chain.doFilter(request,response);}
    }
    private static class SetHolder {static final java.util.Set<String> READ=java.util.Set.of("GET","HEAD","OPTIONS");}
}
