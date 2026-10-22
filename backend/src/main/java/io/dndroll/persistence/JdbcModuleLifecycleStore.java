package io.dndroll.persistence;

import io.dndroll.modules.*;
import io.dndroll.modules.ModuleLifecycle.*;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcModuleLifecycleStore implements Store {
    public List<MigrationEvidence> migrations(UUID c){return jdbc.query("select evidence from module_instance_migration where campaign_id=? order by version",(r,n)->json.readValue(r.getString(1),MigrationEvidence.class),c);}
    public void migration(UUID c,MigrationEvidence v){jdbc.update("insert into module_instance_migration(campaign_id,version,recovery_campaign_id,evidence) values (?,?,?,?::jsonb)",c,v.version(),v.recoveryCampaignId(),json.writeValueAsString(v));}
    public List<LiveEvidence> liveUpdates(UUID c){return jdbc.query("select evidence from module_instance_update where campaign_id=? order by version",(r,n)->json.readValue(r.getString(1),LiveEvidence.class),c);}
    public void liveUpdate(UUID c,LiveEvidence value){jdbc.update("insert into module_instance_update(campaign_id,version,evidence) values (?,?,?::jsonb)",c,value.version(),json.writeValueAsString(value));}
    public byte[] effectiveArchive(UUID c,UUID id){var rows=jdbc.query("select bytes from module_customization_archive where campaign_id=? and customization_id=?",(r,n)->r.getBytes(1),c,id);return rows.isEmpty()?null:rows.getFirst();}
    public byte[] beforeArchive(UUID c,UUID id,long version){return jdbc.queryForObject("select before_archive from module_customization_update where campaign_id=? and customization_id=? and version=?",byte[].class,c,id,version);}
    public void updateArchive(UUID c,UUID id,byte[] bytes,byte[] before,UpdateEvidence evidence){
        jdbc.update("insert into module_customization_update(campaign_id,customization_id,version,evidence,before_archive) values (?,?,?,?::jsonb,?)",c,id,evidence.version(),json.writeValueAsString(evidence),before);
        jdbc.update("insert into module_customization_archive(campaign_id,customization_id,bytes) values (?,?,?) on conflict(campaign_id,customization_id) do update set bytes=excluded.bytes",c,id,bytes);
    }
    public List<UpdateEvidence> updates(UUID c,UUID id){return jdbc.query("select evidence from module_customization_update where campaign_id=? and customization_id=? order by version",(r,n)->json.readValue(r.getString(1),UpdateEvidence.class),c,id);}
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcModuleLifecycleStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public List<Summary> list(UUID c){return jdbc.query("select id,name,version,source_package_id from module_customization where campaign_id=? order by created_at,id",(r,n)->new Summary(r.getObject(1,UUID.class),r.getString(2),r.getLong(3),r.getObject(4,UUID.class)),c);}
    public Copy copy(UUID c,UUID id){var rows=jdbc.query("select * from module_customization where campaign_id=? and id=?",(r,n)->new Copy(r.getObject("id",UUID.class),r.getString("name"),r.getLong("version"),r.getObject("source_package_id",UUID.class),r.getString("source_sha256"),json.readValue(r.getString("baseline"),ModuleBlueprint.Document.class),json.readValue(r.getString("document"),ModuleBlueprint.Document.class)),c,id);return rows.isEmpty()?null:rows.getFirst();}
    public void copy(UUID c,Copy v,boolean create){
        if(create)jdbc.update("insert into module_customization(id,campaign_id,source_package_id,source_sha256,name,version,baseline,document) values (?,?,?,?,?,?,?::jsonb,?::jsonb)",v.id(),c,v.sourcePackageId(),v.sourceSha256(),v.name(),v.version(),json.writeValueAsString(v.baseline()),json.writeValueAsString(v.document()));
        else jdbc.update("update module_customization set name=?,version=?,document=?::jsonb where campaign_id=? and id=?",v.name(),v.version(),json.writeValueAsString(v.document()),c,v.id());
    }
    public Instance instance(UUID c){var rows=jdbc.query("select * from module_instance where campaign_id=?",(r,n)->new Instance(r.getLong("version"),json.readValue(r.getString("source"),Source.class),json.readValue(r.getString("document"),ModuleBlueprint.Document.class),json.readValue(r.getString("mapping"),new TypeReference<Map<String,Map<String,UUID>>>(){}),json.readValue(r.getString("progress"),new TypeReference<Map<String,String>>(){})),c);return rows.isEmpty()?null:rows.getFirst();}
    public void instance(UUID c,Instance v,boolean create){
        if(create)jdbc.update("insert into module_instance(campaign_id,version,source,document,mapping,progress) values (?,?,?::jsonb,?::jsonb,?::jsonb,?::jsonb)",c,v.version(),json.writeValueAsString(v.source()),json.writeValueAsString(v.document()),json.writeValueAsString(v.mapping()),json.writeValueAsString(v.progress()));
        else jdbc.update("update module_instance set version=?,source=?::jsonb,document=?::jsonb,mapping=?::jsonb,progress=?::jsonb where campaign_id=?",v.version(),json.writeValueAsString(v.source()),json.writeValueAsString(v.document()),json.writeValueAsString(v.mapping()),json.writeValueAsString(v.progress()),c);
    }
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,result_kind,result_id,result_version from module_lifecycle_receipt where actor=? and request_id=?",(r,n)->new Receipt(r.getString(1),r.getString(2),new Ack(r.getObject(3,UUID.class),r.getLong(4))),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt v){jdbc.update("insert into module_lifecycle_receipt(actor,request_id,campaign_id,fingerprint,result_kind,result_id,result_version) values (?,?,?,?,?,?,?)",actor,request,c,v.fingerprint(),v.kind(),v.result().id(),v.result().version());}
}
