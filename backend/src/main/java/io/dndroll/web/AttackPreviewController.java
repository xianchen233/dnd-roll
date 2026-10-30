package io.dndroll.web;

import io.dndroll.application.*;
import io.dndroll.application.AttackPreviewService.*;
import io.dndroll.domain.*;
import java.security.Principal;
import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/campaigns/{campaignId}")
public class AttackPreviewController {
    private final AttackPreviewService service;private final JsonMapper json;
    public AttackPreviewController(AttackPreviewService service,JsonMapper json){this.service=service;this.json=json;}
    @Configuration static class Wiring {
        @Bean AttackPreviewService attackPreviewService(Store store,CharacterService.Store cards,DiceService dice,CombatService combat,SceneEvents events,Clock clock,JsonMapper json)throws java.io.IOException{
            try(var in=new ClassPathResource("content/srd-selection.json").getInputStream()){return new AttackPreviewService(store,cards,dice,combat,Arrays.asList(json.readValue(in,ContentEntry[].class)),events::diceChanged,clock,json);}
        }
    }
    @GetMapping("/attack-previews") List<Preview> list(@PathVariable UUID campaignId,Principal p){return service.list(campaignId,p.getName());}
    @GetMapping("/attack-previews/{id}") Preview detail(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.detail(campaignId,id,p.getName());}
    @GetMapping("/attack-previews/{id}/damage-effect") DamageEffect.Plan damageEffect(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p){return service.damageEffect(campaignId,id,p.getName());}
    @PostMapping("/attack-previews/{id}/ammunition-effect") AmmunitionEffect.Plan ammunitionEffect(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.ammunitionEffect(campaignId,id,p.getName(),parse(n,AmmunitionReview.class));}
    @PostMapping("/attack-previews/{id}/ammunition-confirmations") CombatService.Ack confirmAmmunition(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.confirmAmmunition(campaignId,id,p.getName(),parse(n,AmmunitionConfirm.class));}
    @PostMapping("/attack-previews/{id}/monster-death-effect") MonsterDeathEffect.Plan monsterDeathEffect(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.monsterDeathEffect(campaignId,id,p.getName(),parse(n,MonsterDeathReview.class));}
    @PostMapping("/attack-previews/{id}/monster-death-confirmations") CombatService.Ack confirmMonsterDeath(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.confirmMonsterDeath(campaignId,id,p.getName(),parse(n,MonsterDeathConfirm.class));}
    @PostMapping("/attack-previews/{id}/knockout-effect") KnockoutEffect.Plan knockoutEffect(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.knockoutEffect(campaignId,id,p.getName(),parse(n,KnockoutReview.class));}
    @PostMapping("/attack-previews/{id}/knockout-confirmations") CombatService.Ack confirmKnockout(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.confirmKnockout(campaignId,id,p.getName(),parse(n,KnockoutConfirm.class));}
    @PostMapping("/attack-previews/{id}/dagger-knockout-effect") KnockoutEffect.Plan daggerKnockoutEffect(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.daggerKnockoutEffect(campaignId,id,p.getName(),parse(n,KnockoutReview.class));}
    @PostMapping("/attack-previews/{id}/dagger-knockout-confirmations") CombatService.Ack confirmDaggerKnockout(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.confirmDaggerKnockout(campaignId,id,p.getName(),parse(n,KnockoutConfirm.class));}
    @PostMapping("/attack-previews/{id}/knockout-healing-effect") HealingPlan knockoutHealingEffect(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.knockoutHealingEffect(campaignId,id,p.getName(),parse(n,HealingReview.class));}
    @PostMapping("/attack-previews/{id}/knockout-healings") CombatService.Ack healKnockout(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.healKnockout(campaignId,id,p.getName(),parse(n,HealingApply.class));}
    @PostMapping("/attack-previews/{id}/knockout-rest-effect") RestPlan knockoutRestEffect(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.knockoutRestEffect(campaignId,id,p.getName(),parse(n,RestReview.class));}
    @PostMapping("/attack-previews/{id}/knockout-rest-completions") CombatService.Ack completeKnockoutRest(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.completeKnockoutRest(campaignId,id,p.getName(),parse(n,RestComplete.class));}
    @PostMapping("/attack-previews/{id}/knockout-first-aid-rolls") CombatService.Ack rollKnockoutFirstAid(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.rollKnockoutFirstAid(campaignId,id,p.getName(),parse(n,FirstAidAttempt.class));}
    @PostMapping("/attack-previews/{id}/knockout-first-aid-effect") FirstAidPlan knockoutFirstAidEffect(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.knockoutFirstAidEffect(campaignId,id,p.getName(),parse(n,FirstAidReview.class));}
    @PostMapping("/attack-previews/{id}/knockout-first-aid-completions") CombatService.Ack completeKnockoutFirstAid(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.completeKnockoutFirstAid(campaignId,id,p.getName(),parse(n,FirstAidComplete.class));}
    @PostMapping("/attack-previews") CombatService.Ack create(@PathVariable UUID campaignId,Principal p,@RequestBody JsonNode n){return service.create(campaignId,p.getName(),parse(n,Create.class));}
    @PostMapping("/attack-previews/{id}/events") CombatService.Ack command(@PathVariable UUID campaignId,@PathVariable UUID id,Principal p,@RequestBody JsonNode n){return service.command(campaignId,id,p.getName(),parse(n,Command.class));}
    @GetMapping("/attack-results") List<PublicResult> results(@PathVariable UUID campaignId,Principal p){return service.results(campaignId,p.getName());}
    private <T>T parse(JsonNode n,Class<T> type){if(n==null||n.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw CharacterSheet.invalid();CharacterController.shape(n,type);return json.treeToValue(n,type);}
}
