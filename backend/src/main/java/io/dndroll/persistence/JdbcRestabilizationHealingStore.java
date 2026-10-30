package io.dndroll.persistence;
import io.dndroll.application.RestabilizationHealingService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class JdbcRestabilizationHealingStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcRestabilizationHealingStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public void append(UUID c,Entry e){jdbc.update("insert into restabilization_healing_history(campaign_id,target_id,version,origin_version,snapshot) values (?,?,?,?,?::jsonb)",c,e.id(),e.version(),e.originVersion(),json.writeValueAsString(e));}
    public List<Entry> history(UUID c,UUID id){return jdbc.query("select snapshot from restabilization_healing_history where campaign_id=? and target_id=? order by version",(rs,n)->json.readValue(rs.getString(1),Entry.class),c,id);}
}
