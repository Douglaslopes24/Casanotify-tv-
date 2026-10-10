package br.com.casanotify.tv;

import android.security.keystore.*;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;

/** Native Android TV Remote Service. No ADB, accessibility service or injected system privileges. */
public final class AndroidRemote implements AutoCloseable {
    private static final String ALIAS="CasaNotifyAndroidRemote240";
    public interface Listener{void state(boolean connected,String message);}
    private final SSLSocket socket;private final Listener listener;private final Object output=new Object();
    private final RemoteWire.State protocol=new RemoteWire.State();private final CountDownLatch ready=new CountDownLatch(1);
    private volatile boolean closed,connected;
    private AndroidRemote(SSLSocket socket,Listener listener){this.socket=socket;this.listener=listener;}
    private static synchronized KeyStore keys()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(!store.containsAlias(ALIAS)){
            KeyPairGenerator generator=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN).setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_NONE,KeyProperties.DIGEST_SHA1,KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA384,KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                .setCertificateSubject(new X500Principal("CN=CasaNotify Android Remote"))
                .setCertificateSerialNumber(new BigInteger(120,new SecureRandom()).add(BigInteger.ONE))
                .setCertificateNotBefore(new Date(System.currentTimeMillis()-86400000L))
                .setCertificateNotAfter(new Date(System.currentTimeMillis()+315360000000L)).build());generator.generateKeyPair();
        }return store;
    }
    private static X509Certificate certificate()throws Exception{return(X509Certificate)keys().getCertificate(ALIAS);}
    private static SSLSocket open(String host,int port,String fingerprint)throws Exception{
        String address=ControlClient.ipv4(host);KeyStore store=keys();PrivateKey key=(PrivateKey)store.getKey(ALIAS,null);X509Certificate certificate=(X509Certificate)store.getCertificate(ALIAS);
        X509ExtendedKeyManager manager=new X509ExtendedKeyManager(){
            private String alias(String[] types){if(types!=null)for(String type:types)if(type.equals("RSA")||type.startsWith("RSA_"))return ALIAS;return null;}
            public String chooseClientAlias(String[] types,Principal[] issuers,Socket socket){return alias(types);}
            public String chooseEngineClientAlias(String[] types,Principal[] issuers,SSLEngine engine){return alias(types);}
            public String[] getClientAliases(String type,Principal[] issuers){return alias(new String[]{type})==null?null:new String[]{ALIAS};}
            public String[] getServerAliases(String type,Principal[] issuers){return null;}
            public String chooseServerAlias(String type,Principal[] issuers,Socket socket){return null;}
            public X509Certificate[] getCertificateChain(String alias){return ALIAS.equals(alias)?new X509Certificate[]{certificate}:null;}
            public PrivateKey getPrivateKey(String alias){return ALIAS.equals(alias)?key:null;}
        };
        if(fingerprint!=null&&!fingerprint.matches("[0-9a-f]{64}"))throw new CertificateException("Certificado salvo inválido.");
        // Bootstrap is restricted to the pairing port. PIN proof binds the observed RSA keys before persistence.
        if(fingerprint==null&&port!=6467)throw new CertificateException("Vincule o controle primeiro.");
        TrustManager trust=new X509TrustManager(){public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}public void checkClientTrusted(X509Certificate[] chain,String type)throws CertificateException{throw new CertificateException("Somente cliente.");}public void checkServerTrusted(X509Certificate[] chain,String type)throws CertificateException{
            if(chain==null||chain.length==0||!(chain[0].getPublicKey() instanceof RSAPublicKey))throw new CertificateException("Certificado de controle inválido.");chain[0].checkValidity();
            try{if(fingerprint!=null&&!AuthCrypto.same(fingerprint,AuthCrypto.hex(MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded()))))throw new CertificateException("A identificação do controle da TV mudou.");}catch(GeneralSecurityException e){throw new CertificateException("Confira a identificação do controle.",e);}
        }};
        SSLContext context=SSLContext.getInstance("TLS");context.init(new KeyManager[]{manager},new TrustManager[]{trust},new SecureRandom());SSLSocket socket=(SSLSocket)context.getSocketFactory().createSocket();
        try{List<String> allowed=new ArrayList<>();for(String protocol:socket.getSupportedProtocols())if(protocol.equals("TLSv1.2")||protocol.equals("TLSv1.3"))allowed.add(protocol);socket.setEnabledProtocols(allowed.toArray(new String[0]));socket.connect(new InetSocketAddress(address,port),5000);socket.setSoTimeout(10000);socket.startHandshake();return socket;}catch(Exception e){try{socket.close();}catch(Exception ignored){}throw e;}
    }
    public static AndroidRemote connect(String host,String fingerprint,Listener listener)throws Exception{
        AndroidRemote remote=new AndroidRemote(open(host,6466,fingerprint),listener);Thread reader=new Thread(remote::read,"CasaNotify-TV-remote");reader.setDaemon(true);reader.start();
        try{if(!remote.ready.await(12,TimeUnit.SECONDS)||!remote.connected)throw new IOException("A Android TV não confirmou o controle. Confira se está ligada e pareada.");return remote;}catch(Exception e){remote.close();throw e;}
    }
    private void read(){try{socket.setSoTimeout(16000);while(!closed){byte[] reply=protocol.accept(RemoteWire.read(socket.getInputStream()));if(reply!=null)send(reply);if(protocol.ready()&&!connected){connected=true;ready.countDown();listener.state(true,"Controle conectado à Android TV");}}}catch(Exception e){if(!closed)listener.state(false,"Controle desconectado. Toque em Reconectar.");}finally{connected=false;ready.countDown();close();}}
    private void send(byte[] message)throws IOException{synchronized(output){if(closed)throw new IOException("Controle desconectado.");RemoteWire.write(socket.getOutputStream(),message);}}
    public boolean connected(){return connected&&!closed;}
    public void key(int code)throws IOException{if(!connected())throw new IOException("Conecte o controle primeiro.");send(RemoteWire.key(code));}
    public void close(){closed=true;connected=false;try{socket.close();}catch(IOException ignored){}}
    public static final class Pairing implements AutoCloseable {
        private final SSLSocket socket;private final X509Certificate client,server;private final long started=System.nanoTime();private int attempts;
        public Pairing(String host)throws Exception{
            socket=open(host,6467,null);
            try{client=certificate();server=(X509Certificate)socket.getSession().getPeerCertificates()[0];exchange(RemoteWire.pairRequest(),11);exchange(RemoteWire.pairOptions(),20);exchange(RemoteWire.pairConfiguration(),31);}catch(Exception e){close();throw e;}
        }
        private void exchange(byte[] request,int response)throws Exception{RemoteWire.write(socket.getOutputStream(),request);RemoteWire.requirePairing(RemoteWire.read(socket.getInputStream()),response);}
        public String finish(String pin)throws Exception{
            if(++attempts>5||System.nanoTime()-started>TimeUnit.MINUTES.toNanos(2)){close();throw new IOException("Gere um novo PIN no controle.");}
            byte[] secret=RemoteWire.pairingSecret(client.getPublicKey(),server.getPublicKey(),pin);exchange(RemoteWire.pairing(40,RemoteWire.bytes(1,secret)),41);
            String fingerprint=AuthCrypto.hex(MessageDigest.getInstance("SHA-256").digest(server.getEncoded()));close();return fingerprint;
        }
        public void close(){try{socket.close();}catch(IOException ignored){}}
    }
}
