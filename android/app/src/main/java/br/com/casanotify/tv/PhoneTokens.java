package br.com.casanotify.tv;
import android.content.Context;import org.json.*;import java.util.*;

/** Paired clients: hashed keys stay encrypted; roles are approved physically on the TV. */
public final class PhoneTokens {
    private final SecretStore store;private final Map<String,long[]> rate=new HashMap<>();
    public PhoneTokens(Context c){store=new SecretStore(c,"phones");}
    private JSONArray list()throws Exception{String s=store.read();return s.isEmpty()?new JSONArray():new JSONArray(s);}
    public synchronized String issue(String name)throws Exception{return issue(name,"phone");}
    public synchronized String issue(String name,String role)throws Exception{
        if(!role.equals("phone")&&!role.equals("control"))throw new IllegalArgumentException("Tipo de vínculo inválido.");
        JSONArray a=list();if(a.length()>=20)throw new IllegalArgumentException("Remova um celular antes de vincular outro. Limite: 20.");
        String token=AuthCrypto.token();a.put(new JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("role",role).put("hash",AuthCrypto.digest(token)).put("created",System.currentTimeMillis()));store.write(a.toString());return token;
    }
    public synchronized RequestAuth.Key key(String id)throws Exception{JSONArray a=list();for(int i=0;i<a.length();i++){JSONObject item=a.getJSONObject(i);if(AuthCrypto.same(id,AuthCrypto.digest(item.getString("hash"))))return new RequestAuth.Key(item.getString("hash"),item.optString("role","phone"));}return null;}
    public synchronized boolean allowNotice(String id){long now=System.currentTimeMillis();long[] r=rate.get(id);if(r==null||now-r[0]>60000){r=new long[]{now,0};rate.put(id,r);}return ++r[1]<=30;}
    public synchronized JSONArray publicList()throws Exception{JSONArray a=list();for(int i=0;i<a.length();i++)a.getJSONObject(i).remove("hash");return a;}
    public synchronized void revokeControls()throws Exception{JSONArray old=list(),next=new JSONArray();for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).optString("role","phone").equals("control"))next.put(old.getJSONObject(i));store.write(next.toString());rate.clear();}
    public synchronized void revoke(String id)throws Exception{JSONArray old=list(),next=new JSONArray();for(int i=0;i<old.length();i++)if(!id.isEmpty()&&!old.getJSONObject(i).getString("id").equals(id))next.put(old.getJSONObject(i));store.write(next.toString());rate.clear();}
}
