package io.dndroll.web;

import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService users(JdbcTemplate jdbc) {
        return username -> {
            var users = jdbc.query("select username,password_hash from app_user where username=?", (rs, n) -> User.withUsername(rs.getString(1)).password(rs.getString(2)).roles("USER").build(), username);
            if (users.isEmpty()) throw new UsernameNotFoundException("Account not found");
            return users.getFirst();
        };
    }
    @Bean AuthenticationManager authentication(UserDetailsService users, PasswordEncoder passwords) {
        var provider = new DaoAuthenticationProvider(users); provider.setPasswordEncoder(passwords); return new ProviderManager(provider);
    }
    @Bean HttpSessionSecurityContextRepository contexts() { return new HttpSessionSecurityContextRepository(); }
    @Bean CsrfTokenRepository csrfTokens() { return new HttpSessionCsrfTokenRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http, HttpSessionSecurityContextRepository contexts, CsrfTokenRepository csrfTokens, AuthenticationManager authentication) throws Exception {
        http.authenticationManager(authentication)
            .securityContext(context -> context.securityContextRepository(contexts))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/csrf", "/api/auth/login").permitAll().anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> ApiErrors.write(response, 401, "UNAUTHENTICATED", "Login required"))
                .accessDeniedHandler((request, response, error) -> ApiErrors.write(response, 403, "FORBIDDEN", "Request denied; refresh your CSRF token or check access")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").logoutSuccessHandler((request, response, authenticationResult) -> response.setStatus(204)))
            .requestCache(cache -> cache.disable());
        return http.build();
    }
}
