package br.com.casanotify.tv;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.*;
import org.json.*;

/** A single selected receiver; session cookies never enter WebView or persistent storage. */
public final class ControlClient {
    private static final int LIMIT=2*1024*1024;
    private static final Set<String> READ=new HashSet<>(Arrays.asList("/auth/state","/auth/session","/api/config","/api/status","/api/history","/api/cameras","/api/phones","/media/logo","/media/background"));
    private static final Set<String> WRITE=new HashSet<>(Arrays.asList("/auth/register","/auth/login","/auth/logout","/auth/profile","/auth/password","/api/config","/api/notify","/api/clear","/api/media","/api/media/remove","/api/cameras/save","/api/cameras/delete","/api/cameras/test","/api/phones/revoke"));
    interface ConnectionFactory { HttpURLConnection open(URL url)throws Exception; }
    private final String host;private final SSLSocketFactory tls;private final ConnectionFactory factory;
    private String cookie="";
    public ControlClient(String host,SSLSocketFactory tls){this(host,tls,url->(HttpURLConnection)url.openConnection());}
    ControlClient(String host,SSLSocketFactory tls,ConnectionFactory factory){this.host=ipv4(host);this.tls=Objects.requireNonNull(tls);this.factory=factory;}
    public static String ipv4(String text){
        if(text==null)throw new IllegalArgumentException("Informe o IP mostrado na TV.");String[] parts=text.trim().split("\\.",-1);if(parts.length!=4)throw new IllegalArgumentException("Informe somente o IPv4 da TV, por exemplo 192.168.0.10.");StringBuilder result=new StringBuilder();
        for(String p:parts){if(!p.matches("[0-9]{1,3}")||Integer.parseInt(p)>255)throw new IllegalArgumentException("IP inválido.");if(result.length()>0)result.append('.');result.append(Integer.parseInt(p));}
        int first=Integer.parseInt(parts[0]);if(first==0||first==127||first>=224)throw new IllegalArgumentException("Use o IP da TV na rede local ou VPN.");return result.toString();
    }
    public static void validate(String path,String method,String body){
        if(!("GET".equals(method)&&READ.contains(path)||"POST".equals(method)&&WRITE.contains(path)))throw new IllegalArgumentException("Operação não permitida.");
        if(body!=null&&body.getBytes(StandardCharsets.UTF_8).length>1550000)throw new IllegalArgumentException("Solicitação muito grande.");
        if("GET".equals(method)&&body!=null)throw new IllegalArgumentException("Leitura não aceita corpo.");
    }
    static byte[] read(InputStream in,int limit)throws IOException{
        if(in==null)return new byte[0];try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=stream.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Resposta muito grande.");out.write(b,0,n);}return out.toByteArray();}
    }
    public static JSONObject discover(String input)throws Exception{
        String host=ipv4(input);HttpURLConnection c=(HttpURLConnection)new URL("http://"+host+":8765/api/info").openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
        try{if(c.getResponseCode()!=200)throw new IOException("Ative o receptor na TV e confira o IP.");JSONObject info=new JSONObject(new String(read(c.getInputStream(),65536),StandardCharsets.UTF_8));
            if(!"CasaNotify TV".equals(info.optString("app"))||info.optInt("api_version")!=2||info.optInt("control_protocol")!=1||!info.optString("tls_fingerprint").matches("[0-9a-f]{64}"))throw new IOException("Atualize o aplicativo da TV para a versão 2.1.0 ou posterior.");
            return new JSONObject().put("host",host).put("device_name",info.optString("device_name","Minha TV")).put("device_id",info.getString("device_id")).put("tls_fingerprint",info.getString("tls_fingerprint"));
        }finally{c.disconnect();}
    }
    public synchronized void forgetSession(){cookie="";}
    public synchronized JSONObject request(String path,String method,String body,String csrf)throws Exception{
        validate(path,method,body);if(body!=null)new JSONObject(body);
        URL url=new URL("https://"+host+":8766"+path);HttpsURLConnection c=(HttpsURLConnection)factory.open(url);
        c.setSSLSocketFactory(tls);c.setHostnameVerifier((name,session)->name.equals(host));c.setConnectTimeout(5000);c.setReadTimeout(12000);c.setUseCaches(false);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);c.setRequestProperty("Accept",path.startsWith("/media/")?"image/png":"application/json");
        if(!cookie.isEmpty())c.setRequestProperty("Cookie",cookie);
        if(csrf!=null&&!csrf.isEmpty()){if(!csrf.matches("[A-Za-z0-9_-]{1,100}"))throw new IllegalArgumentException("Sessão inválida.");c.setRequestProperty("X-CasaNotify-CSRF",csrf);}
        try{
            if(body!=null){c.setRequestProperty("Content-Type","application/json");byte[] bytes=body.getBytes(StandardCharsets.UTF_8);c.setDoOutput(true);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
            int status=c.getResponseCode();if(status>=300&&status<400)throw new IOException("A TV tentou redirecionar a conexão. Confira o receptor.");
            String set=c.getHeaderField("Set-Cookie");if(set!=null){String first=set.split(";",2)[0];if(first.matches("__Host-casanotify=[A-Za-z0-9_-]{43}"))cookie=first;else if(first.equals("__Host-casanotify="))cookie="";}
            if(status==401)cookie="";
            byte[] bytes=read(status>=400?c.getErrorStream():c.getInputStream(),LIMIT);
            JSONObject result=new JSONObject().put("status",status);
            if(path.startsWith("/media/")&&status==200){if(!"image/png".equals(c.getContentType()))throw new IOException("Formato de imagem inesperado.");return result.put("image",Base64.getEncoder().encodeToString(bytes));}
            return result.put("data",new JSONObject(new String(bytes,StandardCharsets.UTF_8)));
        }finally{c.disconnect();}
    }
}
