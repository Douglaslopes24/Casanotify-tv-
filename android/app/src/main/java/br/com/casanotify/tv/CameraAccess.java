package br.com.casanotify.tv;

import org.json.JSONObject;

/** Camera configuration and RTSP events are owned by Home Assistant. */
public final class CameraAccess {
    public static boolean allowed(String role,String method,String path,JSONObject body){
        boolean camera=path.equals("/api/cameras")||path.startsWith("/api/cameras/")||method.equals("POST")&&path.equals("/api/notify")&&(body.has("camera_id")||body.has("video_url"));
        return !camera||role.equals("ha");
    }
}
