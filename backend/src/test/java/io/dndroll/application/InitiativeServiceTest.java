package io.dndroll.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import io.dndroll.domain.*;
import io.dndroll.application.InitiativeService.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class InitiativeServiceTest {
    final UUID c=UUID.randomUUID();final CharacterService.Store cards=mock(CharacterService.Store.class);final Store store=mock(Store.class);final AtomicReference<State> state=new AtomicReference<>(new State(0,"EMPTY",List.of(),""));final AtomicInteger draws=new AtomicInteger();final Map<UUID,CharacterService.Card> current=new HashMap<>();
    InitiativeService service(){
        when(cards.role(c,"dm")).thenReturn(Role.DM);when(cards.locked(eq(c),any())).thenAnswer(a->((Supplier<?>)a.getArgument(1)).get());when(cards.actorLocked(eq("dm"),any())).thenAnswer(a->((Supplier<?>)a.getArgument(1)).get());
        when(cards.card(eq(c),any())).thenAnswer(a->current.get(a.getArgument(1)));when(store.state(c)).thenAnswer(a->state.get());doAnswer(a->{state.set(a.getArgument(1));return null;}).when(store).state(eq(c),any());
        return new InitiativeService(store,cards,s->{draws.incrementAndGet();return 10;},(id,privateOnly)->{},Clock.systemUTC(),JsonMapper.builder().build());
    }
    UUID card(String name){var id=UUID.randomUUID();current.put(id,new CharacterService.Card(id,c,0,false,false,false,List.of(),CharacterSheet.empty(name)));return id;}
    Participant p(UUID id,String kind,String group,boolean surprise){return new Participant(id,0,kind,group,surprise,List.of(),List.of());}
    @Test void candidatesKeepInputTieOrderAndIdenticalMonsterGroupDrawsOnce(){
        UUID a=card("Zed PC"),b=card("Alpha PC"),x=card("Goblin"),y=card("Goblin");var s=service();
        s.generate(c,"dm",new Generate(UUID.randomUUID(),0,List.of(p(a,"PC","",false),p(b,"PC","",false),p(x,"MONSTER","g",true),p(y,"MONSTER","g",true)),"Declared identical monsters"));
        assertEquals(4,draws.get());assertEquals(List.of(a,b,x,y),state.get().entries().stream().map(Entry::characterId).toList());assertEquals(2,state.get().entries().get(2).result().dice().size());assertEquals(state.get().entries().get(2).result(),state.get().entries().get(3).result());
    }
    @Test void invalidGroupOrStaleRosterDrawsNothingAndSavesNothing(){
        UUID a=card("A"),b=card("B");var s=service();
        assertThrows(DomainException.class,()->s.generate(c,"dm",new Generate(UUID.randomUUID(),0,List.of(p(a,"MONSTER","g",true),p(b,"MONSTER","g",false)),"Different surprise")));
        assertThrows(DomainException.class,()->s.generate(c,"dm",new Generate(UUID.randomUUID(),0,List.of(new Participant(a,1,"PC","",false,List.of(),List.of())),"Stale source")));
        assertEquals(0,draws.get());verify(store,never()).state(eq(c),any());verify(store,never()).history(eq(c),any());
    }
    @Test void mixedAdvantageAndSurpriseCancelWithoutExtraDice(){
        var a=card("Hero");var s=service();s.generate(c,"dm",new Generate(UUID.randomUUID(),0,List.of(new Participant(a,0,"PC","",true,List.of("Feature"),List.of())),"Sourced advantage and surprise"));
        assertEquals(1,draws.get());assertEquals("NORMAL",state.get().entries().getFirst().result().mode());assertTrue(state.get().entries().getFirst().disadvantage().getFirst().contains("突袭"));
    }
}
