package br.com.casanotify.tv;

import org.json.*;

/** Small durable outbox. Production Store encrypts these records with Android Keystore. */
public final class MirrorOutbox {
    public static final int LIMIT=30;
    public static final long MAX_AGE=10*60*1000;
    private final AccountManager.Store store;
    private final AccountManager.Clock clock;
    public MirrorOutbox(AccountManager.Store store){this(store,System::currentTimeMillis);}
    public MirrorOutbox(AccountManager.Store store,AccountManager.Clock clock){this.store=store;this.clock=clock;}
    public static String binding(JSONObject target)throws Exception{return AuthCrypto.digest(target.getString("device_id")+"\n"+target.getString("tls_fingerprint")+"\n"+target.getString("token"));}
    private JSONArray read()throws Exception{
        String raw=store.read();JSONArray old=raw==null||raw.isEmpty()?new JSONArray():new JSONArray(raw),next=new JSONArray();long now=clock.now();
        for(int i=0;i<old.length();i++){JSONObject item=old.getJSONObject(i);long age=now-item.getLong("at");if(age>=0&&age<MAX_AGE)next.put(item);}
        if(old.length()!=next.length())save(next);return next;
    }
    private void save(JSONArray items)throws Exception{if(items.length()==0)store.delete();else store.write(items.toString());}
    public synchronized void enqueue(String binding,String pkg,JSONObject payload)throws Exception{
        JSONArray items=read();String id=payload.getString("id");
        for(int i=items.length()-1;i>=0;i--){JSONObject item=items.getJSONObject(i);if(!binding.equals(item.getString("binding"))||id.equals(item.getJSONObject("payload").getString("id")))items.remove(i);}
        while(items.length()>=LIMIT)items.remove(0);
        items.put(new JSONObject().put("binding",binding).put("package",pkg).put("at",clock.now()).put("payload",new JSONObject(payload.toString())));save(items);
    }
    public synchronized JSONObject first(String binding)throws Exception{
        JSONArray items=read();boolean changed=false;for(int i=items.length()-1;i>=0;i--)if(!binding.equals(items.getJSONObject(i).getString("binding"))){items.remove(i);changed=true;}
        if(changed)save(items);return items.length()==0?null:items.getJSONObject(0);
    }
    public synchronized void acknowledge(String binding,String id)throws Exception{
        JSONArray items=read();for(int i=items.length()-1;i>=0;i--){JSONObject item=items.getJSONObject(i);if(binding.equals(item.getString("binding"))&&id.equals(item.getJSONObject("payload").getString("id")))items.remove(i);}save(items);
    }
    public synchronized int size()throws Exception{return read().length();}
    public synchronized void clear()throws Exception{store.delete();}
}
