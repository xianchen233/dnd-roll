package io.dndroll.persistence;
import io.dndroll.application.StabilizationDamageService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class JdbcStabilizationDamageStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcStabilizationDamageStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public void append(UUID c,Entry e){jdbc.update("insert into stabilization_damage_history(campaign_id,target_id,version,snapshot) values (?,?,?,?::jsonb)",c,e.id(),e.version(),json.writeValueAsString(e));}
    public List<Entry> history(UUID c,UUID id){return jdbc.query("select snapshot from stabilization_damage_history where campaign_id=? and target_id=? order by version",(rs,n)->json.readValue(rs.getString(1),Entry.class),c,id);}
}
