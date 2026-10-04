package br.com.casanotify.tv;

import android.content.Context;
import android.os.*;
import android.provider.Settings;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public final class ApiController implements LanServer.Handler {
    private final Context context;private final Prefs prefs;private final OverlayManager overlay;
    private final AccountManager accounts;private final PhoneTokens phones;private final CameraStore cameras;private final MediaAssets media;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final String fingerprint;private final RequestAuth requestAuth;
    public ApiController(Context c,Prefs p,OverlayManager o)throws Exception{
        context=c;prefs=p;overlay=o;accounts=SecretStore.accounts(c);phones=new PhoneTokens(c);cameras=new CameraStore(c);media=new MediaAssets(c);fingerprint=LocalTls.fingerprint();requestAuth=new RequestAuth(id->id.equals("ha")?new RequestAuth.Key(AuthCrypto.digest(prefs.token()),"ha"):phones.key(id));
    }
    private JSONObject info()throws Exception{return new JSONObject().put("app","CasaNotify TV").put("version","2.2.0").put("api_version",3).put("control_protocol",2).put("auth_mode","password").put("device_id",prefs.deviceId()).put("device_name",prefs.config().optString("device_name")).put("tls_port",Prefs.SECURE_PORT).put("tls_fingerprint",fingerprint);}
    private static LanServer.Response json(JSONObject o){return LanServer.Response.json(200,o.toString());}
    private static String string(JSONObject o,String k,int n)throws JSONException{return Notice.string(o,k,"",n);}
    private static String cookieId(String cookie){if(cookie==null)return "";for(String part:cookie.split(";")){String[] p=part.trim().split("=",2);if(p.length==2&&p[0].equals("__Host-casanotify")&&p[1].matches("[A-Za-z0-9_-]{43}"))return p[1];}return "";}
    private LanServer.Response sessionResponse(AccountManager.Session s)throws Exception{return json(new JSONObject().put("profile",accounts.profile()).put("csrf",s.csrf)).header("Set-Cookie",AccountManager.cookie(s));}
    public LanServer.Response handle(LanServer.Request r)throws Exception{
        try{
            if(r.method.equals("GET")&&r.path.equals("/api/info"))return json(info());
            if(!r.secure)return LanServer.Response.error(426,"Use os aplicativos atualizados e HTTPS na porta 8766.");
            if(r.method.equals("GET")&&r.path.equals("/api/challenge"))return json(new JSONObject().put("challenge",requestAuth.challenge()));
            if(r.method.equals("POST")&&r.path.equals("/api/pair")){
                JSONObject d=new JSONObject(r.body);String kind=Notice.option(d,"client","ha",new String[]{"ha","phone","control"});
                if(!Pairing.redeem(string(d,"code",6),kind))return LanServer.Response.error(401,"Código inválido, vencido ou destinado a outro vínculo.");
                return json(new JSONObject().put("token",kind.equals("ha")?prefs.token():phones.issue(Notice.string(d,"name","Celular",60),kind)));
            }
            RequestAuth.Client client=requestAuth.verify(r);
            if(client==null)return LanServer.Response.error(401,"Acesso exclusivo para clientes vinculados. Atualize o app e a integração. No controle, use Menu → Trocar TV para vincular novamente.");
            if(r.path.startsWith("/auth/")&&!client.role.equals("control"))return LanServer.Response.error(403,"Esta operação exige o aplicativo de controle vinculado.");
            if(r.method.equals("GET")&&r.path.equals("/auth/state"))return json(new JSONObject().put("registered",accounts.exists()).put("terms_version",AccountManager.TERMS_VERSION));
            if(r.method.equals("POST")&&r.path.startsWith("/auth/")){
                JSONObject d=new JSONObject(r.body);
                switch(r.path){
                    case "/auth/register":
                        if(!AccountManager.TERMS_VERSION.equals(string(d,"terms_version",30)))return LanServer.Response.error(400,"Atualize o aplicativo e leia os termos atuais.");
                        accounts.validateRegistration(string(d,"username",40),string(d,"display_name",60),string(d,"password",128),Notice.bool(d,"accepted_terms",false));
                        return sessionResponse(accounts.register(string(d,"username",40),string(d,"display_name",60),string(d,"password",128),Notice.bool(d,"accepted_terms",false),client.id));
                    case "/auth/login":try{return sessionResponse(accounts.login(string(d,"username",40),string(d,"password",128),client.id));}catch(IllegalArgumentException e){return LanServer.Response.error(401,e.getMessage());}
                }
            }
            String sessionId=cookieId(r.headers.get("cookie"));AccountManager.Session session=accounts.session(sessionId,client.id,!r.path.equals("/api/status")&&!r.path.equals("/api/history"));
            boolean admin=client.role.equals("ha");
            if(session!=null&&r.method.equals("POST")&&!AuthCrypto.same(session.csrf,r.headers.get("x-casanotify-csrf")))return LanServer.Response.error(403,"Sessão expirada. Entre novamente.");
            if(client.role.equals("phone")||session==null&&!admin){
                if(r.method.equals("POST")&&r.path.equals("/api/notify")&&client.role.equals("phone")&&phones.allowNotice(client.id)){
                    JSONObject d=new JSONObject(r.body);JSONObject limited=new JSONObject().put("title",Notice.string(d,"title","Celular",160)).put("message",string(d,"message",1200)).put("icon","phone").put("duration",10).put("id","phone-"+string(d,"id",60));
                    Notice n=new Notice(limited,prefs.defaults());return onMain(()->overlay.receive(n));
                }
                return LanServer.Response.error(401,"Entre com usuário e senha.");
            }
            if(r.path.equals("/auth/session")&&r.method.equals("GET")){if(session==null)return LanServer.Response.error(401,"Login da conta necessário.");return json(new JSONObject().put("profile",accounts.profile()).put("csrf",session.csrf));}
            if(r.path.startsWith("/auth/")&&session==null)return LanServer.Response.error(403,"Esta operação exige login da conta.");
            if(r.method.equals("GET"))switch(r.path){
                case "/api/config":return json(prefs.config());
                case "/api/history":return onMain(()->json(new JSONObject().put("items",overlay.history())));
                case "/api/status":return onMain(()->json(info().put("quiet_active",prefs.quietActive()).put("paused",prefs.config().optBoolean("paused")).put("overlay_permission",Settings.canDrawOverlays(context)).put("addresses",new JSONArray(NetworkInfo.addresses())).put("overlay",overlay.state()).put("vpn_active",VpnActivity.active(context))));
                case "/api/cameras":return json(new JSONObject().put("items",cameras.list()));
                case "/api/phones":return json(new JSONObject().put("items",phones.publicList()));
                case "/media/logo":case "/media/background":byte[] bytes=media.bytes(r.path.substring(7));return bytes==null?LanServer.Response.error(404,"Sem imagem personalizada."):new LanServer.Response(200,"image/png",bytes);
            }
            if(r.method.equals("POST")){
                JSONObject d=new JSONObject(r.body);
                switch(r.path){
                    case "/auth/logout":accounts.logout(sessionId);return json(new JSONObject().put("ok",true)).header("Set-Cookie",AccountManager.clearCookie());
                    case "/auth/password":accounts.changePassword(string(d,"current",128),string(d,"password",128));return json(new JSONObject().put("ok",true)).header("Set-Cookie",AccountManager.clearCookie());
                    case "/auth/profile":accounts.rename(string(d,"display_name",60));return json(accounts.profile());
                    case "/api/notify":if(d.has("camera_id")){d.put("video_url",cameras.url(string(d,"camera_id",80)));d.remove("camera_id");}Notice n=new Notice(d,prefs.defaults());return onMain(()->overlay.receive(n));
                    case "/api/clear":Notice.rejectUnknown(d,Collections.singleton("id"));String id=string(d,"id",80);return onMain(()->{overlay.clear(id);return json(new JSONObject().put("status","cleared"));});
                    case "/api/config":return onMain(()->{JSONObject c=prefs.update(d);if(c.optBoolean("paused"))overlay.clear("");return json(c);});
                    case "/api/media":media.save(string(d,"kind",20),string(d,"data",1500000));return json(new JSONObject().put("ok",true));
                    case "/api/media/remove":media.remove(string(d,"kind",20));return json(new JSONObject().put("ok",true));
                    case "/api/cameras/save":cameras.save(string(d,"id",80),string(d,"name",60),string(d,"url",2048));return json(new JSONObject().put("items",cameras.list()));
                    case "/api/cameras/delete":cameras.remove(string(d,"id",80));return json(new JSONObject().put("items",cameras.list()));
                    case "/api/cameras/test":Notice camera=new Notice(new JSONObject().put("title","Câmera ao vivo").put("message","Transmissão RTSP").put("icon","camera").put("video_url",cameras.url(string(d,"id",80))).put("duration",30).put("replace",true),prefs.defaults());return onMain(()->overlay.receive(camera));
                    case "/api/phones/revoke":phones.revoke(string(d,"id",80));return json(new JSONObject().put("items",phones.publicList()));
                }
            }
            return LanServer.Response.error(404,"Rota não encontrada.");
        }catch(JSONException|IllegalArgumentException e){return LanServer.Response.error(400,e.getMessage()==null?"Solicitação inválida.":e.getMessage());}
    }
    private LanServer.Response onMain(Callable<LanServer.Response> action)throws Exception{FutureTask<LanServer.Response> t=new FutureTask<>(action);main.post(t);try{return t.get(3,TimeUnit.SECONDS);}catch(TimeoutException e){t.cancel(false);return LanServer.Response.error(503,"A TV está ocupada. Tente novamente.");}catch(ExecutionException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw(Exception)cause;throw new RuntimeException(cause);}}
}
