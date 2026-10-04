package br.com.casanotify.tv;
import org.json.*;
import java.net.URI;
import java.util.*;
public final class Notice {
 public static final String[] POSITIONS={"top_left","top_center","top_right","center_left","center","center_right","bottom_left","bottom_center","bottom_right"},THEMES={"lime","ocean","ember","rose","paper"},ICONS={"home","bell","door","camera","light","check","warning","info","sensor","motion","temperature","humidity","phone","delivery","chat","battery","wifi","lock","smoke","water","alarm"};
 public static final Set<String> STYLE=new HashSet<>(Arrays.asList("theme","icon","position","duration","width","text_size","opacity","radius","margin","background","text_color","accent","sound","speak","volume","progress","animation","tone","backdrop"));
 private static final Set<String> KEYS=new HashSet<>(STYLE);
 static {KEYS.addAll(Arrays.asList("id","title","message","image_url","image_refresh","urgent","replace","video_url","video_muted"));}
 public static final String[] TONES={"soft","doorbell","chime","pulse","alarm","digital","sound_01","sound_02","sound_03","sound_04","sound_05","sound_06","sound_07","sound_08","sound_09","sound_10"};
 public final String id,title,message,icon,theme,position,background,textColor,accent,imageUrl,animation,videoUrl,tone,backdrop;
 public final int duration,width,textSize,radius,margin,imageRefresh,volume;
 public final double opacity; public final boolean sound,speak,urgent,replace,progress,videoMuted; public final JSONObject json;
 public Notice(JSONObject input,JSONObject defaults)throws JSONException{
  rejectUnknown(input,KEYS);id=string(input,"id",UUID.randomUUID().toString(),80);title=string(input,"title","CasaNotify TV",160);message=string(input,"message","",1200);
  if(title.trim().isEmpty()&&message.trim().isEmpty())throw new IllegalArgumentException("Informe título ou mensagem.");
  JSONObject m=new JSONObject(defaults.toString());if(input.has("theme"))for(String k:new String[]{"background","text_color","accent"})m.remove(k);
  for(String k:KEYS)if(input.has(k))m.put(k,input.get(k));
  theme=option(m,"theme","lime",THEMES);icon=option(m,"icon","bell",ICONS);position=option(m,"position","top_right",POSITIONS);animation=option(m,"animation","slide",new String[]{"slide","fade","none"});
  duration=integer(m,"duration",10,3,120);width=integer(m,"width",380,260,800);textSize=integer(m,"text_size",18,14,36);radius=integer(m,"radius",20,0,40);margin=integer(m,"margin",28,0,100);volume=integer(m,"volume",50,0,100);opacity=decimal(m,"opacity",.8,.4,.8);
  background=color(m,"background",palette(theme,0));textColor=color(m,"text_color",palette(theme,1));accent=color(m,"accent",palette(theme,2));
  tone=option(m,"tone","soft",TONES);backdrop=option(m,"backdrop","none",new String[]{"none","custom"});videoUrl=string(input,"video_url","",2048);validateVideoUrl(videoUrl);videoMuted=bool(input,"video_muted",true);
  imageUrl=string(input,"image_url","",2048);if(!videoUrl.isEmpty()&&!imageUrl.isEmpty())throw new IllegalArgumentException("Escolha imagem ou vídeo RTSP para cada aviso.");validateImageUrl(imageUrl);imageRefresh=integer(input,"image_refresh",0,0,60);if(imageRefresh>0&&imageRefresh<5)throw new IllegalArgumentException("image_refresh deve ser 0 ou 5–60 segundos.");
  sound=bool(m,"sound",false);speak=bool(m,"speak",false);urgent=bool(input,"urgent",false);replace=bool(input,"replace",false);progress=bool(m,"progress",true);
  json=new JSONObject().put("id",id).put("title",title).put("message",message).put("icon",icon).put("theme",theme).put("position",position).put("duration",duration).put("width",width).put("text_size",textSize).put("radius",radius).put("margin",margin).put("volume",volume).put("opacity",opacity).put("background",background).put("text_color",textColor).put("accent",accent).put("image_url",imageUrl).put("image_refresh",imageRefresh).put("sound",sound).put("speak",speak).put("urgent",urgent).put("replace",replace).put("progress",progress).put("animation",animation).put("tone",tone).put("backdrop",backdrop).put("video_url",videoUrl).put("video_muted",videoMuted);
 }
 public static String palette(String theme,int i){switch(theme){case "ocean":return new String[]{"#122330","#F1F7FC","#58C6F5"}[i];case "ember":return new String[]{"#2B2118","#FFF8F0","#FFBE65"}[i];case "rose":return new String[]{"#301C29","#FFF3F7","#F78FAE"}[i];case "paper":return new String[]{"#F5F4ED","#18241B","#34754B"}[i];default:return new String[]{"#18231D","#F3F8F2","#A3E635"}[i];}}
 public static void rejectUnknown(JSONObject o,Set<String> keys){Iterator<String> i=o.keys();while(i.hasNext()){String k=i.next();if(!keys.contains(k))throw new IllegalArgumentException("Campo desconhecido: "+k);}}
 public static String string(JSONObject o,String k,String fallback,int max)throws JSONException{if(!o.has(k))return fallback;Object v=o.get(k);if(!(v instanceof String)||((String)v).length()>max)throw new IllegalArgumentException(k+": use texto de até "+max+" caracteres.");return (String)v;}
 public static String option(JSONObject o,String k,String f,String[] choices)throws JSONException{String v=string(o,k,f,40);if(!Arrays.asList(choices).contains(v))throw new IllegalArgumentException("Valor inválido: "+k);return v;}
 public static double decimal(JSONObject o,String k,double f,double min,double max)throws JSONException{if(!o.has(k))return f;Object v=o.get(k);if(!(v instanceof Number))throw new IllegalArgumentException(k+" deve ser número.");double n=((Number)v).doubleValue();if(!Double.isFinite(n)||n<min||n>max)throw new IllegalArgumentException(k+" deve estar entre "+min+" e "+max);return n;}
 public static int integer(JSONObject o,String k,int f,int min,int max)throws JSONException{double v=decimal(o,k,f,min,max);if(v!=Math.rint(v))throw new IllegalArgumentException(k+" deve ser inteiro.");return (int)v;}
 public static boolean bool(JSONObject o,String k,boolean f)throws JSONException{if(!o.has(k))return f;if(!(o.get(k) instanceof Boolean))throw new IllegalArgumentException(k+" deve ser true ou false.");return o.getBoolean(k);}
 public static String color(JSONObject o,String k,String f)throws JSONException{String v=string(o,k,f,7);if(!v.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException(k+" deve usar #RRGGBB.");return v;}
 public static void validateVideoUrl(String s){if(s.isEmpty())return;try{URI u=new URI(s);if(!"rtsp".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getFragment()!=null||u.getPort()>65535||u.getPort()==0||s.indexOf(13)>=0||s.indexOf(10)>=0)throw new Exception();}catch(Exception e){throw new IllegalArgumentException("Use um endereço rtsp:// válido da câmera.");}}
 public static void validateImageUrl(String s){if(s.isEmpty())return;try{URI u=new URI(s);if(!("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme()))||u.getHost()==null||u.getRawUserInfo()!=null||u.getFragment()!=null)throw new Exception();}catch(Exception e){throw new IllegalArgumentException("Use URL HTTP/HTTPS sem usuário e senha.");}}
}
