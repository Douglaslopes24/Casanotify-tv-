package br.com.casanotify.tv;
import java.security.*;import java.nio.charset.StandardCharsets;import java.util.Locale;
public final class Pairing {
 private static String code="",scope="ha";private static long deadline;private static int attempts;
 public static synchronized String issue(){return issue("ha");}
 public static synchronized String issue(String kind){scope=kind;code=String.format(Locale.ROOT,"%06d",new SecureRandom().nextInt(1000000));deadline=System.nanoTime()+120_000_000_000L;attempts=0;return code;}
 public static synchronized boolean redeem(String s){return redeem(s,"ha");}
 public static synchronized boolean redeem(String s,String kind){if(!scope.equals(kind))return false;if(code.isEmpty()||System.nanoTime()>deadline||attempts>=5)return false;attempts++;if(!MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8),s.getBytes(StandardCharsets.UTF_8)))return false;code="";return true;}
 public static synchronized void cancel(){code="";}
}
