package br.com.casanotify.tv;

/** Only the TV's locally authorized accessibility service can attach an executor. */
public final class TvRemote {
    public interface Executor {boolean execute(String command);}
    private static Executor executor;
    private static long window;
    private static int count;
    public static synchronized void attach(Executor value){executor=value;window=0;count=0;}
    public static synchronized void detach(Executor value){if(executor==value)executor=null;}
    public static synchronized boolean available(){return executor!=null;}
    public static synchronized int execute(String command){
        if(!java.util.Arrays.asList(RemotePolicy.COMMANDS).contains(command))return 400;
        if(executor==null)return 409;
        long now=System.nanoTime();if(window==0||now-window>=2_000_000_000L){window=now;count=0;}
        if(++count>16)return 429;
        try{return executor.execute(command)?200:409;}catch(RuntimeException ignored){return 409;}
    }
}
