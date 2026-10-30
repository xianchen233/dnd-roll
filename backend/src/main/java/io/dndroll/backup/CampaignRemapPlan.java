package io.dndroll.backup;
import java.util.*;
import java.nio.charset.StandardCharsets;
/** Review-only identity proposal; deliberately has no write or JSON-rebinding API. */
public final class CampaignRemapPlan {
    public record Identity(String namespace,UUID id,Long version) {}
    public record Reference(String table,String column,String namespace,UUID id) {}
    public record Mapping(String namespace,UUID source,UUID target,Long version) {}
    public record Alias(UUID timelineId,String namespace,UUID sourceId) {}
    public record Column(String name,String type,String policy,String namespace) {}
    public record Table(String name,String scope,long rows,List<Column> columns){public Table{columns=List.copyOf(columns);}}
    public record Report(int formatVersion,String status,boolean executable,UUID sourceCampaign,UUID targetCampaign,UUID seed,String sourceDigest,List<Mapping> mappings,List<Alias> aliases,List<Table> tables,List<String> blockers){public Report{mappings=List.copyOf(mappings);aliases=List.copyOf(aliases);tables=List.copyOf(tables);blockers=List.copyOf(blockers);}}
    private static final Map<String,RecoveryInventory.Definition> CATALOG=RecoveryInventory.catalog();
    static final Set<String> RUNTIME=Set.of("campaign","scene","token","character_card","campaign_asset","campaign_note","rp_message","note_attachment","rp_attachment","timeline_entry","timeline_session","attack_preview","dice_roll","combat_action","movement_journal");
    static final Set<String> AUTHORITY=Set.of("campaign_member","campaign_invitation","character_controller","token_controller");
    static boolean receipt(String table){return table.endsWith("_receipt");}
    static String scope(String table){return switch(table){case "campaign"->"CAMPAIGN_ID";case "app_user","flyway_schema_history","account_command_receipt"->"GLOBAL_EXCLUDED";case "character_history"->"PARENT_CHARACTER";case "note_history"->"PARENT_NOTE";case "rp_message_history"->"PARENT_MESSAGE";default->"CAMPAIGN_OWNED";};}
    static String referenceNamespace(String table,String column){return switch(column){case "campaign_id"->"campaign";case "current_scene_id","scene_id"->"scene";case "character_id","target_id"->"character_card";case "token_id"->"token";case "asset_id"->"campaign_asset";case "note_id"->"campaign_note";case "message_id"->"rp_message";case "roll_id"->"dice_roll";case "action_id"->"combat_action";case "session_id"->"timeline_session";case "attachment_id"->table.equals("note_attachment_reference")?"note_attachment":"rp_attachment";default->"";};}
    public static List<Column> columns(String table){
        var definition=CATALOG.get(table);if(definition==null)throw new IllegalArgumentException("Unknown recovery table");var result=new ArrayList<Column>();
        for(String signature:definition.signatures())if(signature.startsWith("column|")){
            String[] parts=signature.split("\\|",-1);String name=parts[2],type=parts[3],policy="KEEP_SCALAR",namespace="";
            if(receipt(table))policy="SOURCE_ONLY_SKIP";
            else if(table.equals("app_user")||table.equals("flyway_schema_history"))policy="GLOBAL_KEEP";
            else if(AUTHORITY.contains(table))policy="AUTHORITY_REVIEW_REQUIRED";
            else if(name.equals("storage_key"))policy="IMMUTABLE_FILE_COPY_REQUIRED";
            else if(type.equals("jsonb"))policy="TYPED_JSON_REBIND_REQUIRED";
            else if(name.equals("version")||name.endsWith("_version"))policy="PRESERVE_SOURCE_VERSION";
            else if(name.equals("seq")||name.equals("ordinal"))policy="GENERATED_SEQUENCE_REASSIGN_REQUIRED";
            else if(name.contains("hash")||name.equals("fingerprint"))policy="HASH_RECOMPUTE_REQUIRED";
            else if(type.equals("uuid")){
                if(name.equals("id")&&RUNTIME.contains(table)){policy="PROPOSE_RUNTIME_ID";namespace=table;}
                else if(name.equals("campaign_id")){policy="RUNTIME_REFERENCE";namespace="campaign";}
                else if(table.startsWith("module_"))policy="MODULE_ID_REVIEW_REQUIRED";
                else if(table.equals("campaign_content_entry")&&name.equals("id"))policy="CONTENT_ID_REVIEW_REQUIRED";
                else {namespace=referenceNamespace(table,name);policy=namespace.isEmpty()?"UNCLASSIFIED_UUID":"RUNTIME_REFERENCE";}
            }
            result.add(new Column(name,type,policy,namespace));
        }
        result.sort(Comparator.comparing(Column::name));return List.copyOf(result);
    }
    public static UUID proposal(UUID seed,String digest,UUID source,UUID target,String namespace,UUID id){
        Objects.requireNonNull(seed);Objects.requireNonNull(source);Objects.requireNonNull(target);Objects.requireNonNull(id);
        if(digest==null||!digest.matches("[a-f0-9]{64}")||!RUNTIME.contains(namespace))throw new IllegalArgumentException("Invalid identity proposal");
        if(namespace.equals("campaign")&&id.equals(source))return target;
        return UUID.nameUUIDFromBytes(String.join("|","dnd-roll-remap-proposal-v1",seed.toString(),digest,source.toString(),target.toString(),namespace,id.toString()).getBytes(StandardCharsets.UTF_8));
    }
    private record Key(String namespace,UUID id) {}
    public static Report plan(UUID source,UUID target,UUID seed,String digest,List<Identity> identities,List<Reference> references,Set<UUID> occupied,List<Table> tables){
        return plan(source,target,seed,digest,identities,references,occupied,tables,List.of());
    }
    public static Report plan(UUID source,UUID target,UUID seed,String digest,List<Identity> identities,List<Reference> references,Set<UUID> occupied,List<Table> tables,List<Alias> aliases){
        Objects.requireNonNull(source);Objects.requireNonNull(target);Objects.requireNonNull(seed);if(digest==null||!digest.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid source digest");
        if(identities.size()>10000||references.size()>100000||occupied.size()>1000000||aliases.size()>10000)throw new IllegalArgumentException("Proposal capacity exceeded");
        var blockers=new TreeSet<>(Set.of("REMAP_WRITER_NOT_IMPLEMENTED","TARGET_AUTHORITY_REVIEW_REQUIRED","HASH_REBIND_NOT_IMPLEMENTED"));var keys=new HashSet<Key>();var proposals=new HashMap<UUID,Key>();var mappings=new ArrayList<Mapping>();var valid=new ArrayList<Identity>();
        if(source.equals(target))blockers.add("SOURCE_TARGET_MUST_DIFFER");if(occupied.contains(target))blockers.add("TARGET_ID_COLLISION");
        for(var identity:identities){
            if(identity==null||identity.id()==null||!RUNTIME.contains(identity.namespace())){blockers.add("INVALID_IDENTITY");continue;}
            if(!keys.add(new Key(identity.namespace(),identity.id()))){blockers.add("DUPLICATE_IDENTITY:"+identity.namespace());continue;}
            if(identity.version()!=null&&identity.version()<0)blockers.add("INVALID_VERSION:"+identity.namespace());
            valid.add(identity);
        }
        var anchors=new HashMap<Key,Key>();var acceptedAliases=new ArrayList<Alias>();
        for(var alias:aliases){
            if(alias==null||alias.timelineId()==null||alias.sourceId()==null||!Set.of("rp_message","dice_roll","combat_action").contains(alias.namespace())||!alias.timelineId().equals(alias.sourceId())){blockers.add("INVALID_TIMELINE_ALIAS");continue;}
            var key=new Key("timeline_entry",alias.timelineId());var anchor=new Key(alias.namespace(),alias.sourceId());if(!keys.contains(key)||!keys.contains(anchor)||anchors.putIfAbsent(key,anchor)!=null){blockers.add("INVALID_TIMELINE_ALIAS");continue;}acceptedAliases.add(alias);
        }
        for(var identity:valid){
            var key=new Key(identity.namespace(),identity.id());var anchor=anchors.getOrDefault(key,key);
            if(key.namespace().equals("timeline_entry")&&!anchors.containsKey(key))blockers.add("MISSING_TIMELINE_ALIAS");
            UUID mapped=proposal(seed,digest,source,target,anchor.namespace(),anchor.id());var prior=proposals.putIfAbsent(mapped,anchor);if(occupied.contains(mapped)||(prior!=null&&!prior.equals(anchor)))blockers.add("PROPOSED_ID_COLLISION:"+identity.namespace());
            mappings.add(new Mapping(identity.namespace(),identity.id(),mapped,identity.version()));
        }
        if(!keys.contains(new Key("campaign",source)))blockers.add("SOURCE_CAMPAIGN_MISSING");
        for(var ref:references){
            var declaration=CATALOG.containsKey(ref.table())?columns(ref.table()).stream().filter(c->c.name().equals(ref.column())).findFirst().orElse(null):null;
            if(declaration==null||!declaration.policy().equals("RUNTIME_REFERENCE")||!declaration.namespace().equals(ref.namespace()))blockers.add("REFERENCE_POLICY_MISMATCH:"+ref.table()+":"+ref.column());
            if(ref.id()!=null&&!keys.contains(new Key(ref.namespace(),ref.id())))blockers.add("UNRESOLVED_REFERENCE:"+ref.table()+":"+ref.column());
        }
        if(identities.stream().filter(i->i!=null&&"campaign".equals(i.namespace())).count()!=1)blockers.add("CAMPAIGN_IDENTITY_COVERAGE_MISMATCH");
        var names=new HashSet<String>();for(var table:tables){
            if(!names.add(table.name())||table.rows()<0)blockers.add("TABLE_COVERAGE_MISMATCH");
            if(CATALOG.containsKey(table.name())&&!scope(table.name()).equals(table.scope()))blockers.add("SCOPE_COVERAGE_MISMATCH:"+table.name());
            if(RUNTIME.contains(table.name())&&table.rows()!=keys.stream().filter(k->k.namespace().equals(table.name())).count())blockers.add("IDENTITY_COVERAGE_MISMATCH:"+table.name());
            if(CATALOG.containsKey(table.name())&&!columns(table.name()).equals(table.columns()))blockers.add("COLUMN_COVERAGE_MISMATCH:"+table.name());
            for(var column:table.columns()){
                if(column.policy().equals("UNCLASSIFIED_UUID"))blockers.add("UNCLASSIFIED_UUID:"+table.name()+":"+column.name());
                if(table.rows()>0&&column.policy().equals("TYPED_JSON_REBIND_REQUIRED"))blockers.add("TYPED_JSON_REBIND_NOT_IMPLEMENTED:"+table.name()+":"+column.name());
                if(table.rows()>0&&column.policy().equals("IMMUTABLE_FILE_COPY_REQUIRED"))blockers.add("IMMUTABLE_FILE_COPY_NOT_IMPLEMENTED:"+table.name());
            }
            if(table.rows()>0&&table.name().startsWith("module_"))blockers.add("MODULE_GRAPH_REVIEW_REQUIRED:"+table.name());
        }
        if(!names.equals(CATALOG.keySet()))blockers.add("TABLE_COVERAGE_MISMATCH");
        mappings.sort(Comparator.comparing(Mapping::namespace).thenComparing(m->m.source().toString()));var sorted=new ArrayList<>(tables);sorted.sort(Comparator.comparing(Table::name));
        acceptedAliases.sort(Comparator.comparing(a->a.timelineId().toString()));return new Report(1,"BLOCKED",false,source,target,seed,digest,mappings,acceptedAliases,sorted,new ArrayList<>(blockers));
    }
}
