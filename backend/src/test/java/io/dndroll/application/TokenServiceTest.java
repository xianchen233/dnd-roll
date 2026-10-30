package io.dndroll.application;
import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.application.TokenService.*;
import io.dndroll.domain.DomainException;
import io.dndroll.domain.Movement.Role;
import java.util.*;import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
class TokenServiceTest {
 final UUID campaign=UUID.randomUUID(),scene=UUID.randomUUID(); final Memory store=new Memory();int events;
 final TokenService service=new TokenService(store,id->events++);
 class Memory implements Store {
  Role dm=Role.DM;long version;boolean exists=true;Set<String> members=new HashSet<>(Set.of("alice","bob"));
  Map<UUID,Token> tokens=new HashMap<>();Map<String,Receipt> receipts=new HashMap<>();List<String> locks=new ArrayList<>();
  public <T>T actorLocked(String a,Supplier<T>w){locks.add("actor");return w.get();}public <T>T locked(UUID c,Supplier<T>w){locks.add("campaign");return w.get();}
  public Role role(UUID c,String a){return a.equals("dm")?dm:members.contains(a)?Role.PLAYER:null;}
  public Scene scene(UUID c,UUID s){return exists&&s.equals(scene)?new Scene(s,24,16,version,List.copyOf(tokens.values())):null;}
  public Receipt receipt(String a,UUID r){return receipts.get(a+r);}public void receipt(String a,UUID r,Receipt x){receipts.put(a+r,x);}
  public void save(UUID c,UUID s,Token t,boolean create){tokens.put(t.id(),t);}public long advance(UUID s){return ++version;}
 }
 Command command(UUID r,long v,String n,List<String> cs){return new Command(r,v,n,3,4,true,cs);}Command command(long v,List<String>cs){return command(UUID.randomUUID(),v," Name ",cs);}
 @Test void createCanonicalizesAndRetriesWithoutSecondEffect(){
  var r=UUID.randomUUID();var result=service.create(campaign,scene,"dm",command(r,0," 😀 ",List.of("bob","alice")));assertNotNull(result);
  assertEquals("😀",result.token().name());assertEquals(List.of("alice","bob"),result.token().controllers());assertEquals(0,result.token().version());assertEquals(1,result.sceneVersion());
  assertEquals(result,service.create(campaign,scene,"dm",command(r,0,"😀",List.of("alice","bob"))));assertEquals(1,events);assertEquals(1,store.tokens.size());assertEquals(List.of("actor","campaign","actor","campaign"),store.locks);
 }
 @Test void historicalUpdateNeverRestoresControlsEvenAfterMembershipRemoval(){
  var created=service.create(campaign,scene,"dm",command(0,List.of("alice","bob")));assertNotNull(created);
  var body=command(0,List.of("alice"));var updated=service.update(campaign,scene,created.token().id(),"dm",body);
  var revoked=service.update(campaign,scene,created.token().id(),"dm",command(1,List.of()));store.members.remove("alice");
  assertEquals(updated,service.update(campaign,scene,created.token().id(),"dm",body));assertEquals(revoked.token(),store.tokens.get(created.token().id()));assertEquals(3,events);assertEquals(3,store.version);
  code("VERSION_CONFLICT",()->service.update(campaign,scene,created.token().id(),"dm",command(1,List.of())));
 }
 @Test void dmAndOwnershipCheckedBeforeReceipt(){
  var cmd=command(0,List.of());assertNotNull(service.create(campaign,scene,"dm",cmd));
  for(var role:Arrays.asList(Role.PLAYER,null)){store.dm=role;code("FORBIDDEN",()->service.create(campaign,scene,"dm",cmd));code("FORBIDDEN",()->service.list(campaign,scene,"dm"));}
  store.dm=Role.DM;store.exists=false;code("NOT_FOUND",()->service.create(campaign,scene,"dm",cmd));
 }
 @Test void fingerprintsFreshMembershipAndInputAreStrict(){
  var cmd=command(0,List.of("alice"));assertNotNull(service.create(campaign,scene,"dm",cmd));
  code("IDEMPOTENCY_CONFLICT",()->service.create(campaign,scene,"dm",command(cmd.requestId(),0,"Different",List.of("alice"))));
  code("IDEMPOTENCY_CONFLICT",()->service.create(UUID.randomUUID(),scene,"dm",cmd));
  code("INVALID_INPUT",()->service.create(campaign,scene,"dm",command(1,List.of("outsider"))));code("INVALID_INPUT",()->service.create(campaign,scene,"dm",command(1,List.of("alice","alice"))));
  code("INVALID_INPUT",()->service.create(campaign,scene,"dm",command(UUID.randomUUID(),1,"😀".repeat(101),List.of())));
  code("INVALID_INPUT",()->service.create(campaign,scene,"dm",new Command(UUID.randomUUID(),1,"Name",24,0,false,List.of())));assertEquals(1,events);
 }
 static void code(String c,Runnable r){assertEquals(c,assertThrows(DomainException.class,r::run).code());}
}
