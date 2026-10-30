package io.dndroll.persistence;
import io.dndroll.application.StabilizationService.*;
import io.dndroll.application.CharacterService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class JdbcStabilizationStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcStabilizationStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public State state(UUID c,UUID id){var r=jdbc.query("select snapshot from stabilization_state where campaign_id=? and target_id=?",(rs,n)->json.readValue(rs.getString(1),State.class),c,id);return r.isEmpty()?new State(id,0,null):r.getFirst();}
    public void save(UUID c,State s){var snapshot=json.writeValueAsString(s);jdbc.update("insert into stabilization_state(campaign_id,target_id,version,snapshot) values (?,?,?,?::jsonb) on conflict(campaign_id,target_id) do update set version=excluded.version,snapshot=excluded.snapshot",c,s.id(),s.version(),snapshot);jdbc.update("insert into stabilization_history(campaign_id,target_id,version,snapshot) values (?,?,?,?::jsonb)",c,s.id(),s.version(),snapshot);}
    public List<State> history(UUID c,UUID id){return jdbc.query("select snapshot from stabilization_history where campaign_id=? and target_id=? order by version",(rs,n)->json.readValue(rs.getString(1),State.class),c,id);}
    public Receipt receipt(String actor,UUID request){var r=jdbc.query("select fingerprint,result_id,result_version from stabilization_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new CharacterService.Ack(rs.getObject(2,UUID.class),rs.getLong(3))),actor,request);return r.isEmpty()?null:r.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into stabilization_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values (?,?,?,?,?,?)",actor,request,c,r.fingerprint(),r.result().id(),r.result().version());}
}
