package br.com.casanotify.tv;

import java.util.*;
import org.json.*;

/** A single local owner. Account data is encrypted by the Android-backed Store. */
public final class AccountManager {
    public interface Store { String read() throws Exception; void write(String s) throws Exception; void delete() throws Exception; }
    public interface Clock { long now(); }
    public static final String TERMS_VERSION="2026-09-28";
    private final Store store; private final Clock clock;
    private int failures; private long blockedUntil;
    private final Map<String,Session> sessions=new LinkedHashMap<>();
    public static final class Session {
        public final String id=AuthCrypto.token(),csrf=AuthCrypto.token();public final long created;public long touched;
        Session(long now){created=touched=now;}
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
    public synchronized Session register(String username,String name,String password,boolean accepted)throws Exception{
        validateRegistration(username,name,password,accepted);
        String salt=Base64.getEncoder().encodeToString(AuthCrypto.randomBytes(24));
        JSONObject a=new JSONObject().put("username",username.toLowerCase(Locale.ROOT)).put("display_name",name.trim()).put("salt",salt).put("password_hash",AuthCrypto.hash(password,salt)).put("terms_version",TERMS_VERSION).put("terms_accepted_at",clock.now()).put("failures",0).put("blocked_until",0);
        store.write(a.toString());return createSession();
    }
    private Session createSession(){while(sessions.size()>=16)sessions.remove(sessions.keySet().iterator().next());Session s=new Session(clock.now());sessions.put(s.id,s);return s;}
    public synchronized Session login(String username,String password)throws Exception{
        JSONObject a=account();if(a==null)throw new IllegalArgumentException("Login inválido.");
        long now=clock.now();blockedUntil=Math.max(blockedUntil,a.optLong("blocked_until"));failures=Math.max(failures,a.optInt("failures"));
        if(now<blockedUntil)throw new IllegalArgumentException("Aguarde cinco minutos antes de tentar novamente.");
        if(blockedUntil>0){failures=0;blockedUntil=0;}
        boolean pass=password!=null&&password.length()<=128&&AuthCrypto.same(AuthCrypto.hash(password,a.getString("salt")),a.getString("password_hash"));
        if(!pass||username==null||!AuthCrypto.same(username.toLowerCase(Locale.ROOT),a.getString("username"))){failures++;if(failures>=5)blockedUntil=now+300000;a.put("failures",failures).put("blocked_until",blockedUntil);store.write(a.toString());throw new IllegalArgumentException("Usuário ou senha inválidos.");}
        failures=0;blockedUntil=0;a.put("failures",0).put("blocked_until",0);store.write(a.toString());return createSession();
    }
    public synchronized Session session(String id){return session(id,true);}
    public synchronized Session session(String id,boolean touch){Session s=sessions.get(id);long now=clock.now();if(s==null)return null;if(now-s.created>43200000||now-s.touched>1800000){sessions.remove(id);return null;}if(touch)s.touched=now;return s;}
    public synchronized void logout(String id){sessions.remove(id);}
    public synchronized void changePassword(String old,String replacement)throws Exception{
        passwordPolicy(replacement);JSONObject a=account();Session verified=login(a.getString("username"),old);sessions.remove(verified.id);a=account();String salt=Base64.getEncoder().encodeToString(AuthCrypto.randomBytes(24));a.put("salt",salt).put("password_hash",AuthCrypto.hash(replacement,salt));store.write(a.toString());sessions.clear();
    }
    public synchronized void rename(String name)throws Exception{if(name.trim().isEmpty()||name.length()>60)throw new IllegalArgumentException("Nome inválido.");JSONObject a=account();a.put("display_name",name.trim());store.write(a.toString());}
    public synchronized void reset()throws Exception{store.delete();sessions.clear();failures=0;blockedUntil=0;}
    public static String cookie(Session s){return "__Host-casanotify="+s.id+"; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=43200";}
    public static String clearCookie(){return "__Host-casanotify=; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=0";}
}
