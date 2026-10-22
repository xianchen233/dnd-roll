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
    public record Ack(UUID id,long version) {}
    public record Receipt(String fingerprint,String kind,Ack result) {}
    public interface Store {
        List<Summary> list(UUID c);Copy copy(UUID c,UUID id);void copy(UUID c,Copy value,boolean create);
        Instance instance(UUID c);void instance(UUID c,Instance value,boolean create);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,String actor,UUID request,Receipt value);
    }
    public interface Provisioner {Map<String,Map<String,UUID>> create(UUID source,UUID target,String actor,String name,ModuleArchive.Verified verified,ModuleBlueprint.Document document,Map<String,byte[]> images);}
    private final Store store;private final CharacterService.Store campaigns;private final ModulePackages packages;private final ContentService content;private final Provisioner provisioner;private final SceneService.Events events;
    private final ModuleArchive archive=new ModuleArchive();private final JsonMapper json=JsonMapper.builder().build();
    public ModuleLifecycle(Store store,CharacterService.Store campaigns,ModulePackages packages,ContentService content,Provisioner provisioner,SceneService.Events events){this.store=store;this.campaigns=campaigns;this.packages=packages;this.content=content;this.provisioner=provisioner;this.events=events;}
    private void dm(UUID c,String actor){AccountPolicy.dm(campaigns.role(c,actor));}
    private Copy target(UUID c,UUID id){var value=store.copy(c,id);if(value==null)throw missing();return value;}
    private Instance instance(UUID c){var value=store.instance(c);if(value==null)throw missing();return value;}
    private ModuleArchive.Verified source(UUID c,Copy copy,String actor){return archive.verify(packages.download(c,copy.sourcePackageId(),actor));}
    private List<ModuleArchive.Dependency> missing(UUID c,String actor,ModuleArchive.Manifest manifest){var entries=content.list(c,actor,"","","","","").entries();return manifest.dependencies().stream().filter(d->entries.stream().noneMatch(e->e.id().toString().equals(d.id())&&e.source().revision().equals(d.version())&&e.source().rules().equals(d.rules()))).toList();}
    public Listing list(UUID c,String actor){return campaigns.locked(c,()->{dm(c,actor);return new Listing(campaigns.campaignVersion(c),store.list(c));});}
    public Detail detail(UUID c,UUID id,String actor){return campaigns.locked(c,()->{dm(c,actor);var v=target(c,id);return new Detail(v.id(),v.name(),v.version(),v.sourcePackageId(),v.sourceSha256(),v.baseline(),v.document(),missing(c,actor,source(c,v,actor).manifest()));});}
    public Instance readInstance(UUID c,String actor){return campaigns.locked(c,()->{dm(c,actor);return instance(c);});}
    public UpdatePreview previewUpdate(UUID c,UUID id,String actor,UpdatePreviewRequest r){return campaigns.locked(c,()->{
        dm(c,actor);var copy=target(c,id);if(r.expectedVersion()<0||r.packageId()==null)throw ModuleBlueprint.invalid();AccountPolicy.version(copy.version(),r.expectedVersion());
        var original=source(c,copy,actor);var incoming=archive.verify(packages.download(c,r.packageId(),actor));
        if(!original.manifest().moduleId().equals(incoming.manifest().moduleId())||!original.manifest().rules().equals(incoming.manifest().rules())||original.manifest().moduleVersion().equals(incoming.manifest().moduleVersion()))throw new DomainException("MODULE_VERSION_CONFLICT",409,"Choose a different version of the same module and rules family");
        return new UpdatePreview(copy.version(),r.packageId(),incoming.sha256(),ModuleUpdate.compare(copy.baseline(),copy.document(),incoming.document()),missing(c,actor,incoming.manifest()));
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
        var v=target(c,id);byte[] source=packages.download(c,v.sourcePackageId(),actor);var verified=archive.verify(source);
        // Save-as never aliases an existing identity, even when its bytes happen to match.
        if(packages.list(c,actor).packages().stream().anyMatch(p->p.manifest().moduleId().equals(r.moduleId())&&p.manifest().moduleVersion().equals(r.moduleVersion())))throw new DomainException("MODULE_VERSION_CONFLICT",409,"Choose an unused template identity/version");
        byte[] bytes=archive.export(verified.manifest(),v.document(),archive.images(source),r.moduleId(),r.moduleVersion(),r.name());var result=packages.importPackage(c,actor,new ModulePackages.Command(UUID.randomUUID(),campaigns.campaignVersion(c),false,true),bytes);return new Ack(result.id(),result.version());
    });}
    public Ack launch(UUID c,UUID id,String actor,Launch r){return command(c,id,actor,r.requestId(),r.expectedVersion(),"CAMPAIGN",r,()->{
        CharacterSheet.text(r.name(),100,true);var v=target(c,id);byte[] bytes=packages.download(c,v.sourcePackageId(),actor);var verified=archive.verify(bytes);ModuleBlueprint.validate(v.document(),verified.manifest());
        if(!missing(c,actor,verified.manifest()).isEmpty())throw new DomainException("MODULE_DEPENDENCIES_MISSING",409,"Resolve exact module dependencies before campaign creation");
        if(!verified.manifest().rules().equals("5R"))throw new DomainException("MODULE_RULES_UNSUPPORTED",409,"Live campaigns require an explicitly adapted 5R template");
        UUID target=UUID.randomUUID();var mapping=provisioner.create(c,target,actor,r.name(),verified,v.document(),archive.images(bytes));store.instance(target,new Instance(0,new Source(c,v.sourcePackageId(),v.sourceSha256(),verified.manifest(),id,v.version()),v.document(),mapping,Map.of()),true);return new Ack(target,0);
    });}
    public Ack progress(UUID c,String actor,Progress r){return command(c,c,actor,r.requestId(),r.expectedVersion(),"PROGRESS",r,()->{
        var v=instance(c);if(r.progress()==null||r.progress().size()>500)throw ModuleBlueprint.invalid();var keys=v.document().nodes().stream().map(ModuleArchive.Node::id).toList();
        for(var e:r.progress().entrySet()){if(!keys.contains(e.getKey()))throw ModuleBlueprint.invalid();CharacterSheet.text(e.getValue(),2000,false);}
        var next=new Instance(v.version()+1,v.source(),v.document(),v.mapping(),Map.copyOf(r.progress()));store.instance(c,next,false);return new Ack(c,next.version());
    });}
    private static DomainException missing(){return new DomainException("NOT_FOUND",404,"Module preparation not found");}
}
