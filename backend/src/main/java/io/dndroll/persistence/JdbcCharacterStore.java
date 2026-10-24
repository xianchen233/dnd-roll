package io.dndroll.persistence;

import io.dndroll.application.CharacterService.*;
import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcCharacterStore implements Store {
    private final JdbcTemplate jdbc;private final JdbcSceneStore scenes;private final JsonMapper json;
    public JdbcCharacterStore(JdbcTemplate jdbc,JdbcSceneStore scenes,JsonMapper json){this.jdbc=jdbc;this.scenes=scenes;this.json=json;}
    public <T>T actorLocked(String actor,Supplier<T> work){return scenes.actorLocked(actor,work);}public <T>T locked(UUID campaign,Supplier<T> work){return scenes.locked(campaign,work);}public Role role(UUID c,String actor){return scenes.role(c,actor);}
    public long campaignVersion(UUID c){return jdbc.queryForObject("select version from campaign where id=?",Long.class,c);}public void advanceCampaign(UUID c){jdbc.update("update campaign set version=version+1 where id=?",c);}
    private Card map(ResultSet rs,int n)throws SQLException{UUID id=rs.getObject("id",UUID.class);return new Card(id,rs.getObject("campaign_id",UUID.class),rs.getLong("version"),rs.getBoolean("hidden"),rs.getBoolean("share_full"),rs.getBoolean("import_review"),jdbc.queryForList("select username from character_controller where character_id=? order by username",String.class,id),json.readValue(rs.getString("sheet"),CharacterSheet.class));}
    public List<Card> cards(UUID c){return jdbc.query("select * from character_card where campaign_id=? order by id",this::map,c);}public Card card(UUID c,UUID id){var r=jdbc.query("select * from character_card where campaign_id=? and id=?",this::map,c,id);return r.isEmpty()?null:r.getFirst();}
    public void save(Card c,boolean create){
        if(create)jdbc.update("insert into character_card(id,campaign_id,version,hidden,share_full,import_review,sheet) values (?,?,?,?,?,?,?::jsonb)",c.id(),c.campaignId(),c.version(),c.hidden(),c.shareFull(),c.importReview(),json.writeValueAsString(c.sheet()));
        else jdbc.update("update character_card set version=?,hidden=?,share_full=?,import_review=?,sheet=?::jsonb where id=? and campaign_id=?",c.version(),c.hidden(),c.shareFull(),c.importReview(),json.writeValueAsString(c.sheet()),c.id(),c.campaignId());
        jdbc.update("delete from character_controller where character_id=?",c.id());for(String user:c.controllers())jdbc.update("insert into character_controller(campaign_id,character_id,username) values (?,?,?)",c.campaignId(),c.id(),user);
    }
    public List<History> history(UUID id){return jdbc.query("select * from character_history where character_id=? order by version",(rs,n)->new History(rs.getLong("version"),rs.getString("actor"),rs.getString("kind"),rs.getString("reason"),rs.getTimestamp("at").toInstant(),rs.getString("before_state")==null?null:json.readValue(rs.getString("before_state"),Card.class),json.readValue(rs.getString("after_state"),Card.class)),id);}
    public void history(History h){jdbc.update("insert into character_history(character_id,version,actor,kind,reason,at,before_state,after_state) values (?,?,?,?,?,?,?::jsonb,?::jsonb)",h.after().id(),h.version(),h.actor(),h.kind(),h.reason(),Timestamp.from(h.at()),h.before()==null?null:json.writeValueAsString(h.before()),json.writeValueAsString(h.after()));}
    public Receipt receipt(String actor,UUID request){var r=jdbc.query("select fingerprint,result_id,result_version from character_command_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new Ack(rs.getObject(2,UUID.class),rs.getLong(3))),actor,request);return r.isEmpty()?null:r.getFirst();}
    public void receipt(String actor,UUID request,UUID c,Receipt r){jdbc.update("insert into character_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values (?,?,?,?,?,?)",actor,request,c,r.fingerprint(),r.result().id(),r.result().version());}
    public List<Placement> placements(UUID c,UUID id){return jdbc.query("select id,scene_id,version from token where campaign_id=? and character_id=? order by id",(rs,n)->new Placement(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getLong(3)),c,id);}
    public void link(UUID c,UUID id,UUID token,long version){var r=jdbc.queryForList("select scene_id,version from token where campaign_id=? and id=?",c,token);if(r.isEmpty())throw new DomainException("NOT_FOUND",404,"Token not found");AccountPolicy.version(((Number)r.getFirst().get("version")).longValue(),version);jdbc.update("update token set character_id=?,version=version+1 where id=?",id,token);jdbc.update("update scene set version=version+1 where id=?",r.getFirst().get("scene_id"));}
    public void revokeMember(UUID campaign,String username,String actor){
        for(Card old:cards(campaign))if(old.controllers().contains(username)){
            var next=new Card(old.id(),campaign,old.version()+1,old.hidden(),old.shareFull(),old.importReview(),old.controllers().stream().filter(u->!u.equals(username)).toList(),old.sheet());
            save(next,false);history(new History(next.version(),actor,"MEMBER_REMOVED","Campaign membership removed",java.time.Instant.now(),old,next));
        }
    }
}
