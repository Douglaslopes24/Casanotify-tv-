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
    public static SSLSocketFactory pinned(String fingerprint)throws Exception{return CertificatePin.socketFactory(fingerprint);}
}
