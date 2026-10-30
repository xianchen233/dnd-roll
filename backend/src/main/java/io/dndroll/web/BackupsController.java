package io.dndroll.web;

import io.dndroll.backup.WriteAdmission;
import io.dndroll.backup.BackupJobs;
import java.security.Principal;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/admin/backups")
public class BackupsController {
    private final BackupJobs jobs;
    private final JsonMapper json;
    public BackupsController(BackupJobs jobs,JsonMapper json) {
        this.jobs=jobs;this.json=json;
    }
    @Configuration @EnableScheduling static class Wiring { @Bean WriteAdmission writeAdmission() {return new WriteAdmission();} }
    public record Command(UUID requestId,long expectedVersion) {}
    @GetMapping BackupJobs.Status status(Principal principal) {return jobs.status(principal.getName());}
    @PostMapping BackupJobs.Ack start(Principal principal,@RequestBody JsonNode body) {
        jobs.authorize(principal.getName());CharacterController.shape(body,Command.class);
        var command=json.treeToValue(body,Command.class);return jobs.start(principal.getName(),command.requestId(),command.expectedVersion());
    }
    @PostMapping("/migration") BackupJobs.Ack migrate(Principal principal,@RequestBody JsonNode body) {
        jobs.authorize(principal.getName());CharacterController.shape(body,Command.class);
        var command=json.treeToValue(body,Command.class);return jobs.startMigration(principal.getName(),command.requestId(),command.expectedVersion());
    }
    @GetMapping("/{id}/content") ResponseEntity<FileSystemResource> download(Principal principal,@PathVariable UUID id) {
        var path=jobs.download(principal.getName(),id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"dnd-roll-"+id+".zip\"").body(new FileSystemResource(path));
    }
}
