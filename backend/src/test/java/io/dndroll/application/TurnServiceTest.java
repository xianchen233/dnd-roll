package io.dndroll.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TurnServiceTest {
    final UUID c=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID(),token=UUID.randomUUID();
    final TurnService.Store store=mock(TurnService.Store.class);final CharacterService.Store cards=mock(CharacterService.Store.class);
    final TurnService service=new TurnService(store,cards,mock(InitiativeService.Store.class),mock(CombatService.Store.class),(id,dm)->{},Clock.systemUTC(),JsonMapper.builder().build());
    void state(String status,String current,List<UUID> grants){when(store.state(c)).thenReturn(new TurnService.State(2,status,1,List.of(a,b),current,grants,99,"DM"));}
    CharacterService.Card card(boolean hidden,List<String> controllers){return new CharacterService.Card(b,c,0,hidden,false,false,controllers,CharacterSheet.empty("Hero"));}
    @Test void explorationDoesNotRequireACharacterLinkAndDmCanAdjustAnyToken(){state("IDLE","",List.of());assertTrue(service.canMove(c,token,"player",Role.PLAYER));state("ACTIVE",a.toString(),List.of());assertTrue(service.canMove(c,token,"dm",Role.DM));}
    @Test void playerNeedsLinkedVisibleControlledCurrentOrGrantedCharacter(){when(cards.card(c,a)).thenReturn(new CharacterService.Card(a,c,0,false,false,false,List.of(),CharacterSheet.empty("Visible participant")));state("ACTIVE",a.toString(),List.of());assertFalse(service.canMove(c,token,"player",Role.PLAYER));when(store.characterForToken(c,token)).thenReturn(b);when(cards.card(c,b)).thenReturn(card(false,List.of("player")));assertFalse(service.canMove(c,token,"player",Role.PLAYER));state("ACTIVE",a.toString(),List.of(b));assertTrue(service.canMove(c,token,"player",Role.PLAYER));when(cards.card(c,b)).thenReturn(card(true,List.of("player")));assertFalse(service.canMove(c,token,"player",Role.PLAYER));}
    @Test void tokenControlDoesNotGrantCharacterControlEvenInCurrentTurn(){state("ACTIVE",b.toString(),List.of());when(store.characterForToken(c,token)).thenReturn(b);when(cards.card(c,b)).thenReturn(card(false,List.of("other")));assertFalse(service.canMove(c,token,"player",Role.PLAYER));}
    @Test void entirelyHiddenCombatHasNoPlayerStatusOrInvalidation(){
        state("ACTIVE",b.toString(),List.of());when(cards.role(c,"player")).thenReturn(Role.PLAYER);when(cards.role(c,"dm")).thenReturn(Role.DM);when(cards.card(c,b)).thenReturn(card(true,List.of()));
        when(cards.locked(org.mockito.ArgumentMatchers.eq(c),org.mockito.ArgumentMatchers.any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(1)).get());when(cards.actorLocked(org.mockito.ArgumentMatchers.eq("dm"),org.mockito.ArgumentMatchers.any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(1)).get());
        var events=mock(DiceService.Events.class);var tested=new TurnService(store,cards,mock(InitiativeService.Store.class),mock(CombatService.Store.class),events,Clock.systemUTC(),JsonMapper.builder().build());
        var view=(Map<?,?>)tested.view(c,"player");assertEquals("IDLE",view.get("status"));assertTrue(tested.canMove(c,token,"player",Role.PLAYER));tested.command(c,"dm",new TurnService.Command(UUID.randomUUID(),2,"GRANT",b.toString(),List.of(),"Hidden grant"));verify(events).changed(c,true);
    }
}
