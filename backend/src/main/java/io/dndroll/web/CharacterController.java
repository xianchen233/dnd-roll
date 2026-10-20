package io.dndroll.web;

import io.dndroll.application.CharacterService;
import io.dndroll.application.CharacterService.*;
import io.dndroll.application.SceneService;
import io.dndroll.domain.CharacterSheet;
import java.security.Principal;
import java.time.*;
import java.lang.reflect.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/characters")
public class CharacterController {
    final CharacterService service;final JsonMapper json;
    public CharacterController(CharacterService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {@Bean CharacterService characterService(Store store,SceneService.Events events,Clock clock,JsonMapper json){return new CharacterService(store,events,clock,json);}}
    public record Create(UUID requestId,long expectedVersion,CharacterSheet sheet) {}
    public record Update(UUID requestId,long expectedVersion,CharacterSheet sheet,String reason) {}
    public record Manage(UUID requestId,long expectedVersion,List<String> controllers,boolean hidden,boolean shareFull,boolean approveImport,String reason) {}
    public record Resource(UUID requestId,long expectedVersion,String resourceId,int delta,String reason) {}
    public record Condition(UUID requestId,long expectedVersion,String action,String conditionId,String reason) {}
    public record Copy(UUID requestId,long expectedVersion,String name) {}
    public record Import(UUID requestId,long expectedVersion,Document document) {}
    public record Link(UUID requestId,long expectedVersion,UUID tokenId,long tokenVersion,String reason) {}
    @GetMapping Listing list(@PathVariable UUID campaignId,Principal p){return service.list(campaignId,p.getName());}
    @PostMapping Ack create(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){var r=parse(body,Create.class);return service.create(campaignId,p.getName(),r.requestId(),r.expectedVersion(),r.sheet());}
    @GetMapping("/{id}") Detail detail(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.detail(campaignId,id,p.getName());}
    @PostMapping("/{id}") Ack update(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){var r=parse(body,Update.class);return service.update(campaignId,id,p.getName(),r.requestId(),r.expectedVersion(),r.sheet(),r.reason());}
    @GetMapping("/{id}/history") List<History> history(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.history(campaignId,id,p.getName());}
    @GetMapping("/{id}/access") Access access(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.access(campaignId,id,p.getName());}
    @PostMapping("/{id}/access") Ack access(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){var r=parse(body,Manage.class);return service.access(campaignId,id,p.getName(),r.requestId(),r.expectedVersion(),r.controllers(),r.hidden(),r.shareFull(),r.approveImport(),r.reason());}
    @PostMapping("/{id}/resources") Ack resource(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){var r=parse(body,Resource.class);return service.resource(campaignId,id,p.getName(),r.requestId(),r.expectedVersion(),r.resourceId(),r.delta(),r.reason());}
    @PostMapping("/{id}/conditions") Ack condition(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){var r=parse(body,Condition.class);return service.condition(campaignId,id,p.getName(),r.requestId(),r.expectedVersion(),r.action(),r.conditionId(),r.reason());}
    @PostMapping("/{id}/copy") Ack copy(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){var r=parse(body,Copy.class);return service.copy(campaignId,id,p.getName(),r.requestId(),r.expectedVersion(),r.name());}
    @GetMapping("/{id}/export") Document export(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.export(campaignId,id,p.getName());}
    @PostMapping("/import") Ack importDocument(@PathVariable UUID campaignId,@RequestBody JsonNode body,Principal p){var r=parse(body,Import.class);return service.importDocument(campaignId,p.getName(),r.requestId(),r.expectedVersion(),r.document());}
    @GetMapping("/{id}/placements") List<Placement> placements(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.placements(campaignId,id,p.getName());}
    @PostMapping("/{id}/placements") Ack link(@PathVariable UUID campaignId,@PathVariable UUID id,@RequestBody JsonNode body,Principal p){var r=parse(body,Link.class);return service.link(campaignId,id,p.getName(),r.requestId(),r.expectedVersion(),r.tokenId(),r.tokenVersion(),r.reason());}
    private <T>T parse(JsonNode node,Class<T> type){if(node==null||node.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length+padding(node)>2097152)throw CharacterSheet.invalid();shape(node,type);return json.treeToValue(node,type);}
    // PostgreSQL jsonb text includes separator spaces; bound the persisted shape,
    // rather than UTF-16 characters, before the transaction can hit its size CHECK.
    private long padding(JsonNode n){long size=n.isObject()?Math.max(0,2L*n.size()-1):n.isArray()?Math.max(0,n.size()-1):0;if(n.isContainer())for(JsonNode v:n)size+=padding(v);return size;}
    /** Require every nested record field, including primitive 0/false: no silent defaults. */
    static void shape(JsonNode n,Type type){
        if(n==null||n.isNull())throw CharacterSheet.invalid();
        if(type instanceof ParameterizedType p){
            var args=p.getActualTypeArguments();if(p.getRawType()==List.class){if(!n.isArray()||n.size()>500)throw CharacterSheet.invalid();n.forEach(v->shape(v,args[0]));}
            else if(p.getRawType()==Map.class){if(!n.isObject()||n.size()>100)throw CharacterSheet.invalid();n.properties().forEach(e->shape(e.getValue(),args[1]));}return;
        }
        Class<?> c=(Class<?>)type;
        if(c.isRecord()){
            if(!n.isObject()||n.size()!=c.getRecordComponents().length)throw CharacterSheet.invalid();
            for(var field:c.getRecordComponents()){
                if(!n.has(field.getName()))throw CharacterSheet.invalid();var value=n.get(field.getName());
                boolean nullable=(c==CharacterSheet.Condition.class&&Set.of("remainingRounds","expiresAt").contains(field.getName()))||(c==Condition.class&&field.getName().equals("conditionId"));
                if(value.isNull()&&nullable)continue;shape(value,field.getGenericType());
            }
        }else if(c==String.class||c==UUID.class||c==Instant.class){if(!n.isString())throw CharacterSheet.invalid();}
        else if(c==int.class||c==Integer.class){if(!n.isIntegralNumber()||!n.canConvertToInt())throw CharacterSheet.invalid();}
        else if(c==long.class){if(!n.isIntegralNumber()||!n.canConvertToLong())throw CharacterSheet.invalid();}
        else if(c==boolean.class&&!n.isBoolean())throw CharacterSheet.invalid();
    }
}
