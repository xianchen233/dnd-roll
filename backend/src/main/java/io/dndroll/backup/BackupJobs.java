package io.dndroll.backup;

import io.dndroll.domain.DomainException;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class BackupJobs {
    public record Job(UUID id,String actor,long expectedVersion,String kind,String state,String at,String finishedAt,long bytes) {}
    public record State(long version,List<Job> jobs,boolean retentionFailed) {
        public State(long version,List<Job> jobs){this(version,jobs,false);}
    }
    public record Status(long version,int retention,boolean paused,List<Job> jobs,boolean retentionFailed) {}
    public record Ack(UUID id,long version) {}
    private final Path root;private final WriteAdmission gate;private final PostgresBackup engine;
    private final String operator;private final boolean daily;private final int retention;
    private final JsonMapper json=JsonMapper.builder().build();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private java.nio.channels.FileChannel registryChannel;
    private java.nio.channels.FileLock registryLock;
    private State state=new State(0,new ArrayList<>());
    public BackupJobs(WriteAdmission gate,PostgresBackup engine,@Value("${backups.storage-root:${BACKUP_STORAGE_ROOT:.runtime/backups}}") String root,
        @Value("${backups.operator:${BACKUP_OPERATOR_USERNAME:}}") String operator,@Value("${backups.daily-enabled:true}") boolean daily,
        @Value("${backups.retention:7}") int retention) throws IOException {
        this.gate=gate;this.engine=engine;this.root=Path.of(root).toAbsolutePath().normalize();this.operator=operator;this.daily=daily;this.retention=retention;
        if(Files.exists(this.root.resolve("write-fence"),LinkOption.NOFOLLOW_LINKS))gate.fence();
        if(retention<1||retention>365)throw new IllegalArgumentException("Invalid backup retention");
        if(!operator.isBlank()) {
            Files.createDirectories(this.root);
            if(Files.isSymbolicLink(this.root))throw new IOException("Invalid backup root");
            try {
                registryChannel=java.nio.channels.FileChannel.open(this.root.resolve(".registry.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);
                registryLock=registryChannel.tryLock();
                if(registryLock==null)throw new IOException("Backup registry already active");
            Path saved=this.root.resolve("jobs.json");
            if(Files.exists(saved)) {
                state=json.readValue(Files.readAllBytes(saved),State.class);
                var jobs=new ArrayList<Job>();for(var j:state.jobs())jobs.add(j.state().equals("RUNNING")?changed(j,"INTERRUPTED",0):j);
                state=new State(state.version(),jobs,state.retentionFailed());save();
            }
            }catch(IOException|RuntimeException failure){
                if(registryLock!=null)registryLock.release();if(registryChannel!=null)registryChannel.close();
                throw new IOException("Backup registry unavailable",failure);
            }
        }
    }
    public void authorize(String actor) {if(operator.isBlank()||!operator.equals(actor))throw error("FORBIDDEN",403);}
    public synchronized Status status(String actor) {authorize(actor);return new Status(state.version(),retention,gate.paused(),List.copyOf(state.jobs()),state.retentionFailed());}
    public synchronized Ack start(String actor,UUID request,long version) {
        return start(actor,request,version,"MANUAL");
    }
    private synchronized Ack start(String actor,UUID request,long version,String kind) {
        authorize(actor);if(request==null||version<0)throw error("INVALID_INPUT",400);
        for(var job:state.jobs())if(job.id().equals(request)) {
            if(!job.actor().equals(actor)||job.expectedVersion()!=version||!job.kind().equals(kind))throw error("IDEMPOTENCY_CONFLICT",409);
            return new Ack(request,version+1);
        }
        if(version!=state.version())throw error("VERSION_CONFLICT",409);
        if(state.jobs().stream().anyMatch(j->j.state().equals("RUNNING")))throw error("BACKUP_BUSY",409);
        if(kind.equals("MIGRATION")) {
            try{if(!Files.exists(root.resolve("write-fence")))Files.writeString(root.resolve("write-fence"),"Migration: keep source writes stopped until explicitly abandoned offline.\n",StandardOpenOption.CREATE_NEW);gate.fence();}
            catch(IOException failure){throw error("BACKUP_UNAVAILABLE",503);}
        }
        var job=new Job(request,actor,version,kind,"RUNNING",Instant.now().toString(),"",0);
        add(job);worker.submit(()->run(job));return new Ack(request,state.version());
    }
    public Ack startMigration(String actor,UUID request,long version) { return start(actor,request,version,"MIGRATION"); }
    private void add(Job job) {
        var jobs=new ArrayList<>(state.jobs());jobs.add(job);var previous=state;state=new State(state.version()+1,jobs,state.retentionFailed());
        try{save();}catch(IOException e){state=previous;throw error("BACKUP_UNAVAILABLE",503);}
    }
    private void run(Job job) {
        Path work=root.resolve("work-"+job.id()),staging=root.resolve(job.id()+".partial"),complete=root.resolve(job.id()+".zip");
        try {
            Files.createDirectory(work);
            try(var pause=gate.pause(Duration.ofSeconds(30))) {engine.create(work,staging);}
            Files.move(staging,complete,StandardCopyOption.ATOMIC_MOVE);
            synchronized(this){
                replace(changed(job,"COMPLETE",Files.size(complete)));save();
                try{rotate();}catch(IOException failure){
                    state=new State(state.version(),state.jobs(),true);
                    try{save();}catch(IOException ignored){/* completed archive remains usable; retry rotation on a later task */}
                }
            }
        } catch(Exception failure) {
            org.slf4j.LoggerFactory.getLogger(BackupJobs.class).warn("Backup failed ({})",failure.getClass().getSimpleName());
            if(failure instanceof InterruptedException)Thread.currentThread().interrupt();
            synchronized(this){replace(changed(job,"FAILED",0));try{save();}catch(IOException ignored){/* previous RUNNING is recovered as INTERRUPTED */}}
        } finally {
            // Only known private scratch files; never traverse or delete the live asset tree.
            try{Files.deleteIfExists(staging);Files.deleteIfExists(work.resolve("database.dump"));Files.deleteIfExists(work);}catch(IOException ignored){}
        }
    }
    private Job changed(Job j,String state,long bytes){return new Job(j.id(),j.actor(),j.expectedVersion(),j.kind(),state,j.at(),Instant.now().toString(),bytes);}
    private void replace(Job changed){var jobs=new ArrayList<>(state.jobs());for(int i=0;i<jobs.size();i++)if(jobs.get(i).id().equals(changed.id()))jobs.set(i,changed);state=new State(state.version(),jobs,state.retentionFailed());}
    private void save()throws IOException {
        var temporary=Files.createTempFile(root,".jobs-",".tmp");
        try{Files.write(temporary,json.writeValueAsBytes(state));Files.move(temporary,root.resolve("jobs.json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        finally{Files.deleteIfExists(temporary);}
    }
    private void rotate()throws IOException {
        var automatic=state.jobs().stream().filter(j->j.kind().equals("AUTOMATIC")&&j.state().equals("COMPLETE")).toList();
        for(int i=0;i<automatic.size()-retention;i++)replace(changed(automatic.get(i),"EXPIRED",0));
        // Persist expiry before unlinking, so a crash never advertises a removed file.
        save();
        for(var j:state.jobs())if(j.kind().equals("AUTOMATIC")&&j.state().equals("EXPIRED"))Files.deleteIfExists(root.resolve(j.id()+".zip"));
        state=new State(state.version(),state.jobs(),false);save();
    }
    public synchronized Path download(String actor,UUID id) {
        authorize(actor);
        var job=state.jobs().stream().filter(j->j.id().equals(id)).findFirst().orElseThrow(()->error("NOT_FOUND",404));
        if(!job.state().equals("COMPLETE"))throw error("BACKUP_UNAVAILABLE",409);
        Path file=root.resolve(id+".zip");if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw error("BACKUP_UNAVAILABLE",503);return file;
    }
    @Scheduled(initialDelay=60000,fixedDelay=60000) public synchronized void daily() {
        if(operator.isBlank()||!daily||state.jobs().stream().anyMatch(j->j.state().equals("RUNNING")))return;
        String today=LocalDate.now(ZoneOffset.UTC).toString();
        if(state.jobs().stream().anyMatch(j->j.kind().equals("AUTOMATIC")&&j.at().startsWith(today)))return;
        var job=new Job(UUID.randomUUID(),operator,state.version(),"AUTOMATIC","RUNNING",Instant.now().toString(),"",0);
        try{add(job);worker.submit(()->run(job));}catch(RuntimeException failure){
            // Even a full/unwritable registry must make the failed automatic attempt visible.
            var jobs=new ArrayList<>(state.jobs());jobs.removeIf(j->j.id().equals(job.id()));jobs.add(changed(job,"FAILED",0));
            state=new State(Math.max(state.version(),job.expectedVersion()+1),jobs,state.retentionFailed());
            try{save();}catch(IOException ignored){/* memory status and server log remain available until storage is repaired */}
            org.slf4j.LoggerFactory.getLogger(BackupJobs.class).warn("Automatic backup could not be accepted ({})",failure.getClass().getSimpleName());
        }
    }
    @PreDestroy public void close() throws Exception {
        worker.shutdown();
        if(!worker.awaitTermination(30,TimeUnit.SECONDS)){worker.shutdownNow();if(!worker.awaitTermination(10,TimeUnit.SECONDS))throw new IOException("Backup worker still draining");}
        if(registryLock!=null)registryLock.release();if(registryChannel!=null)registryChannel.close();
    }
    private static DomainException error(String code,int status){return new DomainException(code,status,"Deployment backup operation unavailable");}
}
