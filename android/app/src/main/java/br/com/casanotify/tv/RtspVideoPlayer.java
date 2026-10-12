package br.com.casanotify.tv;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.mediacodec.*;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import java.util.*;

/** Bounded RTSP playback. Credentials and player errors never reach UI/logs. */
public final class RtspVideoPlayer {
    public interface Status {void show(String message);}
    private final Context context;private final TextureView texture;private final String url;
    private final RtspOptions options;private final boolean muted;private final Status status;
    private final Handler main=new Handler(Looper.getMainLooper());
    private ExoPlayer player;private boolean closed,retried;private int generation;
    public RtspVideoPlayer(Context context,TextureView texture,Notice notice,Status status){this.context=context;this.texture=texture;this.url=notice.videoUrl;this.options=notice.rtsp;this.muted=notice.videoMuted;this.status=status;}
    public void start(){if(!closed)open(options.decoder);}
    private void open(String mode){
        releasePlayer();final int run=++generation;
        status.show("Conectando à câmera…");
        try{
            androidx.media3.common.util.Log.setLogLevel(androidx.media3.common.util.Log.LOG_LEVEL_OFF);
            MediaCodecSelector selector=(mime,secure,tunneling)->{
                List<MediaCodecInfo> available=MediaCodecSelector.DEFAULT.getDecoderInfos(mime,secure,tunneling);
                if(!MimeTypes.isVideo(mime)||mode.equals("auto"))return available;
                List<MediaCodecInfo> chosen=new ArrayList<>();
                for(MediaCodecInfo codec:available)if(mode.equals("software")?codec.softwareOnly:codec.hardwareAccelerated)chosen.add(codec);
                return chosen;
            };
            DefaultRenderersFactory renderers=new DefaultRenderersFactory(context).setMediaCodecSelector(selector).setEnableDecoderFallback(false);
            DefaultLoadControl load=new DefaultLoadControl.Builder().setBufferDurationsMs(Math.max(options.bufferMs,1000),Math.max(options.bufferMs,1000)+2000,options.bufferMs,options.bufferMs).build();
            ExoPlayer active=new ExoPlayer.Builder(context,renderers).setLoadControl(load).build();player=active;
            active.setVideoTextureView(texture);active.setVolume(muted?0:1);
            active.setTrackSelectionParameters(active.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO,muted).build());
            active.addListener(new Player.Listener(){
                @Override public void onRenderedFirstFrame(){if(!closed&&generation==run){main.removeCallbacksAndMessages(null);status.show("");}}
                @Override public void onPlayerError(PlaybackException error){
                    if(closed||generation!=run)return;
                    boolean video=error instanceof ExoPlaybackException&&((ExoPlaybackException)error).type==ExoPlaybackException.TYPE_RENDERER&&((ExoPlaybackException)error).rendererFormat!=null&&MimeTypes.isVideo(((ExoPlaybackException)error).rendererFormat.sampleMimeType);
                    if(options.mayRetry(retried,video)){retried=true;main.post(()->{if(!closed&&generation==run)open("software");});}
                    else fail("Câmera indisponível. Confira a rede e um perfil de vídeo compatível com a TV.");
                }
            });
            RtspMediaSource source=new RtspMediaSource.Factory().setForceUseRtpTcp(options.transport.equals("tcp")).setTimeoutMs(6000).createMediaSource(MediaItem.fromUri(Uri.parse(url)));
            active.setMediaSource(source);active.prepare();active.play();
            main.postDelayed(()->{if(!closed&&generation==run)fail("Sem imagem da câmera. Confira a transmissão e o codec.");},12000);
        }catch(RuntimeException ignored){fail("Não foi possível abrir a câmera com os decodificadores desta TV.");}
    }
    private void fail(String message){generation++;main.removeCallbacksAndMessages(null);releasePlayer();if(!closed)status.show(message);}
    private void releasePlayer(){if(player!=null){ExoPlayer old=player;player=null;old.clearVideoTextureView(texture);old.release();}}
    public void close(){closed=true;generation++;main.removeCallbacksAndMessages(null);releasePlayer();}
}
