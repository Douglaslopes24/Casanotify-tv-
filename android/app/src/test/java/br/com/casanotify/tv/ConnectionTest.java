package br.com.casanotify.tv;

import org.junit.*;
import static org.junit.Assert.*;
import org.json.*;

/** Lifecycle and security properties of remembered access and the durable retry outbox. */
public class ConnectionTest {
    private static final String PASSWORD="local testing password 230";
    private static class Memory implements AccountManager.Store{String value="";public String read(){return value;}public void write(String value){this.value=value;}public void delete(){value="";}}
    private static class Clock implements AccountManager.Clock{long now=1800000000000L;public long now(){return now;}}
    private interface Action{void run()throws Exception;}
    private static void rejected(Action action)throws Exception{try{action.run();fail("Expected rejection");}catch(IllegalArgumentException expected){}}
    private final Memory store=new Memory();private final Clock clock=new Clock();
    private JSONObject message(String id)throws Exception{return new JSONObject().put("id",id).put("title","Delivery").put("message","Pedido chegando");}
    private JSONObject target(String id,String pin,String token)throws Exception{return new JSONObject().put("device_id",id).put("tls_fingerprint",pin).put("token",token);}
    @Test public void rememberedLoginSurvivesTvRestartWithoutPersistingPasswordOrRawGrant()throws Exception{
        AccountManager accounts=new AccountManager(store,clock);AccountManager.Session initial=accounts.register("owner","Owner",PASSWORD,true,"phone-A");String grant=accounts.remember(initial);
        assertFalse(store.value.contains(PASSWORD));assertFalse(store.value.contains(grant));assertFalse(accounts.profile().has("trusted_clients"));
        clock.now+=2*24*60*60*1000L;accounts=new AccountManager(store,clock);AccountManager.Session resumed=accounts.resume("phone-A",grant);assertEquals("phone-A",resumed.clientId);assertNotEquals(initial.id,resumed.id);assertNotEquals(initial.csrf,resumed.csrf);
    }
    @Test public void aGrantCannotMoveToAnotherPairedControlOrAcceptGuessedTokens()throws Exception{
        AccountManager a=new AccountManager(store,clock);String grant=a.remember(a.register("owner","Owner",PASSWORD,true,"phone-A"));
        rejected(()->a.resume("phone-B",grant));rejected(()->a.resume("phone-A",AuthCrypto.token()));rejected(()->a.resume("phone-A",""));rejected(()->a.resume("phone-A",null));assertNotNull(a.resume("phone-A",grant));
    }
    @Test public void logoutRevokesRememberedAccessAndAllSessionsForOnlyThatControl()throws Exception{
        AccountManager a=new AccountManager(store,clock);String one=a.remember(a.register("owner","Owner",PASSWORD,true,"one"));String two=a.remember(a.login("owner",PASSWORD,"two"));AccountManager.Session session=a.resume("one",one);a.forgetClient("one");
        assertNull(a.session(session.id));rejected(()->a.resume("one",one));assertNotNull(a.resume("two",two));AccountManager restarted=new AccountManager(store,clock);rejected(()->restarted.resume("one",one));
    }
    @Test public void passwordChangeAndResetInvalidateRememberedDevices()throws Exception{
        AccountManager a=new AccountManager(store,clock);String grant=a.remember(a.register("owner","Owner",PASSWORD,true,"device"));a.changePassword(PASSWORD,"new password for testing");rejected(()->a.resume("device",grant));String next=a.remember(a.login("owner","new password for testing","device"));a.reset();rejected(()->a.resume("device",next));
    }
    @Test public void rememberingAgainRotatesGrantWithoutKeepingAnOlderCopy()throws Exception{
        AccountManager a=new AccountManager(store,clock);AccountManager.Session session=a.register("owner","Owner",PASSWORD,true,"device");String old=a.remember(session),current=a.remember(session);rejected(()->a.resume("device",old));assertNotNull(a.resume("device",current));assertEquals(1,new JSONObject(store.value).getJSONArray("trusted_clients").length());
    }
    @Test public void unboundOrLoggedOutSessionCannotCreateRememberedGrant()throws Exception{
        AccountManager a=new AccountManager(store,clock);AccountManager.Session unbound=a.register("owner","Owner",PASSWORD,true);rejected(()->a.remember(unbound));AccountManager.Session bound=a.login("owner",PASSWORD,"device");a.logout(bound.id);rejected(()->a.remember(bound));
    }
    @Test public void legacyAccountWithoutGrantKeepsExistingLogin()throws Exception{
        AccountManager a=new AccountManager(store,clock);a.register("owner","Owner",PASSWORD,true,"legacy");AccountManager restored=new AccountManager(store,clock);rejected(()->restored.resume("legacy",AuthCrypto.token()));assertNotNull(restored.login("owner",PASSWORD,"legacy"));
    }
    @Test public void disablingRememberedAccessKeepsCurrentSessionButInvalidatesOldGrant()throws Exception{
        AccountManager a=new AccountManager(store,clock);AccountManager.Session session=a.register("owner","Owner",PASSWORD,true,"device");String grant=a.remember(session);a.forgetGrant("device");assertNotNull(a.session(session.id));rejected(()->a.resume("device",grant));
    }
    @Test public void identityRequiresBothOriginalUuidAndCertificate()throws Exception{
        JSONObject saved=target("tv-one","a".repeat(64),"secret");JSONObject same=target("tv-one","a".repeat(64),"secret").put("host","192.168.0.90");assertTrue(TvIdentity.same(saved,same));assertFalse(TvIdentity.same(saved,target("tv-one","b".repeat(64),"secret")));assertFalse(TvIdentity.same(saved,target("tv-two","a".repeat(64),"secret")));assertFalse(TvIdentity.same(new JSONObject(),new JSONObject()));
    }
    @Test public void queueSurvivesProcessRestartAndKeepsUnacknowledgedMessages()throws Exception{
        MirrorOutbox queue=new MirrorOutbox(store,clock);queue.enqueue("tv-A","delivery",message("one"));queue=new MirrorOutbox(store,clock);assertEquals(1,queue.size());assertEquals("one",queue.first("tv-A").getJSONObject("payload").getString("id"));assertEquals(1,queue.size());queue.acknowledge("tv-A","one");assertEquals(0,queue.size());assertEquals("",store.value);
    }
    @Test public void queuedMessagesNeverMoveToAnotherTvOrRotatedKey()throws Exception{
        String first=MirrorOutbox.binding(target("tv-one","a".repeat(64),"first")),second=MirrorOutbox.binding(target("tv-two","a".repeat(64),"first")),rotated=MirrorOutbox.binding(target("tv-one","a".repeat(64),"new"));assertNotEquals(first,second);assertNotEquals(first,rotated);
        MirrorOutbox queue=new MirrorOutbox(store,clock);queue.enqueue(first,"app",message("one"));assertNull(queue.first(second));assertEquals("",store.value);queue.enqueue(first,"app",message("two"));assertNull(queue.first(rotated));
    }
    @Test public void queueExpiresAtTenMinutesAndDiscardsAfterClockRollback()throws Exception{
        MirrorOutbox queue=new MirrorOutbox(store,clock);queue.enqueue("tv","app",message("one"));clock.now+=MirrorOutbox.MAX_AGE-1;assertEquals(1,queue.size());clock.now++;assertNull(queue.first("tv"));assertEquals("",store.value);queue.enqueue("tv","app",message("two"));clock.now--;assertEquals(0,queue.size());
    }
    @Test public void queueBoundDeduplicationAndPrivacyClear()throws Exception{
        MirrorOutbox queue=new MirrorOutbox(store,clock);for(int i=0;i<40;i++)queue.enqueue("tv","app",message("n"+i));assertEquals(30,queue.size());assertEquals("n10",queue.first("tv").getJSONObject("payload").getString("id"));queue.enqueue("tv","app",message("n20"));assertEquals(30,queue.size());queue.clear();assertEquals("",store.value);
    }
    @Test public void acknowledgementsCannotDeleteAnotherBindingOrUnknownId()throws Exception{
        MirrorOutbox queue=new MirrorOutbox(store,clock);queue.enqueue("tv","app",message("one"));queue.acknowledge("other","one");queue.acknowledge("tv","unknown");assertEquals(1,queue.size());queue.acknowledge("tv","one");assertEquals(0,queue.size());
    }
 @Test public void credentialBindingRejectsAnotherKeyOrReceiverButAllowsIpChange()throws Exception{
  JSONObject saved=new JSONObject().put("device_id","tv-a").put("tls_fingerprint","a".repeat(64)).put("token","key-a").put("host","192.168.0.1");
  assertTrue(TvIdentity.sameCredential(saved,new JSONObject(saved.toString()).put("host","192.168.0.2")));
  for(String field:new String[]{"device_id","tls_fingerprint","token"})assertFalse(TvIdentity.sameCredential(saved,new JSONObject(saved.toString()).put(field,"changed")));
  assertFalse(TvIdentity.sameCredential(saved,null));assertFalse(TvIdentity.sameCredential(null,saved));assertFalse(TvIdentity.sameCredential(new JSONObject(),new JSONObject()));
 }

}
