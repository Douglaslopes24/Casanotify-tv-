package br.com.casanotify.tv;
import android.content.*;import android.security.keystore.*;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;import java.security.KeyStore;import java.nio.charset.StandardCharsets;import java.util.Base64;

/** AES-GCM with an Android Keystore key. No exportable key is stored in preferences. */
public final class SecretStore implements AccountManager.Store {
    private final SharedPreferences prefs;private final String field;
    public SecretStore(Context c,String field){prefs=c.getSharedPreferences("secure_casanotify",Context.MODE_PRIVATE);this.field=field;}
    private static synchronized javax.crypto.SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);String alias="CasaNotifyVault2";if(!ks.containsAlias(alias)){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());g.generateKey();}return(javax.crypto.SecretKey)ks.getKey(alias,null);}
    public String read()throws Exception{String v=prefs.getString(field,"");if(v.isEmpty())return "";String[] parts=v.split(":",2);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8);}
    public void write(String s)throws Exception{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());String v=Base64.getEncoder().encodeToString(cipher.getIV())+":"+Base64.getEncoder().encodeToString(cipher.doFinal(s.getBytes(StandardCharsets.UTF_8)));if(!prefs.edit().putString(field,v).commit())throw new java.io.IOException("Não foi possível salvar os dados protegidos.");}
    public void delete(){prefs.edit().remove(field).commit();}
    private static AccountManager account;
    public static synchronized AccountManager accounts(Context c){if(account==null)account=new AccountManager(new SecretStore(c.getApplicationContext(),"owner"));return account;}
}
