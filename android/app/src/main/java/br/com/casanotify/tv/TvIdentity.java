package br.com.casanotify.tv;
import org.json.JSONObject;
/** Saved approval cannot be replaced by a LAN advertisement. */
public final class TvIdentity {
    public static boolean same(JSONObject saved,JSONObject verified){String id=saved.optString("device_id"),pin=saved.optString("tls_fingerprint");return !id.isEmpty()&&pin.matches("[0-9a-f]{64}")&&id.equals(verified.optString("device_id"))&&AuthCrypto.same(pin,verified.optString("tls_fingerprint"));}
    public static boolean sameCredential(JSONObject saved,JSONObject current){return saved!=null&&current!=null&&same(saved,current)&&!saved.optString("token").isEmpty()&&AuthCrypto.same(saved.optString("token"),current.optString("token"));}
}
