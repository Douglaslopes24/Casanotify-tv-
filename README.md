# CasaNotify TV

Notificações personalizadas sobre a tela do Android TV, com integração própria para o Home Assistant e envio opcional de notificações do celular.

**Aplicativo 2.0.1 · Integração 2.0.0 · Android 8+ · Home Assistant 2026.9+ · 37 entidades por TV**

## Downloads

- [Aplicativo Android 2.0.1 — TV e celular](downloads/CasaNotify-TV-2.0.1.apk)
- [Integração Home Assistant 2.0.0](downloads/CasaNotify-TV-Integracao-HA-2.0.0.zip)
- [Código completo](https://github.com/Douglaslopes24/Casanotify-tv-/archive/refs/heads/main.zip)
- [Instalação passo a passo](docs/INSTALACAO.md)

Instale o APK sobre a versão anterior, sem apagar os dados. A assinatura foi preservada. Atualize também a integração: a versão 2 bloqueia comandos antigos por HTTP e pede nova confirmação do certificado da TV.

**Correção 2.0.1:** ajusta a chave e a seleção do certificado HTTPS no Android. Ao atualizar da 2.0.0, o certificado local muda uma vez: compare o novo SHA-256 na TV e aprove novamente no navegador, Home Assistant e celulares vinculados. Login, senha, autenticador e personalizações são mantidos. A integração permanece na versão 2.0.0. [Orientações para conexão e Play Protect](docs/INSTALACAO.md#se-algo-não-funcionar).

## Novidades

- Painel HTTPS com conta local, senha e código TOTP de aplicativo autenticador no celular. Não envia SMS.
- Cadastro com aceite dos termos e agradecimento com o nome escolhido; alteração de nome e senha; recuperação presencial pela TV.
- Câmeras RTSP ao vivo, até 12 perfis protegidos, vídeo sem áudio por padrão. Compatibilidade depende do aparelho, codec e câmera; prefira H.264.
- Seis toques, 21 ícones, cinco paletas, nove posições e ajuste de cores, transparência, tamanho, duração, voz e volume.
- Tema claro, escuro ou do sistema; animação ao iniciar; logo interno e fundo personalizados, com recorte, zoom, rotação e brilho no painel. O ícone instalado no launcher permanece o original.
- Espelhamento opcional de notificações de aplicativos escolhidos no celular Android, como WhatsApp e delivery. Permissão concedida pelo usuário; envio e conteúdo começam desativados.
- VPN IKEv2/IPsec integrada em Android 11+ compatível, com seu próprio servidor e autorização do Android. Acesso às VPNs já instaladas em aparelhos compatíveis.

## Vincular ao Home Assistant

1. Na TV, aceite os termos, autorize a sobreposição e ative o receptor.
2. No HACS, abra **Repositórios personalizados**, informe `https://github.com/Douglaslopes24/Casanotify-tv-` e selecione **Integração**. Baixe CasaNotify TV e reinicie o Home Assistant. Também há instalação manual pelo ZIP.
3. Em **Configurações → Dispositivos e serviços**, configure a TV descoberta. Se não aparecer, adicione **CasaNotify TV** e informe seu IPv4 e a porta de descoberta `8765`.
4. Confira o SHA-256 em **Perfil, tema, imagens e segurança → Segurança do painel** na TV. Confirme no Home Assistant somente se os valores forem iguais.
5. Gere o código em **Vincular Home Assistant** na TV e digite-o. As entidades serão criadas automaticamente.

A descoberta exige mDNS acessível e receptor ativo. Ela não instala a integração nem dispensa o primeiro vínculo. O projeto é independente e não é uma integração oficial do Home Assistant, HACS ou ASNO TV.

## Entidades

| Tipo | Quantidade | Funções |
|---|---:|---|
| Notificação | 1 | Avisos |
| Botão | 2 | Testar; limpar |
| Interruptor | 7 | Pausa; início automático; silêncio; som; voz; progresso; animação inicial |
| Seleção | 7 | Paleta; posição; ícone; animação do aviso; toque; fundo; tema do aplicativo |
| Número | 7 | Duração; largura; fonte; opacidade; cantos; margem; volume |
| Texto | 4 | Nome da TV e três cores |
| Horário | 2 | Início e fim do silêncio |
| Sensor binário | 5 | Conexão; sobreposição; aviso ativo; silêncio; VPN |
| Sensor | 2 | Fila e versão |

Use `notify.send_message` para mensagens simples ou `casanotify_tv.send_notification` para aparência, câmera e som. Selecione a entidade **Avisos** da sua TV. [Exemplos](docs/EXEMPLOS.md).

## Segurança e limites

HTTPS usa a porta `8766` e um certificado local por instalação. O navegador apresenta aviso no primeiro acesso: compare o SHA-256 com a TV antes de aceitar a exceção. Home Assistant e celular validam o certificado aprovado, sem encaminhar credenciais por redirecionamentos. A porta `8765` serve apenas para identificação pública e redirecionamento para o painel seguro.

A conta é de um proprietário local por TV. Senha usa PBKDF2-HMAC-SHA256 com 600.000 iterações; segredos, credenciais e câmeras ficam cifrados com Android Keystore. Sessões usam cookies Secure/HttpOnly/SameSite, proteção CSRF, 30 minutos de inatividade e limite absoluto de 12 horas. Códigos TOTP já usados são recusados. Cinco falhas bloqueiam login por cinco minutos. Não exponha o receptor diretamente à Internet.

Celulares recebem credenciais limitadas ao envio de avisos. O filtro adicional de palavras sensíveis é conservador e não garante detectar todo segredo; mantenha o conteúdo desativado quando não quiser mensagens na TV. RTSP pode trafegar sem criptografia: use rede confiável ou VPN. VPN não fornece servidor e pode impedir descoberta local. A TV precisa estar ligada, conectada e permitir sobreposição. Samsung Tizen e LG webOS não executam o APK diretamente.

## Desenvolvimento e validação

Código Android em `android/`; integração em `custom_components/casanotify_tv/`. Não há chaves privadas de assinatura no repositório. Veja [compilação](android/README.md), [API](android/API.md), [testes e limites da validação](docs/VALIDACAO.md) e [atualizações](CHANGELOG.md).

Validação da correção: 33 testes Java, incluindo TLS 1.2/1.3 com o gerenciador de certificados usado pelo app e os provedores JDK/Conscrypt. A integração e o painel têm os resultados da versão 2.0.0 registrados no relatório. APK compilado e assinatura verificada. **Ainda requer teste em TV física, câmera, celular e servidor VPN. Não há aprovação do Play Protect nesta entrega.**
