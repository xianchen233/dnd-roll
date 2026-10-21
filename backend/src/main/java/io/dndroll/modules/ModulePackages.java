package io.dndroll.modules;

import io.dndroll.application.*;
import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** A campaign-scoped inert library. No package data can mutate live play. */
public final class ModulePackages {
    public record Summary(UUID id,ModuleArchive.Manifest manifest,String sha256,List<ModuleArchive.Dependency> missingDependencies) {}
    public record Listing(long campaignVersion,List<Summary> packages) {}
    public record Preview(ModuleArchive.Manifest manifest,String sha256,List<ModuleArchive.Node> nodes,
        List<ModuleArchive.Dependency> missingDependencies,UUID existingId,boolean requiresLegacyConfirmation) {}
    public record Stored(UUID id,ModuleArchive.Manifest manifest,String sha256) {}
    public record Command(UUID requestId,long expectedVersion,boolean createCopy,boolean confirmLegacy) {}
    public record Ack(UUID id,long version) {}
    public record Receipt(String fingerprint,Ack result) {}
    public interface Store {
        List<Stored> list(UUID c);byte[] bytes(UUID c,UUID id);
        void insert(UUID c,Stored value,byte[] bytes);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,String actor,UUID request,Receipt receipt);
    }
    private final Store store;private final CharacterService.Store campaigns;private final ContentService content;private final SceneService.Events events;
    private final ModuleArchive archive=new ModuleArchive();private final JsonMapper json=JsonMapper.builder().build();
    public ModulePackages(Store store,CharacterService.Store campaigns,ContentService content,SceneService.Events events){this.store=store;this.campaigns=campaigns;this.content=content;this.events=events;}
    private void dm(UUID c,String actor){if(campaigns.role(c,actor)!=Role.DM)throw new DomainException("FORBIDDEN",403,"Module preparation requires current DM permission");}
    private List<ModuleArchive.Dependency> missing(UUID c,String actor,ModuleArchive.Manifest m){
        var entries=content.list(c,actor,"","","","","").entries();
        return m.dependencies().stream().filter(d->entries.stream().noneMatch(e->e.id().toString().equals(d.id())&&e.source().revision().equals(d.version())&&e.source().rules().equals(d.rules()))).toList();
    }
    public Listing list(UUID c,String actor){return campaigns.locked(c,()->{dm(c,actor);return new Listing(campaigns.campaignVersion(c),store.list(c).stream().map(s->new Summary(s.id(),s.manifest(),s.sha256(),missing(c,actor,s.manifest()))).toList());});}
    public Preview preview(UUID c,String actor,byte[] bytes){return campaigns.locked(c,()->{dm(c,actor);var verified=verify(bytes);var existing=existing(store.list(c),verified);return new Preview(verified.manifest(),verified.sha256(),verified.nodes(),missing(c,actor,verified.manifest()),existing==null?null:existing.id(),!verified.manifest().rules().equals("5R"));});}
    public Ack importPackage(UUID c,String actor,Command command,byte[] bytes){
        if(command.requestId()==null||command.expectedVersion()<0)throw CharacterSheet.invalid();
        return campaigns.actorLocked(actor,()->campaigns.locked(c,()->{
            dm(c,actor);var verified=verify(bytes);
            String fp=ModuleArchive.hash(json.writeValueAsBytes(List.of(c,command.expectedVersion(),command.createCopy(),command.confirmLegacy(),verified.sha256())));
            var old=store.receipt(actor,command.requestId());if(old!=null){if(!old.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return old.result();}
            AccountPolicy.version(campaigns.campaignVersion(c),command.expectedVersion());
            if(!verified.manifest().rules().equals("5R")&&!command.confirmLegacy())throw new DomainException("LEGACY_CONFIRMATION_REQUIRED",400,"Explicit legacy rules confirmation required");
            var entries=store.list(c);var previous=existing(entries,verified);UUID id;
            if(previous!=null&&!command.createCopy())id=previous.id();
            else {if(entries.size()>=100)throw CharacterSheet.invalid();id=UUID.randomUUID();store.insert(c,new Stored(id,verified.manifest(),verified.sha256()),bytes);campaigns.advanceCampaign(c);events.changed(c);}
            var ack=new Ack(id,campaigns.campaignVersion(c));store.receipt(c,actor,command.requestId(),new Receipt(fp,ack));return ack;
        }));
    }
    private Stored existing(List<Stored> entries,ModuleArchive.Verified verified){
        var matches=entries.stream().filter(s->s.manifest().moduleId().equals(verified.manifest().moduleId())&&s.manifest().moduleVersion().equals(verified.manifest().moduleVersion())).toList();
        if(matches.stream().anyMatch(s->!s.sha256().equals(verified.sha256())))throw new DomainException("MODULE_VERSION_CONFLICT",409,"Module identity/version already has different bytes; publish a new version");
        return matches.isEmpty()?null:matches.getFirst();
    }
    public byte[] download(UUID c,UUID id,String actor){return campaigns.locked(c,()->{dm(c,actor);byte[] bytes=store.bytes(c,id);if(bytes==null)throw new DomainException("NOT_FOUND",404,"Module package not found");return bytes;});}
    private ModuleArchive.Verified verify(byte[] bytes){
        if(bytes==null||bytes.length<1||bytes.length>ModuleArchive.MAX_BYTES)throw new DomainException("INVALID_MODULE",400,"Invalid or incompatible module package");
        Path temporary=null;
        try {temporary=Files.createTempFile("dnd-module-",".zip");Files.write(temporary,bytes);return archive.verify(temporary);}
        catch(IOException failure){throw new DomainException("MODULE_UNAVAILABLE",503,"Module validation unavailable");}
        finally {if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException ignored){/* Never a published library entry; OS temporary storage only. */}}
    }
}
