package io.dndroll.backup;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.sql.*;
import tools.jackson.databind.json.JsonMapper;
/** Offline, private, non-executable identity proposal. */
public final class CampaignRemapPlanCommand {
    private static final int MAX_IDENTITIES=10000,MAX_REFERENCES=100000,MAX_OCCUPIED=1000000;
    static String predicate(String table){return switch(CampaignRemapPlan.scope(table)){
        case "GLOBAL_EXCLUDED"->null;case "CAMPAIGN_ID"->"id=?";
        case "PARENT_CHARACTER"->"character_id in (select id from public.character_card where campaign_id=?)";
        case "PARENT_NOTE"->"note_id in (select id from public.campaign_note where campaign_id=?)";
        case "PARENT_MESSAGE"->"message_id in (select id from public.rp_message where campaign_id=?)";
        default->"campaign_id=?";
    };}
    public static CampaignRemapPlan.Report plan(Path archive,String url,String username,String password,Path assets,UUID source,UUID target,UUID seed)throws IOException{
        try(var connection=DriverManager.getConnection(url,username,password)){
            connection.setReadOnly(true);connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);connection.setAutoCommit(false);
            var report=plan(connection,archive,assets,source,target,seed);connection.rollback();return report;
        }catch(Exception failure){throw new IOException("Campaign identity proposal failed; verify trusted archive, isolated database, source scope, assets and capacity");}
    }
    static CampaignRemapPlan.Report plan(Connection connection,Path archive,Path assets,UUID source,UUID target,UUID seed)throws Exception{
            var inspected=RecoveryAuditCommand.inspect(connection,archive,assets,RecoveryInventory.Mode.SAME_ID);if(!inspected.result().status().equals("VERIFIED"))throw new IOException();
            var identities=new ArrayList<CampaignRemapPlan.Identity>();var references=new ArrayList<CampaignRemapPlan.Reference>();var occupied=new HashSet<UUID>();var tables=new ArrayList<CampaignRemapPlan.Table>();var aliases=new ArrayList<CampaignRemapPlan.Alias>();
            for(String table:RecoveryInventory.catalog().keySet()){
                var columns=CampaignRemapPlan.columns(table);String relation="public.\""+table+"\"";
                for(var column:columns)if(column.type().equals("uuid"))try(var st=connection.createStatement()){
                    st.setFetchSize(500);try(var rows=st.executeQuery("select distinct \""+column.name()+"\" from "+relation+" where \""+column.name()+"\" is not null limit "+(MAX_OCCUPIED+1))){while(rows.next()){occupied.add(rows.getObject(1,UUID.class));if(occupied.size()>MAX_OCCUPIED)throw new IOException();}}
                }
                String where=predicate(table);long count=0;
                if(where!=null){
                    try(var st=connection.prepareStatement("select count(*) from "+relation+" where "+where)){st.setObject(1,source);try(var rows=st.executeQuery()){rows.next();count=rows.getLong(1);}}
                    boolean version=columns.stream().anyMatch(c->c.name().equals("version"));
                    if(CampaignRemapPlan.RUNTIME.contains(table))try(var st=connection.prepareStatement("select id"+(version?",version":"")+" from "+relation+" where "+where+" limit "+(MAX_IDENTITIES+1))){st.setObject(1,source);try(var rows=st.executeQuery()){while(rows.next()){identities.add(new CampaignRemapPlan.Identity(table,rows.getObject(1,UUID.class),version?rows.getLong(2):null));if(identities.size()>MAX_IDENTITIES)throw new IOException();}}}
                    for(var column:columns)if(column.policy().equals("RUNTIME_REFERENCE"))try(var st=connection.prepareStatement("select distinct \""+column.name()+"\" from "+relation+" where ("+where+") and \""+column.name()+"\" is not null limit "+(MAX_REFERENCES+1))){st.setObject(1,source);try(var rows=st.executeQuery()){while(rows.next()){references.add(new CampaignRemapPlan.Reference(table,column.name(),column.namespace(),rows.getObject(1,UUID.class)));if(references.size()>MAX_REFERENCES)throw new IOException();}}}
                }
                tables.add(new CampaignRemapPlan.Table(table,CampaignRemapPlan.scope(table),count,columns));
            }
            try(var st=connection.prepareStatement("select id,message_id,roll_id,action_id from public.timeline_entry where campaign_id=?")){st.setObject(1,source);try(var rows=st.executeQuery()){while(rows.next()){UUID id=rows.getObject(1,UUID.class);for(int i=2;i<=4;i++){UUID owner=rows.getObject(i,UUID.class);if(owner!=null)aliases.add(new CampaignRemapPlan.Alias(id,switch(i){case 2->"rp_message";case 3->"dice_roll";default->"combat_action";},owner));}}}}
            return CampaignRemapPlan.plan(source,target,seed,inspected.manifest().database().sha256(),identities,references,occupied,tables,aliases);
    }
    public static void write(CampaignRemapPlan.Report report,Path output)throws IOException{Files.write(output,JsonMapper.builder().build().writeValueAsBytes(report),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);}
    public static void main(String[] args){
        try{if(args.length!=7||!args[6].equals("REVIEW_ONLY"))throw new IllegalArgumentException();Path output=Path.of(args[2]);if(Files.exists(output,LinkOption.NOFOLLOW_LINKS))throw new IOException();
            var report=plan(Path.of(args[0]),required("RECOVERY_AUDIT_DATABASE_URL"),required("RECOVERY_AUDIT_DATABASE_USERNAME"),required("RECOVERY_AUDIT_DATABASE_PASSWORD"),Path.of(args[1]),UUID.fromString(args[3]),UUID.fromString(args[4]),UUID.fromString(args[5]));write(report,output);
            System.out.println("CAMPAIGN IDENTITY PROPOSAL BLOCKED: "+report.tables().size()+" tables; "+report.mappings().size()+" relational identity suggestions; executable=false; no campaign was created.");System.exit(2);
        }catch(Exception failure){System.err.println("CAMPAIGN IDENTITY PROPOSAL FAILED: check trusted archive, isolated database, matching assets, identity inputs, capacity and new output path. Source fence unchanged.");System.exit(1);}
    }
    private static String required(String key){String value=System.getenv(key);if(value==null||value.isBlank())throw new IllegalArgumentException();return value;}
}
