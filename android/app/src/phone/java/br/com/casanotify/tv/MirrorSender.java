package br.com.casanotify.tv;

import android.app.job.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.json.*;

/** Delivery lifetime is independent of an Activity or a NotificationListener instance. */
public final class MirrorSender {
    private static final int JOB_ID=876601;
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static final ExecutorService CAPTURE=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(30),new ThreadPoolExecutor.DiscardOldestPolicy());
    private static final AtomicBoolean KICK_QUEUED=new AtomicBoolean();
    private static final AtomicLong EPOCH=new AtomicLong();
    private static MirrorOutbox outbox;private static MirrorReceipts receipts;
    static volatile boolean listenerConnected;
    public static final String HIDDEN_CONTENT="Você recebeu uma nova notificação neste aplicativo.";
    static SharedPreferences settings(Context c){return c.getSharedPreferences("phone",Context.MODE_PRIVATE);}
    private static synchronized MirrorOutbox queue(Context c){if(outbox==null)outbox=new MirrorOutbox(new SecretStore(c.getApplicationContext(),"mirror_outbox"));return outbox;}
    private static synchronized MirrorReceipts receipts(Context c){if(receipts==null)receipts=new MirrorReceipts(new SecretStore(c.getApplicationContext(),"mirror_receipts"));return receipts;}
    public static void foreground(Context c){reconnect(c);MirrorConnectionService.start(c);}
    public static boolean permitted(Context c){if(Build.VERSION.SDK_INT>=27)return c.getSystemService(android.app.NotificationManager.class).isNotificationListenerAccessGranted(new ComponentName(c,PhoneNotificationService.class));String listeners=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");if(listeners==null)return false;ComponentName expected=new ComponentName(c,PhoneNotificationService.class);for(String item:listeners.split(":"))if(expected.equals(ComponentName.unflattenFromString(item)))return true;return false;}
    public static boolean enabled(Context c){return settings(c).getBoolean("enabled",false)&&Ui.accepted(c)&&permitted(c);}
    static void status(Context c,String message){settings(c).edit().putString("last_status",message).apply();}
    public static void reconnect(Context c){
        if(!enabled(c))return;
        if(!listenerConnected)new Handler(Looper.getMainLooper()).post(()->{try{NotificationListenerService.requestRebind(new ComponentName(c,PhoneNotificationService.class));}catch(RuntimeException ignored){}});
        kick(c);
    }
    public static void enqueue(Context context,String pkg,JSONObject payload){
        Context c=context.getApplicationContext();final long capturedEpoch=EPOCH.get();final String capturedBinding;try{JSONObject captured=TvLink.load(c,"phone_target");if(captured==null)return;capturedBinding=MirrorOutbox.binding(captured);}catch(Exception e){return;}
        CAPTURE.execute(()->{try{synchronized(queue(c)){if(capturedEpoch!=EPOCH.get()||!enabled(c)||!settings(c).getStringSet("apps",Collections.emptySet()).contains(pkg))return;JSONObject target=TvLink.load(c,"phone_target");if(target==null||!capturedBinding.equals(MirrorOutbox.binding(target)))return;
            if(!settings(c).getBoolean("content",false))payload.put("message",HIDDEN_CONTENT);
            if(receipts(c).contains(capturedBinding,payload.getString("id")))return;queue(c).enqueue(capturedBinding,pkg,payload);}schedule(c);kick(c);
        }catch(Exception e){status(c,"Não foi possível guardar o aviso protegido.");}});
    }
    public static int pending(Context c){try{return queue(c).size();}catch(Exception e){return 0;}}
    public static void clear(Context c){synchronized(queue(c)){EPOCH.incrementAndGet();try{queue(c).clear();}catch(Exception ignored){}}JobScheduler scheduler=c.getSystemService(JobScheduler.class);if(scheduler!=null)scheduler.cancel(JOB_ID);}
    private static void schedule(Context c){
        if(!enabled(c)||pending(c)==0)return;JobScheduler scheduler=c.getSystemService(JobScheduler.class);if(scheduler==null||scheduler.getPendingJob(JOB_ID)!=null)return;
        try{scheduler.schedule(new JobInfo.Builder(JOB_ID,new ComponentName(c,MirrorRetryJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setBackoffCriteria(30000,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build());}catch(RuntimeException e){status(c,"O Android adiou a tentativa em segundo plano.");}
    }
    public static void kick(Context context){
        Context c=context.getApplicationContext();if(!KICK_QUEUED.compareAndSet(false,true))return;
        WORK.execute(()->{try{drain(c,new AtomicBoolean());}finally{KICK_QUEUED.set(false);schedule(c);}});
    }
    static void runJob(Context context,AtomicBoolean stopped,Runnable finished){Context c=context.getApplicationContext();WORK.execute(()->{try{drain(c,stopped);}finally{finished.run();}});}
    private static void drain(Context c,AtomicBoolean stopped){
        PowerManager.WakeLock wake=null;try{
            if(stopped.get())return;final long epoch=EPOCH.get();
            if(!enabled(c)){clear(c);return;}
            JSONObject target=TvLink.load(c,"phone_target");if(target==null){clear(c);return;}String binding=MirrorOutbox.binding(target);
            if(queue(c).first(binding)==null)return;
            PowerManager power=c.getSystemService(PowerManager.class);if(power!=null){wake=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CasaNotify:notification-delivery");wake.setReferenceCounted(false);wake.acquire(90000);}
            ControlClient client=TvLink.connect(c,"phone_target",target);long deadline=SystemClock.elapsedRealtime()+40000;
            for(int i=0;i<8&&!stopped.get()&&SystemClock.elapsedRealtime()<deadline;i++){
                JSONObject item=queue(c).first(binding);if(item==null)return;
                if(epoch!=EPOCH.get())return;
                JSONObject latest=TvLink.load(c,"phone_target");if(!enabled(c)||latest==null||!binding.equals(MirrorOutbox.binding(latest))){clear(c);return;}
                JSONObject payload=item.getJSONObject("payload");String id=payload.getString("id");if(receipts(c).contains(binding,id)){queue(c).acknowledge(binding,id);continue;}
                if(!settings(c).getStringSet("apps",Collections.emptySet()).contains(item.getString("package"))){queue(c).acknowledge(binding,id);continue;}
                if(!settings(c).getBoolean("content",false))payload.put("message",HIDDEN_CONTENT);
                JSONObject result=client.request("/api/notify","POST",payload.toString(),"");int code=result.optInt("status");
                if(code==401||code==403)throw new TvLink.Revoked();
                if(code>=200&&code<300){receipts(c).delivered(binding,id);queue(c).acknowledge(binding,id);settings(c).edit().putLong("last_sent",System.currentTimeMillis()).putString("last_status","Último aviso entregue à TV").apply();}
                else if(code==400||code==413){queue(c).acknowledge(binding,id);status(c,"Um aviso incompatível foi descartado.");}
                else {status(c,"A TV está ocupada. O envio será tentado novamente.");return;}
            }
        }catch(TvLink.Revoked e){clear(c);settings(c).edit().putBoolean("enabled",false).putString("last_status","Acesso revogado na TV. Abra Ajustes para autorizar um novo vínculo.").apply();}
        catch(TvLink.Upgrade e){status(c,e.getMessage());}
        catch(Exception e){status(c,"Aguardando a TV ou a rede. O vínculo continua salvo.");}
        finally{if(wake!=null)try{if(wake.isHeld())wake.release();}catch(RuntimeException ignored){}}
    }
}
