# CasaNotify TV

Notificações personalizadas sobre a tela do Android TV, com integração própria para o Home Assistant e envio opcional de notificações do celular.

**Aplicativos 2.5.0 · Integração 2.3.0 · Android 8+ · Home Assistant 2026.9+ · 37 entidades por TV**

## Downloads

- [CasaNotify TV 2.5.0 — instalar na TV/receptor](downloads/CasaNotify-TV-2.5.0.apk)
- [CasaNotify Controle 2.5.0 — cadastro, login e ajustes no celular](downloads/CasaNotify-Controle-2.5.0.apk)
- [CasaNotify Celular 2.5.0 — controle com espelhamento opcional](downloads/CasaNotify-Celular-2.5.0.apk)
- [Integração Home Assistant 2.3.0](downloads/CasaNotify-TV-Integracao-HA-2.3.0.zip)
- [Código completo](https://github.com/Douglaslopes24/Casanotify-tv-/archive/refs/heads/main.zip)
- [Instalação passo a passo](docs/INSTALACAO.md)

Instale a edição TV no receptor e **Celular** no telefone para controlar a TV e espelhar avisos. A edição Controle é uma alternativa sem leitura de notificações. As três edições mantêm o identificador e a assinatura originais; uma substitui a outra no mesmo aparelho. Atualize sem apagar os dados. Certificado HTTPS, senha, câmeras, identidade e vínculos existentes são preservados ao atualizar da 2.2.0, sem apagar dados.

**Atualize também a integração para 2.3.0 para corrigir a recuperação do endereço da TV contra anúncios falsos.** A API 3 exige prova criptográfica da chave vinculada em cada comando. Os clientes antigos deixam de comandar a TV. No app Controle, faça um vínculo inicial em **Perfil → Vincular controle / criar conta** na TV; depois use seu usuário e senha existentes e mantenha **Manter conectado** marcado. Os próximos acessos restauram a sessão automaticamente, sem segundo fator ou SMS. O Home Assistant atualizado reaproveita sua chave existente.

O primeiro cadastro, login e todos os ajustes ficam dentro do aplicativo de controle. A TV **não serve mais painel nem tela de login ao navegador**. Os 10 MP3s enviados foram adicionados aos 6 toques existentes.
TV e Controle não incluem leitor de notificações de outros aplicativos. A edição Celular adiciona o espelhamento opcional e continua sujeita às restrições do Android/Play Protect. Nenhuma edição tem aprovação garantida do Google. [Entenda os avisos das capturas](docs/SEGURANCA_ANDROID.md).

**Integração 2.3.0:** ação de câmera por entidade do HA, HTTPS com certificado aprovado, comandos autenticados com desafio de uso único e 16 opções de toque. As 37 entidades e seus identificadores permanecem estáveis.

## Novidades da versão 2.5.0 / HA 2.3.0

- **Início organizado:** Criar aviso, Aparência, Controle remoto e Atividades. O menu **⋮** reúne Home Assistant, Personalização, Conta e conexões, câmeras e demais configurações.
- **Controle pelo vínculo CasaNotify:** setas, OK, Voltar, Início, volume, mudo e reprodução usam o acesso já salvo. Na TV, autorize uma vez o serviço opcional em **Autorizar controle remoto**. Não há outro PIN nem pareamento com Android TV Remote Service. Algumas telas não aceitam navegação por acessibilidade.
- **RTSP separado no aplicativo:** cadastre a câmera e abra uma janela só com vídeo e o botão **Liberar**. Fechar essa janela não limpa a fila de avisos. As automações de câmera continuam pelo Home Assistant.
- **Mais formatos de vídeo:** Media3 1.11.1, leitores RTSP/RTP para H.264, H.265, H.263, MPEG-4 Visual, VP8 e VP9. A reprodução depende do perfil da câmera e dos decodificadores da TV; não há transcodificação nem FFmpeg. Opções automática/hardware/software, transporte TCP/automático e buffer de 250–3000 ms.
- **Som 1 até Som 16:** nomes sequenciais no aplicativo e na integração, preservando IDs das automações e os dez MP3s originais.
- **Continuidade preservada:** descoberta automática, recuperação autenticada do IP, login automático autorizado sem salvar senha, fila cifrada e serviço de avisos independente da tela do app. Forçar parada ou restrições do Android podem interromper o serviço.
- **Segurança:** HTTPS com certificado aprovado, provas HMAC com desafio de uso único, sessões/CSRF e limites de rede da 2.4.1 mantidos. Controle remoto exige chave de controle e sessão; a chave limitada de espelhamento não controla a TV nem acessa câmeras.
- VPN do aplicativo continua oculta e desativada. Tema claro/escuro/sistema, ícones, posições, imagem interna, fundo e animações permanecem disponíveis.

Conta local por usuário e senha, sem segundo fator/SMS; um proprietário por TV. O primeiro cadastro inclui termos e agradecimento personalizado. A edição **Celular** permite escolher os aplicativos cujos avisos serão enviados, mediante autorização Android. Envio e conteúdo começam desativados em uma instalação nova.

[O que foi validado e o que ainda depende de aparelhos físicos](docs/VERSAO_2_5.md).

## Vincular ao Home Assistant

1. Na TV, aceite os termos, autorize a sobreposição e ative o receptor.
2. No HACS, abra **Repositórios personalizados**, informe `https://github.com/Douglaslopes24/Casanotify-tv-` e selecione **Integração**. Baixe CasaNotify TV e reinicie o Home Assistant. Também há instalação manual pelo ZIP.
3. Em **Configurações → Dispositivos e serviços**, configure a TV descoberta. Se não aparecer, adicione **CasaNotify TV** e informe seu IPv4 e a porta de descoberta `8765`.
4. Confira o SHA-256 em **Perfil, tema, imagens e segurança → Identificação segura da TV** na TV. Confirme no Home Assistant somente se os valores forem iguais.
5. Gere o código em **Vincular Home Assistant** na TV e digite-o. As entidades serão criadas automaticamente.

A descoberta exige mDNS acessível e receptor ativo. Ela não instala a integração nem dispensa o primeiro vínculo. O projeto é independente e não é uma integração oficial do Home Assistant, HACS ou ASNO TV.

**O Home Assistant não exige cadastro no app de controle.** Use o código gerado em **Vincular Home Assistant** na TV ou a chave em **Ver chave do Home Assistant**. Os códigos de vínculo do controle e de espelhamento do celular têm finalidades diferentes.

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

Use `notify.send_message` para mensagens simples, `casanotify_tv.send_notification` para aparência e som, ou `casanotify_tv.send_camera_notification` para uma câmera do Home Assistant. Selecione a entidade **Avisos** da sua TV. [Exemplos](docs/EXEMPLOS.md).

## Segurança e limites

O controle usa a porta HTTPS `8766` e valida o certificado da TV aprovado no primeiro vínculo. A tela é empacotada no APK, em uma WebView sem navegação externa; somente o transporte Android se comunica com a TV. Redirecionamentos e identidades diferentes são recusados. A porta `8765` identifica publicamente a TV, sem receber senhas. Os comandos da API 3 exigem HMAC-SHA256 com uma chave vinculada e um desafio de uso único. O HTTP não redireciona nem recebe comandos. Apenas identificação, emissão de desafio e pareamento inicial são públicos; comandos, login, dados e imagens exigem vínculo.

A conta é de um proprietário local por TV. Senha usa PBKDF2-HMAC-SHA256 com 600.000 iterações; segredos, credenciais e câmeras ficam cifrados com Android Keystore. Sessões usam cookies Secure/HttpOnly/SameSite, proteção CSRF, 30 minutos de inatividade e limite absoluto de 12 horas. O app guarda a sessão somente na memória; se Manter conectado foi autorizado, uma credencial cifrada e exclusiva desse controle restaura o acesso após fechar ou reiniciar. Sair da conta, trocar a senha ou revogar o controle cancela esse acesso. A atualização remove os segredos TOTP antigos, mantendo o hash da senha e o bloqueio de tentativas. Cinco falhas bloqueiam login por cinco minutos. Não exponha o receptor diretamente à Internet.

O espelhamento recebe credenciais limitadas ao envio de avisos. Cada controle tem sua própria chave, além da sessão do proprietário vinculada a esse controle. Os vínculos podem ser revogados. A proteção comprova posse da chave; não identifica de forma infalível o programa que a usa caso um aparelho ou chave seja comprometido. O filtro adicional de palavras sensíveis é conservador e não garante detectar todo segredo; mantenha o conteúdo desativado quando não quiser mensagens na TV. RTSP pode trafegar sem criptografia: use rede confiável ou VPN. Uma VPN externa pode impedir descoberta local; o recurso de VPN do app está oculto. A TV precisa estar ligada, conectada e permitir sobreposição. Samsung Tizen e LG webOS não executam o APK diretamente.

## Desenvolvimento e validação

Código Android em `android/`; integração em `custom_components/casanotify_tv/`. Não há chaves privadas de assinatura no repositório. Veja [compilação](android/README.md), [API](android/API.md), [testes e limites da validação](docs/VALIDACAO.md) e [atualizações](CHANGELOG.md).

Consulte [o relatório de segurança da versão 2.4.1](docs/SEGURANCA_2_4_1.md) e [os resultados de validação](docs/VALIDACAO.md). Os testes de ataque foram feitos em processos e servidores de teste locais, sem acessar a TV do usuário. Ainda é necessário testar instalação, controle remoto, segundo plano, áudio, sobreposição e câmera nos aparelhos físicos. Não há garantia de aprovação pelo Play Protect.
