package io.dndroll.web;

import io.dndroll.application.AccountsService.Events;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class AccountEvents implements Events {
    private final SceneEvents sceneEvents;
    private final RealtimeConfiguration realtime;
    public AccountEvents(SceneEvents sceneEvents, RealtimeConfiguration realtime) { this.sceneEvents = sceneEvents; this.realtime = realtime; }
    public void changed(UUID campaignId) { sceneEvents.changed(campaignId); }
    public void removed(String username) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCommit() { realtime.closeUser(username); }
        });
    }
}
