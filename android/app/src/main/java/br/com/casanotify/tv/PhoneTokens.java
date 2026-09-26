package br.com.casanotify.tv;
import android.content.Context;import org.json.*;import java.util.*;

/** Phone credentials can only send notifications; only token hashes are stored on the TV. */
public final class PhoneTokens {
    private final SecretStore store;private final Map<String,long[]> rate=new HashMap<>();
    public PhoneTokens(Context c){store=new SecretStore(c,"phones");}
    private JSONArray list()throws Exception{String s=store.read();return s.isEmpty()?new JSONArray():new JSONArray(s);}
    public synchronized String issue(String name)throws Exception{
        JSONArray a=list();if(a.length()>=10)throw new IllegalArgumentException("Remova um celular antes de vincular outro. Limite: 10.");
        String token=AuthCrypto.token();a.put(new JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("hash",AuthCrypto.digest(token)).put("created",System.currentTimeMillis()));store.write(a.toString());return token;
    }
    public synchronized boolean authorized(String token)throws Exception{
        if(token==null||token.length()>100)return false;String hash=AuthCrypto.digest(token);JSONArray a=list();
        for(int i=0;i<a.length();i++){JSONObject item=a.getJSONObject(i);if(AuthCrypto.same(hash,item.getString("hash"))){long now=System.currentTimeMillis();long[] r=rate.get(hash);if(r==null||now-r[0]>60000){r=new long[]{now,0};rate.put(hash,r);}if(++r[1]>30)throw new IllegalArgumentException("Limite de 30 avisos por minuto neste celular.");return true;}}return false;
    }
    public synchronized JSONArray publicList()throws Exception{JSONArray a=list();for(int i=0;i<a.length();i++)a.getJSONObject(i).remove("hash");return a;}
    public synchronized void revoke(String id)throws Exception{JSONArray old=list(),next=new JSONArray();for(int i=0;i<old.length();i++)if(!id.isEmpty()&&!old.getJSONObject(i).getString("id").equals(id))next.put(old.getJSONObject(i));store.write(next.toString());rate.clear();}
}
