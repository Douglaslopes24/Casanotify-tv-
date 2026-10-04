package br.com.casanotify.tv;
import java.nio.charset.StandardCharsets;import java.security.*;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import java.util.*;

/** HTTPS protects transport. This proof requires a paired key and a fresh, one-use server challenge. */
public final class RequestAuth {
 public interface Resolver { Key find(String id)throws Exception; }
 public interface Clock { long now(); }
 public static final class Key { public final String hash,role;public Key(String hash,String role){this.hash=hash;this.role=role;} }
 public static final class Client { public final String id,role;Client(String id,String role){this.id=id;this.role=role;} }
 private final String bootKey=AuthCrypto.token();private final Resolver resolver;private final Clock clock;
 private final Map<String,Long> used=new HashMap<>();
 private static Clock monotonic(){long base=System.currentTimeMillis(),start=System.nanoTime();return ()->base+(System.nanoTime()-start)/1000000;}
 public RequestAuth(Resolver resolver){this(resolver,monotonic());}
 RequestAuth(Resolver resolver,Clock clock){this.resolver=resolver;this.clock=clock;}
 public static String mac(String key,String text)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return AuthCrypto.hex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));}
 public synchronized String challenge()throws Exception{String value=clock.now()+"."+AuthCrypto.token();return value+"."+mac(bootKey,value);}
 public static String clientId(String token)throws Exception{return AuthCrypto.digest(AuthCrypto.digest(token));}
 private static String canonical(String id,String method,String path,String challenge,String body,String cookie,String csrf)throws Exception{return "CasaNotify-HMAC-v1\n"+id+"\n"+method+"\n"+path+"\n"+challenge+"\n"+AuthCrypto.digest(body==null?"":body)+"\n"+AuthCrypto.digest(cookie==null?"":cookie)+"\n"+(csrf==null?"":csrf);}
 public static Map<String,String> sign(String id,String token,String method,String path,String challenge,String body,String cookie,String csrf)throws Exception{
  Map<String,String> out=new HashMap<>();out.put("X-CasaNotify-Client",id);out.put("X-CasaNotify-Challenge",challenge);out.put("X-CasaNotify-Proof",mac(AuthCrypto.digest(token),canonical(id,method,path,challenge,body,cookie,csrf)));return out;
 }
 public synchronized Client verify(LanServer.Request request)throws Exception{
  String id=request.headers.getOrDefault("x-casanotify-client",""),challenge=request.headers.getOrDefault("x-casanotify-challenge",""),proof=request.headers.getOrDefault("x-casanotify-proof","");
  if(!request.secure||!id.matches("ha|[0-9a-f]{64}")||!proof.matches("[0-9a-f]{64}")||!challenge.matches("[0-9]{1,16}\\.[A-Za-z0-9_-]{43}\\.[0-9a-f]{64}"))return null;
  String[] parts=challenge.split("\\.");long at;try{at=Long.parseLong(parts[0]);}catch(NumberFormatException e){return null;}long now=clock.now();if(now<at||now-at>60000||!AuthCrypto.same(parts[2],mac(bootKey,parts[0]+"."+parts[1])))return null;
  Key key=resolver.find(id);if(key==null||!AuthCrypto.same(proof,mac(key.hash,canonical(id,request.method,request.path,challenge,request.body,request.headers.get("cookie"),request.headers.get("x-casanotify-csrf")))))return null;
  used.entrySet().removeIf(e->e.getValue()<now);if(used.containsKey(challenge)||used.size()>=2048)return null;used.put(challenge,at+60000);return new Client(id,key.role);
 }
}
