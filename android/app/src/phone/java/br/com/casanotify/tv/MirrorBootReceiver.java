package br.com.casanotify.tv;

import android.content.*;

public final class MirrorBootReceiver extends BroadcastReceiver {
    public void onReceive(Context context,Intent intent){String action=intent.getAction();if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))MirrorSender.reconnect(context.getApplicationContext());}
}
