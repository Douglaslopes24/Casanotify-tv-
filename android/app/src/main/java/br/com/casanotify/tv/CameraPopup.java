package br.com.casanotify.tv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;

/** Independent video-only window; closing it never clears notification history/queue. */
public final class CameraPopup {
    private final Context context;private final WindowManager windows;
    private LinearLayout window;private RtspVideoPlayer player;private String cameraId="";
    public CameraPopup(Context context){this.context=context;windows=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);}
    public LanServer.Response show(String id,Notice notice)throws JSONException{
        if(!Settings.canDrawOverlays(context))return LanServer.Response.error(409,"Autorize aparecer sobre outros apps na TV.");
        close();android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();windows.getDefaultDisplay().getRealMetrics(metrics);
        int margin=Ui.dp(context,24),width=Math.max(1,Math.min(Ui.dp(context,480),Math.min(metrics.widthPixels-2*margin,(metrics.heightPixels-2*margin-Ui.dp(context,56))*16/9)));
        LinearLayout root=new LinearLayout(context);root.setOrientation(1);root.setBackgroundColor(Color.BLACK);
        FrameLayout frame=new FrameLayout(context);TextureView texture=new TextureView(context);frame.addView(texture,new FrameLayout.LayoutParams(-1,-1));
        TextView state=new TextView(context);state.setTextColor(Color.WHITE);state.setBackgroundColor(0xaa000000);state.setGravity(Gravity.CENTER);state.setPadding(margin,margin,margin,margin);frame.addView(state,new FrameLayout.LayoutParams(-1,-1));root.addView(frame,new LinearLayout.LayoutParams(width,width*9/16));
        Button release=new Button(context);release.setText("Liberar");release.setAllCaps(false);release.setTextColor(0xff142106);release.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffa3e635));release.setOnClickListener(v->close());release.setOnKeyListener((v,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP)close();return true;});root.addView(release,new LinearLayout.LayoutParams(-1,Ui.dp(context,56)));
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(width,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_SECURE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP|Gravity.RIGHT;lp.x=margin;lp.y=margin;
        try{windows.addView(root,lp);}catch(RuntimeException ignored){return LanServer.Response.error(409,"O Android bloqueou a janela da câmera.");}
        window=root;cameraId=id;release.requestFocus();player=new RtspVideoPlayer(context,texture,notice,text->{if(window!=root)return;state.setText(text);state.setVisibility(text.isEmpty()?View.GONE:View.VISIBLE);});player.start();return LanServer.Response.json(200,status().toString());
    }
    public JSONObject status()throws JSONException{return new JSONObject().put("active",window!=null).put("camera_id",cameraId);}
    public void closeIf(String id){if(cameraId.equals(id))close();}
    public void close(){if(player!=null){player.close();player=null;}if(window!=null){LinearLayout old=window;window=null;try{windows.removeViewImmediate(old);}catch(RuntimeException ignored){}}cameraId="";}
}
