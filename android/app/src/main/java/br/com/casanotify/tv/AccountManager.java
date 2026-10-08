package br.com.casanotify.tv;

import java.util.*;
import org.json.*;

/** A single local owner. Account data is encrypted by the Android-backed Store. */
public final class AccountManager {
    public interface Store { String read() throws Exception; void write(String s) throws Exception; void delete() throws Exception; }
    public interface Clock { long now(); }
    public static final String TERMS_VERSION="2026-10-08";
    private final Store store; private final Clock clock;
    private int failures; private long blockedUntil;
    private final Map<String,Session> sessions=new LinkedHashMap<>();
    public static final class Session {
        public final String id=AuthCrypto.token(),csrf=AuthCrypto.token();public final long created;public long touched;public final String clientId;
        Session(long now,String clientId){created=touched=now;this.clientId=clientId;}
    }
    public AccountManager(Store store){this(store,System::currentTimeMillis);}
    public AccountManager(Store store,Clock clock){this.store=store;this.clock=clock;}
    private JSONObject account()throws Exception{
        String s=store.read();if(s==null||s.isEmpty())return null;JSONObject a=new JSONObject(s);
        // Upgrade the owner in place: keep password hash, salt, consent and lockout.
        if(a.has("totp_secret")||a.has("last_totp")){a.remove("totp_secret");a.remove("last_totp");store.write(a.toString());}
        return a;
    }
    public synchronized boolean exists()throws Exception{return account()!=null;}
    public synchronized JSONObject profile()throws Exception{JSONObject a=account();if(a==null)return new JSONObject();return new JSONObject().put("username",a.getString("username")).put("display_name",a.getString("display_name")).put("two_factor",false).put("terms_version",a.getString("terms_version"));}
    private static void passwordPolicy(String p){if(p==null||p.length()<12||p.length()>128)throw new IllegalArgumentException("Use uma senha de 12 a 128 caracteres.");}
    public synchronized void validateRegistration(String username,String name,String password,boolean accepted)throws Exception{
        if(exists())throw new IllegalArgumentException("Já existe uma conta. Entre com seu login.");
        if(!accepted)throw new IllegalArgumentException("Leia e aceite os termos para continuar.");
        if(username==null||!username.matches("[a-zA-Z0-9._-]{3,40}"))throw new IllegalArgumentException("Usuário: use 3–40 letras, números, ponto, traço ou sublinhado.");
        if(name==null||name.trim().isEmpty()||name.length()>60)throw new IllegalArgumentException("Informe seu nome, até 60 caracteres.");passwordPolicy(password);
    }
    public synchronized Session register(String username,String name,String password,boolean accepted)throws Exception{return register(username,name,password,accepted,"");}
    public synchronized Session register(String username,String name,String password,boolean accepted,String clientId)throws Exception{
        validateRegistration(username,name,password,accepted);
        String salt=Base64.getEncoder().encodeToString(AuthCrypto.randomBytes(24));
        JSONObject a=new JSONObject().put("username",username.toLowerCase(Locale.ROOT)).put("display_name",name.trim()).put("salt",salt).put("password_hash",AuthCrypto.hash(password,salt)).put("terms_version",TERMS_VERSION).put("terms_accepted_at",clock.now()).put("failures",0).put("blocked_until",0);
        store.write(a.toString());return createSession(clientId);
    }
    private Session createSession(String clientId){while(sessions.size()>=16)sessions.remove(sessions.keySet().iterator().next());Session s=new Session(clock.now(),clientId);sessions.put(s.id,s);return s;}
    public synchronized Session login(String username,String password)throws Exception{return login(username,password,"");}
    public synchronized Session login(String username,String password,String clientId)throws Exception{
        JSONObject a=account();if(a==null)throw new IllegalArgumentException("Login inválido.");
        long now=clock.now();blockedUntil=Math.max(blockedUntil,a.optLong("blocked_until"));failures=Math.max(failures,a.optInt("failures"));
        if(now<blockedUntil)throw new IllegalArgumentException("Aguarde cinco minutos antes de tentar novamente.");
        if(blockedUntil>0){failures=0;blockedUntil=0;}
        boolean pass=password!=null&&password.length()<=128&&AuthCrypto.same(AuthCrypto.hash(password,a.getString("salt")),a.getString("password_hash"));
        if(!pass||username==null||!AuthCrypto.same(username.toLowerCase(Locale.ROOT),a.getString("username"))){failures++;if(failures>=5)blockedUntil=now+300000;a.put("failures",failures).put("blocked_until",blockedUntil);store.write(a.toString());throw new IllegalArgumentException("Usuário ou senha inválidos.");}
        failures=0;blockedUntil=0;a.put("failures",0).put("blocked_until",0);store.write(a.toString());return createSession(clientId);
    }
    /** Device-bound renewal grant. Passwords and session cookies are never persisted on the phone. */
    public synchronized String remember(Session session)throws Exception{
        if(session.clientId.isEmpty()||sessions.get(session.id)!=session)throw new IllegalArgumentException("Sessão inválida.");
        JSONObject a=account();JSONArray old=a.optJSONArray("trusted_clients"),next=new JSONArray();
        if(old!=null)for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).getString("client").equals(session.clientId))next.put(old.getJSONObject(i));
        while(next.length()>=20)next.remove(0);
        String token=AuthCrypto.token();next.put(new JSONObject().put("client",session.clientId).put("hash",AuthCrypto.digest(token)));
        a.put("trusted_clients",next);store.write(a.toString());return token;
    }
    public synchronized Session resume(String clientId,String token)throws Exception{
        JSONObject a=account();JSONArray entries=a==null?null:a.optJSONArray("trusted_clients");
        if(token!=null&&token.matches("[A-Za-z0-9_-]{43}")&&entries!=null)for(int i=0;i<entries.length();i++){
            JSONObject item=entries.getJSONObject(i);
            if(AuthCrypto.same(clientId,item.getString("client"))&&AuthCrypto.same(AuthCrypto.digest(token),item.getString("hash")))return createSession(clientId);
        }
        throw new IllegalArgumentException("Entre uma vez com sua senha para autorizar este celular.");
    }
    public synchronized void forgetGrant(String clientId)throws Exception{
        JSONObject a=account();if(a!=null){JSONArray old=a.optJSONArray("trusted_clients"),next=new JSONArray();if(old!=null)for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).getString("client").equals(clientId))next.put(old.getJSONObject(i));a.put("trusted_clients",next);store.write(a.toString());}
    }
    public synchronized void forgetClient(String clientId)throws Exception{forgetGrant(clientId);sessions.values().removeIf(s->s.clientId.equals(clientId));}
    public synchronized Session session(String id){return session(id,true);}
    public synchronized Session session(String id,String clientId,boolean touch){Session s=sessions.get(id);if(s==null||!AuthCrypto.same(s.clientId,clientId))return null;return session(id,touch);}
    public synchronized Session session(String id,boolean touch){Session s=sessions.get(id);long now=clock.now();if(s==null)return null;if(now-s.created>43200000||now-s.touched>1800000){sessions.remove(id);return null;}if(touch)s.touched=now;return s;}
    public synchronized void logout(String id){sessions.remove(id);}
    public synchronized void changePassword(String old,String replacement)throws Exception{
        passwordPolicy(replacement);JSONObject a=account();Session verified=login(a.getString("username"),old);sessions.remove(verified.id);a=account();String salt=Base64.getEncoder().encodeToString(AuthCrypto.randomBytes(24));a.put("salt",salt).put("password_hash",AuthCrypto.hash(replacement,salt));a.remove("trusted_clients");store.write(a.toString());sessions.clear();
    }
    public synchronized void rename(String name)throws Exception{if(name.trim().isEmpty()||name.length()>60)throw new IllegalArgumentException("Nome inválido.");JSONObject a=account();a.put("display_name",name.trim());store.write(a.toString());}
    public synchronized void reset()throws Exception{store.delete();sessions.clear();failures=0;blockedUntil=0;}
    public static String cookie(Session s){return "__Host-casanotify="+s.id+"; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=43200";}
    public static String clearCookie(){return "__Host-casanotify=; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=0";}
}
