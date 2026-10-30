package io.dndroll.backup;
import io.dndroll.application.*;import io.dndroll.domain.*;
import java.util.*;import java.lang.reflect.*;import java.time.*;import java.util.function.Supplier;
import tools.jackson.databind.json.JsonMapper;import tools.jackson.databind.DeserializationFeature;
/** Private in-memory record transformation; never a command or a writer. */
public final class CampaignEvidenceRebind {
 public record Proof(String table,UUID id,long version,String sourceHash,String targetHash) {}
 public record Report(UUID sourceCampaign,UUID targetCampaign,UUID seed,String sourceDigest,String status,boolean executable,boolean consistent,Map<String,Long> rows,List<Proof> proofs,List<String> blockers) {}
 private final CampaignRemapPlan.Report proposal;private final JsonMapper json=JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
 private final Map<String,UUID> mapping=new HashMap<>();private final Map<String,UUID> eventAliases=new HashMap<>();private final Map<UUID,String> generatedEvents=new HashMap<>();private final List<Proof> proofs=new ArrayList<>();private Collection<Object> sourceObjects=List.of();private final Set<UUID> occupied;private final Set<UUID> sourceEventIds=new HashSet<>();
 CampaignEvidenceRebind(CampaignRemapPlan.Report p){this(p,Set.of());}
 private CampaignEvidenceRebind(CampaignRemapPlan.Report p,Set<UUID> occupied){this.occupied=Set.copyOf(occupied);proposal=p;for(var m:p.mappings())if(mapping.put(m.namespace()+":"+m.source(),m.target())!=null)throw new IllegalArgumentException();}
 public static Report inspect(CampaignRemapPlan.Report p,List<CampaignEvidenceGraph.Row> rows,List<CampaignEvidenceGraph.SourceReceipt> receipts){return inspect(p,rows,receipts,Set.of());}
 static Report inspect(CampaignRemapPlan.Report p,List<CampaignEvidenceGraph.Row> rows,List<CampaignEvidenceGraph.SourceReceipt> receipts,Set<UUID> occupied){
  var counts=new TreeMap<String,Long>();rows.forEach(r->counts.merge(CampaignEvidenceGraph.TYPES.containsKey(r.table())?r.table():"UNKNOWN",1L,Long::sum));
  var blockers=new ArrayList<>(List.of("REMAP_WRITER_NOT_IMPLEMENTED","TARGET_AUTHORITY_REVIEW_REQUIRED","PERMISSION_FILE_MODULE_GRAPH_NOT_IMPLEMENTED","OTHER_RUNTIME_ADAPTERS_NOT_IMPLEMENTED"));
  try{validateProposal(p);var graph=CampaignEvidenceGraph.inspect(p.sourceCampaign(),rows,receipts);if(!graph.consistent())throw new IllegalArgumentException();var engine=new CampaignEvidenceRebind(p,occupied);engine.rebindRows(rows);return new Report(p.sourceCampaign(),p.targetCampaign(),p.seed(),p.sourceDigest(),"BLOCKED",false,true,Collections.unmodifiableMap(counts),List.copyOf(engine.proofs),List.copyOf(blockers));}
  catch(Exception e){blockers.add("TYPED_JSON_OR_HASH_REBIND_FAILED");return new Report(p.sourceCampaign(),p.targetCampaign(),p.seed(),p.sourceDigest()!=null&&p.sourceDigest().matches("[a-fA-F0-9]{64}")?p.sourceDigest():"","BLOCKED",false,false,Collections.unmodifiableMap(counts),List.of(),List.copyOf(blockers));}
 }
 static boolean allowedBlocker(String b){return Set.of("REMAP_WRITER_NOT_IMPLEMENTED","TARGET_AUTHORITY_REVIEW_REQUIRED","HASH_REBIND_NOT_IMPLEMENTED").contains(b)||b.startsWith("TYPED_JSON_REBIND_NOT_IMPLEMENTED:")||b.startsWith("IMMUTABLE_FILE_COPY_NOT_IMPLEMENTED:")||b.startsWith("MODULE_GRAPH_REVIEW_REQUIRED:");}
 private static void validateProposal(CampaignRemapPlan.Report p){
  if(p.sourceCampaign().equals(p.targetCampaign())||p.executable()||!p.status().equals("BLOCKED")||p.formatVersion()!=1||!p.sourceDigest().matches("[a-fA-F0-9]{64}")||p.blockers().stream().anyMatch(b->!allowedBlocker(b)))throw new IllegalArgumentException();
  var used=new HashMap<UUID,String>();boolean campaign=false;
  for(var m:p.mappings()){String ns=m.namespace();UUID original=m.source();if(ns.equals("timeline_entry")){var alias=p.aliases().stream().filter(a->a.timelineId().equals(m.source())).findFirst().orElseThrow();ns=alias.namespace();original=alias.sourceId();}UUID expected=CampaignRemapPlan.proposal(p.seed(),p.sourceDigest(),p.sourceCampaign(),p.targetCampaign(),ns,original);String key=ns+":"+original;if(!expected.equals(m.target()))throw new IllegalArgumentException();String prior=used.putIfAbsent(m.target(),key);if(prior!=null&&!prior.equals(key))throw new IllegalArgumentException();if(m.namespace().equals("campaign")&&m.source().equals(p.sourceCampaign())&&m.target().equals(p.targetCampaign()))campaign=true;}
  if(!campaign)throw new IllegalArgumentException();
 }
 private static final Map<Class<?>,Map<String,String>> UUID_FIELDS=Map.ofEntries(
  Map.entry(CharacterService.Card.class,Map.of("id","character_card","campaignId","campaign")),
  Map.entry(DiceService.Roll.class,Map.of("id","dice_roll")),
  Map.entry(CombatService.Action.class,Map.of("id","combat_action","characterId","character_card","targetId","character_card")),
  Map.entry(CombatService.Change.class,Map.of("characterId","character_card")),
  Map.entry(AttackPreviewService.Create.class,Map.of("characterId","character_card","targetId","character_card")),
  Map.entry(AttackPreviewService.Evidence.class,Map.of("actionId","combat_action")),
  Map.entry(AttackPreviewService.Preview.class,Map.of("id","attack_preview")),
  Map.entry(StabilizationService.State.class,Map.of("id","character_card")),Map.entry(RestabilizationService.State.class,Map.of("id","character_card")),
  Map.entry(StabilizationService.Attempt.class,Map.of("id","dice_roll")),Map.entry(RestabilizationService.Attempt.class,Map.of("id","dice_roll")),
  Map.entry(StabilizationHealingService.Entry.class,Map.of("id","character_card","attemptId","dice_roll")),Map.entry(StabilizationHealingService.Review.class,Map.of("attemptId","dice_roll")),
  Map.entry(StabilizationDamageService.Entry.class,Map.of("id","character_card","attemptId","dice_roll")),Map.entry(StabilizationDamageService.Review.class,Map.of("attemptId","dice_roll")),
  Map.entry(RestabilizationHealingService.Entry.class,Map.of("id","character_card","attemptId","dice_roll")),Map.entry(RestabilizationHealingService.Review.class,Map.of("attemptId","dice_roll")),
  Map.entry(RestabilizationDamageService.Entry.class,Map.of("id","character_card","attemptId","dice_roll")),Map.entry(RestabilizationDamageService.Review.class,Map.of("attemptId","dice_roll")),
  Map.entry(InstabilityHealingService.Entry.class,Map.of("id","character_card","attemptId","dice_roll")),Map.entry(InstabilityHealingService.Review.class,Map.of("attemptId","dice_roll")),
  Map.entry(InitialInstabilityHealingService.Entry.class,Map.of("id","character_card","attemptId","dice_roll")),Map.entry(InitialInstabilityHealingService.Review.class,Map.of("attemptId","dice_roll")));

 private UUID id(String namespace,UUID value){if(value==null)return null;UUID next=mapping.get(namespace+":"+value);if(next==null)throw new IllegalArgumentException();return next;}
 private UUID event(String namespace,UUID owner,UUID value){if(value==null)return null;UUID result=UUID.nameUUIDFromBytes(("dnd-roll-rebind-event-v1:"+proposal.seed()+":"+proposal.sourceDigest()+":"+proposal.sourceCampaign()+":"+proposal.targetCampaign()+":"+namespace+":"+owner+":"+value).getBytes(java.nio.charset.StandardCharsets.UTF_8));String key=namespace+":"+owner+":"+value;String prior=generatedEvents.putIfAbsent(result,key);if(occupied.contains(result)||sourceEventIds.contains(result)||mapping.containsValue(result)||proposal.mappings().stream().anyMatch(v->v.source().equals(result))||(prior!=null&&!prior.equals(key)))throw new IllegalArgumentException();return result;}
 private static Object get(Object o,String field)throws Exception{return o.getClass().getMethod(field).invoke(o);}
 private static Object replace(Object o,String field,Object value)throws Exception{var fs=o.getClass().getRecordComponents();var args=new Object[fs.length];var types=new Class<?>[fs.length];for(int i=0;i<fs.length;i++){types[i]=fs[i].getType();args[i]=fs[i].getName().equals(field)?value:fs[i].getAccessor().invoke(o);}return o.getClass().getDeclaredConstructor(types).newInstance(args);}
 /** Only declared record/field identities are rewritten; scalar strings/maps stay opaque. */
 private Object map(Object o,UUID preview,UUID action)throws Exception{
  if(o==null)return null;if(o instanceof List<?> l){var out=new ArrayList<>();for(Object v:l)out.add(map(v,preview,action));return List.copyOf(out);}
  if(o instanceof ContentEntry||o instanceof CharacterSheet.Source)return o;
  if(o instanceof AttackPreviewService.Preview p)preview=p.id();if(o instanceof CombatService.Action a)action=a.id();
  if(o instanceof CharacterSheet.Condition c){String[] parts=c.id().split(":",2);if(parts.length==2&&Set.of("a10-death","a11-knockout","a12-dagger-knockout","a16-stable","a16-unconscious","a19-stable").contains(parts[0]))return annotation(parts[0],UUID.fromString(parts[1]));return c;}
  if(!o.getClass().isRecord())return o;var fs=o.getClass().getRecordComponents();var values=new Object[fs.length];var types=new Class<?>[fs.length];
  for(int i=0;i<fs.length;i++){String f=fs[i].getName();Object value=fs[i].getAccessor().invoke(o);types[i]=fs[i].getType();
   if(value instanceof UUID u){
    if(o instanceof AttackPreviewService.Create&&f.equals("requestId")){values[i]=u;continue;}
    if(o instanceof AttackPreviewService.Event ev&&f.equals("id")){values[i]=ev.kind().equals("ROLL_KNOCKOUT_FIRST_AID")?id("dice_roll",u):event("preview_event",preview,u);continue;}
    if(o instanceof CombatService.Event&&f.equals("id")){values[i]=event("combat_event",action,u);continue;}
    String ns=UUID_FIELDS.getOrDefault(o.getClass(),Map.of()).get(f);if(ns==null)throw new IllegalArgumentException();values[i]=id(ns,u);continue;
   }   if(value instanceof String s&&!s.isEmpty()){
    if(o instanceof DiceService.Roll&&f.equals("characterId")){values[i]=id("character_card",UUID.fromString(s)).toString();continue;}
    if(f.equals("correctionOf")&&(o instanceof AttackPreviewService.Event||o instanceof CombatService.Event)){values[i]=(o instanceof AttackPreviewService.Event&&eventAliases.containsKey(preview+":"+s)?id("dice_roll",UUID.fromString(s)):event(o instanceof AttackPreviewService.Event?"preview_event":"combat_event",o instanceof AttackPreviewService.Event?preview:action,UUID.fromString(s))).toString();continue;}
    if(o instanceof AttackPreviewService.Event&&f.equals("combatEventId")){values[i]=event("combat_event",action,UUID.fromString(s)).toString();continue;}
   }
   values[i]=map(value,preview,action);
  }
  return o.getClass().getDeclaredConstructor(types).newInstance(values);
 }
 private CharacterSheet.Condition annotation(String kind,UUID owner)throws Exception{
  UUID next=id(kind.startsWith("a16")||kind.startsWith("a19")?"dice_roll":"attack_preview",owner);
  if(kind.equals("a10-death"))return MonsterDeathEffect.annotation(next);if(kind.equals("a11-knockout"))return KnockoutEffect.annotation(next);if(kind.equals("a12-dagger-knockout"))return DaggerKnockoutEffect.annotation(next);
  for(Object value:sourceObjects){if(value instanceof StabilizationService.State state&&state.latest()!=null&&state.latest().id().equals(owner)){var a=state.latest();return StabilizationEffect.plan(a.target().sheet(),a.helper().sheet(),next,a.reviewed(),!a.target().id().equals(a.helper().id()),true,a.roll().result().total()).annotations().stream().filter(c->c.id().startsWith(kind+":")).findFirst().orElseThrow();}if(value instanceof RestabilizationService.State state&&state.latest()!=null&&state.latest().id().equals(owner)){var a=state.latest();return RestabilizationEffect.plan(a.target().sheet(),a.helper().sheet(),a.target().sheet().conditions().getFirst(),next,true,a.reviewed(),!a.target().id().equals(a.helper().id()),true,a.roll().result().total()).annotations().stream().filter(c->c.id().startsWith(kind+":")).findFirst().orElseThrow();}}
  throw new IllegalArgumentException();
 }
 List<CampaignEvidenceGraph.Row> rebindRows(List<CampaignEvidenceGraph.Row> rows)throws Exception{
  var source=new TreeMap<String,Object>();for(var r:rows){Class<?> type=CampaignEvidenceGraph.TYPES.get(r.table());if(type==null)throw new IllegalArgumentException();source.put(key(r.table(),r.id(),r.version()),json.readValue(r.json(),type));}
  sourceObjects=source.values();for(Object value:sourceObjects){if(value instanceof CombatService.Action a)for(var ev:a.events())sourceEventIds.add(ev.id());if(value instanceof AttackPreviewService.Preview p)for(var ev:p.events())sourceEventIds.add(ev.id());}for(Object value:source.values())if(value instanceof AttackPreviewService.Preview p)for(var ev:p.events())if(ev.kind().equals("ROLL_KNOCKOUT_FIRST_AID"))eventAliases.put(p.id()+":"+ev.id(),ev.id());var target=new TreeMap<String,Object>();for(var e:source.entrySet()){Object value=e.getValue();UUID action=value instanceof AttackPreviewService.Preview p?p.evidence().actionId():null;target.put(e.getKey(),map(value,null,action));}
  // Regenerate dependencies before journals that embed their entire damage source.
  for(String table:List.of("attack_preview","stabilization_damage_history","stabilization_healing_history","restabilization_damage_history","restabilization_healing_history","instability_healing_history","initial_instability_healing_history"))for(var r:rows.stream().filter(r->r.table().equals(table)).sorted(Comparator.comparing((CampaignEvidenceGraph.Row r)->r.id().toString()).thenComparingLong(CampaignEvidenceGraph.Row::version)).toList()){
   String k=key(table,r.id(),r.version());Object old=source.get(k),next=target.get(k);String field=table.equals("attack_preview")?"hash":"planHash";
   String original=hash(table,proposal.sourceCampaign(),old,source,false);if(!original.equals(get(old,field)))throw new IllegalArgumentException();
   String regenerated=hash(table,proposal.targetCampaign(),next,target,true);target.put(k,replace(next,field,regenerated));proofs.add(new Proof(table,id(table.equals("attack_preview")?"attack_preview":"character_card",r.id()),old instanceof AttackPreviewService.Preview p?p.version():r.version(),original,regenerated));
  }
  var out=new ArrayList<CampaignEvidenceGraph.Row>();for(var r:rows){String ns=switch(r.table()){case "attack_preview","dice_roll","combat_action"->r.table();default->"character_card";};out.add(new CampaignEvidenceGraph.Row(r.table(),id(ns,r.id()),r.version(),r.originVersion(),r.damageVersion(),id("character_card",r.characterId()),id("character_card",r.targetId()),json.writeValueAsString(target.get(key(r.table(),r.id(),r.version())))));}return List.copyOf(out);
 }
 private static String key(String table,UUID id,long version){return table+":"+id+":"+version;}
 private Object find(Map<String,Object> objects,String table,UUID target,long version)throws Exception{for(var e:objects.entrySet())if(e.getKey().startsWith(table+":")){Object v=e.getValue();if(target.equals(get(v,"id"))&&(long)get(v,"version")==version)return v;}throw new IllegalArgumentException();}
 private Object store(Class<?> type,Map<String,Object> objects)throws Exception{return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,args)->{
  String name=m.getName();if(name.equals("locked")||name.equals("actorLocked"))return ((Supplier<?>)args[args.length-1]).get();if(name.equals("role"))return Movement.Role.DM;
  if(name.equals("roll"))return objects.values().stream().filter(v->v instanceof DiceService.Roll d&&d.id().equals(args[1])).findFirst().orElse(null);
  if(name.equals("history")){String table=switch(type.getEnclosingClass().getSimpleName()){case "StabilizationService"->"stabilization_history";case "RestabilizationService"->"restabilization_history";case "StabilizationDamageService"->"stabilization_damage_history";case "RestabilizationDamageService"->"restabilization_damage_history";default->throw new IllegalArgumentException();};var list=new ArrayList<>();for(var e:objects.entrySet())if(e.getKey().startsWith(table+":" )&&get(e.getValue(),"id").equals(args[1]))list.add(e.getValue());return List.copyOf(list);}
  throw new IllegalStateException("Read only adapter operation rejected");});}
 private Object service(Class<?> type,Map<String,Object> objects)throws Exception{var c=type.getConstructors()[0];var args=new Object[c.getParameterCount()];var ts=c.getParameterTypes();for(int n=0;n<ts.length;n++){Class<?> t=ts[n];args[n]=t==JsonMapper.class?json:t==Clock.class?Clock.systemUTC():t==DiceService.class?service(DiceService.class,objects):t.getSimpleName().equals("Store")?store(t,objects):null;}return c.newInstance(args);}
 private String hash(String table,UUID campaign,Object entry,Map<String,Object> objects,boolean rebound)throws Exception{
  if(entry instanceof AttackPreviewService.Preview p)return DiceService.fingerprint(json,List.of(p.evidence()));
  Class<?> type=switch(table){case "stabilization_healing_history"->StabilizationHealingService.class;case "stabilization_damage_history"->StabilizationDamageService.class;case "restabilization_healing_history"->RestabilizationHealingService.class;case "restabilization_damage_history"->RestabilizationDamageService.class;case "instability_healing_history"->InstabilityHealingService.class;case "initial_instability_healing_history"->InitialInstabilityHealingService.class;default->throw new IllegalArgumentException();};
  Object review=get(entry,"review"),before=get(entry,"before");UUID id=(UUID)get(entry,"id");boolean shared=table.startsWith("stabilization_")||table.startsWith("initial_");Object state=find(objects,shared?"stabilization_history":"restabilization_history",id,(long)get(review,"expectedVersion"));
  var args=new ArrayList<Object>(List.of(campaign,"HISTORICAL_REVIEW",before,state));if(!shared)args.add(find(objects,"stabilization_history",id,(long)get(review,"originVersion")));args.add(review);
  var method=Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals("plan")).findFirst().orElseThrow();method.setAccessible(true);Object plan=method.invoke(service(type,objects),args.toArray());Object effect=get(plan,"effect");if(!get(effect,"status").equals("CANDIDATE"))throw new IllegalArgumentException();return (String)get(plan,"hash");
 }
}
