package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountBootstrapTest {
    @Test void optionalOrValidConfigurationOnlyInsertsWithoutUpdatingPermissions() {
        var jdbc = new RecordingJdbc(); var env = new MockEnvironment();
        var bootstrap = new AccountBootstrap(jdbc, env, ENCODER);
        bootstrap.run(new DefaultApplicationArguments()); assertEquals(0, jdbc.calls);
        env.setProperty("BOOTSTRAP_USERNAME", "owner"); env.setProperty("BOOTSTRAP_PASSWORD", " valid-password ");
        bootstrap.run(new DefaultApplicationArguments());
        assertEquals(1, jdbc.calls); assertTrue(jdbc.sql.contains("on conflict (username) do nothing"));
        assertEquals("encoded: valid-password ", jdbc.args[2]);
    }
    @Test void partialBlankOrInvalidConfigurationFailsBeforeWriting() {
        var jdbc = new RecordingJdbc();
        for (var pair : new String[][] {{"owner", ""}, {"", "valid-password"}, {"INVALID", "valid-password"}, {"owner", "short"}, {"owner", "密".repeat(25)}}) {
            var env = new MockEnvironment().withProperty("BOOTSTRAP_USERNAME", pair[0]).withProperty("BOOTSTRAP_PASSWORD", pair[1]);
            assertThrows(IllegalStateException.class, () -> new AccountBootstrap(jdbc, env, ENCODER).run(new DefaultApplicationArguments()));
        }
        assertEquals(0, jdbc.calls);
    }
    static final PasswordEncoder ENCODER = new PasswordEncoder() {
        public String encode(CharSequence value) { return "encoded:" + value; }
        public boolean matches(CharSequence value, String hash) { return false; }
    };
    static class RecordingJdbc extends JdbcTemplate {
        int calls; String sql; Object[] args;
        @Override public int update(String sql, Object... args) { this.sql = sql; this.args = args; calls++; return 1; }
    }
}
