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
    @GetMapping("/module-instance") Instance instance(@PathVariable UUID campaignId,Principal p){return service.readInstance(campaignId,p.getName());}
    @PostMapping("/module-instance") Ack progress(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){return service.progress(campaignId,p.getName(),ModuleBlueprint.command(body,Progress.class));}
}
