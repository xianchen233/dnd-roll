package io.dndroll.backup;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.flywaydb.core.Flyway;

/** Offline entry point: never starts Spring, a listener or automatic migrations. */
public final class RestoreCommand {
    public static void restore(Path archive,String url,String username,String password,Path assets,String executable) throws Exception {
        assets=assets.toAbsolutePath().normalize();
        if(Files.exists(assets,LinkOption.NOFOLLOW_LINKS)||assets.getParent()==null||!Files.isDirectory(assets.getParent()))throw new java.io.IOException("Target asset root must be new");
        try(var target=DriverManager.getConnection(url,username,password)) {
            try(var s=target.createStatement();var rows=s.executeQuery("select count(*) from (select c.relnamespace ns from pg_class c union all select p.pronamespace from pg_proc p union all select t.typnamespace from pg_type t) objects join pg_namespace n on objects.ns=n.oid where n.nspname not like 'pg_%' and n.nspname<>'information_schema'")) {
                rows.next();if(rows.getLong(1)!=0)throw new java.io.IOException("Target database must be empty and isolated");
            }
            var resolved=new TreeMap<String,Integer>();
            for(var migration:Flyway.configure().dataSource(url,username,password).load().info().all())
                if(migration.getVersion()!=null)resolved.put(migration.getVersion().toString(),migration.getChecksum());
            var manifest=new BackupArchive().verify(archive,PostgresBackup.major(target),resolved);
            var staging=Files.createTempDirectory(assets.getParent(),".restore-");
            // Quarantined on any failure; no source writes, target activation or destructive cleanup.
            try(var zip=new ZipFile(archive.toFile())) {
                try(var in=zip.getInputStream(zip.getEntry("database.dump"))){Files.copy(in,staging.resolve("database.dump"));}
                for(var file:manifest.assets())try(var in=zip.getInputStream(zip.getEntry("assets/"+file.key()))){Files.copy(in,staging.resolve(file.key()));}
            }
            if(!manifest.database().equals(PostgresBackup.file("database.dump",staging.resolve("database.dump"))))throw new java.io.IOException("Staged database verification failed");
            for(var file:manifest.assets())if(!file.equals(PostgresBackup.file(file.key(),staging.resolve(file.key()))))throw new java.io.IOException("Staged asset verification failed");
            // Only the proven-empty target's default schema; never CASCADE or --clean.
            // The logical archive creates its own public schema in the restore transaction.
            try(var s=target.createStatement()){s.execute("drop schema if exists public");}
            String database=java.net.URI.create(url.substring(5)).getPath().substring(1);
            PostgresBackup.process(executable,List.of("--dbname="+database,"--no-owner","--no-acl","--exit-on-error","--single-transaction",staging.resolve("database.dump").toString()),url,username,password);
            target.setAutoCommit(false);
            try(var s=target.createStatement()){s.execute("set local timezone='UTC'");}
            if(!manifest.migrations().equals(PostgresBackup.migrations(target))||!manifest.tables().equals(PostgresBackup.tables(target))||!manifest.assets().equals(PostgresBackup.files(target)))throw new java.io.IOException("Restored state verification failed");
            for(var file:manifest.assets())if(!file.equals(PostgresBackup.file(file.key(),staging.resolve(file.key()))))throw new java.io.IOException("Restored asset verification failed");
            target.rollback();Files.delete(staging.resolve("database.dump"));Files.move(staging,assets,StandardCopyOption.ATOMIC_MOVE);
        }
    }
    public static void main(String[] args) {
        try {
            if(args.length!=2)throw new IllegalArgumentException();
            restore(Path.of(args[0]),required("RESTORE_DATABASE_URL"),required("RESTORE_DATABASE_USERNAME"),required("RESTORE_DATABASE_PASSWORD"),Path.of(args[1]),System.getenv().getOrDefault("BACKUP_PG_RESTORE","pg_restore"));
            System.out.println("RESTORE VERIFIED: database, migrations, all rows, references and immutable files match. Keep target isolated until source writes are stopped.");
        } catch(Exception failure){System.err.println("RESTORE FAILED: source untouched; keep target quarantined. Check archive compatibility, empty target, new asset root and PostgreSQL clients.");System.exit(1);}
    }
    private static String required(String key){String value=System.getenv(key);if(value==null||value.isBlank())throw new IllegalArgumentException();return value;}
}
