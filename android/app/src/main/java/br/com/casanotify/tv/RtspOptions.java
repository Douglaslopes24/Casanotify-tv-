package br.com.casanotify.tv;
import org.json.*;

/** Validated independently of Android, including per-request resource limits. */
public final class RtspOptions {
    public final String decoder,transport;
    public final int bufferMs;
    public RtspOptions(JSONObject data)throws JSONException{
        decoder=Notice.option(data,"video_decoder","auto",new String[]{"auto","hardware","software"});
        transport=Notice.option(data,"video_transport","tcp",new String[]{"tcp","auto"});
        bufferMs=Notice.integer(data,"video_buffer_ms",800,250,3000);
    }
    public JSONObject put(JSONObject target)throws JSONException{return target.put("video_decoder",decoder).put("video_transport",transport).put("video_buffer_ms",bufferMs);}
    public boolean mayRetry(boolean retried,boolean videoDecoderError){return decoder.equals("auto")&&!retried&&videoDecoderError;}
}
