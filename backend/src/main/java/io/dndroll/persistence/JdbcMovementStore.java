package io.dndroll.persistence;

import io.dndroll.application.*;
import io.dndroll.application.MovementService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcMovementStore implements Store,SceneService.MovementAudit,TurnService.MovementTurns {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcMovementStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public Budget budget(UUID c,UUID character){var rows=jdbc.query("select data from movement_budget where campaign_id=? and character_id=?",(rs,n)->json.readValue(rs.getString(1),Budget.class),c,character);return rows.isEmpty()?null:rows.getFirst();}
    public void budget(UUID c,UUID character,Budget b){jdbc.update("insert into movement_budget(campaign_id,character_id,data) values (?,?,?::jsonb) on conflict(campaign_id,character_id) do update set data=excluded.data",c,character,json.writeValueAsString(b));}
    public String anchor(UUID c,UUID character){var rows=jdbc.queryForList("select id,scene_id,x,y,version,module_retired from token where campaign_id=? and character_id=? order by id",c,character);return DiceService.fingerprint(json,rows);}
    public boolean controls(UUID c,UUID token,String actor){return jdbc.queryForObject("select count(*) from token_controller tc join token t on t.id=tc.token_id where t.campaign_id=? and t.id=? and tc.username=?",Integer.class,c,token,actor)>0;}
    public boolean fiveFoot(UUID scene){var rows=jdbc.queryForList("select distance_per_cell from scene_map where scene_id=?",Double.class,scene);return rows.isEmpty()||rows.getFirst()==5.0;}
    public List<Journal> history(UUID c,UUID character){return jdbc.query("select data from movement_journal where campaign_id=? and character_id=? order by seq desc limit 50",(rs,n)->json.readValue(rs.getString(1),Journal.class),c,character);}
    public void journal(UUID c,UUID character,Journal j){jdbc.update("insert into movement_journal(id,campaign_id,character_id,data) values (?,?,?,?::jsonb)",j.id(),c,character,json.writeValueAsString(j));}
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,result from movement_path_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),json.readValue(rs.getString(2),Result.class)),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into movement_path_receipt(actor,request_id,campaign_id,fingerprint,result) values (?,?,?,?,?::jsonb)",actor,request,c,r.fingerprint(),json.writeValueAsString(r.result()));}
    public void unmeasured(UUID c,UUID token){
        var ids=jdbc.queryForList("select character_id from token where campaign_id=? and id=? and character_id is not null",UUID.class,c,token);if(ids.isEmpty())return;var id=ids.getFirst();var b=budget(c,id);
        if(b!=null)budget(c,id,new Budget(UUID.randomUUID().toString(),b.used(),b.allowance(),false,anchor(c,id)));
    }
    public void entered(UUID c,TurnService.History h){
        var a=h.after();var b=h.before();
        if(h.kind().equals("START"))for(UUID id:a.order())budget(c,id,new Budget(UUID.randomUUID().toString(),0,null,true,anchor(c,id)));
        boolean entry=Set.of("START","END_TURN","SKIP","SWITCH").contains(h.kind())&&a.status().equals("ACTIVE")&&(!b.status().equals("ACTIVE")||!a.currentId().equals(b.currentId())||a.round()!=b.round());
        if(entry){UUID id=UUID.fromString(a.currentId());budget(c,id,new Budget(UUID.randomUUID().toString(),0,null,true,anchor(c,id)));}
    }
}
