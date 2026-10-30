package io.dndroll.persistence;
import io.dndroll.application.InstabilityHealingService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class JdbcInstabilityHealingStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcInstabilityHealingStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public void append(UUID c,Entry e){jdbc.update("insert into instability_healing_history(campaign_id,target_id,version,origin_version,damage_version,snapshot) values (?,?,?,?,?,?::jsonb)",c,e.id(),e.version(),e.originVersion(),e.damageVersion(),json.writeValueAsString(e));}
    public List<Entry> history(UUID c,UUID id){return jdbc.query("select snapshot from instability_healing_history where campaign_id=? and target_id=? order by version",(rs,n)->json.readValue(rs.getString(1),Entry.class),c,id);}
}
