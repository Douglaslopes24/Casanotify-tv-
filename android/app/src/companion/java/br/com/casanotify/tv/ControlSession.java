package br.com.casanotify.tv;
import android.content.Context;import android.os.Build;import org.json.*;import java.io.*;

/** Persistent device grant, volatile session. All secrets stay outside JavaScript. */
public final class ControlSession {
    private final Context context;private ControlClient client;private String csrf="";private boolean resumed;
    public ControlSession(Context context){this.context=context.getApplicationContext();}
    public synchronized void close(){if(client!=null)client.forgetSession();client=null;csrf="";resumed=false;}
    private void saveGrant(String grant)throws Exception{synchronized(TvLink.class){JSONObject saved=TvLink.load(context,"control_target");if(saved==null)return;if(grant.isEmpty())saved.remove("resume_token");else saved.put("resume_token",grant);new SecretStore(context,"control_target").write(saved.toString());}}
    private void capture(JSONObject result)throws Exception{if(result.optInt("status")!=200)return;JSONObject data=result.optJSONObject("data");if(data==null)return;if(data.has("csrf"))csrf=data.getString("csrf");if(data.has("resume_token")){String token=data.getString("resume_token");data.remove("resume_token");saveGrant(token);}}
    private boolean resume()throws Exception{
        JSONObject target=TvLink.load(context,"control_target");String token=target==null?"":target.optString("resume_token");resumed=true;if(token.isEmpty())return false;
        JSONObject result=client.request("/auth/resume","POST",new JSONObject().put("resume_token",token).toString(),"");capture(result);
        if(result.optInt("status")==401||result.optInt("status")==403){saveGrant("");return false;}
        return result.optInt("status")==200;
    }
    private void ready()throws Exception{if(client==null){client=TvLink.connect(context,"control_target");resumed=false;}if(!resumed)resume();}
    public synchronized JSONObject request(String path,String method,String body,String ignoredCsrf)throws Exception{
        ControlClient.validate(path,method,body);ready();JSONObject result;
        try{result=client.request(path,method,body,csrf);}catch(IOException e){close();if(!method.equals("GET"))throw e;ready();result=client.request(path,method,body,csrf);}
        if(result.optInt("status")==401&&!path.equals("/auth/login")&&!path.equals("/auth/register")&&resume())result=client.request(path,method,body,csrf);
        capture(result);
        if(result.optInt("status")==200){
            if(path.equals("/auth/logout")||path.equals("/auth/password")){saveGrant("");csrf="";client.forgetSession();}
            if(path.equals("/auth/login")||path.equals("/auth/register")){JSONObject data=new JSONObject(body);if(!data.optBoolean("remember",false))saveGrant("");}
            if((path.equals("/auth/session")||path.equals("/auth/login")||path.equals("/auth/register"))&&!csrf.isEmpty())linkMirroring(false);
        }
        return result;
    }
    public synchronized boolean repairMirroring()throws Exception{
        ready();JSONObject result=client.request("/auth/session","GET",null,csrf);if(result.optInt("status")!=200)return false;capture(result);return linkMirroring(true);
    }
    private boolean linkMirroring(boolean force){
        try{
            Class.forName("br.com.casanotify.tv.PhoneNotificationService");
            if(!force&&context.getSharedPreferences("phone",Context.MODE_PRIVATE).getBoolean("mirror_unlinked",false))return false;
            JSONObject target=TvLink.load(context,"control_target"),phone=TvLink.load(context,"phone_target");if(target==null)return false;
            if(!force&&phone!=null&&TvIdentity.same(target,phone))return true;
            JSONObject reply=client.request("/api/phones/link","POST",new JSONObject().put("name",Build.MODEL).toString(),csrf);if(reply.optInt("status")!=200)return false;
            String token=reply.getJSONObject("data").getString("token");if(!token.matches("[A-Za-z0-9_-]{43}"))return false;
            JSONObject limited=new JSONObject(target.toString());limited.remove("resume_token");limited.put("token",token);
            synchronized(TvLink.class){new SecretStore(context,"phone_target").write(limited.toString());new SecretStore(context,"mirror_outbox").delete();}
            context.getSharedPreferences("phone",Context.MODE_PRIVATE).edit().putBoolean("enabled",false).putBoolean("mirror_unlinked",false).apply();return true;
        }catch(Exception ignored){return false;}
    }
}
