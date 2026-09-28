package br.com.casanotify.tv;
import org.junit.*;import static org.junit.Assert.*;import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.security.cert.Certificate;import javax.net.ssl.*;import java.util.concurrent.atomic.AtomicInteger;

public class ControlClientTest {
    private static final String COOKIE="__Host-casanotify="+"a".repeat(43);
    private static class Reply extends HttpsURLConnection {
        int status=200;String setCookie=COOKIE+"; Path=/; Secure; HttpOnly",payload="{\"ok\":true}",type="application/json";boolean disconnected;ByteArrayOutputStream sent=new ByteArrayOutputStream();
        Reply(URL url){super(url);}public void connect(){}public void disconnect(){disconnected=true;}public boolean usingProxy(){return false;}public String getCipherSuite(){return "TLS_AES_128_GCM_SHA256";}public Certificate[] getLocalCertificates(){return null;}public Certificate[] getServerCertificates(){return null;}
        public int getResponseCode(){return status;}public String getHeaderField(String name){return name.equals("Set-Cookie")?setCookie:null;}public String getContentType(){return type;}public InputStream getInputStream(){return new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));}public InputStream getErrorStream(){return getInputStream();}public OutputStream getOutputStream(){return sent;}
    }
    @Test public void destinationAndRoutesRejectArbitraryNetworkAndMethod()throws Exception{
        assertEquals("192.168.0.10",ControlClient.ipv4("192.168.000.010"));
        for(String bad:new String[]{"https://192.168.0.10","host.example","127.0.0.1","224.0.0.1","192.168.0.256","192.168.0.1:8766","192.168.0.1/path"})try{ControlClient.ipv4(bad);fail(bad);}catch(IllegalArgumentException expected){}
        for(String bad:new String[]{"https://evil.example","//evil.example/path","/auth/confirm","/api/config?x=1","/api/../auth/login","/api/pair"})try{ControlClient.validate(bad,"POST","{}");fail(bad);}catch(IllegalArgumentException expected){}
        try{ControlClient.validate("/api/config","DELETE",null);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void cookiesStayNativeRedirectsBlockedAndCsrfSent()throws Exception{
        Reply[] last=new Reply[1];SSLSocketFactory tls=CertificatePin.socketFactory("a".repeat(64));ControlClient client=new ControlClient("192.168.0.10",tls,url->last[0]=new Reply(url));
        JSONObject data=client.request("/auth/login","POST","{\"password\":\"fixture only\"}","");assertEquals(200,data.getInt("status"));assertFalse(data.toString().contains(COOKIE));assertSame(tls,last[0].getSSLSocketFactory());assertFalse(last[0].getInstanceFollowRedirects());assertEquals("https://192.168.0.10:8766/auth/login",last[0].getURL().toString());assertTrue(last[0].disconnected);
        client.request("/api/config","POST","{}","fixture-csrf");assertEquals(COOKIE,last[0].getRequestProperty("Cookie"));assertEquals("fixture-csrf",last[0].getRequestProperty("X-CasaNotify-CSRF"));client.forgetSession();client.request("/auth/state","GET",null,"");assertNull(last[0].getRequestProperty("Cookie"));
        ControlClient redirect=new ControlClient("192.168.0.10",tls,url->{Reply r=new Reply(url);r.status=302;return r;});try{redirect.request("/auth/state","GET",null,"");fail();}catch(IOException expected){}
    }
    @Test public void oversizedReplyRejectedAndUnauthorizedDropsSession()throws Exception{
        AtomicInteger calls=new AtomicInteger();Reply[] last=new Reply[1];ControlClient client=new ControlClient("192.168.0.10",CertificatePin.socketFactory("a".repeat(64)),url->{Reply r=new Reply(url);last[0]=r;if(calls.incrementAndGet()==2)r.status=401;return r;});client.request("/auth/login","POST","{}","");client.request("/api/status","GET",null,"");client.request("/auth/state","GET",null,"");assertNull(last[0].getRequestProperty("Cookie"));try{ControlClient.read(new ByteArrayInputStream(new byte[20]),10);fail();}catch(IOException expected){}
    }
    @Test public void realPinnedConnectionRejectsChangedCertificateBeforeCredentials()throws Exception{
        KeyStore keys=KeyStore.getInstance("PKCS12");try(InputStream in=new FileInputStream(System.getProperty("casanotify.testKeystore"))){keys.load(in,"test-fixture-only".toCharArray());}
        java.security.cert.X509Certificate cert=(java.security.cert.X509Certificate)keys.getCertificate("local");SSLContext serverTls=SSLContext.getInstance("TLS");serverTls.init(new KeyManager[]{new DeviceKeyManager("local",(PrivateKey)keys.getKey("local","test-fixture-only".toCharArray()),cert)},null,null);String pin=AuthCrypto.hex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));AtomicInteger reached=new AtomicInteger();
        try(LanServer server=new LanServer(0,r->{reached.incrementAndGet();assertEquals("fixture password",new JSONObject(r.body).getString("password"));return LanServer.Response.json(200,"{\"ok\":true}").header("Set-Cookie",COOKIE+"; Path=/; Secure; HttpOnly");},serverTls)){
            server.start();ControlClient.ConnectionFactory local=url->(HttpURLConnection)new URL("https://127.0.0.1:"+server.port()+url.getPath()).openConnection();ControlClient valid=new ControlClient("192.168.0.10",CertificatePin.socketFactory(pin),local);assertTrue(valid.request("/auth/login","POST","{\"password\":\"fixture password\"}","").getJSONObject("data").getBoolean("ok"));assertEquals(1,reached.get());ControlClient changed=new ControlClient("192.168.0.10",CertificatePin.socketFactory("0".repeat(64)),local);try{changed.request("/auth/login","POST","{\"password\":\"must not arrive\"}","");fail();}catch(SSLException expected){}assertEquals(1,reached.get());
        }
    }
}
