package io.dndroll.persistence;
import io.dndroll.application.RestabilizationService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class JdbcRestabilizationStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcRestabilizationStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public State state(UUID c,UUID id){var r=jdbc.query("select snapshot from restabilization_state where campaign_id=? and target_id=?",(rs,n)->json.readValue(rs.getString(1),State.class),c,id);return r.isEmpty()?new State(id,0,null):r.getFirst();}
    public void save(UUID c,State s){var snapshot=json.writeValueAsString(s);jdbc.update("insert into restabilization_state(campaign_id,target_id,version,origin_version,snapshot) values (?,?,?,?,?::jsonb) on conflict(campaign_id,target_id) do update set version=excluded.version,origin_version=excluded.origin_version,snapshot=excluded.snapshot",c,s.id(),s.version(),s.latest().originVersion(),snapshot);jdbc.update("insert into restabilization_history(campaign_id,target_id,version,origin_version,snapshot) values (?,?,?,?,?::jsonb)",c,s.id(),s.version(),s.latest().originVersion(),snapshot);}
    public List<State> history(UUID c,UUID id){return jdbc.query("select snapshot from restabilization_history where campaign_id=? and target_id=? order by version",(rs,n)->json.readValue(rs.getString(1),State.class),c,id);}
}
