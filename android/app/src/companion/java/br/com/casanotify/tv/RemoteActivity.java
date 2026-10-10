package br.com.casanotify.tv;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Large native controls, inspired by a physical remote. Pairing is owned by Android TV Remote Service. */
public final class RemoteActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final List<Button> keys=new ArrayList<>();
    private volatile AndroidRemote remote;
    private volatile AndroidRemote.Pairing pairing;
    private TextView status,name;
    private Button connect,pair;
    private volatile boolean visible;private boolean busy;
    private volatile int generation;
    private AlertDialog pinDialog;
    public void onCreate(Bundle saved){
        Ui.theme(this);super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout page=Ui.page(this,"Controle da TV");page.addView(Ui.text(this,"Navegue, ajuste o volume e controle a reprodução.",16));
        LinearLayout connection=Ui.card(this,page,"Minha Android TV","");name=(TextView)connection.getChildAt(0);name.setText("Minha TV");status=Ui.text(this,"Carregando vínculo do controle…",15);connection.addView(status);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);connection.addView(actions);
        connect=Ui.button(this,actions,"Reconectar",this::connect);connect.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        pair=Ui.button(this,actions,"Vincular controle",this::pair);pair.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout remoteCard=Ui.card(this,page,"Seu controle","");
        GridLayout pad=new GridLayout(this);pad.setColumnCount(3);pad.setRowCount(3);remoteCard.addView(pad,new LinearLayout.LayoutParams(-1,-2));
        for(int row=0;row<3;row++)for(int column=0;column<3;column++){
            int code=row==0&&column==1?19:row==1&&column==0?21:row==1&&column==1?23:row==1&&column==2?22:row==2&&column==1?20:0;
            View view=code==0?new View(this):key(code==19?"▲":code==20?"▼":code==21?"◀":code==22?"▶":"OK",code,code==23);
            GridLayout.LayoutParams layout=new GridLayout.LayoutParams(GridLayout.spec(row),GridLayout.spec(column,1f));layout.width=0;layout.height=Ui.dp(this,76);int margin=Ui.dp(this,4);layout.setMargins(margin,margin,margin,margin);pad.addView(view,layout);
        }
        row(remoteCard,new String[]{"↶ Voltar","⌂ Início"},new int[]{4,3});
        row(remoteCard,new String[]{"VOL −","Mudo","VOL +"},new int[]{25,164,24});
        row(remoteCard,new String[]{"▶ / Ⅱ","⏻ Espera"},new int[]{85,26});
        remoteCard.addView(Ui.text(this,"Os botões controlam o aplicativo que estiver na tela da TV. O volume depende da saída de áudio e do suporte do aparelho.",14));
        LinearLayout help=Ui.card(this,page,"Primeira conexão","Celular e TV precisam estar na mesma rede. Ao vincular, a Android TV mostra um PIN de seis caracteres. Digite-o uma única vez; a autorização fica salva neste celular.");
        help.addView(Ui.text(this,"Requer Android TV Remote Service na TV. Algumas TV Boxes com Android comum não incluem esse serviço. O código é do controle nativo da Android TV e é diferente do vínculo do CasaNotify.",14));
        Ui.button(this,help,"Esquecer este controle",()->new AlertDialog.Builder(this).setTitle("Remover vínculo local?").setMessage("O próximo uso pedirá um novo PIN. Para revogar a autorização também na TV, remova o controle nos ajustes da própria Android TV.").setPositiveButton("Esquecer",(d,w)->{disconnect();new SecretStore(this,"android_remote").delete();status.setText("Vincule novamente quando desejar.");}).setNegativeButton("Cancelar",null).show());
        Ui.button(this,page,"Voltar aos ajustes",this::finish);enabled(false);
    }
    private Button key(String label,int code,boolean center){
        Button button=new Button(this);button.setText(label);button.setAllCaps(false);button.setTextSize(center?25:20);button.setTypeface(Typeface.DEFAULT,Typeface.BOLD);button.setTextColor(Ui.dark(this)||center?0xffffffff:0xff243244);
        String description=code==19?"Mover para cima":code==20?"Mover para baixo":code==21?"Mover para esquerda":code==22?"Mover para direita":code==23?"Confirmar seleção":code==24?"Aumentar volume":code==25?"Diminuir volume":code==164?"Ativar ou desativar som":code==85?"Reproduzir ou pausar":code==4?"Voltar":code==26?"Ligar ou colocar em espera":"Tela inicial";button.setContentDescription(description);
        StateListDrawable states=new StateListDrawable();GradientDrawable focus=shape(center,true),normal=shape(center,false);states.addState(new int[]{android.R.attr.state_pressed},focus);states.addState(new int[]{android.R.attr.state_focused},focus);states.addState(new int[]{},normal);button.setBackground(states);button.setOnClickListener(v->send(code));keys.add(button);return button;
    }
    private GradientDrawable shape(boolean center,boolean pressed){GradientDrawable result=center?new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{pressed?0xffce5885:0xffba426f,pressed?0xff668ffc:0xff535bcd}):new GradientDrawable();if(!center)result.setColor(Ui.dark(this)?pressed?0xff46526a:0xff2b3547:pressed?0xffd7e0fa:0xffe9edf6);result.setCornerRadius(Ui.dp(this,24));result.setStroke(Ui.dp(this,1),Ui.dark(this)?0xff56627c:0xffc6cfe1);return result;}
    private void row(LinearLayout parent,String[] labels,int[] codes){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,-2);line.topMargin=Ui.dp(this,14);parent.addView(row,line);for(int i=0;i<codes.length;i++){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,Ui.dp(this,66),1);params.setMargins(Ui.dp(this,4),0,Ui.dp(this,4),0);row.addView(key(labels[i],codes[i],false),params);}}
    private void enabled(boolean on){for(Button key:keys){key.setEnabled(on);key.setAlpha(on?1f:.45f);}}
    private void ui(int version,Runnable action){runOnUiThread(()->{if(visible&&!isDestroyed()&&version==generation)action.run();});}
    private JSONObject selected()throws Exception{JSONObject target=TvLink.load(this,"control_target");if(target==null)throw new IllegalArgumentException("Primeiro selecione a TV no CasaNotify.");return target;}
    private void connect(){
        if(busy)return;disconnect();busy=true;enabled(false);status.setText("Conectando ao controle da TV…");int version=generation;
        worker.execute(()->{try{
            JSONObject target=selected(),saved=TvLink.load(this,"android_remote");if(saved==null||!target.optString("device_id").equals(saved.optString("device_id"))){ui(version,()->status.setText("Toque em Vincular controle Android TV para o primeiro acesso."));return;}
            final AndroidRemote.Listener listener=(on,message)->ui(version,()->{enabled(on);status.setText(message);});AndroidRemote connection;
            try{connection=AndroidRemote.connect(target.getString("host"),saved.getString("fingerprint"),listener);}catch(javax.net.ssl.SSLException e){throw e;}catch(Exception failure){TvLink.connect(this,"control_target");target=selected();connection=AndroidRemote.connect(target.getString("host"),saved.getString("fingerprint"),listener);}
            if(!visible||generation!=version){connection.close();return;}AndroidRemote old=remote;remote=connection;if(old!=null)old.close();String label=target.optString("device_name","Minha TV");ui(version,()->{name.setText(label);enabled(true);status.setText("Conectado · vínculo salvo");});
        }catch(Exception e){ui(version,()->{enabled(false);status.setText(e instanceof javax.net.ssl.SSLException?"O certificado do controle mudou ou a autorização foi recusada. Confira a TV antes de vincular novamente.":e instanceof IllegalArgumentException?e.getMessage():"TV indisponível. Confira a rede e o Android TV Remote Service. Seu vínculo continua salvo.");});}finally{ui(version,()->busy=false);}});
    }
    private void pair(){
        if(busy)return;disconnect();busy=true;status.setText("Solicitando o PIN na tela da TV…");int version=generation;
        worker.execute(()->{try{TvLink.connect(this,"control_target");JSONObject target=selected();AndroidRemote.Pairing session=new AndroidRemote.Pairing(target.getString("host"));if(!visible||generation!=version){session.close();return;}pairing=session;ui(version,()->showPin(target,session,version));}catch(Exception e){ui(version,()->status.setText("Não foi possível abrir o pareamento. Mantenha a TV ligada e confira se o Android TV Remote Service está disponível."));}finally{ui(version,()->busy=false);}});
    }
    private void showPin(JSONObject target,AndroidRemote.Pairing session,int version){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(Ui.dp(this,20),Ui.dp(this,10),Ui.dp(this,20),Ui.dp(this,10));box.addView(Ui.text(this,"Digite o PIN que acabou de aparecer na Android TV. Pode conter números e letras de A a F.",16));EditText input=Ui.input(this,box,"PIN da Android TV",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(6)});
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Vincular controle remoto").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Vincular",null).create();pinDialog=dialog;dialog.setOnDismissListener(d->{session.close();if(pairing==session)pairing=null;if(pinDialog==dialog)pinDialog=null;});dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String code=input.getText().toString().trim();if(!code.matches("[0-9a-fA-F]{6}")){input.setError("Use os seis caracteres exibidos na TV");return;}dialog.getButton(-1).setEnabled(false);worker.execute(()->{try{String fingerprint=session.finish(code);if(!visible||generation!=version||pairing!=session)return;JSONObject current=selected();if(!current.optString("device_id").equals(target.optString("device_id")))throw new IllegalArgumentException("A TV selecionada mudou.");new SecretStore(this,"android_remote").write(new JSONObject().put("device_id",target.getString("device_id")).put("fingerprint",fingerprint).toString());ui(version,()->{dialog.dismiss();connect();});}catch(Exception e){ui(version,()->{dialog.getButton(-1).setEnabled(true);input.setError(e instanceof IllegalArgumentException?e.getMessage():"Pareamento expirou. Cancele e gere um novo PIN.");});}});}));dialog.show();
    }
    private void send(int code){AndroidRemote active=remote;if(active==null||!active.connected()){status.setText("Toque em Reconectar para continuar.");enabled(false);return;}int version=generation;worker.execute(()->{try{active.key(code);}catch(Exception e){ui(version,()->{enabled(false);status.setText("Comando não confirmado. Reconecte antes de tentar novamente.");});}});}
    private void disconnect(){if(pinDialog!=null)pinDialog.dismiss();AndroidRemote active=remote;remote=null;if(active!=null)active.close();AndroidRemote.Pairing pending=pairing;pairing=null;if(pending!=null)pending.close();enabled(false);}
    protected void onResume(){super.onResume();visible=true;busy=false;generation++;connect();}
    protected void onPause(){visible=false;generation++;disconnect();super.onPause();}
    protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
