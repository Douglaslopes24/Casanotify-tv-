package br.com.casanotify.tv;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Phone-only settings: one TV link, explicit notification consent, clear connection diagnostics. */
public final class PhoneActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences settings;
    private LinearLayout apps;
    private TextView tvState,deliveryState,permissionState,selectionState;
    private JSONObject target;
    private Switch enabled;
    private boolean refreshing;
    private final Runnable ticker=new Runnable(){public void run(){refresh();handler.postDelayed(this,2500);}};
    public void onCreate(Bundle b){
        Ui.theme(this);super.onCreate(b);settings=MirrorSender.settings(this);
        LinearLayout page=Ui.page(this,"Seu celular, sua TV");page.addView(Ui.text(this,"Tudo conectado. Do seu jeito.",16));
        LinearLayout tv=Ui.card(this,page,"Minha TV","O vínculo fica salvo neste celular. A troca de IP é recuperada automaticamente na mesma rede.");
        tvState=Ui.text(this,"Carregando vínculo…",17);tv.addView(tvState);
        Ui.button(this,tv,"Abrir o controle da TV",()->startActivity(new Intent(this,ControlActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        Ui.button(this,tv,"Testar conexão e enviar aviso",this::testConnection);
        LinearLayout notifications=Ui.card(this,page,"Avisos dos aplicativos","Você escolhe o que chega à TV, mesmo com a tela do CasaNotify fechada.");
        permissionState=Ui.text(this,"",14);notifications.addView(permissionState);
        Ui.button(this,notifications,"Acesso às notificações no Android",this::requestListenerAccess);
        enabled=toggle(notifications,"Enviar avisos para minha TV",settings.getBoolean("enabled",false));
        enabled.setOnCheckedChangeListener((v,on)->{
            if(refreshing)return;
            if(on&&(target==null||!Ui.accepted(this)||!MirrorSender.permitted(this))){refreshing=true;enabled.setChecked(false);refreshing=false;Ui.message(this,"Vamos conectar","Entre no controle da TV uma vez, aceite os termos e autorize o acesso às notificações no Android. Depois, escolha seus aplicativos e ative o envio.");return;}
            settings.edit().putBoolean("enabled",on).putLong("enabled_at",System.currentTimeMillis()).apply();if(on){notificationPermission();MirrorSender.foreground(this);}else{MirrorSender.clear(this);MirrorConnectionService.stop(this);}refresh();
        });
        Switch content=toggle(notifications,"Mostrar conteúdo das mensagens",settings.getBoolean("content",false));
        content.setOnCheckedChangeListener((v,on)->{settings.edit().putBoolean("content",on).apply();if(!on)MirrorSender.clear(this);});
        notifications.addView(Ui.text(this,"Desligado: aparece somente o nome do aplicativo. Ligado: pessoas próximas à TV podem ler a mensagem. Códigos e senhas são filtrados quando identificados.",14));
        LinearLayout delivery=Ui.card(this,page,"Avisos com o app fechado","Se a TV ficar indisponível, até 30 avisos aguardam uma nova tentativa. Avisos com mais de 10 minutos são descartados na próxima verificação.");
        deliveryState=Ui.text(this,"",15);delivery.addView(deliveryState);
        Ui.button(this,delivery,"Ativar conexão contínua",()->{notificationPermission();MirrorSender.foreground(this);refresh();Ui.toast(this,"Nova tentativa solicitada. Seu vínculo permanece salvo.");});
        Ui.button(this,delivery,"Bateria e início automático no Android",()->{try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception e){Ui.toast(this,"Abra as informações do CasaNotify nos ajustes do Android.");}});
        delivery.addView(Ui.text(this,"Ao ativar o envio, o CasaNotify mantém uma notificação de conexão com botão Pausar. Fechar a tela ou remover dos recentes não desliga esse serviço. O Android ainda pode adiar o envio por economia de bateria. Em Xiaomi/POCO, confira Início automático e Bateria sem restrições nas informações do aplicativo. Forçar parada interrompe o app até ele ser aberto novamente. Mantenha o Play Protect ativo; se o sistema negar o acesso às notificações, o espelhamento ficará indisponível.",14));
        LinearLayout selected=Ui.card(this,page,"Aplicativos autorizados","Somente os aplicativos marcados abaixo podem enviar avisos.");
        selectionState=Ui.text(this,"",14);selected.addView(selectionState);
        EditText search=Ui.input(this,selected,"Buscar aplicativo",android.text.InputType.TYPE_CLASS_TEXT);search.setHint("Nome do aplicativo");
        apps=new LinearLayout(this);apps.setOrientation(LinearLayout.VERTICAL);selected.addView(apps);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){filterApps(s.toString());}public void afterTextChanged(Editable s){}});
        loadApps();
        LinearLayout personal=Ui.card(this,page,"Seu espaço","Aparência e privacidade em um só lugar.");
        Ui.button(this,personal,"Tema · claro, escuro ou sistema",this::themeDialog);
        Ui.button(this,personal,"Termos e privacidade",()->Ui.termsDialog(this,!Ui.accepted(this),null));
        Ui.button(this,personal,"Autorizar novamente os avisos",()->new AlertDialog.Builder(this).setTitle("Renovar autorização de avisos?").setMessage("Use se você revogou a chave de avisos na TV. O controle precisa estar conectado à sua conta. Uma nova chave limitada será criada, sem repetir IP ou código.").setPositiveButton("Autorizar",(d,w)->repairLink()).setNegativeButton("Cancelar",null).show());
        Ui.button(this,personal,"Remover vínculo dos avisos",()->new AlertDialog.Builder(this).setTitle("Desligar e remover vínculo?").setMessage("O controle da TV continua disponível. Os avisos pendentes serão apagados. Você pode revogar a chave também em Conta e conexões.").setPositiveButton("Remover",(d,w)->{settings.edit().putBoolean("enabled",false).putBoolean("mirror_unlinked",true).apply();synchronized(TvLink.class){new SecretStore(this,"phone_target").delete();}MirrorSender.clear(this);MirrorConnectionService.stop(this);refresh();}).setNegativeButton("Cancelar",null).show());
        page.addView(Ui.text(this,"CasaNotify Celular · 2.4.0\nObrigado por conectar sua casa.",14));
        Ui.button(this,page,"Voltar ao controle",this::finish);
        if(!Ui.accepted(this))page.post(()->Ui.termsDialog(this,true,()->{if(settings.getBoolean("enabled",false))notificationPermission();MirrorSender.foreground(this);refresh();}));
        restoreLink();
    }
    private Switch toggle(LinearLayout box,String label,boolean checked){Switch control=new Switch(this);control.setText(label);control.setTextSize(16);control.setTextColor(Ui.dark(this)?0xffeef4ee:0xff172b20);control.setMinHeight(Ui.dp(this,64));control.setPadding(0,Ui.dp(this,8),0,Ui.dp(this,8));control.setChecked(checked);box.addView(control,new LinearLayout.LayoutParams(-1,-2));return control;}
    private void onUi(Runnable action){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())action.run();});}
    private void restoreLink(){worker.execute(()->{ControlSession session=new ControlSession(this);try{if(TvLink.load(this,"phone_target")==null&&!settings.getBoolean("mirror_unlinked",false))session.request("/auth/session","GET",null,"");}catch(Exception ignored){}finally{session.close();}onUi(this::refresh);});}
    private void repairLink(){worker.execute(()->{ControlSession session=new ControlSession(this);try{if(!session.repairMirroring())throw new Exception();MirrorSender.clear(this);settings.edit().putBoolean("mirror_unlinked",false).apply();onUi(()->{refresh();Ui.message(this,"Vínculo renovado","Ative o envio e escolha seus aplicativos. Obrigado por conectar seu celular.");});}catch(Exception e){onUi(()->Ui.message(this,"Entre no controle","Abra o controle da TV e entre na sua conta. Depois, volte aqui para autorizar os avisos."));}finally{session.close();}});}
    private void testConnection(){tvState.setText("Localizando minha TV e conferindo o vínculo…");worker.execute(()->{try{ControlClient client=TvLink.connect(this,"phone_target");JSONObject result=client.request("/api/notify","POST",new JSONObject().put("id","phone-test-"+System.currentTimeMillis()).put("title","Seu celular está conectado!").put("message","O vínculo foi salvo. Obrigado por usar o CasaNotify TV.").toString(),"");if(result.optInt("status")!=200)throw new Exception();onUi(()->{refresh();Ui.toast(this,"Teste entregue à TV.");});}catch(Exception e){onUi(()->{refresh();Ui.message(this,"Conexão",e instanceof TvLink.Revoked||e instanceof TvLink.Upgrade?e.getMessage():"Não foi possível entregar o teste. Confira a rede e o receptor da TV. Seu vínculo continua salvo.");});}});}
    private void loadApps(){worker.execute(()->{List<ResolveInfo> list=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);list.sort(Comparator.comparing(a->a.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT)));onUi(()->{Set<String> seen=new HashSet<>();for(ResolveInfo item:list){String pkg=item.activityInfo.packageName;if(pkg.equals(getPackageName())||!seen.add(pkg))continue;CheckBox check=new CheckBox(this);check.setText(item.loadLabel(getPackageManager()));check.setTextSize(16);check.setMinHeight(Ui.dp(this,54));check.setChecked(settings.getStringSet("apps",Collections.emptySet()).contains(pkg));check.setOnCheckedChangeListener((v,on)->{Set<String> selection=new HashSet<>(settings.getStringSet("apps",Collections.emptySet()));if(on)selection.add(pkg);else selection.remove(pkg);settings.edit().putStringSet("apps",selection).apply();if(!on)MirrorSender.clear(this);refresh();});apps.addView(check,new LinearLayout.LayoutParams(-1,-2));}if(seen.isEmpty())apps.addView(Ui.text(this,"Nenhum aplicativo visível. Use este recurso no celular.",16));});});}
    private void filterApps(String text){String query=text.trim().toLowerCase(Locale.ROOT);for(int i=0;i<apps.getChildCount();i++){android.view.View view=apps.getChildAt(i);if(view instanceof CheckBox)view.setVisibility(((CheckBox)view).getText().toString().toLowerCase(Locale.ROOT).contains(query)?android.view.View.VISIBLE:android.view.View.GONE);}}
    protected void onResume(){super.onResume();MirrorSender.foreground(this);handler.removeCallbacks(ticker);ticker.run();}
    protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
    private void refresh(){
        if(tvState==null)return;try{target=TvLink.load(this,"phone_target");}catch(Exception e){target=null;}
        tvState.setText(target==null?"Abra o controle e entre na sua conta para vincular os avisos.":target.optString("device_name","Minha TV")+"\nVínculo salvo · "+target.optString("host"));
        permissionState.setText(MirrorSender.permitted(this)?"✓ Acesso autorizado pelo Android":"Acesso às notificações ainda não autorizado");
        refreshing=true;enabled.setChecked(settings.getBoolean("enabled",false));refreshing=false;
        int selected=settings.getStringSet("apps",Collections.emptySet()).size();selectionState.setText(selected+" aplicativo"+(selected==1?" selecionado":"s selecionados"));
        String listener=(MirrorConnectionService.running?"Serviço contínuo ativo\n":"Serviço contínuo parado\n")+(!settings.getBoolean("enabled",false)?"Envio desligado":!MirrorSender.permitted(this)?"Acesso bloqueado pelo Android":MirrorSender.listenerConnected?"Pronto para receber novos avisos":"Aguardando o serviço de notificações do Android");
        long sent=settings.getLong("last_sent",0);deliveryState.setText(listener+"\n"+MirrorSender.pending(this)+" aviso(s) aguardando envio\n"+settings.getString("last_status","Nenhum envio realizado")+(sent>0?" · "+java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new Date(sent)):""));
    }
    private void notificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},2401);}
    private void requestListenerAccess(){if(!Ui.accepted(this)){Ui.termsDialog(this,true,this::requestListenerAccess);return;}new AlertDialog.Builder(this).setTitle("Permitir avisos na TV").setMessage("O acesso a notificações permite ler dados de outros aplicativos. O CasaNotify encaminha apenas os aplicativos escolhidos para a sua TV, por conexão protegida. O conteúdo começa oculto. Até 30 avisos podem ficar cifrados no celular enquanto aguardam envio; expiram após 10 minutos e são apagados na próxima verificação. Você pode desligar o envio ou revogar a permissão a qualquer momento.").setPositiveButton("Abrir ajustes do Android",(d,w)->{try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(Exception e){Ui.message(this,"Permissão","Abra o acesso a notificações nas configurações do Android.");}}).setNegativeButton("Agora não",null).show();}
    private void themeDialog(){String[] ids={"system","light","dark"};String current=new Prefs(this).config().optString("ui_theme","system");new AlertDialog.Builder(this).setTitle("Como você prefere?").setSingleChoiceItems(new String[]{"Seguir o sistema","Claro","Escuro"},Arrays.asList(ids).indexOf(current),(d,w)->{try{new Prefs(this).update(new JSONObject().put("ui_theme",ids[w]));d.dismiss();recreate();}catch(Exception e){Ui.toast(this,"Não foi possível salvar o tema.");}}).setNegativeButton("Cancelar",null).show();}
    protected void onDestroy(){handler.removeCallbacks(ticker);worker.shutdownNow();super.onDestroy();}
}
