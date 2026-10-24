package io.dndroll.web;

import io.dndroll.application.SceneService.Events;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class SceneEvents implements Events {
    private static final Logger log = LoggerFactory.getLogger(SceneEvents.class);
    private final JdbcTemplate jdbc;
    private final SimpMessagingTemplate messaging;
    public record Invalidation(String type, UUID campaignId) {}
    public SceneEvents(JdbcTemplate jdbc, SimpMessagingTemplate messaging) { this.jdbc = jdbc; this.messaging = messaging; }
    public void changed(UUID campaignId) {
        diceChanged(campaignId, false);
    }
    public void diceChanged(UUID campaignId, boolean dmOnly) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCommit() {
                // Delivery is a hint. A broker failure must not turn a committed move into an HTTP failure.
                try {
                    var members = jdbc.query("select username from campaign_member where campaign_id=? and (not ? or role='DM')", (rs, n) -> rs.getString(1), campaignId, dmOnly);
                    var event = new Invalidation("SCENE_CHANGED", campaignId);
                    for (var member : members) messaging.convertAndSendToUser(member, "/queue/scene-changes", event);
                } catch (RuntimeException failure) { log.warn("Scene invalidation delivery failed; clients will refresh", failure); }
            }
        });
    }
}
