package io.dndroll.persistence;

import io.dndroll.modules.ModuleOperations.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcModuleOperationsStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcModuleOperationsStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public List<Definition> list(UUID c){return jdbc.query("select definition from module_operation where campaign_id=? order by id",(r,n)->json.readValue(r.getString(1),Definition.class),c);}
    public Definition definition(UUID c,UUID id){var rows=jdbc.query("select definition from module_operation where campaign_id=? and id=?",(r,n)->json.readValue(r.getString(1),Definition.class),c,id);return rows.isEmpty()?null:rows.getFirst();}
    public void save(UUID c,Definition v,boolean create){if(create)jdbc.update("insert into module_operation(campaign_id,id,definition) values (?,?,?::jsonb)",c,v.id(),json.writeValueAsString(v));else jdbc.update("update module_operation set definition=?::jsonb where campaign_id=? and id=?",json.writeValueAsString(v),c,v.id());}
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,result from module_operation_receipt where actor=? and request_id=?",(r,n)->new Receipt(r.getString(1),json.readTree(r.getString(2))),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt v){jdbc.update("insert into module_operation_receipt(campaign_id,actor,request_id,fingerprint,result) values (?,?,?,?,?::jsonb)",c,actor,request,v.fingerprint(),json.writeValueAsString(v.result()));}
    public List<Execution> executions(UUID c,UUID id){return jdbc.query("select evidence from module_operation_execution where campaign_id=? and operation_id=? order by at,id",(r,n)->json.readValue(r.getString(1),Execution.class),c,id);}
    public void execution(UUID c,UUID id,Execution v){jdbc.update("insert into module_operation_execution(campaign_id,operation_id,id,at,evidence) values (?,?,?,?,?::jsonb)",c,id,v.id(),java.sql.Timestamp.from(v.at()),json.writeValueAsString(v));}
}
