package io.dndroll.persistence;

import io.dndroll.application.DiceService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcDiceStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcDiceStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public Roll roll(UUID c,UUID id){var rows=jdbc.query("select data from dice_roll where campaign_id=? and id=?",(rs,n)->json.readValue(rs.getString(1),Roll.class),c,id);return rows.isEmpty()?null:rows.getFirst();}
    public void save(UUID c,Roll r){jdbc.update("insert into dice_roll(id,campaign_id,character_id,visibility,data) values (?,?,?,?,?::jsonb)",r.id(),c,r.characterId().isEmpty()?null:UUID.fromString(r.characterId()),r.visibility(),json.writeValueAsString(r));}
    public List<Roll> list(UUID c,boolean dm,UUID before,int limit){return jdbc.query("""
        select r.data from dice_roll r left join character_card ch on ch.campaign_id=r.campaign_id and ch.id=r.character_id
        where r.campaign_id=? and (? or (r.visibility='PUBLIC' and (r.character_id is null or (ch.id is not null and not ch.hidden and not ch.import_review))))
        and (?::uuid is null or r.ordinal < (select ordinal from dice_roll where campaign_id=? and id=?))
        order by r.ordinal desc limit ?
        """,(rs,n)->json.readValue(rs.getString(1),Roll.class),c,dm,before,c,before,limit);}
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,result_id from dice_command_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),rs.getObject(2,UUID.class)),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into dice_command_receipt(actor,request_id,campaign_id,fingerprint,result_id) values (?,?,?,?,?)",actor,request,c,r.fingerprint(),r.id());}
}
