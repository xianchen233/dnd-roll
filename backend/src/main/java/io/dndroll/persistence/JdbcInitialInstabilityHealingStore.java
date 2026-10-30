package io.dndroll.persistence;
import io.dndroll.application.InitialInstabilityHealingService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class JdbcInitialInstabilityHealingStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcInitialInstabilityHealingStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public void append(UUID c,Entry e){jdbc.update("insert into initial_instability_healing_history(campaign_id,target_id,version,damage_version,snapshot) values (?,?,?,?,?::jsonb)",c,e.id(),e.version(),e.damageVersion(),json.writeValueAsString(e));}
    public List<Entry> history(UUID c,UUID id){return jdbc.query("select snapshot from initial_instability_healing_history where campaign_id=? and target_id=? order by version",(rs,n)->json.readValue(rs.getString(1),Entry.class),c,id);}
}
