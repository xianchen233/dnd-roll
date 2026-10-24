package io.dndroll.persistence;

import io.dndroll.application.TimelineService.*;
import io.dndroll.application.TimelineImages;
import io.dndroll.application.AssetService;
import io.dndroll.domain.DomainException;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcTimelineStore implements Store,TimelineImages.Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcTimelineStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public Message message(UUID c,UUID id){var r=jdbc.query("select data from rp_message where campaign_id=? and id=?",(rs,n)->json.readValue(rs.getString(1),Message.class),c,id);return r.isEmpty()?null:r.getFirst();}
    public void save(UUID c,Message m,boolean create){
        String data=json.writeValueAsString(m);
        if(create){jdbc.update("insert into rp_message(id,campaign_id,character_id,kind,data) values (?,?,?,?,?::jsonb)",m.id(),c,m.characterId().isEmpty()?null:UUID.fromString(m.characterId()),m.kind(),data);jdbc.update("insert into timeline_entry(id,campaign_id,kind,message_id,at) values (?,?,?,?,?)",m.id(),c,m.kind(),m.id(),Timestamp.from(m.at()));}
        else jdbc.update("update rp_message set data=?::jsonb where campaign_id=? and id=?",data,c,m.id());
        jdbc.update("insert into rp_message_history(message_id,version,data) values (?,?,?::jsonb)",m.id(),m.version(),data);
        for(var image:m.attachments())jdbc.update("insert into rp_attachment_reference(campaign_id,message_id,version,attachment_id) values (?,?,?,?)",c,m.id(),m.version(),image);
    }
    public List<Message> history(UUID id){return jdbc.query("select data from rp_message_history where message_id=? order by version",(rs,n)->json.readValue(rs.getString(1),Message.class),id);}
    private Row row(java.sql.ResultSet rs,int n)throws java.sql.SQLException{return new Row(rs.getObject("id",UUID.class),rs.getString("kind"),rs.getTimestamp("at").toInstant(),rs.getString("session_id")==null?"":rs.getString("session_id"));}
    public Row row(UUID c,UUID id){var rows=jdbc.query("select * from timeline_entry where campaign_id=? and id=?",this::row,c,id);return rows.isEmpty()?null:rows.getFirst();}
    public boolean readable(UUID c,UUID id,boolean dm){return Boolean.TRUE.equals(jdbc.queryForObject("""
        select exists(select 1 from timeline_entry e
        left join rp_message m on m.id=e.message_id left join dice_roll r on r.id=e.roll_id left join combat_action a on a.id=e.action_id
        left join character_card ch on ch.id=coalesce(m.character_id,r.character_id,a.character_id) and ch.campaign_id=e.campaign_id
        left join character_card target on target.id=a.target_id and target.campaign_id=e.campaign_id
        where e.campaign_id=? and e.id=? and (? or (
            (coalesce(m.character_id,r.character_id,a.character_id) is null or (ch.id is not null and not ch.hidden and not ch.import_review))
            and (r.id is null or r.visibility='PUBLIC')
            and (a.id is null or (a.visibility='PUBLIC' and target.id is not null and not target.hidden and not target.import_review)))))
        """,Boolean.class,c,id,dm));}
    public List<Row> list(UUID c,boolean dm,UUID before,Filter f,int limit){return jdbc.query("""
        select e.* from timeline_entry e
        left join rp_message m on m.id=e.message_id
        left join dice_roll r on r.id=e.roll_id
        left join combat_action a on a.id=e.action_id
        left join character_card ch on ch.id=coalesce(m.character_id,r.character_id,a.character_id) and ch.campaign_id=e.campaign_id
        left join character_card target on target.id=a.target_id and target.campaign_id=e.campaign_id
        where e.campaign_id=? and (? or (
            (coalesce(m.character_id,r.character_id,a.character_id) is null or (ch.id is not null and not ch.hidden and not ch.import_review))
            and (r.id is null or r.visibility='PUBLIC')
            and (a.id is null or (a.visibility='PUBLIC' and target.id is not null and not target.hidden and not target.import_review))))
        and (?::uuid is null or e.ordinal < (select ordinal from timeline_entry where campaign_id=? and id=?))
        and strpos(lower(concat(m.data->>'text',' ',m.data->>'name',' ',r.data->>'name',' ',r.data->'result'->>'expression',' ',a.data->>'name',' ',a.data->>'targetName',' ',a.data->'attack'->>'name')),lower(?))>0
        and (?='' or e.kind=?)
        and (?::uuid is null or coalesce(m.character_id,r.character_id,a.character_id)=?::uuid or a.target_id=?::uuid)
        and (?::uuid is null or e.session_id=?::uuid)
        and (?::timestamptz is null or e.at>=?::timestamptz)
        and (?::timestamptz is null or e.at<=?::timestamptz)
        order by e.ordinal desc limit ?
        """,this::row,c,dm,before,c,before,f.q(),f.kind(),f.kind(),nullable(f.characterId()),nullable(f.characterId()),nullable(f.characterId()),nullable(f.sessionId()),nullable(f.sessionId()),nullable(f.from()),nullable(f.from()),nullable(f.to()),nullable(f.to()),limit);}
    private String nullable(String value){return value.isEmpty()?null:value;}
    public List<Session> sessions(UUID c){return jdbc.query("select id,name,archived from timeline_session where campaign_id=? order by id",(rs,n)->new Session(rs.getObject(1,UUID.class),rs.getString(2),rs.getBoolean(3)),c);}
    public Session session(UUID c,SessionCommand command){var active=sessions(c).stream().filter(s->!s.archived()).findFirst();if(command.kind().equals("START")){if(active.isPresent())throw new DomainException("VERSION_CONFLICT",409,"Archive current session first");var s=new Session(UUID.randomUUID(),command.name(),false);jdbc.update("insert into timeline_session(id,campaign_id,name) values (?,?,?)",s.id(),c,s.name());return s;}var old=active.orElseThrow(()->new DomainException("VERSION_CONFLICT",409,"No active session"));jdbc.update("update timeline_session set archived=true where id=?",old.id());return new Session(old.id(),old.name(),true);}
    public TimelineImages.Image image(UUID c,UUID id){var rows=jdbc.query("select data,actor,storage_key,sha256 from rp_attachment where campaign_id=? and id=?",(rs,n)->new TimelineImages.Image(json.readValue(rs.getString(1),AssetService.Asset.class),rs.getString(2),rs.getString(3),rs.getString(4)),c,id);return rows.isEmpty()?null:rows.getFirst();}
    public void saveImage(UUID c,TimelineImages.Image i){jdbc.update("insert into rp_attachment(id,campaign_id,actor,storage_key,sha256,data) values (?,?,?,?,?,?::jsonb)",i.metadata().id(),c,i.actor(),UUID.fromString(i.storageKey()),i.sha256(),json.writeValueAsString(i.metadata()));}
    public boolean readableImage(UUID c,UUID id,String actor,boolean dm){return Boolean.TRUE.equals(jdbc.queryForObject("""
        select exists(select 1 from rp_attachment i where i.campaign_id=? and i.id=? and (
            (i.actor=? and not exists(select 1 from rp_attachment_reference ref where ref.attachment_id=i.id))
            or exists(select 1 from rp_attachment_reference ref join rp_message m on m.id=ref.message_id
                left join character_card ch on ch.id=m.character_id and ch.campaign_id=m.campaign_id
                where ref.attachment_id=i.id and (? or m.character_id is null or (ch.id is not null and not ch.hidden and not ch.import_review)))))
        """,Boolean.class,c,id,actor,dm));}
    public Receipt receipt(String ns,String actor,UUID id){var rows=jdbc.query("select fingerprint,result_id,result_version from timeline_command_receipt where namespace=? and actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new Ack(rs.getObject(2,UUID.class),rs.getLong(3))),ns,actor,id);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(String ns,UUID c,String actor,UUID id,Receipt r){jdbc.update("insert into timeline_command_receipt(namespace,campaign_id,actor,request_id,fingerprint,result_id,result_version) values (?,?,?,?,?,?,?)",ns,c,actor,id,r.fingerprint(),r.result().id(),r.result().version());}
}
