package br.com.casanotify.tv;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.SSLEngine;

/** Selects the existing device key without exporting or replacing it. */
public final class DeviceKeyManager extends X509ExtendedKeyManager {
    private final String alias;
    private final PrivateKey key;
    private final X509Certificate certificate;

    public DeviceKeyManager(String alias, PrivateKey key, X509Certificate certificate) {
        this.alias = alias;
        this.key = key;
        this.certificate = certificate;
    }
    private String serverAlias(String keyType) {
        String algorithm=key.getAlgorithm();
        return keyType != null && (keyType.equals(algorithm)||keyType.startsWith(algorithm+"_")) ? alias : null;
    }
    @Override public String[] getClientAliases(String keyType, Principal[] issuers) { return null; }
    @Override public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) { return null; }
    @Override public String[] getServerAliases(String keyType, Principal[] issuers) {
        return serverAlias(keyType) == null ? null : new String[]{alias};
    }
    @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return serverAlias(keyType);
    }
    @Override public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return serverAlias(keyType);
    }
    @Override public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return null;
    }
    @Override public X509Certificate[] getCertificateChain(String requestedAlias) {
        return alias.equals(requestedAlias) ? new X509Certificate[]{certificate} : null;
    }
    @Override public PrivateKey getPrivateKey(String requestedAlias) {
        return alias.equals(requestedAlias) ? key : null;
    }
}
