package io.dndroll.persistence;

import io.dndroll.application.ContentService.*;
import io.dndroll.domain.ContentEntry;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcContentStore implements Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;
    public JdbcContentStore(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
    public List<ContentEntry> entries(UUID c){return jdbc.query("select id,data from campaign_content_entry where campaign_id=?",(r,n)->new ContentEntry(r.getObject(1,UUID.class),json.readValue(r.getString(2),ContentEntry.Data.class),false,"CAMPAIGN","发布者声明有权与本团分享","本团自定义；不代表官方规则"),c);}
    public Policy policy(UUID c){var r=jdbc.query("select mode,approved_ids from campaign_content_settings where campaign_id=?",(rs,n)->new Policy(rs.getString(1),Arrays.asList(json.readValue(rs.getString(2),UUID[].class))),c);return r.isEmpty()?new Policy("REVIEW",List.of()):r.getFirst();}
    public void policy(UUID c,Policy p){jdbc.update("insert into campaign_content_settings(campaign_id,mode,approved_ids) values (?,?,?::jsonb) on conflict(campaign_id) do update set mode=excluded.mode,approved_ids=excluded.approved_ids",c,p.mode(),json.writeValueAsString(p.approvedIds()));}
    public void entry(UUID c,ContentEntry e){jdbc.update("insert into campaign_content_entry(id,campaign_id,data) values (?,?,?::jsonb)",e.id(),c,json.writeValueAsString(e.data()));}
    public Receipt receipt(String actor,UUID request){var r=jdbc.query("select fingerprint,result_id,result_version from content_command_receipt where actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new Ack(rs.getObject(2,UUID.class),rs.getLong(3))),actor,request);return r.isEmpty()?null:r.getFirst();}
    public void receipt(String actor,UUID request,UUID c,Receipt r){jdbc.update("insert into content_command_receipt(actor,request_id,campaign_id,fingerprint,result_id,result_version) values (?,?,?,?,?,?)",actor,request,c,r.fingerprint(),r.result().id(),r.result().version());}
}
