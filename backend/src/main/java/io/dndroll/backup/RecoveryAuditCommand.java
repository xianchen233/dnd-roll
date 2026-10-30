package io.dndroll.backup;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import tools.jackson.databind.json.JsonMapper;

/** Offline read-only audit. It neither copies a campaign nor releases a write fence. */
public final class RecoveryAuditCommand {
    public record Result(int formatVersion,String mode,String status,int assetsVerified,RecoveryInventory.Report inventory) {}
    public static Result audit(Path archive,String url,String username,String password,Path assets,RecoveryInventory.Mode mode)throws IOException {
        try(var connection=DriverManager.getConnection(url,username,password)){
            connection.setReadOnly(true);connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);connection.setAutoCommit(false);
            var inspection=inspect(connection,archive,assets,mode);connection.rollback();return inspection.result();
        }catch(Exception failure){throw new IOException("Recovery audit failed; verify archive, database compatibility and assets");}
    }
    record Inspection(BackupArchive.Manifest manifest,Result result) {}
    static Inspection inspect(Connection connection,Path archive,Path assets,RecoveryInventory.Mode mode)throws Exception{
            if(!connection.isReadOnly()||connection.getAutoCommit()||connection.getTransactionIsolation()!=Connection.TRANSACTION_REPEATABLE_READ)throw new IOException();
            try(var st=connection.createStatement()){st.execute("set local timezone='UTC'");st.execute("set local search_path=public,pg_catalog");}
            var checksums=RecoveryInventory.migrations();var manifest=new BackupArchive().verify(archive,PostgresBackup.major(connection),checksums);
            if(!checksums.equals(PostgresBackup.migrations(connection)))throw new IOException();
            var inventory=RecoveryInventory.inspect(RecoveryInventory.schema(connection),manifest.tables(),PostgresBackup.tables(connection),mode);
            if(!Files.isDirectory(assets,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(assets)||!manifest.assets().equals(PostgresBackup.files(connection)))throw new IOException();
            for(var file:manifest.assets()){
                var path=assets.resolve(file.key());if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||!file.equals(PostgresBackup.file(file.key(),path)))throw new IOException();
            }
            return new Inspection(manifest,new Result(1,mode.name(),inventory.blockers().isEmpty()?"VERIFIED":"BLOCKED",manifest.assets().size(),inventory));
    }
    public static void write(Result report,Path output)throws IOException{Files.write(output,JsonMapper.builder().build().writeValueAsBytes(report),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);}
    public static void main(String[] args){
        try{
            if(args.length!=4)throw new IllegalArgumentException();Path output=Path.of(args[2]);if(Files.exists(output,LinkOption.NOFOLLOW_LINKS))throw new IOException();
            var report=audit(Path.of(args[0]),required("RECOVERY_AUDIT_DATABASE_URL"),required("RECOVERY_AUDIT_DATABASE_USERNAME"),required("RECOVERY_AUDIT_DATABASE_PASSWORD"),Path.of(args[1]),RecoveryInventory.Mode.valueOf(args[3]));write(report,output);
            System.out.println("RECOVERY AUDIT "+report.status()+": "+report.inventory().tables().size()+" tables; "+report.assetsVerified()+" immutable files; no database writes or campaign remapping.");
            if(!report.status().equals("VERIFIED"))System.exit(2);
        }catch(Exception failure){System.err.println("RECOVERY AUDIT FAILED: check trusted archive, isolated database, matching schema, immutable assets and new report path. No write fence was changed.");System.exit(1);}
    }
    private static String required(String key){String value=System.getenv(key);if(value==null||value.isBlank())throw new IllegalArgumentException();return value;}
}
