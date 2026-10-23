package io.dndroll.modules;

import io.dndroll.application.*;
import io.dndroll.domain.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Immutable template provenance and editable definitions never alias live play. */
public final class ModuleLifecycle {
    public record Copy(UUID id,String name,long version,UUID sourcePackageId,String sourceSha256,ModuleBlueprint.Document baseline,ModuleBlueprint.Document document) {}
    public record Summary(UUID id,String name,long version,UUID sourcePackageId) {}
    public record Listing(long campaignVersion,List<Summary> customizations) {}
    public record Detail(UUID id,String name,long version,UUID sourcePackageId,String sourceSha256,ModuleBlueprint.Document baseline,ModuleBlueprint.Document document,List<ModuleArchive.Dependency> missingDependencies) {}
    public record Source(UUID campaignId,UUID packageId,String sha256,ModuleArchive.Manifest manifest,UUID customizationId,long customizationVersion) {}
    public record Instance(long version,Source source,ModuleBlueprint.Document document,Map<String,Map<String,UUID>> mapping,Map<String,String> progress) {}
    public record Create(UUID requestId,long expectedVersion,UUID packageId,String name) {}
    public record Edit(UUID requestId,long expectedVersion,String name,ModuleBlueprint.Document document) {}
    public record Save(UUID requestId,long expectedVersion,String moduleId,String moduleVersion,String name) {}
    public record Launch(UUID requestId,long expectedVersion,String name) {}
    public record Progress(UUID requestId,long expectedVersion,Map<String,String> progress) {}
    public record UpdatePreviewRequest(long expectedVersion,UUID packageId) {}
    public record UpdatePreview(long version,UUID packageId,String sha256,List<ModuleUpdate.Change> changes,List<ModuleArchive.Dependency> missingDependencies) {}
    public record ImpactRequest(long expectedVersion,UUID packageId,List<ModuleUpdate.Selection> selections) {}
    public record ApplyUpdate(UUID requestId,long expectedVersion,UUID packageId,List<ModuleUpdate.Selection> selections,String sha256,String impactHash) {}
    public record Impact(long version,UUID packageId,String sha256,String impactHash,ModuleBlueprint.Document document,ModuleArchive.Manifest manifest,List<ModuleUpdate.Change> changes,List<ModuleArchive.Dependency> missingDependencies) {}
    public record UpdateEvidence(long version,String actor,UUID requestId,UUID packageId,String sha256,String impactHash,List<ModuleUpdate.Selection> selections,Copy before) {}
    public record Ack(UUID id,long version) {}
    public record LiveReview(long expectedVersion,long customizationVersion) {}
    public record LiveApply(UUID requestId,long expectedVersion,long customizationVersion,String impactHash) {}
    public record LiveImpact(long version,long customizationVersion,String impactHash,List<ModuleUpdate.Change> changes,List<String> blockers) {}
    public record LiveEvidence(long version,String actor,UUID requestId,String impactHash,Instance before) {}
    public record MigrationApply(UUID requestId,long expectedVersion,long customizationVersion,String impactHash,List<String> confirmations) {}
    public record MigrationImpact(long version,long customizationVersion,String impactHash,List<String> confirmations,List<String> blockers,List<ModuleUpdate.Change> changes,Object runtime) {}
    public record MigrationEvidence(long version,String actor,UUID requestId,String impactHash,Instance before,UUID recoveryCampaignId,Object runtime) {}
    public record Receipt(String fingerprint,String kind,Ack result) {}
    public interface Store {
        List<Summary> list(UUID c);Copy copy(UUID c,UUID id);void copy(UUID c,Copy value,boolean create);
        Instance instance(UUID c);void instance(UUID c,Instance value,boolean create);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,String actor,UUID request,Receipt value);
        byte[] effectiveArchive(UUID c,UUID id);void updateArchive(UUID c,UUID id,byte[] bytes,byte[] before,UpdateEvidence evidence);
        List<UpdateEvidence> updates(UUID c,UUID id);
        byte[] beforeArchive(UUID c,UUID id,long version);
        List<LiveEvidence> liveUpdates(UUID c);void liveUpdate(UUID c,LiveEvidence value);
        List<MigrationEvidence> migrations(UUID c);void migration(UUID c,MigrationEvidence value);
    }
    public interface Provisioner {
        Map<String,Map<String,UUID>> create(UUID source,UUID target,String actor,String name,ModuleArchive.Verified verified,ModuleBlueprint.Document document,Map<String,byte[]> images);
        Object runtime(UUID c);
        ModuleBlueprint.Document recovery(UUID c,Instance before);
        List<String> migrationBlockers(UUID c,Instance before,ModuleBlueprint.Document next);
        Map<String,Map<String,UUID>> migrate(UUID c,String actor,Instance before,ModuleBlueprint.Document next);
    }
    private final Store store;private final CharacterService.Store campaigns;private final ModulePackages packages;private final ContentService content;private final Provisioner provisioner;private final SceneService.Events events;
    private final ModuleArchive archive=new ModuleArchive();private final JsonMapper json=JsonMapper.builder().build();
    public ModuleLifecycle(Store store,CharacterService.Store campaigns,ModulePackages packages,ContentService content,Provisioner provisioner,SceneService.Events events){this.store=store;this.campaigns=campaigns;this.packages=packages;this.content=content;this.provisioner=provisioner;this.events=events;}
    private void dm(UUID c,String actor){AccountPolicy.dm(campaigns.role(c,actor));}
    private Copy target(UUID c,UUID id){var value=store.copy(c,id);if(value==null)throw missing();return value;}
    private Instance instance(UUID c){var value=store.instance(c);if(value==null)throw missing();return value;}
    private byte[] sourceBytes(UUID c,Copy copy,String actor){byte[] effective=store.effectiveArchive(c,copy.id());return effective==null?packages.download(c,copy.sourcePackageId(),actor):effective;}
    private ModuleArchive.Verified source(UUID c,Copy copy,String actor){return archive.verify(sourceBytes(c,copy,actor));}
    private List<ModuleArchive.Dependency> missing(UUID c,String actor,ModuleArchive.Manifest manifest){var entries=content.list(c,actor,"","","","","").entries();return manifest.dependencies().stream().filter(d->entries.stream().noneMatch(e->e.id().toString().equals(d.id())&&e.source().revision().equals(d.version())&&e.source().rules().equals(d.rules()))).toList();}
    public Listing list(UUID c,String actor){return campaigns.locked(c,()->{dm(c,actor);return new Listing(campaigns.campaignVersion(c),store.list(c));});}
    public Detail detail(UUID c,UUID id,String actor){return campaigns.locked(c,()->{dm(c,actor);var v=target(c,id);return new Detail(v.id(),v.name(),v.version(),v.sourcePackageId(),v.sourceSha256(),v.baseline(),v.document(),missing(c,actor,source(c,v,actor).manifest()));});}
    public Instance readInstance(UUID c,String actor){return campaigns.locked(c,()->{dm(c,actor);return instance(c);});}
    public UpdatePreview previewUpdate(UUID c,UUID id,String actor,UpdatePreviewRequest r){return campaigns.locked(c,()->{
        dm(c,actor);var copy=target(c,id);if(r.expectedVersion()<0||r.packageId()==null)throw ModuleBlueprint.invalid();AccountPolicy.version(copy.version(),r.expectedVersion());
        var original=archive.verify(packages.download(c,copy.sourcePackageId(),actor));var incoming=archive.verify(packages.download(c,r.packageId(),actor));
        if(!original.manifest().moduleId().equals(incoming.manifest().moduleId())||!original.manifest().rules().equals(incoming.manifest().rules())||original.manifest().moduleVersion().equals(incoming.manifest().moduleVersion()))throw new DomainException("MODULE_VERSION_CONFLICT",409,"Choose a different version of the same module and rules family");
        return new UpdatePreview(copy.version(),r.packageId(),incoming.sha256(),ModuleUpdate.compare(copy.baseline(),copy.document(),incoming.document()),missing(c,actor,incoming.manifest()));
    });}
    private record Planned(Impact impact,byte[] bytes,byte[] before) {}
    public UpdatePreview updateOptions(UUID c,UUID id,String actor,UpdatePreviewRequest r){return campaigns.locked(c,()->{
        var preview=previewUpdate(c,id,actor,r);var copy=target(c,id);var current=source(c,copy,actor);
        current=new ModuleArchive.Verified(current.manifest(),copy.document().nodes(),current.sha256(),copy.document());
        return new UpdatePreview(copy.version(),r.packageId(),preview.sha256(),ModuleUpdate.comparePackages(archive.verify(packages.download(c,copy.sourcePackageId(),actor)),current,archive.verify(packages.download(c,r.packageId(),actor))),preview.missingDependencies());
    });}
    private Planned plan(UUID c,UUID id,String actor,ImpactRequest r){
        dm(c,actor);var copy=target(c,id);previewUpdate(c,id,actor,new UpdatePreviewRequest(r.expectedVersion(),r.packageId()));
        byte[] originalBytes=packages.download(c,copy.sourcePackageId(),actor),before=sourceBytes(c,copy,actor),incomingBytes=packages.download(c,r.packageId(),actor);
        var original=archive.verify(originalBytes);var current=archive.verify(before);var incoming=archive.verify(incomingBytes);
        current=new ModuleArchive.Verified(current.manifest(),copy.document().nodes(),current.sha256(),copy.document());
        var prepared=ModuleUpdate.prepare(original,current,incoming,archive.images(before),archive.images(incomingBytes),r.selections());
        byte[] bytes=archive.export(prepared.manifest(),prepared.document(),prepared.images(),original.manifest().moduleId(),original.manifest().moduleVersion(),copy.name());
        var result=archive.verify(bytes);var absent=missing(c,actor,result.manifest());
        String hash=DiceService.fingerprint(json,List.of(c,id,copy.version(),copy.sourceSha256(),ModuleArchive.hash(before),incoming.sha256(),r.selections(),result.sha256(),absent));
        return new Planned(new Impact(copy.version(),r.packageId(),incoming.sha256(),hash,prepared.document(),result.manifest(),prepared.changes(),absent),bytes,before);
    }
    public Impact impactUpdate(UUID c,UUID id,String actor,ImpactRequest r){return campaigns.locked(c,()->plan(c,id,actor,r).impact());}
    public List<UpdateEvidence> updates(UUID c,UUID id,String actor){return campaigns.locked(c,()->{dm(c,actor);target(c,id);return store.updates(c,id);});}
    public byte[] recoverUpdate(UUID c,UUID id,long version,String actor){return campaigns.locked(c,()->{
        dm(c,actor);target(c,id);var evidence=store.updates(c,id).stream().filter(v->v.version()==version).findFirst().orElseThrow(ModuleLifecycle::missing);
        byte[] bytes=store.beforeArchive(c,id,version);var verified=archive.verify(bytes);
        return archive.export(verified.manifest(),evidence.before().document(),archive.images(bytes),UUID.nameUUIDFromBytes((id+":"+version).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),"1.0.0",evidence.before().name());
    });}
    public Ack applyUpdate(UUID c,UUID id,String actor,ApplyUpdate r){return command(c,id,actor,r.requestId(),r.expectedVersion(),"UPDATE",r,()->{
        var planned=plan(c,id,actor,new ImpactRequest(r.expectedVersion(),r.packageId(),r.selections()));var impact=planned.impact();
        if(!Objects.equals(r.sha256(),impact.sha256())||!Objects.equals(r.impactHash(),impact.impactHash()))throw new DomainException("MODULE_IMPACT_CONFLICT",409,"Review the current impact before applying");
        if(!impact.missingDependencies().isEmpty())throw new DomainException("MODULE_DEPENDENCIES_MISSING",409,"Resolve exact module dependencies before applying");
        var old=target(c,id);var next=new Copy(id,old.name(),old.version()+1,old.sourcePackageId(),old.sourceSha256(),old.baseline(),impact.document());
        store.updateArchive(c,id,planned.bytes(),planned.before(),new UpdateEvidence(next.version(),actor,r.requestId(),r.packageId(),r.sha256(),r.impactHash(),List.copyOf(r.selections()),old));
        store.copy(c,next,false);return new Ack(id,next.version());
    });}
    private interface Mutation {Ack apply();}
    private Ack command(UUID c,UUID id,String actor,UUID request,long expected,String kind,Object body,Mutation change){
        if(request==null||expected<0)throw ModuleBlueprint.invalid();
        return campaigns.actorLocked(actor,()->campaigns.locked(c,()->{
            dm(c,actor);if(id!=null){if(kind.equals("PROGRESS"))instance(c);else target(c,id);}
            String fp=DiceService.fingerprint(json,List.of(c,id==null?"":id,kind,body));var old=store.receipt(actor,request);
            if(old!=null){if(!fp.equals(old.fingerprint()))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");if(old.kind().equals("CAMPAIGN"))campaigns.locked(old.result().id(),()->{dm(old.result().id(),actor);return true;});return old.result();}
            long actual=id==null?campaigns.campaignVersion(c):kind.equals("PROGRESS")?instance(c).version():target(c,id).version();AccountPolicy.version(actual,expected);
            var result=change.apply();store.receipt(c,actor,request,new Receipt(fp,kind,result));events.changed(c);return result;
        }));
    }
    public Ack create(UUID c,String actor,Create r){return command(c,null,actor,r.requestId(),r.expectedVersion(),"COPY",r,()->{
        CharacterSheet.text(r.name(),100,true);if(r.packageId()==null||store.list(c).size()>=100)throw ModuleBlueprint.invalid();var verified=archive.verify(packages.download(c,r.packageId(),actor));UUID id=UUID.randomUUID();
        store.copy(c,new Copy(id,r.name(),0,r.packageId(),verified.sha256(),verified.document(),verified.document()),true);campaigns.advanceCampaign(c);return new Ack(id,0);
    });}
    public Ack edit(UUID c,UUID id,String actor,Edit r){return command(c,id,actor,r.requestId(),r.expectedVersion(),"EDIT",r,()->{
        CharacterSheet.text(r.name(),100,true);var v=target(c,id);ModuleBlueprint.validate(r.document(),source(c,v,actor).manifest());store.copy(c,new Copy(id,r.name(),v.version()+1,v.sourcePackageId(),v.sourceSha256(),v.baseline(),r.document()),false);return new Ack(id,v.version()+1);
    });}
    public Ack save(UUID c,UUID id,String actor,Save r){return command(c,id,actor,r.requestId(),r.expectedVersion(),"TEMPLATE",r,()->{
        var v=target(c,id);byte[] source=sourceBytes(c,v,actor);var verified=archive.verify(source);
        // Save-as never aliases an existing identity, even when its bytes happen to match.
        if(packages.list(c,actor).packages().stream().anyMatch(p->p.manifest().moduleId().equals(r.moduleId())&&p.manifest().moduleVersion().equals(r.moduleVersion())))throw new DomainException("MODULE_VERSION_CONFLICT",409,"Choose an unused template identity/version");
        byte[] bytes=archive.export(verified.manifest(),v.document(),archive.images(source),r.moduleId(),r.moduleVersion(),r.name());var result=packages.importPackage(c,actor,new ModulePackages.Command(UUID.randomUUID(),campaigns.campaignVersion(c),false,true),bytes);return new Ack(result.id(),result.version());
    });}
    public Ack launch(UUID c,UUID id,String actor,Launch r){return command(c,id,actor,r.requestId(),r.expectedVersion(),"CAMPAIGN",r,()->{
        CharacterSheet.text(r.name(),100,true);var v=target(c,id);byte[] bytes=sourceBytes(c,v,actor);var verified=archive.verify(bytes);ModuleBlueprint.validate(v.document(),verified.manifest());
        if(!missing(c,actor,verified.manifest()).isEmpty())throw new DomainException("MODULE_DEPENDENCIES_MISSING",409,"Resolve exact module dependencies before campaign creation");
        if(!verified.manifest().rules().equals("5R"))throw new DomainException("MODULE_RULES_UNSUPPORTED",409,"Live campaigns require an explicitly adapted 5R template");
        UUID target=UUID.randomUUID();var mapping=provisioner.create(c,target,actor,r.name(),verified,v.document(),archive.images(bytes));store.instance(target,new Instance(0,new Source(c,v.sourcePackageId(),v.sourceSha256(),verified.manifest(),id,v.version()),v.document(),mapping,Map.of()),true);return new Ack(target,0);
    });}
    public Ack progress(UUID c,String actor,Progress r){return command(c,c,actor,r.requestId(),r.expectedVersion(),"PROGRESS",r,()->{
        var v=instance(c);if(r.progress()==null||r.progress().size()>500)throw ModuleBlueprint.invalid();var keys=v.document().nodes().stream().map(ModuleArchive.Node::id).toList();
        for(var e:r.progress().entrySet()){if(!keys.contains(e.getKey()))throw ModuleBlueprint.invalid();CharacterSheet.text(e.getValue(),2000,false);}
        var next=new Instance(v.version()+1,v.source(),v.document(),v.mapping(),Map.copyOf(r.progress()));store.instance(c,next,false);return new Ack(c,next.version());
    });}
    // The source is immutable provenance. Lock in launch order (source, then target),
    // rechecking both memberships under the same transaction before reading a receipt.
    private <T>T liveLocked(UUID c,String actor,java.util.function.Supplier<T> work){
        UUID source=readInstance(c,actor).source().campaignId();
        return campaigns.actorLocked(actor,()->campaigns.locked(source,()->campaigns.locked(c,()->{dm(c,actor);dm(source,actor);return work.get();})));
    }
    private LiveImpact livePlan(UUID c,String actor,LiveReview r){
        var live=instance(c);var s=live.source();var copy=target(s.campaignId(),s.customizationId());
        AccountPolicy.version(live.version(),r.expectedVersion());AccountPolicy.version(copy.version(),r.customizationVersion());
        var manifest=source(s.campaignId(),copy,actor).manifest();ModuleBlueprint.validate(copy.document(),manifest);
        var changes=ModuleUpdate.compare(live.document(),live.document(),copy.document());var blockers=new ArrayList<String>();
        // Only literal preparation text changes are supported here. Structural,
        // visibility and executable definitions must pass a later migration gate.
        for(var change:changes)if(!change.path().matches("/nodes/[0-9a-f-]+/(name|text)"))blockers.add(change.path());
        var old=s.manifest();
        if(!old.assets().equals(manifest.assets())||!old.dependencies().equals(manifest.dependencies())||!old.rights().equals(manifest.rights())||!old.rules().equals(manifest.rules()))blockers.add("/manifest");
        if(!missing(s.campaignId(),actor,manifest).isEmpty())blockers.add("/dependencies");
        String hash=DiceService.fingerprint(json,List.of(c,live,copy,manifest,changes,blockers));
        return new LiveImpact(live.version(),copy.version(),hash,changes,List.copyOf(blockers));
    }
    public LiveImpact liveImpact(UUID c,String actor,LiveReview r){return liveLocked(c,actor,()->livePlan(c,actor,r));}
    public List<LiveEvidence> liveUpdates(UUID c,String actor){return campaigns.locked(c,()->{dm(c,actor);instance(c);return store.liveUpdates(c);});}
    public Ack applyLive(UUID c,String actor,LiveApply r){
        if(r.requestId()==null||r.expectedVersion()<0||r.customizationVersion()<0)throw ModuleBlueprint.invalid();
        return liveLocked(c,actor,()->{
            String fp=DiceService.fingerprint(json,List.of(c,"LIVE",r));var receipt=store.receipt(actor,r.requestId());
            if(receipt!=null){if(!fp.equals(receipt.fingerprint()))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return receipt.result();}
            var impact=livePlan(c,actor,new LiveReview(r.expectedVersion(),r.customizationVersion()));
            if(!Objects.equals(impact.impactHash(),r.impactHash()))throw new DomainException("MODULE_IMPACT_CONFLICT",409,"Review current instance impact");
            if(!impact.blockers().isEmpty())throw new DomainException("MODULE_LIVE_MIGRATION_REQUIRED",409,"Structural changes require explicit migration; live play was preserved");
            var before=instance(c);var s=before.source();var copy=target(s.campaignId(),s.customizationId());long version=before.version()+1;
            var next=new Instance(version,new Source(s.campaignId(),s.packageId(),s.sha256(),s.manifest(),s.customizationId(),copy.version()),copy.document(),before.mapping(),before.progress());
            store.liveUpdate(c,new LiveEvidence(version,actor,r.requestId(),r.impactHash(),before));store.instance(c,next,false);
            var ack=new Ack(c,version);store.receipt(c,actor,r.requestId(),new Receipt(fp,"LIVE",ack));events.changed(c);return ack;
        });
    }
    private static DomainException missing(){return new DomainException("NOT_FOUND",404,"Module preparation not found");}

    private MigrationImpact migrationPlan(UUID c,String actor,LiveReview r){
        var impact=livePlan(c,actor,r);var before=instance(c);var s=before.source();var next=target(s.campaignId(),s.customizationId()).document();
        var blockers=new ArrayList<String>();
        for(String b:impact.blockers())if(b.startsWith("/manifest")||b.equals("/dependencies")||b.contains("admissionMode")||b.contains("approvedDependencyIds"))blockers.add(b);
        // Published materials are independent play records; changes require their own review.
        var oldDocs=before.document().nodes().stream().filter(n->n.kind().equals("DOCUMENT")).toList();
        var newDocs=next.nodes().stream().filter(n->n.kind().equals("DOCUMENT")).toList();if(!oldDocs.equals(newDocs))blockers.add("/published-materials");
        blockers.addAll(provisioner.migrationBlockers(c,before,next));
        ModuleBlueprint.Document recovery=null;
        if(blockers.isEmpty())try{recovery=provisioner.recovery(c,before);ModuleBlueprint.validate(recovery,s.manifest());}
        catch(DomainException invalidRecovery){blockers.add("/recovery-definition");}
        var confirmations=new ArrayList<String>();
        if(recovery!=null)for(var unit:recovery.play().units())if(!before.mapping().get("units").containsKey(unit.nodeId()))confirmations.add("RECOVER_EXTRA_UNIT "+unit.nodeId()+"; independent card with DM-only control; no combat or history replay");
        for(var change:impact.changes())confirmations.add("APPLY "+change.path());
        for(var scene:before.document().play().scenes())if(next.play().scenes().stream().noneMatch(n->n.nodeId().equals(scene.nodeId())))confirmations.add("RETIRE_SCENE "+scene.nodeId()+"; retain rows and history; switch current scene if necessary");
        for(var unit:before.document().play().units())if(next.play().units().stream().noneMatch(n->n.nodeId().equals(unit.nodeId())))confirmations.add("RETAIN_COMBAT_IDENTITY "+unit.nodeId()+"; old HP, resources, conditions and turns remain; replacement does not enter combat automatically");
        confirmations.add("CREATE_DM_ONLY_RECOVERY; preserve live HP/resources/conditions; maps fully covered; original combat and records stay in original campaign");
        if(confirmations.size()>500)blockers.add("/confirmation-limit");
        Object runtime=provisioner.runtime(c);String hash=DiceService.fingerprint(json,List.of(impact.impactHash(),runtime,confirmations,blockers));
        return new MigrationImpact(before.version(),r.customizationVersion(),hash,List.copyOf(confirmations),List.copyOf(blockers),impact.changes(),runtime);
    }
    public MigrationImpact migrationImpact(UUID c,String actor,LiveReview r){return liveLocked(c,actor,()->migrationPlan(c,actor,r));}
    public List<MigrationEvidence> migrations(UUID c,String actor){return campaigns.locked(c,()->{dm(c,actor);instance(c);return store.migrations(c);});}
    public Ack migrate(UUID c,String actor,MigrationApply r){
        if(r.requestId()==null||r.expectedVersion()<0||r.customizationVersion()<0||r.confirmations()==null)throw ModuleBlueprint.invalid();
        return liveLocked(c,actor,()->{
            String fp=DiceService.fingerprint(json,List.of(c,"MIGRATE",r));var receipt=store.receipt(actor,r.requestId());
            if(receipt!=null){if(!fp.equals(receipt.fingerprint()))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");var evidence=store.migrations(c).stream().filter(e->e.requestId().equals(r.requestId())&&e.actor().equals(actor)).findFirst().orElseThrow(ModuleLifecycle::missing);dm(evidence.recoveryCampaignId(),actor);return receipt.result();}
            var impact=migrationPlan(c,actor,new LiveReview(r.expectedVersion(),r.customizationVersion()));
            if(!Objects.equals(impact.impactHash(),r.impactHash()))throw new DomainException("MODULE_IMPACT_CONFLICT",409,"Review current runtime impact");
            if(!impact.blockers().isEmpty())throw new DomainException("MODULE_LIVE_MIGRATION_REQUIRED",409,"Resolve structural migration blockers");
            if(r.confirmations().size()!=impact.confirmations().size()||!new HashSet<>(r.confirmations()).equals(new HashSet<>(impact.confirmations())))throw new DomainException("MODULE_CONFIRMATION_REQUIRED",409,"Confirm every exact structural impact");
            var before=instance(c);var s=before.source();var copy=target(s.campaignId(),s.customizationId());Object runtime=provisioner.runtime(c);
            var oldDocument=provisioner.recovery(c,before);UUID recovery=UUID.randomUUID();
            byte[] bytes=sourceBytes(s.campaignId(),copy,actor);var verified=archive.verify(bytes);
            var recoveryMapping=provisioner.create(s.campaignId(),recovery,actor,"迁移前恢复副本",verified,oldDocument,archive.images(bytes));
            store.instance(recovery,new Instance(0,s,oldDocument,recoveryMapping,before.progress()),true);
            var mapping=provisioner.migrate(c,actor,before,copy.document());long version=before.version()+1;
            store.migration(c,new MigrationEvidence(version,actor,r.requestId(),r.impactHash(),before,recovery,runtime));
            store.instance(c,new Instance(version,new Source(s.campaignId(),s.packageId(),s.sha256(),s.manifest(),s.customizationId(),copy.version()),copy.document(),mapping,before.progress()),false);
            var ack=new Ack(c,version);store.receipt(c,actor,r.requestId(),new Receipt(fp,"MIGRATE",ack));events.changed(c);return ack;
        });
    }
}
