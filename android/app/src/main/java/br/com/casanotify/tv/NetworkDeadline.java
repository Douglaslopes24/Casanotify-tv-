package br.com.casanotify.tv;

import java.util.concurrent.*;

/** Wall-clock network deadline, independent of a peer sending one byte per read timeout. */
public final class NetworkDeadline implements AutoCloseable {
    private static final ScheduledThreadPoolExecutor TIMER=new ScheduledThreadPoolExecutor(1,r->{Thread t=new Thread(r,"CasaNotify-network-deadlines");t.setDaemon(true);return t;});
    static {TIMER.setRemoveOnCancelPolicy(true);}
    private final ScheduledFuture<?> task;
    public NetworkDeadline(long millis,Runnable cancel){task=TIMER.schedule(()->{try{cancel.run();}catch(RuntimeException ignored){}},millis,TimeUnit.MILLISECONDS);}
    public void close(){task.cancel(false);}
}
