package br.com.casanotify.tv;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

/** Local opt-in. No event processing, text extraction, screenshots, or gestures. */
public final class CasaRemoteService extends AccessibilityService implements TvRemote.Executor {
    @Override protected void onServiceConnected(){TvRemote.attach(this);}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
    @Override public boolean onUnbind(Intent intent){TvRemote.detach(this);return super.onUnbind(intent);}
    @Override public void onDestroy(){TvRemote.detach(this);super.onDestroy();}
    @Override public boolean execute(String command){
        if(command.equals("back"))return performGlobalAction(GLOBAL_ACTION_BACK);
        if(command.equals("home"))return performGlobalAction(GLOBAL_ACTION_HOME);
        AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        switch(command){
            case "volume_up":audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);return true;
            case "volume_down":audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);return true;
            case "mute":audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_TOGGLE_MUTE,AudioManager.FLAG_SHOW_UI);return true;
            case "play_pause":long now=SystemClock.uptimeMillis();audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,0));audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,0));return true;
        }
        int direction;switch(command){case "up":direction=View.FOCUS_UP;break;case "down":direction=View.FOCUS_DOWN;break;case "left":direction=View.FOCUS_LEFT;break;case "right":direction=View.FOCUS_RIGHT;break;case "ok":direction=0;break;default:return false;}
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;
        AccessibilityNodeInfo focused=null,next=null;
        try{
            focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if(direction==0){
                for(int depth=0;focused!=null&&depth<8;depth++){
                    if(focused.isEnabled()&&focused.isClickable())return focused.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    AccessibilityNodeInfo parent=focused.getParent();focused.recycle();focused=parent;
                }
                return false;
            }
            if(focused!=null)next=focused.focusSearch(direction);
            else next=firstFocusable(root);
            return next!=null&&next.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        }finally{if(next!=null)next.recycle();if(focused!=null)focused.recycle();root.recycle();}
    }
    private static AccessibilityNodeInfo firstFocusable(AccessibilityNodeInfo root){
        Deque<AccessibilityNodeInfo> queue=new ArrayDeque<>();queue.add(AccessibilityNodeInfo.obtain(root));int seen=0;
        try{while(!queue.isEmpty()&&seen++<128){AccessibilityNodeInfo node=queue.removeFirst();if(node.isVisibleToUser()&&node.isEnabled()&&node.isFocusable())return node;int children=Math.min(node.getChildCount(),32);for(int i=0;i<children&&seen+queue.size()<128;i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)queue.addLast(child);}node.recycle();}return null;}finally{while(!queue.isEmpty())queue.removeFirst().recycle();}
    }
}
