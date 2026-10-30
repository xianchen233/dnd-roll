package io.dndroll.backup;
import java.nio.file.*;import java.sql.*;import java.util.*;import java.io.IOException;import tools.jackson.databind.json.JsonMapper;
/** Offline review only, in a single audited snapshot. */
public final class CampaignEvidenceRebindCommand {
 public static CampaignEvidenceRebind.Report inspect(Path archive,String url,String username,String password,Path assets,UUID source,UUID target,UUID seed)throws IOException{
  try(var c=DriverManager.getConnection(url,username,password)){c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
   var proposal=CampaignRemapPlanCommand.plan(c,archive,assets,source,target,seed);
   if(proposal.blockers().stream().anyMatch(b->!CampaignEvidenceRebind.allowedBlocker(b)))throw new IOException();
   var rows=CampaignEvidenceGraphCommand.read(c,source);var receipts=new ArrayList<CampaignEvidenceGraph.SourceReceipt>();try(var q=c.prepareStatement("select result_id,fingerprint,actor from dice_command_receipt where campaign_id=?")){q.setObject(1,source);try(var r=q.executeQuery()){while(r.next())receipts.add(new CampaignEvidenceGraph.SourceReceipt(r.getObject(1,UUID.class),r.getString(2),r.getString(3)));}}
   var occupied=new HashSet<UUID>();for(String table:RecoveryInventory.catalog().keySet())for(var column:CampaignRemapPlan.columns(table))if(column.type().equals("uuid"))try(var q=c.createStatement()){q.setFetchSize(500);try(var r=q.executeQuery("select distinct \""+column.name()+"\" from public.\""+table+"\" where \""+column.name()+"\" is not null limit 1000001")){while(r.next()){occupied.add(r.getObject(1,UUID.class));if(occupied.size()>1000000)throw new IOException();}}}
   var report=CampaignEvidenceRebind.inspect(proposal,rows,receipts,occupied);c.rollback();return report;
  }catch(Exception failure){throw new IOException("Evidence rebinding failed; verify trusted isolated inputs and proposal");}
 }
 public static void main(String[] args){try{if(args.length!=7||!args[6].equals("REVIEW_ONLY"))throw new IOException();Path out=Path.of(args[2]);if(Files.exists(out,LinkOption.NOFOLLOW_LINKS))throw new IOException();var report=inspect(Path.of(args[0]),System.getenv("RECOVERY_AUDIT_DATABASE_URL"),System.getenv("RECOVERY_AUDIT_DATABASE_USERNAME"),System.getenv("RECOVERY_AUDIT_DATABASE_PASSWORD"),Path.of(args[1]),UUID.fromString(args[3]),UUID.fromString(args[4]),UUID.fromString(args[5]));Files.write(out,JsonMapper.builder().build().writeValueAsBytes(report),StandardOpenOption.CREATE_NEW);System.out.println("EVIDENCE REBIND REVIEW BLOCKED: consistent="+report.consistent()+"; executable=false; no campaign created");System.exit(2);}catch(Exception failure){System.err.println("EVIDENCE REBIND REVIEW FAILED: verify trusted isolated inputs and new output path; source fence unchanged");System.exit(1);}}
}
