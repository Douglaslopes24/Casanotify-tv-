package br.com.casanotify.tv;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.*;

/** Real loopback sockets only; malicious payloads never reach a user's device. */
public class NetworkSecurityTest {
    private static String request(int port,String method,String body,String extra){return method+" /api/config HTTP/1.1\r\nHost: 127.0.0.1:"+port+"\r\nContent-Type: application/json\r\nContent-Length: "+body.getBytes(StandardCharsets.UTF_8).length+"\r\n"+extra+"\r\n"+body;}
    private static String exchange(int port,String request)throws Exception{try(Socket socket=new Socket("127.0.0.1",port)){socket.setSoTimeout(2000);socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));return new String(socket.getInputStream().readAllBytes(),StandardCharsets.UTF_8);}}
    private static void closed(Socket socket)throws Exception{socket.setSoTimeout(2000);try{int n=0;while(socket.getInputStream().read()!=-1)assertTrue(++n<1024);}catch(SocketException expected){}}
    private static SSLContext serverTls()throws Exception{
        KeyStore keys=KeyStore.getInstance("PKCS12");try(InputStream in=new FileInputStream(System.getProperty("casanotify.testKeystore"))){keys.load(in,"test-fixture-only".toCharArray());}
        SSLContext tls=SSLContext.getInstance("TLS");tls.init(new KeyManager[]{new DeviceKeyManager("local",(PrivateKey)keys.getKey("local","test-fixture-only".toCharArray()),(X509Certificate)keys.getCertificate("local"))},null,null);return tls;
    }
    @Test public void strictJsonRejectsAmbiguousNumbersKeysSyntaxAndUtf8()throws Exception{
        for(String raw:new String[]{"{\"a\":1,\"a\":2}","{\"a\":1,\"\\u0061\":2}","{a:1}","{\"x\":NaN}","{\"x\":Infinity}","{\"x\":1e99999}","{\"x\":"+"9".repeat(65)+"}","{\"x\":01}","{\"x\":1,}","{}{}","[]","{\"x\":\"\\u００００\"}","{\""+"x".repeat(129)+"\":0}"}){
            try{BoundedJson.object(raw,131072);fail(raw);}catch(IllegalArgumentException expected){}
        }
        try{BoundedJson.object(new byte[]{123,34,120,34,58,34,(byte)0xc0,(byte)0xaf,34,125},100);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void jsonDepthAndWorkAreBoundedBeforePlatformParser()throws Exception{
        for(String raw:new String[]{"{\"x\":"+"[".repeat(10000)+"0"+"]".repeat(10000)+"}","{\"x\":["+"0,".repeat(5000)+"0]}"})try{BoundedJson.object(raw,131072);fail();}catch(IllegalArgumentException expected){}
        assertEquals("Câmera da sala",BoundedJson.object("{\"nome\":\"Câmera da sala\",\"config\":{\"x\":[true,null,-1.25e2]}}",1000).getString("nome"));
        assertEquals(1_000_000,BoundedJson.object("{\"data\":\""+"A".repeat(1_000_000)+"\"}",1600000).getString("data").length());
    }
    @Test public void malformedRequestsNeverReachHandlerAndValidTrafficRecovers()throws Exception{
        AtomicInteger handled=new AtomicInteger();try(LanServer server=new LanServer(0,r->{handled.incrementAndGet();return LanServer.Response.json(200,"{}");})){
            server.start();String[] requests={request(server.port(),"POST","{\"x\":1,\"x\":2}",""),request(server.port(),"POST","{\"x\":"+"[".repeat(1000)+"0"+"]".repeat(1000)+"}",""),request(server.port(),"GET","{}",""),request(server.port(),"DELETE","{}",""),request(server.port(),"POST","{}","Content-Encoding: gzip\r\n")};
            for(String raw:requests)assertTrue(exchange(server.port(),raw).startsWith("HTTP/1.1 4"));assertEquals(0,handled.get());assertTrue(exchange(server.port(),request(server.port(),"POST","{}","")).startsWith("HTTP/1.1 200"));assertEquals(1,handled.get());
        }
    }
    @Test public void slowHeadersAndBodiesHaveAnAbsoluteDeadline()throws Exception{
        for(boolean body:new boolean[]{false,true})try(LanServer server=new LanServer(0,r->{assertEquals("GET",r.method);return LanServer.Response.json(200,"{}");},null,250,250)){
            server.start();try(Socket socket=new Socket("127.0.0.1",server.port())){
                OutputStream out=socket.getOutputStream();if(body){String head=request(server.port(),"POST","{\"x\":12345}","");out.write(head.substring(0,head.indexOf("\r\n\r\n")+4).getBytes(StandardCharsets.UTF_8));}
                long start=System.nanoTime();try{for(int i=0;i<20;i++){out.write(body?' ': 'G');out.flush();Thread.sleep(40);}}catch(SocketException expected){}
                closed(socket);assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<1500);
            }
            assertTrue(exchange(server.port(),request(server.port(),"GET","","")).startsWith("HTTP/1.1 200"));
        }
    }
    @Test public void stalledTlsHandshakeIsClosedBeforeReadTimeout()throws Exception{
        try(LanServer server=new LanServer(0,r->LanServer.Response.json(200,"{}"),serverTls(),250,250)){
            server.start();long start=System.nanoTime();try(Socket socket=new Socket("127.0.0.1",server.port())){closed(socket);}assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<1500);
        }
    }
    @Test public void onePeerCannotOccupyEveryWorker()throws Exception{
        try(LanServer server=new LanServer(0,r->LanServer.Response.json(200,"{}"),null,3000,250)){
            server.start();try(Socket first=new Socket("127.0.0.1",server.port());Socket second=new Socket("127.0.0.1",server.port());Socket third=new Socket("127.0.0.1",server.port())){
                closed(third);assertFalse(first.isClosed());assertFalse(second.isClosed());
            }
        }
    }
    @Test public void peersThatDoNotReadCannotHoldResponseWorkers()throws Exception{
        CountDownLatch responses=new CountDownLatch(2);byte[] large=new byte[8*1024*1024];
        try(LanServer server=new LanServer(0,r->{if(r.path.equals("/large")){responses.countDown();return new LanServer.Response(200,"image/png",large);}return LanServer.Response.json(200,"{}");},null,1000,250)){
            server.start();Socket first=new Socket(),second=new Socket();try{
                for(Socket socket:new Socket[]{first,second}){socket.setReceiveBufferSize(1024);socket.connect(new InetSocketAddress("127.0.0.1",server.port()));socket.getOutputStream().write(request(server.port(),"GET","","").replace("/api/config","/large").getBytes(StandardCharsets.UTF_8));}
                assertTrue(responses.await(1,TimeUnit.SECONDS));Thread.sleep(600);
                assertTrue(exchange(server.port(),request(server.port(),"GET","","")).startsWith("HTTP/1.1 200"));
            }finally{first.close();second.close();}
        }
    }
    @Test public void realTlsClientCannotBeHeldByTrickledResponseHeaders()throws Exception{
        KeyStore keys=KeyStore.getInstance("PKCS12");try(InputStream in=new FileInputStream(System.getProperty("casanotify.testKeystore"))){keys.load(in,"test-fixture-only".toCharArray());}
        String pin=AuthCrypto.hex(MessageDigest.getInstance("SHA-256").digest(keys.getCertificate("local").getEncoded()));
        ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});
        try(SSLServerSocket server=(SSLServerSocket)serverTls().getServerSocketFactory().createServerSocket(0,5,InetAddress.getByName("127.0.0.1"))){
            Future<?> attack=worker.submit(()->{try(SSLSocket peer=(SSLSocket)server.accept()){peer.setSoTimeout(2000);peer.startHandshake();OutputStream out=peer.getOutputStream();out.write("HTTP/1.1 200 OK\r\nX-Slow: ".getBytes(StandardCharsets.US_ASCII));out.flush();for(int n=0;n<50;n++){out.write('a');out.flush();Thread.sleep(40);}}catch(Exception expected){}});
            ControlClient client=new ControlClient("192.168.0.10",CertificatePin.socketFactory(pin),"fixture token",url->(HttpURLConnection)new URL("https://127.0.0.1:"+server.getLocalPort()+url.getPath()).openConnection(),300);
            long start=System.nanoTime();try{client.request("/api/status","GET",null,"");fail();}catch(IOException expected){}assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<1500);attack.get(3,TimeUnit.SECONDS);
        }finally{worker.shutdownNow();}
    }

}
