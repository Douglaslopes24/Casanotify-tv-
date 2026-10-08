package br.com.casanotify.tv;

import android.app.job.*;
import android.os.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android may defer this job under battery restrictions; never a hidden foreground service. */
public final class MirrorRetryJob extends JobService {
    private AtomicBoolean stopped;
    public boolean onStartJob(JobParameters params){
        AtomicBoolean flag=new AtomicBoolean();stopped=flag;
        MirrorSender.runJob(this,flag,()->new Handler(Looper.getMainLooper()).post(()->{if(!flag.get())jobFinished(params,MirrorSender.enabled(this)&&MirrorSender.pending(this)>0);}));return true;
    }
    public boolean onStopJob(JobParameters params){if(stopped!=null)stopped.set(true);return MirrorSender.enabled(this)&&MirrorSender.pending(this)>0;}
}
