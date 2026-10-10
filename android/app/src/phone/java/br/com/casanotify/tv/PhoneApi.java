package br.com.casanotify.tv;
import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import javax.net.ssl.HttpsURLConnection;

public final class PhoneApi {
    public static String ip(String value){return ControlClient.ipv4(value);}
    public static JSONObject discover(String host)throws Exception{JSONObject d=request(new URL("http://"+ip(host)+":"+Prefs.PORT+"/api/info"),null,null,null);if(!d.optString("app").equals("CasaNotify TV")||d.optInt("api_version")!=3||!d.optString("tls_fingerprint").matches("[0-9a-f]{64}"))throw new IOException("Atualize a TV para CasaNotify TV 2.4.1.");d.put("host",ip(host));return d;}
    public static JSONObject call(JSONObject target,String path,JSONObject data)throws Exception{
        if(path.equals("/api/pair"))return request(new URL("https://"+ip(target.getString("host"))+":"+Prefs.SECURE_PORT+path),target.getString("tls_fingerprint"),null,data);
        ControlClient client=new ControlClient(target.getString("host"),LocalTls.pinned(target.getString("tls_fingerprint")),target.getString("token"));JSONObject result=client.request(path,data==null?"GET":"POST",data==null?null:data.toString(),"");if(result.getInt("status")>=300)throw new IOException("A TV recusou o aviso. Atualize os aplicativos e confira o vínculo.");return result.getJSONObject("data");
    }
    private static JSONObject request(URL url,String fingerprint,String token,JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setUseCaches(false);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept-Encoding","identity");
        if(c instanceof HttpsURLConnection){HttpsURLConnection tls=(HttpsURLConnection)c;tls.setSSLSocketFactory(LocalTls.pinned(fingerprint));tls.setHostnameVerifier((hostname,session)->hostname.equals(url.getHost()));}
        try(NetworkDeadline deadline=new NetworkDeadline(15000,c::disconnect)){if(body!=null){c.setRequestMethod("POST");c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
            int status=c.getResponseCode();if(status<200||status>=300)throw new IOException(status==401?"Vínculo inválido ou código vencido.":"A TV recusou a solicitação ("+status+").");ControlClient.checkResponse(c,131072);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>131072)throw new IOException("Resposta muito grande.");out.write(b,0,n);}return BoundedJson.object(out.toByteArray(),131072);}
        }finally{c.disconnect();}
    }
}
