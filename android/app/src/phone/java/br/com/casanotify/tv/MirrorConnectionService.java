package br.com.casanotify.tv;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.net.*;
import android.os.*;

/** User-enabled TV connection, visible foreground notification and explicit pause action. */
public final class MirrorConnectionService extends Service {
    private static final int ID=2401;
    private static final String CHANNEL="casanotify_connection",PAUSE="br.com.casanotify.tv.PAUSE_MIRROR";
    static volatile boolean running;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivity;private ConnectivityManager.NetworkCallback network;
    private final Runnable tick=new Runnable(){public void run(){if(!MirrorSender.enabled(MirrorConnectionService.this)){stopSelf();return;}MirrorSender.reconnect(MirrorConnectionService.this);try{getSystemService(NotificationManager.class).notify(ID,notification());}catch(RuntimeException ignored){}handler.postDelayed(this,30000);}};
    public static void start(Context context){if(!MirrorSender.enabled(context))return;try{context.startForegroundService(new Intent(context,MirrorConnectionService.class));}catch(RuntimeException e){MirrorSender.status(context,"O Android adiou o serviço contínuo. Abra os ajustes de avisos para retomá-lo.");}}
    public static void stop(Context context){context.stopService(new Intent(context,MirrorConnectionService.class));}
    public void onCreate(){super.onCreate();NotificationChannel channel=new NotificationChannel(CHANNEL,"Conexão contínua com a TV",NotificationManager.IMPORTANCE_LOW);channel.setDescription("Mantém os avisos autorizados funcionando com a tela do aplicativo fechada.");channel.setShowBadge(false);getSystemService(NotificationManager.class).createNotificationChannel(channel);}
    public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&PAUSE.equals(intent.getAction())){MirrorSender.settings(this).edit().putBoolean("enabled",false).apply();MirrorSender.clear(this);stopSelf();return START_NOT_STICKY;}
        if(!MirrorSender.enabled(this)){stopSelf();return START_NOT_STICKY;}
        try{if(Build.VERSION.SDK_INT>=29)startForeground(ID,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);else startForeground(ID,notification());}catch(RuntimeException e){MirrorSender.status(this,"O Android não permitiu iniciar a conexão contínua.");stopSelf();return START_NOT_STICKY;}
        running=true;if(network==null){connectivity=getSystemService(ConnectivityManager.class);network=new ConnectivityManager.NetworkCallback(){public void onAvailable(Network n){MirrorSender.reconnect(MirrorConnectionService.this);}};try{if(connectivity!=null)connectivity.registerDefaultNetworkCallback(network);}catch(RuntimeException ignored){}}
        handler.removeCallbacks(tick);handler.post(tick);return START_STICKY;
    }
    private Notification notification(){
        PendingIntent open=PendingIntent.getActivity(this,2401,new Intent(this,PhoneActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pause=PendingIntent.getService(this,2402,new Intent(this,MirrorConnectionService.class).setAction(PAUSE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle("CasaNotify · avisos para a TV")
            .setContentText(MirrorSender.listenerConnected?"Serviço de avisos ativo. Você pode fechar a tela do app.":"Serviço ativo · aguardando o leitor de notificações do Android")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PRIVATE)
            .addAction(new Notification.Action.Builder(null,"Pausar avisos",pause).build()).build();
    }
    public IBinder onBind(Intent intent){return null;}
    public void onDestroy(){running=false;handler.removeCallbacksAndMessages(null);if(connectivity!=null&&network!=null)try{connectivity.unregisterNetworkCallback(network);}catch(RuntimeException ignored){}stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
