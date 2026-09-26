package br.com.casanotify.tv;

import android.security.keystore.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.*;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;

/** Version 2.0.1 migrates TLS key authorizations once; later upgrades preserve its fingerprint. */
public final class LocalTls {
    private static final String ALIAS="CasaNotifyHttps201";
    private static SSLContext server;
    private static X509Certificate certificate;
    public static synchronized SSLContext server()throws Exception{
        if(server!=null)return server;
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias(ALIAS)){
            KeyPairGenerator g=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore");
            g.initialize(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                // Conscrypt hashes the TLS transcript itself and signs that digest using NONEwithECDSA.
                .setDigests(KeyProperties.DIGEST_NONE,KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA384,KeyProperties.DIGEST_SHA512)
                .setCertificateSubject(new X500Principal("CN=CasaNotify TV, O=Local Device"))
                .setCertificateSerialNumber(new BigInteger(120,new SecureRandom()).add(BigInteger.ONE))
                .setCertificateNotBefore(new Date(System.currentTimeMillis()-86400000L))
                .setCertificateNotAfter(new Date(System.currentTimeMillis()+315360000000L)).build());g.generateKeyPair();
        }
        final PrivateKey key=(PrivateKey)store.getKey(ALIAS,null);certificate=(X509Certificate)store.getCertificate(ALIAS);
        DeviceKeyManager manager=new DeviceKeyManager(ALIAS,key,certificate);
        server=SSLContext.getInstance("TLS");server.init(new KeyManager[]{manager},null,new SecureRandom());return server;
    }
    public static String fingerprint()throws Exception{server();return AuthCrypto.hex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));}
    public static SSLSocketFactory pinned(String fingerprint)throws Exception{
        if(!fingerprint.matches("[0-9a-fA-F]{64}"))throw new IllegalArgumentException("Impressão digital inválida.");
        TrustManager[] managers={new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
            public void checkClientTrusted(X509Certificate[] chain,String auth)throws java.security.cert.CertificateException{throw new java.security.cert.CertificateException("Client certificates unsupported");}
            public void checkServerTrusted(X509Certificate[] chain,String auth)throws java.security.cert.CertificateException{
                try{if(chain.length==0)throw new Exception();chain[0].checkValidity();String actual=AuthCrypto.hex(MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded()));if(!AuthCrypto.same(fingerprint.toLowerCase(Locale.ROOT),actual))throw new Exception();}
                catch(Exception e){throw new java.security.cert.CertificateException("O certificado da TV não corresponde ao certificado aprovado.");}
            }
        }};SSLContext context=SSLContext.getInstance("TLS");context.init(null,managers,new SecureRandom());return context.getSocketFactory();
    }
}
