package br.com.casanotify.tv;

import android.app.Notification;
import android.content.*;
import android.net.*;
import android.os.Bundle;
import android.service.notification.*;
import org.json.*;
import java.util.*;

/** Android-managed, explicit opt-in listener. Independent of the control screen. */
public final class PhoneNotificationService extends NotificationListenerService {
    private final Map<String,String> recent=new LinkedHashMap<>();
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback network;
    public void onCreate(){super.onCreate();connectivity=getSystemService(ConnectivityManager.class);network=new ConnectivityManager.NetworkCallback(){public void onAvailable(Network network){MirrorSender.kick(PhoneNotificationService.this);}};try{if(connectivity!=null)connectivity.registerDefaultNetworkCallback(network);}catch(RuntimeException ignored){}}
    public void onListenerConnected(){super.onListenerConnected();MirrorSender.listenerConnected=true;MirrorSender.kick(this);}
    public void onListenerDisconnected(){MirrorSender.listenerConnected=false;super.onListenerDisconnected();MirrorSender.reconnect(this);}
    public void onNotificationPosted(StatusBarNotification sbn){
        SharedPreferences settings=MirrorSender.settings(this);String pkg=sbn.getPackageName();
        if(!MirrorSender.enabled(this)||pkg.equals(getPackageName())||!settings.getStringSet("apps",Collections.emptySet()).contains(pkg))return;
        Notification n=sbn.getNotification();if(n==null||n.extras==null||n.visibility==Notification.VISIBILITY_SECRET||(n.flags&(Notification.FLAG_ONGOING_EVENT|Notification.FLAG_GROUP_SUMMARY))!=0)return;
        Bundle e=n.extras;String title=String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE,"")),body=String.valueOf(e.getCharSequence(Notification.EXTRA_BIG_TEXT,e.getCharSequence(Notification.EXTRA_TEXT,"")));
        title=MirrorFilter.limit(title,400);body=MirrorFilter.limit(body,2000);if(MirrorFilter.sensitive(title,body))return;
        try{
            String digest=AuthCrypto.digest(title+"\n"+body),key=sbn.getKey();if(digest.equals(recent.get(key)))return;
            String label=pkg;try{label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
            String message=settings.getBoolean("content",false)?MirrorFilter.limit(title+(body.isEmpty()?"":" — "+body),1200):MirrorSender.HIDDEN_CONTENT;
            JSONObject payload=new JSONObject().put("id",AuthCrypto.digest(key+":"+sbn.getPostTime()+":"+digest).substring(0,32)).put("title",MirrorFilter.limit(label,160)).put("message",message);
            MirrorSender.enqueue(this,pkg,payload);recent.put(key,digest);while(recent.size()>100)recent.remove(recent.keySet().iterator().next());
        }catch(Exception ignored){}
    }
    public void onNotificationRemoved(StatusBarNotification sbn){recent.remove(sbn.getKey());}
    public void onDestroy(){MirrorSender.listenerConnected=false;if(connectivity!=null&&network!=null)try{connectivity.unregisterNetworkCallback(network);}catch(RuntimeException ignored){}recent.clear();super.onDestroy();}
}
