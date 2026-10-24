package io.dndroll.web;

import io.dndroll.domain.DomainException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import tools.jackson.core.JacksonException;

@RestControllerAdvice
public class ApiErrors {
    public record Error(String code, String message) {}
    @ExceptionHandler(DomainException.class)
    ResponseEntity<Error> domain(DomainException error) { return ResponseEntity.status(error.status()).body(new Error(error.code(), error.getMessage())); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestPartException.class, HttpMediaTypeNotSupportedException.class, JacksonException.class})
    ResponseEntity<Error> invalid(Exception ignored) { return ResponseEntity.badRequest().body(new Error("INVALID_INPUT", "Invalid request body or identifier")); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Error> oversized(Exception ignored) { return ResponseEntity.status(413).body(new Error("INVALID_INPUT", "Image exceeds upload limit")); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Error> unauthenticated(AuthenticationException ignored) { return ResponseEntity.status(401).body(new Error("UNAUTHENTICATED", "Invalid username or password")); }
    public static void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        // Only fixed server strings are passed here, never request or hidden data.
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
