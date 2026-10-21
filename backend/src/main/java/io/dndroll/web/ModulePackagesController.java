package io.dndroll.web;

import io.dndroll.modules.ModulePackages;
import io.dndroll.modules.ModulePackages.*;
import io.dndroll.application.*;
import io.dndroll.domain.CharacterSheet;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/module-packages")
public class ModulePackagesController {
    private final ModulePackages service;
    private final JsonMapper json=JsonMapper.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    public ModulePackagesController(ModulePackages service){this.service=service;}
    @Configuration static class Wiring {
        @Bean ModulePackages modulePackages(Store store,CharacterService.Store campaigns,ContentService content,SceneService.Events events){return new ModulePackages(store,campaigns,content,events);}
    }
    @GetMapping Listing list(@PathVariable UUID campaignId,Principal p){return service.list(campaignId,p.getName());}
    @PostMapping(value="/preview",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    Preview preview(@PathVariable UUID campaignId,@RequestPart("file") MultipartFile file,HttpServletRequest request,Principal p)throws Exception{
        parts(request,Set.of("file"));return service.preview(campaignId,p.getName(),file.getBytes());
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    Ack upload(@PathVariable UUID campaignId,@RequestPart("command") String command,@RequestPart("file") MultipartFile file,HttpServletRequest request,Principal p)throws Exception{
        parts(request,Set.of("file","command"));if(command.length()>4096)throw CharacterSheet.invalid();
        Command parsed;
        try {var n=json.readTree(command);CharacterController.shape(n,Command.class);parsed=json.treeToValue(n,Command.class);}
        catch(RuntimeException invalid){throw CharacterSheet.invalid();}
        return service.importPackage(campaignId,p.getName(),parsed,file.getBytes());
    }
    @GetMapping("/{id}/content") ResponseEntity<byte[]> download(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"module-"+id+".zip\"").body(service.download(campaignId,id,p.getName()));
    }
    private static void parts(HttpServletRequest r,Set<String> expected)throws Exception{var parts=r.getParts();if(parts.size()!=expected.size()||!parts.stream().map(jakarta.servlet.http.Part::getName).collect(java.util.stream.Collectors.toSet()).equals(expected))throw CharacterSheet.invalid();}
}
