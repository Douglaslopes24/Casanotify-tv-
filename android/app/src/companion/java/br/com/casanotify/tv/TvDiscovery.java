package br.com.casanotify.tv;

import android.content.Context;
import android.net.nsd.*;
import android.net.wifi.WifiManager;
import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.*;

/** Bounded DNS-SD discovery. Advertisements are hints, never trusted certificate identities. */
public final class TvDiscovery {
    private final NsdManager manager;
    private final Deque<NsdServiceInfo> pending=new ArrayDeque<>();
    private final Set<String> names=new HashSet<>();
    private final Map<String,JSONObject> results=new LinkedHashMap<>();
    private boolean closed,resolving;
    private TvDiscovery(Context context){manager=(NsdManager)context.getSystemService(Context.NSD_SERVICE);}
    public static List<JSONObject> scan(Context context)throws InterruptedException{
        TvDiscovery scan=new TvDiscovery(context);WifiManager.MulticastLock lock=null;
        try{
            WifiManager wifi=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wifi!=null){lock=wifi.createMulticastLock("CasaNotify-discovery");lock.setReferenceCounted(false);lock.acquire();}
            if(scan.manager!=null){scan.manager.discoverServices("_casanotify._tcp.",NsdManager.PROTOCOL_DNS_SD,scan.listener);new CountDownLatch(1).await(6,TimeUnit.SECONDS);}
        }catch(RuntimeException ignored){}finally{scan.close();if(lock!=null&&lock.isHeld())lock.release();}
        synchronized(scan){return new ArrayList<>(scan.results.values());}
    }
    private final NsdManager.DiscoveryListener listener=new NsdManager.DiscoveryListener(){
        public void onDiscoveryStarted(String type){}
        public void onServiceFound(NsdServiceInfo info){synchronized(TvDiscovery.this){if(closed||names.size()>=16||!info.getServiceType().startsWith("_casanotify._tcp")||!names.add(info.getServiceName()))return;pending.add(info);next();}}
        public void onServiceLost(NsdServiceInfo info){}
        public void onDiscoveryStopped(String type){}
        public void onStartDiscoveryFailed(String type,int code){close();}
        public void onStopDiscoveryFailed(String type,int code){}
    };
    private synchronized void next(){
        if(closed||resolving||pending.isEmpty())return;resolving=true;
        try{manager.resolveService(pending.removeFirst(),new NsdManager.ResolveListener(){
            public void onResolveFailed(NsdServiceInfo info,int code){synchronized(TvDiscovery.this){resolving=false;next();}}
            public void onServiceResolved(NsdServiceInfo info){synchronized(TvDiscovery.this){
                try{if(!closed&&info.getPort()==8765&&info.getHost() instanceof Inet4Address&&results.size()<8){String host=ControlClient.ipv4(info.getHost().getHostAddress());Map<String,byte[]> attrs=info.getAttributes();String id=txt(attrs,"id"),name=txt(attrs,"name");results.put(host,new JSONObject().put("host",host).put("device_id",id).put("device_name",name.isEmpty()?info.getServiceName():name));}}catch(Exception ignored){}
                resolving=false;next();
            }}
        });}catch(RuntimeException e){resolving=false;next();}
    }
    private static String txt(Map<String,byte[]> attributes,String key){byte[] value=attributes.get(key);return value==null||value.length>200?"":new String(value,StandardCharsets.UTF_8);}
    private synchronized void close(){if(closed)return;closed=true;pending.clear();try{if(manager!=null)manager.stopServiceDiscovery(listener);}catch(RuntimeException ignored){}}
}
