package io.dndroll.persistence;

import io.dndroll.modules.ModulePackages.*;
import io.dndroll.modules.ModuleArchive;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcModuleStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcModuleStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public List<Stored> list(UUID c){return jdbc.query("select id,manifest,sha256 from module_package where campaign_id=? order by created_at,id",(r,n)->new Stored(r.getObject(1,UUID.class),json.readValue(r.getString(2),ModuleArchive.Manifest.class),r.getString(3)),c);}
    public byte[] bytes(UUID c,UUID id){var r=jdbc.query("select archive from module_package where campaign_id=? and id=?",(rs,n)->rs.getBytes(1),c,id);return r.isEmpty()?null:r.getFirst();}
    public void insert(UUID c,Stored s,byte[] bytes){jdbc.update("insert into module_package(id,campaign_id,module_id,module_version,sha256,manifest,archive) values (?,?,?,?,?,?::jsonb,?)",s.id(),c,UUID.fromString(s.manifest().moduleId()),s.manifest().moduleVersion(),s.sha256(),json.writeValueAsString(s.manifest()),bytes);}
    public Receipt receipt(String actor,UUID request){var r=jdbc.query("select fingerprint,result_id,result_version from module_command_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new Ack(rs.getObject(2,UUID.class),rs.getLong(3))),actor,request);return r.isEmpty()?null:r.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into module_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values (?,?,?,?,?,?)",actor,request,c,r.fingerprint(),r.result().id(),r.result().version());}
}
