package io.dndroll.backup;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.*;
import io.dndroll.domain.CharacterSheet;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;

class CampaignEvidenceRebindHashIT {
    @TempDir Path root;

    @Test void realNonemptyJdbcEvidenceRegeneratesHashAndRejectsArchivedHashCorruption() throws Exception {
        var owner = new RecoveryAuditIT(); owner.root = root;
        try (var f = owner.new Fixture()) {
            var json = JsonMapper.builder().build();
            UUID source = UUID.randomUUID(), target = UUID.randomUUID(), actionId = UUID.randomUUID(), previewId = UUID.randomUUID();
            var cards = new ArrayList<CharacterService.Card>();
            for (UUID id : List.of(source, target)) {
                var sheet = CharacterSheet.empty("PRIVATE_HASH_" + source);
                var card = new CharacterService.Card(id, f.campaign, 0, false, false, false, List.of(), sheet);
                cards.add(card);
                f.jdbc.update("insert into character_card(id,campaign_id,version,sheet) values (?,?,0,?::jsonb)", id, f.campaign, json.writeValueAsString(sheet));
                f.jdbc.update("insert into character_history(character_id,version,actor,kind,reason,at,after_state) values (?,0,'PRIVATE_AUDIT_ACTOR','CREATE','PRIVATE_HASH_REASON',now(),?::jsonb)", id, json.writeValueAsString(card));
            }
            // Match the real service: previews are built from JDBC-read cards, after JSONB ordering.
            for (int i = 0; i < cards.size(); i++) {
                var old = cards.get(i);
                var storedSheet = json.readValue(f.jdbc.queryForObject("select sheet::text from character_card where id=?", String.class, old.id()), CharacterSheet.class);
                cards.set(i, new CharacterService.Card(old.id(), f.campaign, 0, false, false, false, List.of(), storedSheet));
            }
            var action = new CombatService.Action(actionId, 0, 0, source, "PRIVATE_SOURCE", 0, target, "PRIVATE_TARGET", null, "DM", "PRIVATE_AUDIT_ACTOR", Instant.EPOCH, "", List.of());
            f.jdbc.update("insert into combat_action(id,campaign_id,character_id,target_id,visibility,data) values (?,?,?,?,'DM',?::jsonb)", actionId, f.campaign, source, target, json.writeValueAsString(action));
            var review = new AttackPreviewService.Create(UUID.randomUUID(), source, 0, target, 0, "local-resource-" + source, true, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
            var evidence = new AttackPreviewService.Evidence(review, cards.get(0), cards.get(1), null, List.of("PRIVATE_MANUAL"), null, null, null, actionId);
            String originalHash = DiceService.fingerprint(json, List.of(evidence));
            var preview = new AttackPreviewService.Preview(previewId, 0, "MANUAL_REQUIRED", "PRIVATE_AUDIT_ACTOR", Instant.EPOCH, evidence, originalHash, List.of());
            f.jdbc.update("insert into attack_preview(id,campaign_id,character_id,target_id,data) values (?,?,?,?,?::jsonb)", previewId, f.campaign, source, target, json.writeValueAsString(preview));
            var backup = new PostgresBackup(new DriverManagerDataSource(f.url, f.username, f.password), f.assets.toString(), RecoveryAuditIT.required("BACKUP_PG_DUMP"), f.url, f.username, f.password);
            Path good = root.resolve("nonempty.zip");
            backup.create(Files.createDirectory(root.resolve("nonempty-work")), good);
            UUID destination = UUID.randomUUID(), seed = UUID.randomUUID();
            var before = f.snapshot();
            var report = CampaignEvidenceRebindCommand.inspect(good, f.url, f.username, f.password, f.assets, f.campaign, destination, seed);
            assertTrue(report.consistent(), report.blockers().toString());
            assertEquals(1, report.proofs().size());
            assertEquals(originalHash, report.proofs().getFirst().sourceHash());
            assertNotEquals(originalHash, report.proofs().getFirst().targetHash());
            assertFalse(report.executable());
            assertFalse(json.writeValueAsString(report).contains("PRIVATE_"));
            assertEquals(before, f.snapshot());
            f.jdbc.update("update attack_preview set data=jsonb_set(data,'{hash}',to_jsonb(?::text)) where id=?", "b".repeat(64), previewId);
            Path bad = root.resolve("archived-corrupt-hash.zip");
            backup.create(Files.createDirectory(root.resolve("corrupt-work")), bad);
            before = f.snapshot();
            var rejected = CampaignEvidenceRebindCommand.inspect(bad, f.url, f.username, f.password, f.assets, f.campaign, destination, seed);
            assertFalse(rejected.consistent());
            assertTrue(rejected.proofs().isEmpty());
            assertEquals(before, f.snapshot());
        }
    }
}
