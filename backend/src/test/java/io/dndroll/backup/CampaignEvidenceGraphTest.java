package io.dndroll.backup;
import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.*;import io.dndroll.domain.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;import java.time.Instant;
class CampaignEvidenceGraphTest {
 final UUID campaign=UUID.randomUUID(),target=UUID.randomUUID(),helper=UUID.randomUUID(),rollId=UUID.randomUUID();final JsonMapper json=JsonMapper.builder().build();
 CharacterService.Card card(UUID id,long v){return new CharacterService.Card(id,campaign,v,false,false,false,List.of(),CharacterSheet.empty("SECRET_TEXT"));}
 CampaignEvidenceGraph.Row row(String table,UUID id,long v,Object o){return new CampaignEvidenceGraph.Row(table,id,v,null,null,null,null,json.writeValueAsString(o));}
 List<CampaignEvidenceGraph.Row> fixture(){var t=card(target,0);var h=card(helper,0);var d=new DiceService.Roll(rollId,helper.toString(),0,"PRIVATE_ACTOR","SECRET_TEXT",Instant.EPOCH,"DM","skill:medicine",h.sheet().calculate().get("skill:medicine"),List.of(),List.of(),Dice.roll("1d20",List.of(),List.of(),sides->10));var a=new StabilizationService.Attempt(rollId,"ROLLED",t,h,d,true,"SECRET_TEXT","PRIVATE_ACTOR",Instant.EPOCH,null);var state=new StabilizationService.State(target,1,a);return new ArrayList<>(List.of(row("character_card",target,0,t),row("character_card",helper,0,h),row("dice_roll",rollId,-1,d),row("stabilization_history",target,1,state),row("stabilization_state",target,1,state),row("character_history",target,0,t),row("character_history",helper,0,h)));}
 List<CampaignEvidenceGraph.SourceReceipt> receipts(){return List.of(new CampaignEvidenceGraph.SourceReceipt(rollId,DiceService.fingerprint(json,List.of("A16_STABILIZATION",campaign,target,0L,0L,helper,0L,card(helper,0).sheet().calculate().get("skill:medicine"))),"PRIVATE_ACTOR"));}
 CampaignEvidenceGraph.Report inspect(List<CampaignEvidenceGraph.Row> rows){return CampaignEvidenceGraph.inspect(campaign,rows,receipts());}
 @Test void consistentTypedGraphIsPrivateImmutableAndBlocked(){var rows=fixture();var p=inspect(rows);assertTrue(p.consistent(),p.blockers().toString());assertEquals("BLOCKED",p.status());assertFalse(p.executable());assertFalse(p.toString().contains("SECRET_TEXT"));assertFalse(p.toString().contains("PRIVATE_ACTOR"));assertThrows(UnsupportedOperationException.class,()->p.edges().clear());Collections.reverse(rows);assertEquals(p,inspect(rows));}
 @Test void unknownNestedShapeBlocksWithoutLeakingText(){var rows=fixture();var r=rows.getFirst();rows.set(0,new CampaignEvidenceGraph.Row(r.table(),r.id(),r.version(),null,null,null,null,r.json().replace("\"sheet\":{","\"sheet\":{\"private\":\"SECRET_TEXT\",")));assertFalse(inspect(rows).consistent());}
 @Test void missingRecordFieldBlocks(){var rows=fixture();var r=rows.getFirst();rows.set(0,new CampaignEvidenceGraph.Row(r.table(),r.id(),r.version(),null,null,null,null,r.json().replace("\"version\":0,","")));assertFalse(inspect(rows).consistent());}
 @Test void unknownAdapterBlocks(){assertFalse(inspect(List.of(row("unknown",target,1,Map.of()))).consistent());}
 @Test void sqlVersionMismatchBlocks(){var rows=fixture();rows.add(row("character_history",target,7,card(target,0)));assertTrue(inspect(rows).blockers().contains("SQL_JSON_VERSION_MISMATCH"));}
 @Test void differentHistoricalCardBlocks(){var rows=fixture();var c=card(target,0);rows.add(row("character_history",target,0,new CharacterService.Card(target,campaign,0,true,false,false,List.of(),c.sheet())));assertTrue(inspect(rows).blockers().contains("DUPLICATE_EVIDENCE_KEY"));}
 @Test void missingHistoricalCardBlocks(){var rows=fixture();rows.removeIf(r->r.id().equals(helper));assertFalse(inspect(rows).consistent());}
 @Test void unrelatedMedicineRollCannotSupplyDedicatedProof(){assertTrue(CampaignEvidenceGraph.inspect(campaign,fixture()).blockers().contains("DEDICATED_ROLL_PROVENANCE_MISMATCH"));}
 @Test void attemptRollAliasMismatchBlocks(){var rows=fixture();var state=json.readValue(rows.get(3).json(),StabilizationService.State.class);var a=state.latest();rows.set(3,row("stabilization_history",target,1,new StabilizationService.State(target,1,new StabilizationService.Attempt(UUID.randomUUID(),a.status(),a.target(),a.helper(),a.roll(),a.reviewed(),a.reason(),a.actor(),a.at(),null))));assertTrue(inspect(rows).blockers().contains("ATTEMPT_ROLL_ALIAS_MISMATCH"));}
 @Test void missingLedgerHistoryBlocks(){var rows=fixture();rows.remove(3);assertTrue(inspect(rows).blockers().contains("CURRENT_LEDGER_HISTORY_MISMATCH"));}
 @Test void crossCampaignSnapshotBlocks(){var rows=fixture();var c=card(target,0);rows.set(0,row("character_card",target,0,new CharacterService.Card(target,UUID.randomUUID(),0,false,false,false,List.of(),c.sheet())));assertTrue(inspect(rows).blockers().contains("CROSS_CAMPAIGN_CARD"));}
 @Test void unknownCanonicalOwnerBlocks(){var c=card(target,0);var s=c.sheet();var bad=new CharacterService.Card(target,campaign,0,false,false,false,List.of(),s.change(s.name(),s.combat(),s.resources(),List.of(MonsterDeathEffect.annotation(UUID.randomUUID())),s.overrides()));assertTrue(inspect(List.of(row("character_card",target,0,bad))).blockers().contains("MISSING_ANNOTATION_OWNER"));}
 @Test void a19OriginVersionCannotUseSharedStateInsteadOfDamageJournal(){var rows=fixture();var old=json.readValue(rows.get(3).json(),StabilizationService.State.class).latest();var a=new RestabilizationService.Attempt(UUID.randomUUID(),"ROLLED",1,old.target(),old.helper(),old.roll(),true,"SECRET_TEXT","PRIVATE_ACTOR",Instant.EPOCH,null);rows.add(row("restabilization_history",target,1,new RestabilizationService.State(target,1,a)));assertTrue(inspect(rows).blockers().contains("MISSING_LEDGER_VERSION:A19_ORIGIN"));}
 @Test void a22RequiresA21DamageVersionAndSharedResultVersion(){var before=card(target,0);var after=card(target,1);var review=new InstabilityHealingService.Review(1,2,0,rollId,1,true);var e=new InstabilityHealingService.Entry(target,2,3,7,rollId,"PRIVATE_ACTOR",Instant.EPOCH,"SECRET_TEXT",review,"a".repeat(64),before,after);var rows=fixture();rows.add(new CampaignEvidenceGraph.Row("instability_healing_history",target,2,3L,7L,null,null,json.writeValueAsString(e)));var p=inspect(rows);assertTrue(p.blockers().contains("MISSING_LEDGER_VERSION:DAMAGE_ORIGIN"));assertTrue(p.blockers().contains("MISSING_LEDGER_VERSION:SHARED_RESULT"));}
 @Test void a23DoesNotUseIndependentLedgerForDamageVersion(){var e=new InitialInstabilityHealingService.Entry(target,2,7,rollId,"PRIVATE_ACTOR",Instant.EPOCH,"SECRET_TEXT",new InitialInstabilityHealingService.Review(1,0,rollId,1,true),"a".repeat(64),card(target,0),card(target,1));var rows=fixture();rows.add(new CampaignEvidenceGraph.Row("initial_instability_healing_history",target,2,null,7L,null,null,json.writeValueAsString(e)));assertTrue(inspect(rows).blockers().contains("MISSING_LEDGER_VERSION:DAMAGE_ORIGIN"));}
 @Test void sqlOriginAndDamageMustEqualJournalJson(){var e=new InstabilityHealingService.Entry(target,2,3,7,rollId,"PRIVATE_ACTOR",Instant.EPOCH,"SECRET_TEXT",new InstabilityHealingService.Review(1,2,0,rollId,1,true),"a".repeat(64),card(target,0),card(target,1));var p=inspect(List.of(new CampaignEvidenceGraph.Row("instability_healing_history",target,2,9L,8L,null,null,json.writeValueAsString(e))));assertTrue(p.blockers().contains("SQL_JSON_ORIGIN_MISMATCH"));assertTrue(p.blockers().contains("SQL_JSON_DAMAGE_MISMATCH"));}
 @Test void sourceReceiptActorCannotBeClaimedByAnotherAccount(){var receipt=receipts().getFirst();assertTrue(CampaignEvidenceGraph.inspect(campaign,fixture(),List.of(new CampaignEvidenceGraph.SourceReceipt(receipt.rollId(),receipt.fingerprint(),"OTHER_ACTOR"))).blockers().contains("DEDICATED_ROLL_PROVENANCE_MISMATCH"));}
 @Test void a20SharedOriginCannotBeAnIndependentLedgerVersion(){var e=new RestabilizationHealingService.Entry(target,2,9,rollId,"PRIVATE_ACTOR",Instant.EPOCH,"SECRET_TEXT",new RestabilizationHealingService.Review(1,8,0,rollId,1,true),"a".repeat(64),card(target,0),card(target,1));var p=inspect(List.of(new CampaignEvidenceGraph.Row("restabilization_healing_history",target,2,9L,null,null,null,json.writeValueAsString(e))));assertTrue(p.blockers().contains("MISSING_LEDGER_VERSION:SHARED_RESULT"));}
 @Test void a21SharedOriginAndReviewMustReferToAdjacentSharedVersions(){var e=new RestabilizationDamageService.Entry(target,2,9,rollId,"PRIVATE_ACTOR",Instant.EPOCH,"SECRET_TEXT",new RestabilizationDamageService.Review(1,3,0,rollId,1,false,true),"a".repeat(64),card(target,0),card(target,1));var p=inspect(List.of(new CampaignEvidenceGraph.Row("restabilization_damage_history",target,2,9L,null,null,null,json.writeValueAsString(e))));assertTrue(p.blockers().contains("JOURNAL_ORIGIN_REVIEW_MISMATCH"));}
 @Test void currentCardWithoutAuthoritativeHistoryBlocks(){var rows=fixture();rows.removeIf(r->r.table().equals("character_history"));assertTrue(inspect(rows).blockers().contains("CURRENT_CARD_HISTORY_MISMATCH"));}
 @Test void nextHistoricalVersionRequiresExactBeforeSnapshot(){var rows=fixture();rows.add(row("character_history",target,1,card(target,1)));assertTrue(inspect(rows).blockers().contains("MISSING_OR_DIFFERENT_HISTORY_BEFORE"));}
 @Test void duplicateEventIdentityWithinActionBlocks(){
  var rows=fixture();UUID actionId=UUID.randomUUID();
  var event=new CombatService.Event(UUID.randomUUID(),"PRIVATE_ACTOR",Instant.EPOCH,"VERDICT","HIT","SECRET_TEXT","",null,List.of(),false);
  var action=new CombatService.Action(actionId,2,2,target,"SECRET_TEXT",0,helper,"SECRET_TEXT",null,"DM","PRIVATE_ACTOR",Instant.EPOCH,"HIT",List.of(event,event));
  rows.add(new CampaignEvidenceGraph.Row("combat_action",actionId,-1,null,null,target,helper,json.writeValueAsString(action)));
  assertTrue(inspect(rows).blockers().contains("DUPLICATE_EVENT_ID"));
 }
 @Test void a19CannotClaimParentOnlyStatus(){
  var rows=fixture();var old=json.readValue(rows.get(3).json(),StabilizationService.State.class).latest();
  var attempt=new RestabilizationService.Attempt(rollId,"RESTABILIZED",1,old.target(),old.helper(),old.roll(),true,"SECRET_TEXT","PRIVATE_ACTOR",Instant.EPOCH,null);
  rows.add(row("restabilization_history",target,1,new RestabilizationService.State(target,1,attempt)));
  assertTrue(inspect(rows).blockers().contains("UNKNOWN_ATTEMPT_STATUS"));
 }
 @Test void failedA19RollCannotOwnSuccessfulCanonicalAnnotation(){
  var h=card(helper,0);var base=card(target,0);var sheet=base.sheet();var b=sheet.combat();
  var zero=sheet.change(sheet.name(),new CharacterSheet.Combat(12,0,0,b.speed(),0,1,b.acMode(),b.manualAc(),b.source()),sheet.resources(),List.of(),sheet.overrides());
  var firstId=UUID.randomUUID();var unconscious=StabilizationEffect.plan(zero,h.sheet(),firstId,true,true,true,10).annotations().get(1);
  var unstable=zero.change(zero.name(),zero.combat(),zero.resources(),List.of(unconscious),zero.overrides());
  var t0=new CharacterService.Card(target,campaign,0,false,false,false,List.of(),zero);
  var t1=new CharacterService.Card(target,campaign,1,false,false,false,List.of(),unstable);
  var successfulAnnotations=RestabilizationEffect.plan(unstable,h.sheet(),unconscious,rollId,true,true,true,true,10).annotations();
  var t2=new CharacterService.Card(target,campaign,2,false,false,false,List.of(),unstable.change(unstable.name(),unstable.combat(),unstable.resources(),successfulAnnotations,unstable.overrides()));
  var firstRoll=new DiceService.Roll(firstId,helper.toString(),0,"PRIVATE_ACTOR","SECRET_TEXT",Instant.EPOCH,"DM","skill:medicine",h.sheet().calculate().get("skill:medicine"),List.of(),List.of(),Dice.roll("1d20",List.of(),List.of(),sides->10));
  var failed=new DiceService.Roll(rollId,helper.toString(),0,"PRIVATE_ACTOR","SECRET_TEXT",Instant.EPOCH,"DM","skill:medicine",firstRoll.calculation(),List.of(),List.of(),Dice.roll("1d20",List.of(),List.of(),sides->9));
  var original=new StabilizationService.Attempt(firstId,"APPLIED",t0,h,firstRoll,true,"SECRET_TEXT","PRIVATE_ACTOR",Instant.EPOCH,null);
  var later=new RestabilizationService.Attempt(rollId,"APPLIED",2,t1,h,failed,true,"SECRET_TEXT","PRIVATE_ACTOR",Instant.EPOCH,null);
  var rows=List.of(row("character_history",target,0,t0),row("character_history",target,1,t1),row("character_history",target,2,t2),row("character_history",helper,0,h),row("dice_roll",firstId,-1,firstRoll),row("dice_roll",rollId,-1,failed),row("stabilization_history",target,1,new StabilizationService.State(target,1,original)),row("restabilization_history",target,1,new RestabilizationService.State(target,1,later)));
  assertTrue(inspect(rows).blockers().contains("NONCANONICAL_ANNOTATION"));
 }
 @Test void canonicalAnnotationCannotBorrowAnotherTargetsPreview(){
  var rows=fixture();UUID previewId=UUID.randomUUID();var h=card(helper,0);
  var review=new AttackPreviewService.Create(UUID.randomUUID(),helper,0,helper,0,"fixed-key",true,List.of(),List.of(),List.of(),List.of(),List.of(),List.of());
  var evidence=new AttackPreviewService.Evidence(review,h,h,null,List.of(),null,null,null,UUID.randomUUID());
  rows.add(new CampaignEvidenceGraph.Row("attack_preview",previewId,-1,null,null,helper,helper,json.writeValueAsString(new AttackPreviewService.Preview(previewId,0,"PREVIEW","PRIVATE_ACTOR",Instant.EPOCH,evidence,"a".repeat(64),List.of()))));
  var old=card(target,0);var s=old.sheet();var annotated=new CharacterService.Card(target,campaign,0,false,false,false,List.of(),s.change(s.name(),s.combat(),s.resources(),List.of(MonsterDeathEffect.annotation(previewId)),s.overrides()));
  rows.set(0,row("character_card",target,0,annotated));rows.set(5,row("character_history",target,0,annotated));
  assertTrue(inspect(rows).blockers().contains("ANNOTATION_TARGET_MISMATCH"));
 }
}
