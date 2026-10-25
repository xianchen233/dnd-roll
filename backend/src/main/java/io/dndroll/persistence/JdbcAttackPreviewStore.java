package io.dndroll.persistence;

import io.dndroll.application.AttackPreviewService.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcAttackPreviewStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcAttackPreviewStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public Preview get(UUID c,UUID id){var r=jdbc.query("select data from attack_preview where campaign_id=? and id=?",(rs,n)->json.readValue(rs.getString(1),Preview.class),c,id);return r.isEmpty()?null:r.getFirst();}
    public void save(UUID c,Preview p,boolean create){
        boolean published=p.events().stream().anyMatch(e->e.kind().equals("PUBLISH"));
        if(create)jdbc.update("insert into attack_preview(id,campaign_id,character_id,target_id,published,data) values (?,?,?,?,?,?::jsonb)",p.id(),c,p.evidence().source().id(),p.evidence().target().id(),published,json.writeValueAsString(p));
        else jdbc.update("update attack_preview set data=?::jsonb,published=? where campaign_id=? and id=?",json.writeValueAsString(p),published,c,p.id());
    }
    public List<Preview> list(UUID c,boolean published){return jdbc.query("select p.data from attack_preview p join character_card s on s.campaign_id=p.campaign_id and s.id=p.character_id join character_card t on t.campaign_id=p.campaign_id and t.id=p.target_id where p.campaign_id=? and (not ? or (p.published and not s.hidden and not s.import_review and not t.hidden and not t.import_review)) order by p.seq desc limit 50",(rs,n)->json.readValue(rs.getString(1),Preview.class),c,published);}
    public Receipt receipt(String actor,UUID request){var r=jdbc.query("select fingerprint,result from attack_preview_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),json.readValue(rs.getString(2),io.dndroll.application.CombatService.Ack.class)),actor,request);return r.isEmpty()?null:r.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into attack_preview_receipt(actor,request_id,campaign_id,fingerprint,result) values (?,?,?,?,?::jsonb)",actor,request,c,r.fingerprint(),json.writeValueAsString(r.result()));}
}
