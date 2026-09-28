package br.com.casanotify.tv;
import javax.net.ssl.*;import java.security.*;import java.security.cert.X509Certificate;import java.util.Locale;
/** Trust only the exact TV certificate approved by the owner; no fallback. */
public final class CertificatePin {
    public static SSLSocketFactory socketFactory(String fingerprint)throws Exception{
        if(fingerprint==null||!fingerprint.matches("[0-9a-fA-F]{64}"))throw new IllegalArgumentException("Impressão digital inválida.");
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
