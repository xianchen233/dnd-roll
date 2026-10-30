package io.dndroll.backup;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;
class CampaignRemapPlanIT {
    @TempDir Path root;
    class Fixture implements AutoCloseable {
        final RecoveryAuditIT owner=new RecoveryAuditIT();final RecoveryAuditIT.Fixture f;final UUID shared=UUID.randomUUID(),other=UUID.randomUUID(),otherCard=UUID.randomUUID(),otherScene=UUID.randomUUID(),note=UUID.randomUUID(),message=UUID.randomUUID(),seed=UUID.randomUUID(),target=UUID.randomUUID();final Path archive;
        Fixture()throws Exception{
            owner.root=root;f=owner.new Fixture();
            f.jdbc.update("insert into campaign(id,name) values (?,'PRIVATE_OTHER_CAMPAIGN')",other);
            for(UUID c:List.of(f.campaign,other)){
                UUID card=c.equals(f.campaign)?shared:otherCard;f.jdbc.update("insert into character_card(id,campaign_id,version,sheet) values (?,?,7,'{\"private\":\"PRIVATE_GAME_TEXT\"}')",card,c);
                f.jdbc.update("insert into character_history(character_id,version,actor,kind,reason,at,after_state) values (?,7,'PRIVATE_AUDIT_ACTOR','EDIT','PRIVATE_GAME_TEXT',now(),'{}')",card);
                f.jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'PRIVATE_SCENE',10,10,10)",c.equals(f.campaign)?shared:otherScene,c);
                UUID n=c.equals(f.campaign)?note:UUID.randomUUID(),m=c.equals(f.campaign)?message:UUID.randomUUID();
                f.jdbc.update("insert into campaign_note(id,campaign_id,visibility,data) values (?,?,'DM','{\"visibility\":\"DM\",\"private\":\"PRIVATE_GAME_TEXT\"}')",n,c);f.jdbc.update("insert into note_history(note_id,version,data) values (?,1,'{}')",n);
                f.jdbc.update("insert into rp_message(id,campaign_id,kind,data) values (?,?,'OOC','{}')",m,c);f.jdbc.update("insert into rp_message_history(message_id,version,data) values (?,1,'{}')",m);
                f.jdbc.update("insert into timeline_entry(id,campaign_id,kind,message_id,at) values (?,?,'OOC',?,now())",m,c,m);
            }
            f.jdbc.update("update campaign set current_scene_id=? where id=?",shared,f.campaign);
            f.jdbc.update("insert into character_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values ('PRIVATE_AUDIT_ACTOR',?,?,?, ?,7)",UUID.randomUUID(),f.campaign,"a".repeat(64),shared);
            archive=backup("seeded");
        }
        Path backup(String name)throws Exception{Path file=root.resolve(name+".zip");new PostgresBackup(new DriverManagerDataSource(f.url,f.username,f.password),f.assets.toString(),RecoveryAuditIT.required("BACKUP_PG_DUMP"),f.url,f.username,f.password).create(Files.createDirectory(root.resolve(name+"-work")),file);return file;}
        CampaignRemapPlan.Report plan(Path file,UUID destination)throws Exception{return CampaignRemapPlanCommand.plan(file,f.url,f.username,f.password,f.assets,f.campaign,destination,seed);}
        public void close()throws Exception{f.close();}
    }
    @Test void completeScopedProposalPreservesRowsSeparatesNamespacesAndNeverLeaksPrivateJson()throws Exception{
        try(var f=new Fixture()){var before=f.f.snapshot();var p=assertDoesNotThrow(()->f.plan(f.archive,f.target));assertEquals("BLOCKED",p.status());assertFalse(p.executable());assertEquals(76,p.tables().size());assertEquals(before,f.f.snapshot());assertEquals(p,f.plan(f.archive,f.target));
            var mapped=p.mappings().stream().filter(m->m.source().equals(f.shared)).toList();assertEquals(2,mapped.size());assertNotEquals(mapped.get(0).target(),mapped.get(1).target());assertTrue(p.mappings().stream().noneMatch(m->m.source().equals(f.otherCard)||m.source().equals(f.other)));
            for(String name:List.of("character_history","note_history","rp_message_history"))assertEquals(1,p.tables().stream().filter(t->t.name().equals(name)).findFirst().orElseThrow().rows());
            var receipt=p.tables().stream().filter(t->t.name().equals("character_command_receipt")).findFirst().orElseThrow();assertEquals(1,receipt.rows());assertTrue(receipt.columns().stream().allMatch(c->c.policy().equals("SOURCE_ONLY_SKIP")));
            assertEquals(p.mappings().stream().filter(m->m.namespace().equals("rp_message")).findFirst().orElseThrow().target(),p.mappings().stream().filter(m->m.namespace().equals("timeline_entry")).findFirst().orElseThrow().target());
            assertTrue(p.blockers().stream().anyMatch(v->v.startsWith("TYPED_JSON_REBIND_NOT_IMPLEMENTED:")));assertTrue(p.blockers().stream().noneMatch(v->v.startsWith("UNRESOLVED_REFERENCE:")));String json=JsonMapper.builder().build().writeValueAsString(p);for(String secret:List.of("PRIVATE_",f.f.password,f.f.url,root.toString()))assertFalse(json.contains(secret));
        }
    }
    @Test void existingTargetBlocksAndDatabaseRetainsCompositeCrossCampaignSceneGuard()throws Exception{
        try(var f=new Fixture()){var p=assertDoesNotThrow(()->f.plan(f.archive,f.other));assertTrue(p.blockers().contains("TARGET_ID_COLLISION"));assertThrows(org.springframework.dao.DataAccessException.class,()->f.f.jdbc.update("update campaign set current_scene_id=? where id=?",f.otherScene,f.f.campaign));p=f.plan(f.archive,f.target);assertTrue(p.blockers().stream().noneMatch(v->v.startsWith("UNRESOLVED_REFERENCE:")));}
    }
    @Test void changedArchiveRowsOrUnknownSchemaFailBeforeIdentityProposal()throws Exception{
        try(var f=new Fixture()){assertDoesNotThrow(()->f.plan(f.archive,f.target));f.f.jdbc.update("update campaign set name='changed' where id=?",f.f.campaign);var failure=assertThrows(java.io.IOException.class,()->f.plan(f.archive,f.target));assertFalse(failure.toString().contains(f.f.password));Path changed=f.backup("changed");f.f.jdbc.execute("alter table stabilization_state add column unreviewed_source jsonb");assertThrows(java.io.IOException.class,()->f.plan(changed,f.target));}
    }
    @Test void createNewOutputAndMissingSourceKeepAllDataAndNeverSucceed()throws Exception{
        try(var f=new Fixture()){var p=assertDoesNotThrow(()->f.plan(f.archive,f.target));Path out=root.resolve("plan.json");CampaignRemapPlanCommand.write(p,out);byte[] bytes=Files.readAllBytes(out);assertThrows(FileAlreadyExistsException.class,()->CampaignRemapPlanCommand.write(p,out));assertArrayEquals(bytes,Files.readAllBytes(out));var missing=CampaignRemapPlanCommand.plan(f.archive,f.f.url,f.f.username,f.f.password,f.f.assets,UUID.randomUUID(),f.target,f.seed);assertTrue(missing.blockers().contains("SOURCE_CAMPAIGN_MISSING"));assertFalse(missing.executable());}
    }
}
