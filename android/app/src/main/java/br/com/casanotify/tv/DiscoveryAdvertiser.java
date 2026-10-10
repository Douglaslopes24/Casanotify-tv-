package br.com.casanotify.tv;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

/** Advertises the receiver while enabled. No token or pairing code is published. */
public final class DiscoveryAdvertiser implements AutoCloseable {
    private final NsdManager manager;
    private final Prefs prefs;
    private volatile boolean closed;
    private volatile boolean registered;
    private NsdManager.RegistrationListener listener;

    public DiscoveryAdvertiser(Context context, Prefs prefs) {
        this.manager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.prefs = prefs;
    }

    public void start() {
        if (manager == null || closed || listener != null) return;
        String id = prefs.deviceId();
        NsdServiceInfo info = new NsdServiceInfo();
        info.setServiceName("CasaNotify-" + id.substring(0, 8));
        info.setServiceType("_casanotify._tcp.");
        info.setPort(Prefs.PORT);
        info.setAttribute("id", id);
        info.setAttribute("api", "3");
        info.setAttribute("name", prefs.config().optString("device_name","Minha TV"));
        info.setAttribute("version", "2.4.0");
        listener = new NsdManager.RegistrationListener() {
            public void onServiceRegistered(NsdServiceInfo service) {
                registered = true;
                if (closed) unregister();
            }
            public void onRegistrationFailed(NsdServiceInfo service, int error) {
                registered = false;
            }
            public void onServiceUnregistered(NsdServiceInfo service) {
                registered = false;
            }
            public void onUnregistrationFailed(NsdServiceInfo service, int error) {
                registered = false;
            }
        };
        try {
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener);
        } catch (RuntimeException ignored) {
            // Manual connection by IP remains available if firmware blocks NSD.
        }
    }

    private synchronized void unregister() {
        if (!registered || listener == null) return;
        registered = false;
        try { manager.unregisterService(listener); } catch (RuntimeException ignored) { }
    }

    public void close() {
        closed = true;
        unregister();
    }
}
