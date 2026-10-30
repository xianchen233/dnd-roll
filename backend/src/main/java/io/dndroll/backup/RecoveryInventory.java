package io.dndroll.backup;

import java.sql.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Schema/row coverage only. No campaign mapper, writer or user-data traversal. */
public final class RecoveryInventory {
    public enum Mode { SAME_ID, CAMPAIGN_REMAP }
    public record Definition(List<String> signatures,String remapPolicy,Map<String,String> restoredChecks){
        public Definition{signatures=List.copyOf(signatures);restoredChecks=Map.copyOf(restoredChecks);}
        boolean matches(List<String> observed){return signatures.equals(observed.stream().map(s->restoredChecks.getOrDefault(s,s)).sorted().toList());}
    }
    public record Table(String name,long rows,String remapPolicy) {}
    public record Report(int formatVersion,String mode,String status,List<Table> tables,List<String> blockers){public Report{tables=List.copyOf(tables);blockers=List.copyOf(blockers);}}
    public static Map<String,Integer> migrations(){
        try(var in=RecoveryInventory.class.getResourceAsStream("/recovery/m3-recovery-catalog.json")){
            var node=JsonMapper.builder().build().readTree(in);var result=new TreeMap<String,Integer>();node.get("migrations").properties().forEach(e->result.put(e.getKey(),e.getValue().asInt()));
            if(result.size()!=34)throw new IllegalStateException();return Collections.unmodifiableMap(result);
        }catch(Exception failure){throw new IllegalStateException("Recovery migration catalog unavailable");}
    }
    public static Map<String,Definition> catalog(){
        try(var in=RecoveryInventory.class.getResourceAsStream("/recovery/m3-recovery-catalog.json")){
            var node=JsonMapper.builder().build().readTree(in);var result=new TreeMap<String,Definition>();
            if(node.get("formatVersion").asInt()!=1||node.get("migrationVersion").asInt()!=34)throw new IllegalStateException("Unsupported recovery catalog");
            for(var e:node.get("tables").properties()){
                var signatures=new ArrayList<String>();e.getValue().get("signatures").forEach(v->signatures.add(v.asString()));var restoredChecks=new TreeMap<String,String>();
                if(e.getValue().has("restoredChecks"))e.getValue().get("restoredChecks").properties().forEach(v->{String original=v.getValue().asString();if(!v.getKey().startsWith("constraint|c|")||!original.startsWith("constraint|c|")||!signatures.contains(original))throw new IllegalStateException();restoredChecks.put(v.getKey(),original);});
                result.put(e.getKey(),new Definition(signatures,e.getValue().get("remapPolicy").asString(),restoredChecks));
            }
            return Collections.unmodifiableMap(result);
        }catch(Exception failure){throw new IllegalStateException("Recovery catalog unavailable");}
    }
    public static Map<String,List<String>> schema(Connection c)throws SQLException{
        var result=new TreeMap<String,List<String>>();
        String sql="select t.tablename, s.signature from pg_tables t cross join lateral (select 'column|'||a.attnum||'|'||a.attname||'|'||format_type(a.atttypid,a.atttypmod)||'|'||a.attnotnull||'|'||coalesce(pg_get_expr(d.adbin,d.adrelid),'') signature from pg_attribute a left join pg_attrdef d on d.adrelid=a.attrelid and d.adnum=a.attnum where a.attrelid=('public.'||quote_ident(t.tablename))::regclass and a.attnum>0 and not a.attisdropped union all select 'constraint|'||p.contype::text||'|'||pg_get_constraintdef(p.oid,true) from pg_constraint p where p.conrelid=('public.'||quote_ident(t.tablename))::regclass union all select 'index|'||pg_get_indexdef(i.indexrelid,0,true) from pg_index i where i.indrelid=('public.'||quote_ident(t.tablename))::regclass) s where t.schemaname='public' order by t.tablename,s.signature";
        try(var st=c.createStatement();var rows=st.executeQuery(sql)){while(rows.next())result.computeIfAbsent(rows.getString(1),k->new ArrayList<>()).add(rows.getString(2));}
        var frozen=new TreeMap<String,List<String>>();result.forEach((k,v)->frozen.put(k,List.copyOf(v)));return Collections.unmodifiableMap(frozen);
    }
    public static Report inspect(Map<String,List<String>> schema,Map<String,BackupArchive.Table> declared,Map<String,BackupArchive.Table> actual,Mode mode){
        var expected=catalog();var blockers=new TreeSet<String>();var tables=new ArrayList<Table>();
        if(!schema.keySet().equals(expected.keySet()))blockers.add("SCHEMA_TABLE_SET_MISMATCH");
        if(!declared.keySet().equals(expected.keySet()))blockers.add("DECLARED_TABLE_SET_MISMATCH");
        if(!actual.keySet().equals(expected.keySet()))blockers.add("ACTUAL_TABLE_SET_MISMATCH");
        for(var e:schema.entrySet()){
            if(!expected.containsKey(e.getKey()))blockers.add("UNCLASSIFIED_TABLE:"+e.getKey());
            else if(!expected.get(e.getKey()).matches(e.getValue()))blockers.add("SCHEMA_SIGNATURE_MISMATCH:"+e.getKey());
        }
        for(var e:actual.entrySet()){
            String name=e.getKey();var definition=expected.get(name);var row=e.getValue();
            if(row==null||row.rows()<0||row.sha256()==null||!row.sha256().matches("[a-f0-9]{64}")){blockers.add("INVALID_TABLE_METADATA:"+name);continue;}
            var original=declared.get(name);
            if(!row.equals(original))blockers.add("ROW_DIGEST_MISMATCH:"+name);
            String policy=definition==null?"UNCLASSIFIED":definition.remapPolicy();tables.add(new Table(name,row.rows(),policy));
            if(mode==Mode.CAMPAIGN_REMAP&&row.rows()>0&&!Set.of("SCHEMA_METADATA_KEEP","AUTHORITY_KEEP_ACCOUNTS").contains(policy))blockers.add("REMAP_REQUIRED:"+policy+":"+name);
        }
        for(var e:declared.entrySet())if(e.getValue()==null||e.getValue().rows()<0||e.getValue().sha256()==null||!e.getValue().sha256().matches("[a-f0-9]{64}"))blockers.add("INVALID_TABLE_METADATA:"+e.getKey());
        if(mode==Mode.CAMPAIGN_REMAP)blockers.add("REMAP_WRITER_NOT_IMPLEMENTED");
        tables.sort(Comparator.comparing(Table::name));return new Report(1,mode.name(),blockers.isEmpty()?"SCHEMA_AND_ROWS_VERIFIED":"BLOCKED",tables,new ArrayList<>(blockers));
    }
}
