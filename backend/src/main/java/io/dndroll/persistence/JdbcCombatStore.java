package io.dndroll.persistence;

import io.dndroll.application.CombatService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcCombatStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcCombatStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public Action action(UUID c,UUID id){var rows=jdbc.query("select data from combat_action where campaign_id=? and id=?",(rs,n)->json.readValue(rs.getString(1),Action.class),c,id);return rows.isEmpty()?null:rows.getFirst();}
    public void save(UUID c,Action a,boolean create){if(create)jdbc.update("insert into combat_action(id,campaign_id,character_id,target_id,visibility,data) values (?,?,?,?,?,?::jsonb)",a.id(),c,a.characterId(),a.targetId(),a.visibility(),json.writeValueAsString(a));else jdbc.update("update combat_action set data=?::jsonb where campaign_id=? and id=?",json.writeValueAsString(a),c,a.id());}
    public List<Action> list(UUID c,boolean dm,UUID before,int limit){return jdbc.query("select a.data from combat_action a join character_card s on s.id=a.character_id join character_card t on t.id=a.target_id where a.campaign_id=? and (? or (a.visibility='PUBLIC' and not s.hidden and not s.import_review and not t.hidden and not t.import_review)) and (?::uuid is null or a.seq<(select seq from combat_action where campaign_id=? and id=?)) order by a.seq desc limit ?",(rs,n)->json.readValue(rs.getString(1),Action.class),c,dm,before,c,before,limit);}
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,result_id,result_version from combat_command_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new Ack(rs.getObject(2,UUID.class),rs.getLong(3))),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into combat_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values (?,?,?,?,?,?)",actor,request,c,r.fingerprint(),r.result().id(),r.result().version());}
}
