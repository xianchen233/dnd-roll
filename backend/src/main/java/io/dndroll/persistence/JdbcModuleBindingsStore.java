package io.dndroll.persistence;

import io.dndroll.modules.ModuleBindings.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcModuleBindingsStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcModuleBindingsStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public List<Binding> list(UUID c){return jdbc.query("select data from module_operation_binding where campaign_id=? order by node_id",(r,n)->json.readValue(r.getString(1),Binding.class),c);}
    public Binding binding(UUID c,UUID node){var rows=jdbc.query("select data from module_operation_binding where campaign_id=? and node_id=?",(r,n)->json.readValue(r.getString(1),Binding.class),c,node);return rows.isEmpty()?null:rows.getFirst();}
    public void put(UUID c,Binding b,boolean create){if(create)jdbc.update("insert into module_operation_binding(campaign_id,node_id,operation_id,data) values (?,?,?,?::jsonb)",c,b.nodeId(),b.operationId(),json.writeValueAsString(b));else jdbc.update("update module_operation_binding set data=?::jsonb where campaign_id=? and node_id=?",json.writeValueAsString(b),c,b.nodeId());}
    public List<Evidence> history(UUID c,UUID node){return jdbc.query("select evidence from module_operation_binding_history where campaign_id=? and node_id=? order by version",(r,n)->json.readValue(r.getString(1),Evidence.class),c,node);}
    public void history(UUID c,UUID node,Evidence e){jdbc.update("insert into module_operation_binding_history(campaign_id,node_id,version,evidence) values (?,?,?,?::jsonb)",c,node,e.version(),json.writeValueAsString(e));}
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,result from module_operation_binding_receipt where actor=? and request_id=?",(r,n)->new Receipt(r.getString(1),json.readValue(r.getString(2),Ack.class)),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(UUID c,UUID node,String actor,UUID request,Receipt r){jdbc.update("insert into module_operation_binding_receipt(campaign_id,node_id,actor,request_id,fingerprint,result) values (?,?,?,?,?,?::jsonb)",c,node,actor,request,r.fingerprint(),json.writeValueAsString(r.result()));}
}
