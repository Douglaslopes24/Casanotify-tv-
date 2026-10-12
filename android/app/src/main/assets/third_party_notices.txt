# Avisos de terceiros

O código próprio do CasaNotify permanece sob a licença MIT da raiz. Os MP3s são arquivos fornecidos pelo usuário; consulte `docs/SONS.json`.

## Interoperabilidade com Android TV Remote Service

`RemoteWire.java` e `AndroidRemote.java` são implementações Java do protocolo, adaptadas em 09/10/2026 para Android Keystore, validação de certificado fixado, tamanhos limitados e comandos permitidos. Não incorporam a biblioteca Python em tempo de execução.

Referências e material de protocolo usados sob Apache License 2.0:

- **androidtvremote2**, autores e contribuidores do projeto tronikos/androidtvremote2. Arquivos `pairing.py`, `remote.py`, `polo.proto` e `remotemessage.proto`: https://github.com/tronikos/androidtvremote2
- **Google TV Pairing Protocol (Polo)** — Copyright 2009 Google Inc. All Rights Reserved. https://android.googlesource.com/platform/external/google-tv-pairing-protocol/+/refs/heads/master/proto/polo.proto
- O arquivo Polo de referência registra sua alteração dos campos de `OuterMessage` com base em https://github.com/louis49/androidtv-remote/blob/main/src/pairing/pairingmessage.proto . Os números dos campos de Remote v2 foram conferidos no `remotemessage.proto` distribuído por androidtvremote2.

Texto integral da licença em `LICENSES/Apache-2.0.txt`; cópia também incluída nos APKs em `assets/licenses/Apache-2.0.txt`. Este aviso é incluído em `assets/third_party_notices.txt`. Não há endosso ou afiliação com Google, Android TV, Home Assistant ou os autores dessas referências.

## Reprodução RTSP e dependências Android da versão 2.5

Os APKs incluem AndroidX Media3 1.11.1 (Common, Container, DataSource, Decoder,
ExoPlayer, RTSP, Extractor e Database), do Android Open Source Project,
https://github.com/androidx/media, sob Apache License 2.0. Não incluem FFmpeg,
VLC ou decodificadores externos. Usam os MediaCodecs disponibilizados pela TV.

Dependências transitivas, também sob Apache License 2.0:

- AndroidX Annotation JVM 1.6.0, Annotation Experimental 1.3.1 e ExifInterface
  1.3.6 — Android Open Source Project, https://android.googlesource.com/platform/frameworks/support/.
- Guava 33.3.1-android, FailureAccess 1.0.2 e o artefato vazio ListenableFuture
  9999.0-empty-to-avoid-conflict-with-guava — Google e contribuidores,
  https://github.com/google/guava.
- Error Prone Annotations 2.28.0 — Google, https://github.com/google/error-prone.
- J2ObjC Annotations 3.0.0 — Google, https://github.com/google/j2objc.
- Kotlin Standard Library/Common 1.8.0 — JetBrains e contribuidores,
  https://github.com/JetBrains/kotlin/tree/v1.8.0. Aviso original em
  `LICENSES/Kotlin-NOTICE.txt`.
- JetBrains Annotations 13.0 — JetBrains, https://github.com/JetBrains/java-annotations.

Checker Framework Qualifiers 3.43.0, do Checker Framework, usa MIT.
Seu aviso e texto integral estão em `LICENSES/Checker-Framework-MIT.txt`,
extraídos de META-INF/LICENSE.txt do próprio JAR.

O POM publicado de JSR-305 3.0.2 identifica Apache License 2.0:
https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.pom.
O aviso do FindBugs identifica BSD de três cláusulas para a implementação de
referência. Esse aviso e o texto BSD também são incluídos, em
`LICENSES/JSR305-NOTICE.txt` e `LICENSES/JSR305-BSD-3-Clause.txt`.
Fonte: https://github.com/findbugsproject/findbugs/blob/master/findbugs/licenses/LICENSE-jsr305.txt.

As coordenadas, URLs oficiais, tamanhos e SHA-256 de todos os artefatos usados
pelo compilador manual constam em `android/runtime-deps.lock.json`. Os textos
em `LICENSES/` são distribuídos também em `assets/licenses/` nos APKs.
