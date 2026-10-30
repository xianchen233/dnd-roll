package io.dndroll.persistence;

import io.dndroll.application.NotesService.*;
import io.dndroll.application.NoteImages;
import io.dndroll.application.AssetService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.*;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcNotesStore implements Store,NoteImages.Store {
    private final JdbcTemplate jdbc;private final JsonMapper json;private final AssetService.Files files;private final TransactionTemplate reconcile;
    public JdbcNotesStore(JdbcTemplate jdbc,JsonMapper json,AssetService.Files files,org.springframework.transaction.PlatformTransactionManager transactions){this.jdbc=jdbc;this.json=json;this.files=files;reconcile=new TransactionTemplate(transactions);reconcile.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);}
    public void copyImage(UUID c,NoteImages.Image image,byte[] bytes){
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Image publication requires the note transaction");
        String key=image.storageKey();TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){
            if(status==STATUS_ROLLED_BACK)files.discard(key);
            else if(status==STATUS_UNKNOWN)try{reconcile.execute(ignored->{jdbc.query("select id from campaign where id=? for update",(r,n)->r.getObject(1,UUID.class),c);boolean saved=Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from note_attachment where campaign_id=? and id=? and storage_key=?::uuid)",Boolean.class,c,image.metadata().id(),key));if(!saved)files.discard(key);return null;});}catch(RuntimeException unknown){/* Preserve opaque files when the commit outcome remains unknown. */}
        }});
        files.publish(key,bytes);saveImage(c,image);
    }
    public Note note(UUID c,UUID id){var rows=jdbc.query("select data from campaign_note where campaign_id=? and id=?",(rs,n)->json.readValue(rs.getString(1),Note.class),c,id);return rows.isEmpty()?null:rows.getFirst();}
    public void save(UUID c,Note note,boolean create){
        var data=json.writeValueAsString(note);
        if(create)jdbc.update("insert into campaign_note(id,campaign_id,visibility,data) values (?,?,?,?::jsonb)",note.id(),c,note.visibility(),data);
        else jdbc.update("update campaign_note set data=?::jsonb where campaign_id=? and id=?",data,c,note.id());
        jdbc.update("insert into note_history(note_id,version,data) values (?,?,?::jsonb)",note.id(),note.version(),data);
        for(var image:note.attachments())jdbc.update("insert into note_attachment_reference(campaign_id,note_id,version,attachment_id,visibility) values (?,?,?,?,?)",c,note.id(),note.version(),image,note.visibility());
    }
    public List<Note> history(UUID id){return jdbc.query("select data from note_history where note_id=? order by version",(rs,n)->json.readValue(rs.getString(1),Note.class),id);}
    public List<Note> list(UUID c,boolean dm,UUID before,Filter f,int limit){return jdbc.query("""
        select data from campaign_note n where campaign_id=? and (? or visibility='TEAM')
        and (?='' or visibility=?) and strpos(lower(concat(data->>'title',' ',data->>'text')),lower(?))>0
        and (?::uuid is null or ordinal<(select ordinal from campaign_note where campaign_id=? and id=?))
        order by ordinal desc limit ?
        """,(rs,n)->json.readValue(rs.getString(1),Note.class),c,dm,f.visibility(),f.visibility(),f.q(),before,c,before,limit);}
    public Receipt receipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,result_id,result_version from note_command_receipt where namespace='notes' and actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new Ack(rs.getObject(2,UUID.class),rs.getLong(3))),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void receipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into note_command_receipt(namespace,campaign_id,actor,request_id,fingerprint,result_id,result_version) values ('notes',?,?,?,?,?,?)",c,actor,request,r.fingerprint(),r.result().id(),r.result().version());}
    public NoteImages.Image image(UUID c,UUID id){var rows=jdbc.query("select data,actor,visibility,storage_key,sha256 from note_attachment where campaign_id=? and id=?",(rs,n)->new NoteImages.Image(json.readValue(rs.getString(1),AssetService.Asset.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)),c,id);return rows.isEmpty()?null:rows.getFirst();}
    public void saveImage(UUID c,NoteImages.Image image){jdbc.update("insert into note_attachment(id,campaign_id,actor,visibility,storage_key,sha256,data) values (?,?,?,?,?,?,?::jsonb)",image.metadata().id(),c,image.actor(),image.visibility(),UUID.fromString(image.storageKey()),image.sha256(),json.writeValueAsString(image.metadata()));}
    public boolean readableImage(UUID c,UUID id,String actor,boolean dm){return Boolean.TRUE.equals(jdbc.queryForObject("""
        select exists(select 1 from note_attachment i where i.campaign_id=? and i.id=? and (? or i.visibility='TEAM') and (
            (i.actor=? and not exists(select 1 from note_attachment_reference r where r.attachment_id=i.id))
            or exists(select 1 from note_attachment_reference r join campaign_note n on n.campaign_id=r.campaign_id and n.id=r.note_id
                where r.attachment_id=i.id and (? or n.visibility='TEAM'))))
        """,Boolean.class,c,id,dm,actor,dm));}
    public boolean referencedBy(UUID c,UUID image,UUID note){return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from note_attachment_reference where campaign_id=? and attachment_id=? and note_id=?)",Boolean.class,c,image,note));}
    public Receipt uploadReceipt(String actor,UUID request){var rows=jdbc.query("select fingerprint,result_id,result_version from note_command_receipt where namespace='attachments' and actor=? and request_id=?",(rs,n)->new Receipt(rs.getString(1),new Ack(rs.getObject(2,UUID.class),rs.getLong(3))),actor,request);return rows.isEmpty()?null:rows.getFirst();}
    public void uploadReceipt(UUID c,String actor,UUID request,Receipt r){jdbc.update("insert into note_command_receipt(namespace,campaign_id,actor,request_id,fingerprint,result_id,result_version) values ('attachments',?,?,?,?,?,?)",c,actor,request,r.fingerprint(),r.result().id(),r.result().version());}
}
