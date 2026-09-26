package br.com.casanotify.tv;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** PBKDF2-HMAC-SHA256 and RFC 6238 TOTP; no custom cryptographic primitives. */
public final class AuthCrypto {
    public static final int ITERATIONS = 600000;
    private static final SecureRandom RANDOM = new SecureRandom();
    public static byte[] randomBytes(int n) { byte[] b = new byte[n]; RANDOM.nextBytes(b); return b; }
    public static String token() { return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32)); }
    public static String hash(String password, String salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, 256);
        try { return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
        finally { spec.clearPassword(); }
    }
    public static boolean same(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
    public static String digest(String value) throws Exception { return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    public static String hex(byte[] bytes) { StringBuilder b = new StringBuilder(); for(byte v:bytes)b.append(String.format(Locale.ROOT,"%02x",v&255)); return b.toString(); }
    public static String base32(byte[] bytes) {
        String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"; StringBuilder s=new StringBuilder(); int buffer=0,bits=0;
        for(byte b:bytes){buffer=(buffer<<8)|(b&255);bits+=8;while(bits>=5){s.append(alphabet.charAt((buffer>>(bits-5))&31));bits-=5;}}
        if(bits>0)s.append(alphabet.charAt((buffer<<(5-bits))&31));return s.toString();
    }
    public static byte[] unbase32(String secret) {
        String value=secret.toUpperCase(Locale.ROOT).replace(" ","");byte[] out=new byte[value.length()*5/8];int buffer=0,bits=0,i=0;
        for(char c:value.toCharArray()){int n="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c);if(n<0)throw new IllegalArgumentException("Invalid TOTP seed");buffer=(buffer<<5)|n;bits+=5;if(bits>=8){out[i++]=(byte)(buffer>>(bits-8));bits-=8;}}
        return Arrays.copyOf(out,i);
    }
    public static String totp(String secret,long counter,int digits)throws Exception {
        Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(unbase32(secret),"HmacSHA1"));byte[] h=mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());int off=h[h.length-1]&15;
        int n=((h[off]&127)<<24)|((h[off+1]&255)<<16)|((h[off+2]&255)<<8)|(h[off+3]&255);int divisor=digits==8?100000000:1000000;return String.format(Locale.ROOT,"%0"+digits+"d",n%divisor);
    }
    public static long verifyTotp(String secret,String code,long now,long used)throws Exception {
        if(code==null||!code.matches("[0-9]{6}"))return -1;long current=now/30000,matched=-1;
        for(long step=current-1;step<=current+1;step++)if(step>used&&same(totp(secret,step,6),code))matched=step;return matched;
    }
}
