package io.dndroll.backup;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** PostgreSQL and files are sampled while application admission and table writes are closed. */
@Component
public class PostgresBackup {
    private final DataSource source;
    private final Path assets;
    private final String executable, url, username, password;
    public PostgresBackup(DataSource source,@Value("${assets.storage-root}") String assets,
        @Value("${backups.pg-dump:${BACKUP_PG_DUMP:pg_dump}}") String executable,
        @Value("${spring.datasource.url}") String url,@Value("${spring.datasource.username}") String username,
        @Value("${spring.datasource.password}") String password) {
        this.source=source;this.assets=Path.of(assets).toAbsolutePath().normalize();this.executable=executable;
        this.url=url;this.username=username;this.password=password;
    }
    public void create(Path work,Path archive) throws Exception {
        Files.createDirectories(assets);
        if(Files.isSymbolicLink(assets))throw new IOException("Invalid asset root");
        try(var connection=source.getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.createStatement()) {
                statement.execute("set local lock_timeout='30s'");
                statement.execute("set local timezone='UTC'");
                var tables=tableNames(connection);
                for(String table:tables)statement.execute("lock table public.\""+table+"\" in share mode");
                int major=major(connection);
                var migrations=migrations(connection);
                var digests=tables(connection);
                var files=files(connection);
                var dump=work.resolve("database.dump");
                process(executable,List.of("--format=custom","--no-owner","--no-acl","--schema=public","--file="+dump),url,username,password);
                var database=file("database.dump",dump);
                var manifest=new BackupArchive.Manifest("dnd-roll.backup",1,Instant.now().toString(),major,migrations,digests,database,files);
                new BackupArchive().write(archive,manifest,dump,assets);
            } finally {connection.rollback();}
        }
    }
    public static int major(Connection connection)throws SQLException {
        try(var s=connection.createStatement();var r=s.executeQuery("show server_version_num")){r.next();return Integer.parseInt(r.getString(1))/10000;}
    }
    public static Map<String,Integer> migrations(Connection connection)throws SQLException {
        var result=new TreeMap<String,Integer>();
        try(var s=connection.createStatement();var r=s.executeQuery("select version,checksum from public.flyway_schema_history where success and version is not null order by installed_rank")) {
            while(r.next())result.put(r.getString(1),r.getInt(2));
        }
        return result;
    }
    static List<String> tableNames(Connection c)throws SQLException {
        var names=new ArrayList<String>();
        try(var s=c.createStatement();var r=s.executeQuery("select tablename from pg_tables where schemaname='public' order by tablename")) {
            while(r.next()) {String name=r.getString(1);if(!name.matches("[a-z_][a-z0-9_]{0,62}"))throw new SQLException("Unsupported schema");names.add(name);}
        }
        return names;
    }
    public static Map<String,BackupArchive.Table> tables(Connection c)throws SQLException {
        var result=new TreeMap<String,BackupArchive.Table>();
        for(String table:tableNames(c)) {
            var digest=BackupArchive.sha256();long count=0;
            try(var s=c.createStatement()) {
                s.setFetchSize(500);
                try(var r=s.executeQuery("select row_to_json(t)::text as data from public.\""+table+"\" t order by row_to_json(t)::text collate \"C\"")) {
                    while(r.next()){digest.update(r.getString(1).getBytes(StandardCharsets.UTF_8));digest.update((byte)'\n');count++;}
                }
            }
            result.put(table,new BackupArchive.Table(count,HexFormat.of().formatHex(digest.digest())));
        }
        return result;
    }
    public static List<BackupArchive.File> files(Connection c)throws SQLException {
        var files=new ArrayList<BackupArchive.File>();var seen=new HashSet<String>();
        String sql="select storage_key::text,byte_size,sha256 from campaign_asset union all select storage_key::text,(data->>'byteSize')::bigint,sha256 from rp_attachment union all select storage_key::text,(data->>'byteSize')::bigint,sha256 from note_attachment order by 1";
        try(var s=c.createStatement();var r=s.executeQuery(sql)) {
            while(r.next()){String key=r.getString(1);if(!seen.add(key))throw new SQLException("Duplicate immutable storage identity");files.add(new BackupArchive.File(key,r.getLong(2),r.getString(3)));}
        }
        return files;
    }
    static BackupArchive.File file(String key,Path path)throws IOException {
        long size=Files.size(path);try(var in=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)){return new BackupArchive.File(key,size,BackupArchive.digest(in,size));}
    }
    public static void process(String executable,List<String> args,String jdbcUrl,String username,String password)throws Exception {
        var uri=URI.create(jdbcUrl.substring("jdbc:".length()));
        if(!"postgresql".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||!uri.getPath().matches("/[A-Za-z0-9_]+"))throw new IOException("Unsupported database connection configuration");
        var command=new ArrayList<String>();command.add(executable);command.addAll(args);
        var builder=new ProcessBuilder(command);var env=builder.environment();
        // No shell, connection strings or passwords in argv, job output or HTTP errors.
        env.put("PGHOST",uri.getHost());env.put("PGPORT",Integer.toString(uri.getPort()<0?5432:uri.getPort()));env.put("PGDATABASE",uri.getPath().substring(1));
        env.put("PGUSER",username);env.put("PGPASSWORD",password);env.put("PGCONNECT_TIMEOUT","10");env.put("PGOPTIONS","-c timezone=UTC -c datestyle=ISO");
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
        var process=builder.start();
        try {if(!process.waitFor(5,TimeUnit.MINUTES)||process.exitValue()!=0)throw new IOException("PostgreSQL archive command failed");}
        finally {if(process.isAlive()){process.destroyForcibly();process.waitFor(10,TimeUnit.SECONDS);}}
    }
}
