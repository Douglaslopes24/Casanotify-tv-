package br.com.casanotify.tv;
import android.content.Context;import org.json.*;import java.util.*;

public final class CameraStore {
    private final SecretStore store;
    public CameraStore(Context c){store=new SecretStore(c,"cameras");}
    private JSONArray all()throws Exception{String s=store.read();return s.isEmpty()?new JSONArray():new JSONArray(s);}
    public synchronized JSONArray list()throws Exception{JSONArray out=all();for(int i=0;i<out.length();i++)out.getJSONObject(i).remove("url");return out;}
    public synchronized String url(String id)throws Exception{JSONArray a=all();for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("id").equals(id))return a.getJSONObject(i).getString("url");throw new IllegalArgumentException("Câmera não encontrada.");}
    public synchronized void save(String id,String name,String url)throws Exception{Notice.validateVideoUrl(url);if(url.isEmpty()||name.trim().isEmpty()||name.length()>60)throw new IllegalArgumentException("Informe o nome e o endereço RTSP.");JSONArray a=all();int index=-1;for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("id").equals(id))index=i;if(index<0&&a.length()>=12)throw new IllegalArgumentException("Limite de 12 câmeras.");if(!id.isEmpty()&&index<0)throw new IllegalArgumentException("Câmera não encontrada.");JSONObject item=new JSONObject().put("id",index<0?UUID.randomUUID().toString():id).put("name",name.trim()).put("url",url);if(index<0)a.put(item);else a.put(index,item);store.write(a.toString());}
    public synchronized void remove(String id)throws Exception{JSONArray a=all(),out=new JSONArray();for(int i=0;i<a.length();i++)if(!a.getJSONObject(i).getString("id").equals(id))out.put(a.getJSONObject(i));store.write(out.toString());}
}
