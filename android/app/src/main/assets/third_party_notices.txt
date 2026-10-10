# Avisos de terceiros

O código próprio do CasaNotify permanece sob a licença MIT da raiz. Os MP3s são arquivos fornecidos pelo usuário; consulte `docs/SONS.json`.

## Interoperabilidade com Android TV Remote Service

`RemoteWire.java` e `AndroidRemote.java` são implementações Java do protocolo, adaptadas em 09/10/2026 para Android Keystore, validação de certificado fixado, tamanhos limitados e comandos permitidos. Não incorporam a biblioteca Python em tempo de execução.

Referências e material de protocolo usados sob Apache License 2.0:

- **androidtvremote2**, autores e contribuidores do projeto tronikos/androidtvremote2. Arquivos `pairing.py`, `remote.py`, `polo.proto` e `remotemessage.proto`: https://github.com/tronikos/androidtvremote2
- **Google TV Pairing Protocol (Polo)** — Copyright 2009 Google Inc. All Rights Reserved. https://android.googlesource.com/platform/external/google-tv-pairing-protocol/+/refs/heads/master/proto/polo.proto
- O arquivo Polo de referência registra sua alteração dos campos de `OuterMessage` com base em https://github.com/louis49/androidtv-remote/blob/main/src/pairing/pairingmessage.proto . Os números dos campos de Remote v2 foram conferidos no `remotemessage.proto` distribuído por androidtvremote2.

Texto integral da licença em `LICENSES/Apache-2.0.txt`; cópia também incluída nos APKs em `assets/licenses/Apache-2.0.txt`. Este aviso é incluído em `assets/third_party_notices.txt`. Não há endosso ou afiliação com Google, Android TV, Home Assistant ou os autores dessas referências.
