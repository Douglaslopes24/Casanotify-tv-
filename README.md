# CasaNotify TV

Notificações personalizadas sobre a tela do Android TV, com integração própria para o Home Assistant e envio opcional de notificações do celular.

**Aplicativos 2.1.0 · Integração 2.0.1 · Android 8+ · Home Assistant 2026.9+ · 37 entidades por TV**

## Downloads

- [CasaNotify TV 2.1.0 — instalar na TV/receptor](downloads/CasaNotify-TV-2.1.0.apk)
- [CasaNotify Controle 2.1.0 — cadastro, login e ajustes no celular](downloads/CasaNotify-Controle-2.1.0.apk)
- [CasaNotify Celular 2.1.0 — controle com espelhamento opcional](downloads/CasaNotify-Celular-2.1.0.apk)
- [Integração Home Assistant 2.0.1](downloads/CasaNotify-TV-Integracao-HA-2.0.1.zip)
- [Código completo](https://github.com/Douglaslopes24/Casanotify-tv-/archive/refs/heads/main.zip)
- [Instalação passo a passo](docs/INSTALACAO.md)

Escolha a edição para o aparelho e instale sobre a versão anterior, sem apagar os dados. As três mantêm a assinatura e o identificador originais; uma substitui a outra se instaladas no mesmo aparelho. A integração permanece em 2.0.1. Quem já usa o APK 2.0.1 mantém também o certificado HTTPS na atualização para 2.1.0.

**Aplicativos 2.1.0:** o primeiro cadastro, login e todos os ajustes do painel agora estão dentro do **CasaNotify Controle**, sem abrir navegador. O receptor continua na TV. O login usa somente usuário e senha; contas existentes preservam a senha e removem o segredo do autenticador na migração. O código inicial da TV apenas aprova a criação de uma conta.

TV e Controle não incluem leitor de notificações de outros aplicativos. A edição Celular adiciona o espelhamento opcional e continua sujeita às restrições do Android/Play Protect. Nenhuma edição tem aprovação garantida do Google. [Entenda os avisos das capturas](docs/SEGURANCA_ANDROID.md).

**Integração 2.0.1:** trata respostas incompletas da TV, permite diagnóstico antes da primeira conexão e esclarece a vinculação. A revisão verifica as 37 entidades, a atualização de IP sem duplicação e a aprovação de novo certificado sem mudar os IDs das entidades.

## Novidades

- Aplicativo de controle com cadastro e login por usuário/senha, conectado por HTTPS à TV. Sem autenticação de dois fatores nem SMS.
- Cadastro com aceite dos termos e agradecimento com o nome escolhido; alteração de nome e senha; recuperação presencial pela TV.
- Câmeras RTSP ao vivo, até 12 perfis protegidos, vídeo sem áudio por padrão. Compatibilidade depende do aparelho, codec e câmera; prefira H.264.
- Seis toques, 21 ícones, cinco paletas, nove posições e ajuste de cores, transparência, tamanho, duração, voz e volume.
- Tema claro, escuro ou do sistema; animação ao iniciar; logo interno e fundo personalizados, com recorte, zoom, rotação e brilho no aplicativo de controle. O ícone instalado no launcher permanece o original.
- Espelhamento opcional na edição CasaNotify Celular, para aplicativos escolhidos, como WhatsApp e delivery. Depende da autorização de acesso às notificações no Android; envio e conteúdo começam desativados em uma instalação nova.
- VPN IKEv2/IPsec integrada em Android 11+ compatível, com seu próprio servidor e autorização do Android. Acesso às VPNs já instaladas em aparelhos compatíveis.

## Vincular ao Home Assistant

1. Na TV, aceite os termos, autorize a sobreposição e ative o receptor.
2. No HACS, abra **Repositórios personalizados**, informe `https://github.com/Douglaslopes24/Casanotify-tv-` e selecione **Integração**. Baixe CasaNotify TV e reinicie o Home Assistant. Também há instalação manual pelo ZIP.
3. Em **Configurações → Dispositivos e serviços**, configure a TV descoberta. Se não aparecer, adicione **CasaNotify TV** e informe seu IPv4 e a porta de descoberta `8765`.
4. Confira o SHA-256 em **Perfil, tema, imagens e segurança → Identificação segura da TV** na TV. Confirme no Home Assistant somente se os valores forem iguais.
5. Gere o código em **Vincular Home Assistant** na TV e digite-o. As entidades serão criadas automaticamente.

A descoberta exige mDNS acessível e receptor ativo. Ela não instala a integração nem dispensa o primeiro vínculo. O projeto é independente e não é uma integração oficial do Home Assistant, HACS ou ASNO TV.

**O Home Assistant não exige cadastro no app de controle.** Use o código gerado em **Vincular Home Assistant** na TV ou a chave em **Ver chave do Home Assistant**. O código inicial de cadastro e o código de espelhamento do celular têm finalidades diferentes.

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

O controle usa a porta HTTPS `8766` e valida o certificado da TV aprovado no primeiro vínculo. A tela é empacotada no APK, em uma WebView sem navegação externa; somente o transporte Android se comunica com a TV. Redirecionamentos e identidades diferentes são recusados. A porta `8765` identifica publicamente a TV, sem receber senhas. Home Assistant conserva a API 2.

A conta é de um proprietário local por TV. Senha usa PBKDF2-HMAC-SHA256 com 600.000 iterações; segredos, credenciais e câmeras ficam cifrados com Android Keystore. Sessões usam cookies Secure/HttpOnly/SameSite, proteção CSRF, 30 minutos de inatividade e limite absoluto de 12 horas. O app de controle guarda a sessão somente na memória; pede login novamente ao ser recriado. A atualização remove os segredos TOTP antigos, mantendo o hash da senha e o bloqueio de tentativas. Cinco falhas bloqueiam login por cinco minutos. Não exponha o receptor diretamente à Internet.

O espelhamento recebe credenciais limitadas ao envio de avisos; o app Controle usa a sessão do proprietário para os ajustes. O filtro adicional de palavras sensíveis é conservador e não garante detectar todo segredo; mantenha o conteúdo desativado quando não quiser mensagens na TV. RTSP pode trafegar sem criptografia: use rede confiável ou VPN. VPN não fornece servidor e pode impedir descoberta local. A TV precisa estar ligada, conectada e permitir sobreposição. Samsung Tizen e LG webOS não executam o APK diretamente.

## Desenvolvimento e validação

Código Android em `android/`; integração em `custom_components/casanotify_tv/`. Não há chaves privadas de assinatura no repositório. Veja [compilação](android/README.md), [API](android/API.md), [testes e limites da validação](docs/VALIDACAO.md) e [atualizações](CHANGELOG.md).

Validação da integração 2.0.1: **58 testes Python aprovados com Home Assistant 2026.9.3**, incluindo HTTPS real em localhost com certificados RSA e EC P-256, e análise Ruff. Os aplicativos 2.1.0 passaram em **37 testes Java**, incluindo migração sem TOTP e TLS real com rejeição de certificado incorreto antes do envio de senha. O painel passou nos testes DOM pelo transporte comum e pelo canal do aplicativo, sem `fetch` de rede. Os três APKs tiveram versão, assinatura, alinhamento e permissões inspecionados. [Resultados e limites](docs/VALIDACAO.md). **Ainda requer teste em TV física, câmera, celular e servidor VPN. Não há aprovação do Play Protect nesta entrega.**
