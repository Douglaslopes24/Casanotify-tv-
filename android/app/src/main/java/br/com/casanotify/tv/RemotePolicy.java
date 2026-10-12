package br.com.casanotify.tv;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;

/** Commands are explicit, bounded user actions; never arbitrary input or shell. */
public final class RemotePolicy {
    public static final String[] COMMANDS={"up","down","left","right","ok","back","home","volume_up","volume_down","mute","play_pause"};
    public static boolean allowed(String role,boolean session){return "control".equals(role)&&session;}
    public static String command(JSONObject body)throws JSONException{
        Notice.rejectUnknown(body,Collections.singleton("command"));
        return Notice.option(body,"command","",COMMANDS);
    }
}
