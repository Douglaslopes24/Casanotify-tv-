package br.com.casanotify.tv;
import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import javax.net.ssl.HttpsURLConnection;

public final class PhoneApi {
    public static String ip(String value){String[] p=value.trim().split("\\.",-1);if(p.length!=4)throw new IllegalArgumentException("Informe o IPv4 mostrado na TV, sem http://.");StringBuilder b=new StringBuilder();for(String v:p){if(!v.matches("[0-9]{1,3}")||Integer.parseInt(v)>255)throw new IllegalArgumentException("IP inválido.");if(b.length()>0)b.append('.');b.append(Integer.parseInt(v));}return b.toString();}
    public static JSONObject discover(String host)throws Exception{JSONObject d=request(new URL("http://"+ip(host)+":"+Prefs.PORT+"/api/info"),null,null,null);if(!d.optString("app").equals("CasaNotify TV")||d.optInt("api_version")!=3||!d.optString("tls_fingerprint").matches("[0-9a-f]{64}"))throw new IOException("Atualize a TV para CasaNotify TV 2.4.0.");d.put("host",ip(host));return d;}
    public static JSONObject call(JSONObject target,String path,JSONObject data)throws Exception{
        if(path.equals("/api/pair"))return request(new URL("https://"+ip(target.getString("host"))+":"+Prefs.SECURE_PORT+path),target.getString("tls_fingerprint"),null,data);
        ControlClient client=new ControlClient(target.getString("host"),LocalTls.pinned(target.getString("tls_fingerprint")),target.getString("token"));JSONObject result=client.request(path,data==null?"GET":"POST",data==null?null:data.toString(),"");if(result.getInt("status")>=300)throw new IOException("A TV recusou o aviso. Atualize os aplicativos e confira o vínculo.");return result.getJSONObject("data");
    }
    private static JSONObject request(URL url,String fingerprint,String token,JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setUseCaches(false);c.setInstanceFollowRedirects(false);
        if(c instanceof HttpsURLConnection){HttpsURLConnection tls=(HttpsURLConnection)c;tls.setSSLSocketFactory(LocalTls.pinned(fingerprint));tls.setHostnameVerifier((hostname,session)->hostname.equals(url.getHost()));}
        try{if(body!=null){c.setRequestMethod("POST");c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
            int status=c.getResponseCode();if(status<200||status>=300)throw new IOException(status==401?"Vínculo inválido ou código vencido.":"A TV recusou a solicitação ("+status+").");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>131072)throw new IOException("Resposta muito grande.");out.write(b,0,n);}return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}
        }finally{c.disconnect();}
    }
}
