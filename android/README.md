# Aplicativo Android — CasaNotify TV 2.0.0

Aplicativo nativo Java com receptor Android TV, painel HTTPS, conta local/TOTP, câmeras RTSP, personalização, notificações do celular e VPN IKEv2 em aparelhos compatíveis.

## Compilar

JDK 17, SDK Platform 35 e Build Tools 35.0.0, na pasta `android`:

```sh
python3 tools/build_apk.py --sdk /caminho/Android/Sdk --jdk /caminho/jdk17 --signing-properties /caminho/privado/keystore.properties
```

`storeFile` é resolvido a partir da pasta do arquivo de propriedades. Sem a assinatura original, o script cria outra chave; o APK resultante não atualiza a instalação anterior. O projeto público não contém chaves privadas.

Não há bibliotecas de terceiros em tempo de execução no APK. O projeto Gradle pode ser aberto no Android Studio; a compilação efetivamente validada nesta entrega foi feita pelo script com ferramentas oficiais do SDK.

## Testes

Na raiz do repositório:

```sh
python3 android/tools/test_core.py --jdk /caminho/jdk17 --libs /pasta/com/jars
cd android/tests
npm install
npm test
```

A pasta de JARs precisa conter `junit.jar` (JUnit 4.13.2), `hamcrest.jar` (Hamcrest Core 1.3) e `json.jar` (org.json 20240303). O script gera e descarta um certificado exclusivo de teste.

Teste visual opcional: instale os navegadores com `npx playwright install chromium` e execute `npm run test:browser` em ambiente que permita iniciar Chromium. Nesta entrega esse teste foi bloqueado pelo ambiente; veja [o relatório](../docs/VALIDACAO.md).

Leia [instalação](../docs/INSTALACAO.md) e [API](API.md). A API 2 exige HTTPS para comandos e novo vínculo ao atualizar da API 1.
