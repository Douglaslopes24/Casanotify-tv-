package br.com.casanotify.tv;
import android.app.Activity;import android.content.Intent;import android.os.Bundle;
/** Compatibility entry point: all commands now share the CasaNotify session. */
public final class RemoteActivity extends Activity { @Override public void onCreate(Bundle state){super.onCreate(state);startActivity(new Intent(this,ControlActivity.class));finish();} }
