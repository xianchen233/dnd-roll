package io.dndroll.persistence;

import io.dndroll.application.TurnService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcTurnStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcTurnStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public State state(UUID c){var rows=jdbc.query("select data from campaign_combat where campaign_id=?",(rs,n)->json.readValue(rs.getString(1),State.class),c);return rows.isEmpty()?new State(0,"IDLE",0,List.of(),"",List.of(),0,""):rows.getFirst();}
    public void state(UUID c,State s){jdbc.update("insert into campaign_combat(campaign_id,data) values (?,?::jsonb) on conflict(campaign_id) do update set data=excluded.data",c,json.writeValueAsString(s));}
    public List<History> history(UUID c){return jdbc.query("select data from combat_turn_history where campaign_id=? order by version",(rs,n)->json.readValue(rs.getString(1),History.class),c);}
    public void history(UUID c,History h){jdbc.update("insert into combat_turn_history(campaign_id,version,data) values (?,?,?::jsonb)",c,h.after().version(),json.writeValueAsString(h));}
    public UUID characterForToken(UUID c,UUID token){var rows=jdbc.query("select character_id from token where campaign_id=? and id=?",(rs,n)->rs.getObject(1,UUID.class),c,token);return rows.isEmpty()?null:rows.getFirst();}
}
