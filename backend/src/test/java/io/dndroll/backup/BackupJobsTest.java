package io.dndroll.backup;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BackupJobsTest {
    @TempDir Path root;
    final JsonMapper json=JsonMapper.builder().build();
    BackupJobs.Job job(String kind,String state,int day){return new BackupJobs.Job(UUID.randomUUID(),"operator",day,kind,state,"2020-01-"+String.format("%02d",day)+"T00:00:00Z","",10);}
    void waitFor(BackupJobs jobs,String state)throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(System.nanoTime()<until&&jobs.status("operator").jobs().stream().noneMatch(j->j.state().equals(state)))Thread.sleep(10);
        assertTrue(jobs.status("operator").jobs().stream().anyMatch(j->j.state().equals(state)));
    }
    @Test void dailyRetainsSevenAutomaticArchivesAndNeverTouchesManualOrLiveHistory()throws Exception {
        var old=new ArrayList<BackupJobs.Job>();for(int i=1;i<=8;i++)old.add(job("AUTOMATIC","COMPLETE",i));
        var manual=job("MANUAL","COMPLETE",9);old.add(manual);
        for(var job:old)Files.writeString(root.resolve(job.id()+".zip"),"retained archive");
        Files.write(root.resolve("jobs.json"),json.writeValueAsBytes(new BackupJobs.State(9,old)));
        var live=Files.writeString(root.resolve("live-history-sentinel"),"RP history and images");
        var engine=mock(PostgresBackup.class);doAnswer(i->{Files.writeString(i.getArgument(1),"new archive");return null;}).when(engine).create(any(),any());
        var jobs=new BackupJobs(new WriteAdmission(),engine,root.toString(),"operator",true,7);
        try {
            jobs.daily();waitFor(jobs,"EXPIRED");jobs.daily();
            var status=jobs.status("operator");assertEquals(7,status.jobs().stream().filter(j->j.kind().equals("AUTOMATIC")&&j.state().equals("COMPLETE")).count());
            assertEquals(10,status.jobs().size(),"one automatic job per UTC day, no repeated tick");
            assertFalse(Files.exists(root.resolve(old.getFirst().id()+".zip")));assertTrue(Files.exists(root.resolve(manual.id()+".zip")));
            assertEquals("RP history and images",Files.readString(live));
        } finally {jobs.close();}
    }
    @Test void failedJobsPersistAndRetryCannotCreateAnotherBackupOrBypassAuthority()throws Exception {
        var engine=mock(PostgresBackup.class);doThrow(new java.io.IOException("private internal path" )).when(engine).create(any(),any());
        var gate=new WriteAdmission();var jobs=new BackupJobs(gate,engine,root.toString(),"operator",false,7);
        UUID id=UUID.randomUUID();
        try {
            var ack=jobs.start("operator",id,0);waitFor(jobs,"FAILED");
            assertEquals(ack,jobs.start("operator",id,0));assertFalse(gate.paused());
            assertThrows(io.dndroll.domain.DomainException.class,()->jobs.start("other-dm",id,0));
            assertFalse(json.writeValueAsString(jobs.status("operator")).contains("private internal path"));
        } finally {jobs.close();}
        var recovered=new BackupJobs(new WriteAdmission(),engine,root.toString(),"operator",false,7);
        try{assertEquals("FAILED",recovered.status("operator").jobs().getFirst().state());assertEquals(id,recovered.start("operator",id,0).id());verify(engine,times(1)).create(any(),any());}
        finally{recovered.close();}
    }
    @Test void restartMarksInterruptedWorkAndKeepsOriginalReceipt()throws Exception {
        var pending=job("MANUAL","RUNNING",1);
        Files.write(root.resolve("jobs.json"),json.writeValueAsBytes(new BackupJobs.State(2,List.of(pending))));
        var engine=mock(PostgresBackup.class);var jobs=new BackupJobs(new WriteAdmission(),engine,root.toString(),"operator",false,7);
        try{assertEquals("INTERRUPTED",jobs.status("operator").jobs().getFirst().state());assertEquals(pending.id(),jobs.start("operator",pending.id(),1).id());verifyNoInteractions(engine);}
        finally{jobs.close();}
    }
    @Test void migrationKeepsSourceFencedAfterCompletionFailureAndRestart()throws Exception {
        var engine=mock(PostgresBackup.class);doAnswer(i->{Files.writeString(i.getArgument(1),"migration archive");return null;}).when(engine).create(any(),any());
        var gate=new WriteAdmission();var jobs=new BackupJobs(gate,engine,root.toString(),"operator",false,7);
        UUID id=UUID.randomUUID();
        try{jobs.startMigration("operator",id,0);waitFor(jobs,"COMPLETE");assertTrue(gate.paused(),"migration must not reopen the source");assertThrows(IllegalStateException.class,gate::enter);}
        finally{jobs.close();}
        var after=new WriteAdmission();var restarted=new BackupJobs(after,engine,root.toString(),"operator",false,7);
        try{assertTrue(after.paused());assertThrows(IllegalStateException.class,after::enter);assertEquals(id,restarted.startMigration("operator",id,0).id());}
        finally{restarted.close();}
    }
    @Test void rotationFailureDoesNotInvalidateANewlyCompletedBackup()throws Exception {
        var old=new ArrayList<BackupJobs.Job>();for(int i=1;i<=7;i++)old.add(job("AUTOMATIC","COMPLETE",i));
        for(var j:old)Files.writeString(root.resolve(j.id()+".zip"),"old backup");
        Path blocked=root.resolve(old.getFirst().id()+".zip");Files.delete(blocked);Files.createDirectory(blocked);Files.writeString(blocked.resolve("sentinel"),"cannot delete this directory");
        Files.write(root.resolve("jobs.json"),json.writeValueAsBytes(new BackupJobs.State(7,old)));
        var engine=mock(PostgresBackup.class);doAnswer(i->{Files.writeString(i.getArgument(1),"valid new backup");return null;}).when(engine).create(any(),any());
        var jobs=new BackupJobs(new WriteAdmission(),engine,root.toString(),"operator",true,7);
        try {
            jobs.daily();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(System.nanoTime()<until&&jobs.status("operator").jobs().getLast().state().equals("RUNNING"))Thread.sleep(10);
            var status=jobs.status("operator");assertEquals("COMPLETE",status.jobs().getLast().state());
            assertTrue(json.writeValueAsString(status).contains("\"retentionFailed\":true"));
            assertEquals("valid new backup",Files.readString(jobs.download("operator",status.jobs().getLast().id())));
        } finally {jobs.close();}
    }
    @Test void aSecondInstanceCannotRewriteTheActiveJobRegistry()throws Exception {
        var engine=mock(PostgresBackup.class);var first=new BackupJobs(new WriteAdmission(),engine,root.toString(),"operator",false,7);
        try{assertThrows(java.io.IOException.class,()->{var second=new BackupJobs(new WriteAdmission(),engine,root.toString(),"operator",false,7);second.close();});}
        finally{first.close();}
    }
    @Test void automaticRegistryWriteFailureIsVisibleInsteadOfSilentlyDroppingTheTask()throws Exception {
        var engine=mock(PostgresBackup.class);var gate=new WriteAdmission();var jobs=new BackupJobs(gate,engine,root.toString(),"operator",true,7);
        try{
            Files.createDirectory(root.resolve("jobs.json"));jobs.daily();
            var status=jobs.status("operator");assertEquals(1,status.jobs().size());assertEquals("FAILED",status.jobs().getFirst().state());
            assertFalse(gate.paused());verifyNoInteractions(engine);
        }finally{jobs.close();}
    }
}
