package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.modules.*;
import io.dndroll.modules.ModuleLifecycle.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/campaigns/{campaignId}")
public class ModuleLifecycleController {
    private final ModuleLifecycle service;
    public ModuleLifecycleController(ModuleLifecycle service){this.service=service;}
    @Configuration static class Wiring {@Bean ModuleLifecycle moduleLifecycle(Store store,CharacterService.Store campaigns,ModulePackages packages,ContentService content,Provisioner provisioner,SceneService.Events events){return new ModuleLifecycle(store,campaigns,packages,content,provisioner,events);}}
    @GetMapping("/module-customizations") Listing list(@PathVariable UUID campaignId,Principal p){return service.list(campaignId,p.getName());}
    @PostMapping("/module-customizations") Ack create(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){return service.create(campaignId,p.getName(),ModuleBlueprint.command(body,Create.class));}
    @GetMapping("/module-customizations/{id}") Detail detail(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.detail(campaignId,id,p.getName());}
    @PostMapping("/module-customizations/{id}") Ack edit(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){return service.edit(campaignId,id,p.getName(),ModuleBlueprint.command(body,Edit.class));}
    @PostMapping("/module-customizations/{id}/templates") Ack save(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){return service.save(campaignId,id,p.getName(),ModuleBlueprint.command(body,Save.class));}
    @PostMapping("/module-customizations/{id}/campaigns") Ack launch(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){return service.launch(campaignId,id,p.getName(),ModuleBlueprint.command(body,Launch.class));}
    @PostMapping("/module-customizations/{id}/update-preview") UpdatePreview previewUpdate(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){return service.previewUpdate(campaignId,id,p.getName(),ModuleBlueprint.command(body,UpdatePreviewRequest.class));}
    @PostMapping("/module-customizations/{id}/update-impact") Impact impactUpdate(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){return service.impactUpdate(campaignId,id,p.getName(),ModuleBlueprint.command(body,ImpactRequest.class));}
    @PostMapping("/module-customizations/{id}/update-options") UpdatePreview updateOptions(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){return service.updateOptions(campaignId,id,p.getName(),ModuleBlueprint.command(body,UpdatePreviewRequest.class));}
    @PostMapping("/module-customizations/{id}/updates") Ack applyUpdate(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){return service.applyUpdate(campaignId,id,p.getName(),ModuleBlueprint.command(body,ApplyUpdate.class));}
    @GetMapping("/module-customizations/{id}/updates") java.util.List<UpdateEvidence> updates(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.updates(campaignId,id,p.getName());}
    @GetMapping("/module-customizations/{id}/updates/{version}/content") org.springframework.http.ResponseEntity<byte[]> recoverUpdate(@PathVariable UUID campaignId,@PathVariable UUID id,@PathVariable long version,Principal p){return org.springframework.http.ResponseEntity.ok().header("Content-Type","application/zip").header("Content-Disposition","attachment; filename=module-before-update.zip").body(service.recoverUpdate(campaignId,id,version,p.getName()));}
    @GetMapping("/module-instance") Instance instance(@PathVariable UUID campaignId,Principal p){return service.readInstance(campaignId,p.getName());}
    @PostMapping("/module-instance/migration-impact") MigrationImpact migrationImpact(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){return service.migrationImpact(campaignId,p.getName(),ModuleBlueprint.command(body,LiveReview.class));}
    @PostMapping("/module-instance/migrations") Ack migrate(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){return service.migrate(campaignId,p.getName(),ModuleBlueprint.command(body,MigrationApply.class));}
    @GetMapping("/module-instance/migrations") java.util.List<MigrationEvidence> migrations(@PathVariable UUID campaignId,Principal p){return service.migrations(campaignId,p.getName());}
    @PostMapping("/module-instance/update-impact") LiveImpact liveImpact(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){return service.liveImpact(campaignId,p.getName(),ModuleBlueprint.command(body,LiveReview.class));}
    @PostMapping("/module-instance/updates") Ack applyLive(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){return service.applyLive(campaignId,p.getName(),ModuleBlueprint.command(body,LiveApply.class));}
    @GetMapping("/module-instance/updates") java.util.List<LiveEvidence> liveUpdates(@PathVariable UUID campaignId,Principal p){return service.liveUpdates(campaignId,p.getName());}
    @PostMapping("/module-instance") Ack progress(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){return service.progress(campaignId,p.getName(),ModuleBlueprint.command(body,Progress.class));}
}
