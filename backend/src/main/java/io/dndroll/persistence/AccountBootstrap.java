package io.dndroll.persistence;

import io.dndroll.domain.AccountPolicy;
import io.dndroll.domain.DomainException;
import java.util.UUID;
import org.springframework.boot.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Operator-only initial identity. Insert-only so restarts never reset accounts or permissions. */
@Component
public class AccountBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final Environment env;
    private final PasswordEncoder passwords;
    public AccountBootstrap(JdbcTemplate jdbc, Environment env, PasswordEncoder passwords) { this.jdbc = jdbc; this.env = env; this.passwords = passwords; }
    @Transactional
    public void run(ApplicationArguments ignored) {
        String username = env.getProperty("BOOTSTRAP_USERNAME");
        String password = env.getProperty("BOOTSTRAP_PASSWORD");
        if ((username == null || username.isEmpty()) && (password == null || password.isEmpty())) return;
        try { AccountPolicy.credentials(username, password); }
        catch (DomainException invalid) { throw new IllegalStateException("BOOTSTRAP_USERNAME and BOOTSTRAP_PASSWORD must both meet account requirements"); }
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?) on conflict (username) do nothing", UUID.randomUUID(), username, passwords.encode(password));
    }
}
