package br.com.casanotify.tv;
import org.junit.*;import static org.junit.Assert.*;import javax.net.ssl.*;import java.security.*;import java.io.*;import java.nio.charset.StandardCharsets;

public class TlsTest {
    @Test public void realTlsTransportCookieAndOrigin()throws Exception{
        KeyStore keys=KeyStore.getInstance("PKCS12");try(InputStream in=new FileInputStream(System.getProperty("casanotify.testKeystore"))){keys.load(in,"test-fixture-only".toCharArray());}
        KeyManagerFactory km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys,"test-fixture-only".toCharArray());SSLContext serverTls=SSLContext.getInstance("TLS");serverTls.init(km.getKeyManagers(),null,null);
        KeyStore trust=KeyStore.getInstance(KeyStore.getDefaultType());trust.load(null,null);trust.setCertificateEntry("local",keys.getCertificate("local"));TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(trust);SSLContext clientTls=SSLContext.getInstance("TLS");clientTls.init(null,tm.getTrustManagers(),null);
        try(LanServer server=new LanServer(0,r->{assertTrue(r.secure);return LanServer.Response.json(200,"{\"ok\":true}").header("Set-Cookie","__Host-test=value; Path=/; Secure; HttpOnly; SameSite=Strict");},serverTls)){
            server.start();String host="127.0.0.1:"+server.port();String ok=request(clientTls,server.port(),"https://"+host);assertTrue(ok.startsWith("HTTP/1.1 200"));assertTrue(ok.contains("Set-Cookie: __Host-test="));assertTrue(ok.contains("frame-ancestors 'none'"));assertTrue(request(clientTls,server.port(),"http://"+host).startsWith("HTTP/1.1 403"));assertTrue(request(clientTls,server.port(),"https://evil.example").startsWith("HTTP/1.1 403"));
        }
    }
    private String request(SSLContext tls,int port,String origin)throws Exception{try(SSLSocket s=(SSLSocket)tls.getSocketFactory().createSocket("127.0.0.1",port)){s.setSoTimeout(5000);s.startHandshake();s.getOutputStream().write(("POST /api/config HTTP/1.1\r\nHost: 127.0.0.1:"+port+"\r\nOrigin: "+origin+"\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}").getBytes(StandardCharsets.US_ASCII));ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;while((n=s.getInputStream().read(bytes))>=0)out.write(bytes,0,n);return out.toString("UTF-8");}}
}
