package br.com.casanotify.tv;
import android.content.*;import android.provider.Settings;
public final class BootReceiver extends BroadcastReceiver{public void onReceive(Context c,Intent i){if(!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))return;Prefs p=new Prefs(c);if(Ui.accepted(c)&&p.enabled()&&p.config().optBoolean("auto_start")&&Settings.canDrawOverlays(c))try{c.startForegroundService(new Intent(c,NotifyService.class));}catch(RuntimeException e){NotifyService.lastError="Abra o app e ative o receptor após reiniciar.";}}}
