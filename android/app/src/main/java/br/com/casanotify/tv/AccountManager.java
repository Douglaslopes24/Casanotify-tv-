package br.com.casanotify.tv;

import java.util.*;
import org.json.*;

/** A single local owner. Account data is encrypted by the Android-backed Store. */
public final class AccountManager {
    public interface Store { String read() throws Exception; void write(String s) throws Exception; void delete() throws Exception; }
    public interface Clock { long now(); }
    public static final String TERMS_VERSION="2026-09-26";
    private final Store store; private final Clock clock;
    private JSONObject pending; private long pendingUntil; private int pendingAttempts;
    private int failures; private long blockedUntil;
    private final Map<String,Session> sessions=new LinkedHashMap<>();
    public static final class Session {
        public final String id=AuthCrypto.token(),csrf=AuthCrypto.token();public final long created;public long touched;
        Session(long now){created=touched=now;}
    }
    public AccountManager(Store store){this(store,System::currentTimeMillis);}
    public AccountManager(Store store,Clock clock){this.store=store;this.clock=clock;}
    private JSONObject account()throws Exception{String s=store.read();return s==null||s.isEmpty()?null:new JSONObject(s);}
    public synchronized boolean exists()throws Exception{return account()!=null;}
    public synchronized JSONObject profile()throws Exception{JSONObject a=account();if(a==null)return new JSONObject();return new JSONObject().put("username",a.getString("username")).put("display_name",a.getString("display_name")).put("two_factor",true).put("terms_version",a.getString("terms_version"));}
    private static void passwordPolicy(String p){if(p==null||p.length()<12||p.length()>128)throw new IllegalArgumentException("Use uma senha de 12 a 128 caracteres.");}
    public synchronized JSONObject begin(String username,String name,String password,boolean accepted)throws Exception{
        if(exists())throw new IllegalArgumentException("Já existe uma conta. Entre com seu login.");
        if(!accepted)throw new IllegalArgumentException("Leia e aceite os termos para continuar.");
        if(!username.matches("[a-zA-Z0-9._-]{3,40}"))throw new IllegalArgumentException("Usuário: use 3–40 letras, números, ponto, traço ou sublinhado.");
        if(name.trim().isEmpty()||name.length()>60)throw new IllegalArgumentException("Informe seu nome, até 60 caracteres.");passwordPolicy(password);
        String salt=Base64.getEncoder().encodeToString(AuthCrypto.randomBytes(24)),secret=AuthCrypto.base32(AuthCrypto.randomBytes(20));
        pending=new JSONObject().put("username",username.toLowerCase(Locale.ROOT)).put("display_name",name.trim()).put("salt",salt).put("password_hash",AuthCrypto.hash(password,salt)).put("totp_secret",secret).put("pending_id",AuthCrypto.token()).put("terms_version",TERMS_VERSION).put("terms_accepted_at",clock.now());
        pendingUntil=clock.now()+600000;pendingAttempts=0;
        return new JSONObject().put("pending_id",pending.getString("pending_id")).put("secret",secret).put("account",username).put("issuer","CasaNotify TV");
    }
    public synchronized Session confirm(String id,String code)throws Exception{
        if(pending==null||clock.now()>pendingUntil||pendingAttempts>=5||!AuthCrypto.same(pending.optString("pending_id"),id))throw new IllegalArgumentException("Cadastro vencido. Gere outro código na TV.");
        long counter=AuthCrypto.verifyTotp(pending.getString("totp_secret"),code,clock.now(),-1);
        if(counter<0){pendingAttempts++;throw new IllegalArgumentException("Código do autenticador inválido.");}
        pending.remove("pending_id");pending.put("last_totp",counter).put("failures",0).put("blocked_until",0);store.write(pending.toString());pending=null;return createSession();
    }
    private Session createSession(){while(sessions.size()>=16)sessions.remove(sessions.keySet().iterator().next());Session s=new Session(clock.now());sessions.put(s.id,s);return s;}
    public synchronized Session login(String username,String password,String code)throws Exception{
        JSONObject a=account();if(a==null)throw new IllegalArgumentException("Login inválido.");
        long now=clock.now();blockedUntil=Math.max(blockedUntil,a.optLong("blocked_until"));failures=Math.max(failures,a.optInt("failures"));
        if(now<blockedUntil)throw new IllegalArgumentException("Aguarde cinco minutos antes de tentar novamente.");
        if(blockedUntil>0){failures=0;blockedUntil=0;}
        boolean pass=password!=null&&password.length()<=128&&AuthCrypto.same(AuthCrypto.hash(password,a.getString("salt")),a.getString("password_hash"));
        long step=AuthCrypto.verifyTotp(a.getString("totp_secret"),code,now,a.optLong("last_totp",-1));
        if(!pass||!AuthCrypto.same(username.toLowerCase(Locale.ROOT),a.getString("username"))||step<0){failures++;if(failures>=5)blockedUntil=now+300000;a.put("failures",failures).put("blocked_until",blockedUntil);store.write(a.toString());throw new IllegalArgumentException("Usuário, senha ou código inválidos. Se o código já foi usado, aguarde o próximo.");}
        failures=0;blockedUntil=0;a.put("last_totp",step).put("failures",0).put("blocked_until",0);store.write(a.toString());return createSession();
    }
    public synchronized Session session(String id){return session(id,true);}
    public synchronized Session session(String id,boolean touch){Session s=sessions.get(id);long now=clock.now();if(s==null)return null;if(now-s.created>43200000||now-s.touched>1800000){sessions.remove(id);return null;}if(touch)s.touched=now;return s;}
    public synchronized void logout(String id){sessions.remove(id);}
    public synchronized void changePassword(String old,String code,String replacement)throws Exception{
        passwordPolicy(replacement);JSONObject a=account();Session verified=login(a.getString("username"),old,code);sessions.remove(verified.id);a=account();String salt=Base64.getEncoder().encodeToString(AuthCrypto.randomBytes(24));a.put("salt",salt).put("password_hash",AuthCrypto.hash(replacement,salt));store.write(a.toString());sessions.clear();
    }
    public synchronized void rename(String name)throws Exception{if(name.trim().isEmpty()||name.length()>60)throw new IllegalArgumentException("Nome inválido.");JSONObject a=account();a.put("display_name",name.trim());store.write(a.toString());}
    public synchronized void reset()throws Exception{store.delete();pending=null;sessions.clear();failures=0;blockedUntil=0;}
    public static String cookie(Session s){return "__Host-casanotify="+s.id+"; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=43200";}
    public static String clearCookie(){return "__Host-casanotify=; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=0";}
}
