package br.com.casanotify.tv;

import org.json.*;

/** Encrypted in production; stores only delivery identifiers, never notification contents. */
public final class MirrorReceipts {
    private static final int LIMIT=512;
    private final AccountManager.Store store;private final AccountManager.Clock clock;
    public MirrorReceipts(AccountManager.Store store){this(store,System::currentTimeMillis);}
    public MirrorReceipts(AccountManager.Store store,AccountManager.Clock clock){this.store=store;this.clock=clock;}
    private JSONArray read()throws Exception{String raw=store.read();JSONArray items=raw.isEmpty()?new JSONArray():new JSONArray(raw),valid=new JSONArray();long now=clock.now();for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);long age=now-item.getLong("at");if(age>=0&&age<MirrorOutbox.MAX_AGE)valid.put(item);}if(valid.length()!=items.length())save(valid);return valid;}
    private void save(JSONArray items)throws Exception{if(items.length()==0)store.delete();else store.write(items.toString());}
    public synchronized boolean contains(String binding,String id)throws Exception{JSONArray items=read();for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);if(binding.equals(item.getString("binding"))&&id.equals(item.getString("id")))return true;}return false;}
    public synchronized void delivered(String binding,String id)throws Exception{JSONArray items=read();for(int i=items.length()-1;i>=0;i--){JSONObject item=items.getJSONObject(i);if(!binding.equals(item.getString("binding"))||id.equals(item.getString("id")))items.remove(i);}while(items.length()>=LIMIT)items.remove(0);items.put(new JSONObject().put("binding",binding).put("id",id).put("at",clock.now()));save(items);}
    public static boolean recover(long posted,long enabledAt,long now){return posted>0&&posted>=enabledAt&&posted<=now&&now-posted<MirrorOutbox.MAX_AGE;}
}
