package br.com.casanotify.tv;
import android.app.Notification;import android.content.*;import android.os.Bundle;import android.service.notification.*;import org.json.*;import java.util.*;import java.util.concurrent.*;

/** Opt-in mirroring of user-selected apps. No SMS, contacts, accessibility or persistent message log. */
public final class PhoneNotificationService extends NotificationListenerService {
    private final ExecutorService sender=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Map<String,String> recent=new LinkedHashMap<>();
    public void onNotificationPosted(StatusBarNotification sbn){
        SharedPreferences settings=getSharedPreferences("phone",MODE_PRIVATE);String pkg=sbn.getPackageName();
        if(!Ui.accepted(this)||!settings.getBoolean("enabled",false)||pkg.equals(getPackageName())||!settings.getStringSet("apps",Collections.emptySet()).contains(pkg))return;
        Notification n=sbn.getNotification();if(n.visibility==Notification.VISIBILITY_SECRET||(n.flags&(Notification.FLAG_ONGOING_EVENT|Notification.FLAG_GROUP_SUMMARY))!=0)return;
        Bundle e=n.extras;String title=String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE,"")),body=String.valueOf(e.getCharSequence(Notification.EXTRA_BIG_TEXT,e.getCharSequence(Notification.EXTRA_TEXT,"")));
        title=MirrorFilter.limit(title,400);body=MirrorFilter.limit(body,2000);if(MirrorFilter.sensitive(title,body))return;String label=pkg;try{label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
        String digest;try{digest=AuthCrypto.digest(title+"\n"+body);}catch(Exception ex){return;}String key=sbn.getKey();if(digest.equals(recent.get(key)))return;recent.put(key,digest);while(recent.size()>100)recent.remove(recent.keySet().iterator().next());
        final String app=label,message=settings.getBoolean("content",false)?MirrorFilter.limit(title+(body.isEmpty()?"":" — "+body),1200):"Você recebeu uma nova notificação neste aplicativo.";
        try{sender.execute(()->{try{if(!settings.getBoolean("enabled",false)||!settings.getStringSet("apps",Collections.emptySet()).contains(pkg))return;String saved=new SecretStore(this,"phone_target").read();if(saved.isEmpty())return;JSONObject target=new JSONObject(saved);JSONObject payload=new JSONObject().put("id",AuthCrypto.digest(pkg+":"+sbn.getId()).substring(0,32)).put("title",MirrorFilter.limit(app,160)).put("message",settings.getBoolean("content",false)?message:"Você recebeu uma nova notificação neste aplicativo.");PhoneApi.call(target,"/api/notify",payload);settings.edit().putString("last_status","Último aviso enviado à TV").apply();}catch(Exception error){settings.edit().putString("last_status","Falha no envio: confira a conexão, a VPN e o vínculo da TV").apply();}});}catch(RejectedExecutionException ignored){}
    }
    public void onNotificationRemoved(StatusBarNotification sbn){recent.remove(sbn.getKey());}
    public void onDestroy(){sender.shutdownNow();recent.clear();super.onDestroy();}
}
