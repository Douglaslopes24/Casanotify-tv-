# Aplicativos Android — CasaNotify 2.4.0

Receptor Android TV em Java e app de controle com interface empacotada em WebView, HTTPS com certificado fixado e conta local por usuário/senha, câmeras RTSP comandadas pelo Home Assistant, personalização, notificações do celular e controle nativo Android TV Remote Service. A configuração de VPN está desativada.

## Compilar

JDK 17, SDK Platform 35 e Build Tools 35.0.0, na pasta `android`:

```sh
python3 tools/build_apk.py --variant tv --sdk /caminho/Android/Sdk --jdk /caminho/jdk17 --signing-properties /caminho/privado/keystore.properties
python3 tools/build_apk.py --variant control --sdk /caminho/Android/Sdk --jdk /caminho/jdk17 --signing-properties /caminho/privado/keystore.properties
python3 tools/build_apk.py --variant phone --sdk /caminho/Android/Sdk --jdk /caminho/jdk17 --signing-properties /caminho/privado/keystore.properties
```

`storeFile` é resolvido a partir da pasta do arquivo de propriedades. Sem a assinatura original, o script cria outra chave; o APK resultante não atualiza a instalação anterior. O projeto público não contém chaves privadas.

Não há bibliotecas de terceiros em tempo de execução no APK. As referências Apache 2.0 do protocolo do controle estão em `../THIRD_PARTY_NOTICES.md`, com licença e aviso incluídos nos APKs. O projeto Gradle pode ser aberto no Android Studio; a compilação efetivamente validada nesta entrega foi feita pelo script com ferramentas oficiais do SDK.

`tv` é a edição padrão. O manifesto comum declara apenas rede e configurações compartilhadas. Cada edição acrescenta seus componentes em `app/src/tv`, `app/src/control` ou `app/src/phone`. As classes `PhoneActivity`, `PhoneApi` e `PhoneNotificationService` existem somente no source set `phone`; o código do listener não entra nos APKs TV e Controle. `app/src/companion` contém o controle comum às edições Controle/Celular. Gradle oferece as variantes `tvRelease`, `controlRelease` e `phoneRelease`.

As edições preservam `br.com.casanotify.tv` e a assinatura original para atualizar instalações existentes. São alternativas por aparelho, não aplicativos que coexistem nele. A edição Celular continua declarando explicitamente o serviço de notificações e permanece sujeita às restrições do Android/Play Protect.

## Testes

Na raiz do repositório:

```sh
python3 android/tools/test_core.py --jdk /caminho/jdk17 --libs /pasta/com/jars
cd android/tests
npm install
npm test
```

A pasta de JARs precisa conter `junit.jar` (JUnit 4.13.2), `hamcrest.jar` (Hamcrest Core 1.3), `json.jar` (org.json 20240303) e `conscrypt.jar` (`org.conscrypt:conscrypt-openjdk-uber:2.5.2`, disponível no Maven Central). São dependências exclusivas de teste; não entram no APK. O script gera e descarta um certificado EC P-256 de teste e verifica TLS 1.2/1.3 com JDK e Conscrypt. A opção `--add-opens` do comando Java permite o funcionamento dessa versão do Conscrypt no JDK 17; não altera o aplicativo Android.

Confira também os artefatos assinados, incluindo permissões, componentes, ausência das classes de espelhamento na edição TV, versão, alinhamento e certificado de assinatura:

```sh
python3 android/tools/verify_apks.py --build-tools /caminho/Android/Sdk/build-tools/35.0.0 --jdk /caminho/jdk17 --tv android/build/CasaNotify-TV-2.4.0.apk --control android/build/CasaNotify-Controle-2.4.0.apk --phone android/build/CasaNotify-Celular-2.4.0.apk --expected-certificate b132bd2e9d9dd5db7e338f0179b16c1a13eefd2e84e34ee93bfac063b57b48fc
```

Teste visual opcional: instale os navegadores com `npx playwright install chromium` e execute `npm run test:browser` em ambiente que permita iniciar Chromium. Nesta entrega esse teste foi bloqueado pelo ambiente; veja [o relatório](../docs/VALIDACAO.md).

Leia [instalação](../docs/INSTALACAO.md) e [API](API.md). A API 3 exige HTTPS fixado e prova da chave vinculada por comando. Atualize também a integração HA para 2.2.0; veja as instruções de migração.
