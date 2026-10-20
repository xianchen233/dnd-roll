package io.dndroll.persistence;

import io.dndroll.application.InitiativeService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcInitiativeStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcInitiativeStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public State state(UUID c){var rows=jdbc.query("select data from campaign_initiative where campaign_id=?",(rs,n)->json.readValue(rs.getString(1),State.class),c);return rows.isEmpty()?new State(0,"EMPTY",List.of(),""):rows.getFirst();}
    public void state(UUID c,State state){jdbc.update("insert into campaign_initiative(campaign_id,data) values (?,?::jsonb) on conflict(campaign_id) do update set data=excluded.data",c,json.writeValueAsString(state));}
    public List<History> history(UUID c){return jdbc.query("select data from initiative_history where campaign_id=? order by version",(rs,n)->json.readValue(rs.getString(1),History.class),c);}
    public void history(UUID c,History h){jdbc.update("insert into initiative_history(campaign_id,version,data) values (?,?,?::jsonb)",c,h.version(),json.writeValueAsString(h));}
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,campaign_id,result_version from initiative_command_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new Ack(rs.getObject(2,UUID.class),rs.getLong(3))),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into initiative_command_receipt(actor,request_id,campaign_id,fingerprint,result_version) values (?,?,?,?,?)",actor,request,c,r.fingerprint(),r.result().version());}
}
