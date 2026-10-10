package br.com.casanotify.tv;
import android.content.Context;import org.json.*;import java.io.*;import java.util.*;

/** Recover an address while retaining the original key, UUID and certificate approval. */
public final class TvLink {
    public static JSONObject load(Context context,String field)throws Exception{String raw=new SecretStore(context,field).read();return raw.isEmpty()?null:new JSONObject(raw);}
    public static ControlClient client(JSONObject target)throws Exception{return new ControlClient(target.getString("host"),LocalTls.pinned(target.getString("tls_fingerprint")),target.getString("token"));}
    private static boolean probe(JSONObject saved,ControlClient client)throws Exception{
        JSONObject result=client.request("/api/hello","GET",null,"");int status=result.getInt("status");
        if(status==401||status==403)throw new Revoked();
        if(status==404)throw new Upgrade();
        if(status!=200)throw new IOException("A TV está ocupada.");
        if(!TvIdentity.same(saved,result.getJSONObject("data")))throw new IOException("A identidade da TV mudou.");return true;
    }
    private static void persistHost(Context context,String field,JSONObject saved,String host)throws Exception{
        synchronized(TvLink.class){JSONObject latest=load(context,field);if(latest==null||!latest.optString("token").equals(saved.optString("token"))||!TvIdentity.same(saved,latest))throw new IOException("O vínculo mudou.");latest.put("host",host);new SecretStore(context,field).write(latest.toString());
            String other=field.equals("control_target")?"phone_target":"control_target";JSONObject linked=load(context,other);if(linked!=null&&TvIdentity.same(saved,linked)){linked.put("host",host);new SecretStore(context,other).write(linked.toString());}
        }
    }
    public static void move(Context context,String field,String host)throws Exception{
        JSONObject saved=load(context,field);if(saved==null)throw new IOException("Vincule a TV.");JSONObject candidate=new JSONObject(saved.toString()).put("host",ControlClient.ipv4(host));probe(saved,client(candidate));persistHost(context,field,saved,candidate.getString("host"));
    }
    public static ControlClient connect(Context context,String field)throws Exception{return connect(context,field,load(context,field));}
    public static ControlClient connect(Context context,String field,JSONObject saved)throws Exception{
        if(saved==null||saved.optString("token").isEmpty())throw new IOException("Faça o primeiro vínculo com a TV.");
        try{ControlClient client=client(saved);probe(saved,client);return client;}catch(Revoked|Upgrade e){throw e;}catch(IOException e){}
        for(JSONObject candidate:TvDiscovery.scan(context)){
            if(!saved.optString("device_id").equals(candidate.optString("device_id")))continue;
            JSONObject found=new JSONObject(saved.toString());found.put("host",candidate.getString("host"));
            try{ControlClient client=client(found);probe(saved,client);
                persistHost(context,field,saved,found.getString("host"));
                return client;
            }catch(Revoked|Upgrade e){throw e;}catch(IOException ignored){}
        }
        throw new IOException("TV offline. Seu vínculo continua salvo; confira a rede e se o receptor está ativo.");
    }
    public static final class Revoked extends IOException{public Revoked(){super("O acesso foi revogado na TV. Gere um novo vínculo somente se desejar autorizar este celular novamente.");}}
    public static final class Upgrade extends IOException{public Upgrade(){super("Atualize o receptor da TV para 2.4.1 para usar a conexão automática.");}}
}
