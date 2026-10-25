package io.dndroll.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.stubbing.Answer;
import tools.jackson.databind.json.JsonMapper;

class AttackPreviewServiceTest {
    final UUID c=UUID.randomUUID(),source=UUID.randomUUID(),target=UUID.randomUUID();
    final JsonMapper json=JsonMapper.builder().build();
    final AttackPreviewService.Store store=mock(AttackPreviewService.Store.class);
    final CharacterService.Store cards=mock(CharacterService.Store.class);
    final DiceService dice=mock(DiceService.class);final CombatService combat=mock(CombatService.class);
    final Map<UUID,AttackPreviewService.Preview> saved=new HashMap<>();final Map<UUID,AttackPreviewService.Receipt> receipts=new HashMap<>();
    List<ContentEntry> catalog;AttackPreviewService service;CharacterService.Card attacker,defender;
    @BeforeEach void setup()throws Exception{
        try(var in=getClass().getResourceAsStream("/content/srd-selection.json")){catalog=Arrays.asList(json.readValue(in,ContentEntry[].class));}
        var entry=catalog.stream().filter(e->e.data().type().equals("ATTACK")).findFirst().orElseThrow();
        attacker=new CharacterService.Card(source,c,0,false,false,false,List.of(),entry.fill(CharacterSheet.empty("Hero"),"STR",""));
        defender=new CharacterService.Card(target,c,0,false,false,false,List.of(),CharacterSheet.empty("Target"));
        when(cards.role(c,"dm")).thenReturn(Role.DM);when(cards.role(c,"player")).thenReturn(Role.PLAYER);
        when(cards.card(c,source)).thenAnswer(i->attacker);when(cards.card(c,target)).thenAnswer(i->defender);
        Answer<Object> work=i->((java.util.function.Supplier<?>)i.getArgument(1)).get();
        when(cards.locked(eq(c),any())).thenAnswer(work);when(cards.actorLocked(anyString(),any())).thenAnswer(work);
        when(store.get(eq(c),any())).thenAnswer(i->saved.get(i.getArgument(1)));
        doAnswer(i->{saved.put(((AttackPreviewService.Preview)i.getArgument(1)).id(),i.getArgument(1));return null;}).when(store).save(eq(c),any(),anyBoolean());
        when(store.receipt(anyString(),any())).thenAnswer(i->receipts.get(i.getArgument(1)));
        doAnswer(i->{receipts.put(i.getArgument(2),i.getArgument(3));return null;}).when(store).receipt(eq(c),anyString(),any(),any());
        when(store.list(eq(c),anyBoolean())).thenAnswer(i->List.copyOf(saved.values()));
        when(dice.roll(eq(c),eq("dm"),any())).thenAnswer(i->{DiceService.Command v=i.getArgument(2);var result=Dice.roll(v.key().equals("EXPRESSION")?v.expression():"1d20+0",v.advantage(),v.disadvantage(),s->s==20?20:2);return new DiceService.Roll(UUID.randomUUID(),v.characterId(),v.expectedVersion(),"dm","Hero",Instant.EPOCH,"DM",v.key(),new CharacterSheet.Calculation(0,0,List.of(),""),v.advantage(),v.disadvantage(),result);});
        when(dice.plannedDamage(eq(c),eq("dm"),any(),any(),anyString(),anyInt(),anyInt(),anyInt())).thenAnswer(i->{int count=i.getArgument(5),sides=i.getArgument(6),modifier=i.getArgument(7);return new DiceService.Roll(UUID.randomUUID(),source.toString(),0,"dm","Hero",Instant.EPOCH,"DM","damage:"+attacker.sheet().attacks().getFirst().id(),new CharacterSheet.Calculation(0,0,List.of(),""),List.of(),List.of(),Dice.roll(count+"d"+sides+"+"+modifier,List.of(),List.of(),s->2));});
        when(combat.declare(eq(c),eq("dm"),any())).thenReturn(new CombatService.Ack(UUID.randomUUID(),0));
        when(combat.append(eq(c),any(),eq("dm"),any())).thenAnswer(i->{CombatService.Command v=i.getArgument(3);return new CombatService.Ack(i.getArgument(1),v.expectedVersion()+1);});
        service=new AttackPreviewService(store,cards,dice,combat,catalog,(id,dm)->{},Clock.fixed(Instant.EPOCH,ZoneOffset.UTC),json);
    }
    AttackPreviewService.Create input(boolean reviewed){return new AttackPreviewService.Create(UUID.randomUUID(),source,0,target,0,attacker.sheet().attacks().getFirst().id(),reviewed,List.of(),List.of(),List.of(),List.of(),List.of(),List.of());}
    @Test void serverCriticalUsesIndependentDamageRollAndPersistsTrustedImmutableEvidence(){
        var body=input(true);var ack=service.create(c,"dm",body);var p=service.detail(c,ack.id(),"dm");
        assertEquals("PREVIEW",p.status());assertEquals("CRITICAL",p.evidence().resolution().review().candidate().verdict());
        assertEquals(2,p.evidence().resolution().dice().size());assertEquals(4,p.evidence().resolution().damage());
        assertEquals(catalog.getFirst(),p.evidence().entry());assertEquals(attacker,p.evidence().source());assertEquals(64,p.hash().length());
        assertEquals(ack,service.create(c,"dm",body));verify(dice,times(1)).roll(eq(c),eq("dm"),any());verify(cards,never()).save(any(),anyBoolean());
    }
    @Test void unreviewedAndForgedCatalogIdentityFallBackWithoutRollingOrPartialCandidate(){
        var ack=service.create(c,"dm",input(false));assertEquals("MANUAL_REQUIRED",service.detail(c,ack.id(),"dm").status());verifyNoInteractions(dice,combat);
        var sheet=attacker.sheet();var a=sheet.attacks().getFirst();var forged=new CharacterSheet.Attack(a.id(),a.name(),a.ability(),a.proficient(),0,0,"9d10",a.damageType(),"",a.source());
        attacker=new CharacterService.Card(source,c,0,false,false,false,List.of(),new CharacterSheet(sheet.name(),sheet.avatar(),sheet.species(),sheet.background(),sheet.classes(),sheet.abilities(),sheet.skills(),sheet.saves(),sheet.combat(),List.of(forged),sheet.casting(),sheet.spells(),sheet.features(),sheet.resources(),sheet.equipment(),sheet.money(),sheet.conditions(),sheet.modifiers(),sheet.overrides(),sheet.notes()));
        var p=service.detail(c,service.create(c,"dm",input(true)).id(),"dm");assertEquals("MANUAL_REQUIRED",p.status());assertNull(p.evidence().resolution());verifyNoInteractions(dice,combat);
    }
    @Test void currentDmAuthorizationPrecedesHistoricalReceiptAndPrivateRead(){
        var body=input(true);var ack=service.create(c,"dm",body);when(cards.role(c,"dm")).thenReturn(Role.PLAYER);
        assertEquals(403,assertThrows(DomainException.class,()->service.create(c,"dm",body)).status());
        assertEquals(403,assertThrows(DomainException.class,()->service.detail(c,ack.id(),"dm")).status());
    }
    @Test void staleSourceAndTargetRejectBeforeEntropy(){
        defender=new CharacterService.Card(target,c,1,false,false,false,List.of(),defender.sheet());
        assertEquals(409,assertThrows(DomainException.class,()->service.create(c,"dm",input(true))).status());verifyNoInteractions(dice,combat);
    }
    @Test void savedReviewCannotChangeWhenCallerMutatesItsFactorList(){
        var factors=new ArrayList<AttackCalculator.Factor>();var i=input(false);
        var body=new AttackPreviewService.Create(i.requestId(),source,0,target,0,i.attackId(),false,factors,List.of(),List.of(),List.of(),List.of(),List.of());
        var p=service.detail(c,service.create(c,"dm",body).id(),"dm");factors.add(new AttackCalculator.Factor("Later mutation",new CharacterSheet.Source("SRD 5.2.1","5.2.1","5R","")));
        assertTrue(p.evidence().review().exceptions().isEmpty());
    }
}
