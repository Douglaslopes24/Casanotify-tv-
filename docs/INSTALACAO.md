# Instalar CasaNotify 2.5.0

## Atualizar sem perder o vínculo

Instale **CasaNotify-TV-2.5.0.apk** na TV e **CasaNotify-Celular-2.5.0.apk** no telefone, como atualização, sem apagar os dados. Para usar somente o controle, há **CasaNotify-Controle-2.5.0.apk**, sem leitor de notificações. As três edições usam o mesmo identificador e assinatura: uma substitui a outra no mesmo aparelho.

Atualize a integração Home Assistant para **2.3.0** e reinicie o HA, mantendo a integração existente. As 37 entidades, seus IDs, a conta, o certificado e os vínculos das versões 2.2/2.3/2.4 são preservados. Abra os apps. Os termos foram atualizados em 12/10/2026 para o controle remoto opcional e a câmera manual. Confira o aceite solicitado na TV e nos ajustes de avisos. Na TV, deixe o receptor ativo; no celular, confira os ajustes de avisos para ativar o serviço contínuo.

A 2.3.0 verifica a identidade da TV antes de salvar um IP descoberto. Se um anúncio falso tiver alterado o endereço na versão anterior e a recuperação automática não ocorrer, use **Reconfigurar** na integração existente e informe o IP exibido na TV. Confira o certificado na própria TV; não desative sua verificação.

## 1. TV receptora

1. Instale a edição **TV**, abra e aceite os termos.
2. Autorize **Sobreposição** no Android e volte ao CasaNotify.
3. Toque em **Ativar receptor**. Celular, TV e Home Assistant devem conseguir se comunicar pela rede.
4. Se desejar, ative **Iniciar após ligar a TV**.

Permissões e primeira ativação exigem ação na própria TV. Android 8+ é necessário. Samsung Tizen e LG webOS precisam de um aparelho Android externo.

## 2. Primeiro vínculo e login do celular

1. Instale e abra a edição **Celular** ou **Controle**.
2. Aguarde a busca automática e selecione sua TV. Há o botão **Manual** se mDNS estiver bloqueado.
3. Compare o SHA-256 mostrado com **Perfil → Identificação segura da TV** na TV.
4. Gere o código em **Perfil → Vincular controle / criar conta** e digite no celular. Esse vínculo é feito uma vez.
5. Crie sua conta ou entre com usuário e senha existentes. Deixe **Manter conectado neste celular** marcado para acesso automático; a senha não fica salva. Não há segundo fator ou SMS.

O início contém **Criar aviso, Aparência, Controle remoto e Atividades**. No menu **⋮ → Configurações**, ficam Home Assistant, Personalização, Conta e conexões, Câmeras RTSP e demais ajustes. A TV não serve painel ao navegador. Fechar a tela ou perder a rede não apaga o vínculo. O IP é recuperado por descoberta autenticada; sair da conta, trocar senha ou revogar o controle exige novo login.

## 3. Controle remoto pelo CasaNotify

1. Atualize e mantenha o receptor CasaNotify ativo na TV.
2. Na própria TV, toque em **Autorizar controle remoto**, leia a explicação e ative o serviço opcional nas configurações de acessibilidade do Android.
3. No aplicativo já vinculado e autenticado, abra **Controle remoto**. Use setas, OK, Voltar, Início, volume, Mudo e Reproduzir/pausar.

Não há outro IP, PIN ou pareamento com Android TV Remote Service. Os comandos usam o mesmo vínculo protegido e a sessão da conta. A autorização local permite consultar o foco da janela ativa e executar os comandos disponíveis; o serviço não coleta textos, capturas de tela nem processa eventos de acessibilidade. Desative-o na TV para revogar essa capacidade.

Alguns aplicativos e TV Boxes não expõem o foco/ações necessários; setas e OK podem não funcionar nessas telas. A saída de áudio também pode limitar volume e mudo. Não há comando de ligar uma TV desligada.

## 4. Avisos com o aplicativo fechado

Na edição **Celular**:

1. Faça o primeiro login do controle. Ele também cria a chave limitada dos avisos, sem outro IP ou código.
2. Abra **⋮ → Notificações dos aplicativos** e aceite os termos atualizados.
3. Autorize o acesso às notificações nas configurações do Android e escolha quais aplicativos podem enviar avisos.
4. Ative o envio e permita a notificação do serviço. Por padrão, o conteúdo das mensagens é ocultado.
5. No cartão **Avisos com o app fechado**, toque em **Ativar conexão contínua** se o serviço estiver parado. Confira a notificação **CasaNotify · avisos para a TV**.
6. Feche a tela do CasaNotify ou remova-a dos recentes e envie uma notificação de um aplicativo escolhido. O serviço é independente dessa tela. A notificação permanente tem um botão **Pausar avisos**.

No Xiaomi/POCO/HyperOS e em outros fabricantes, use o botão **Bateria e início automático no Android** e ajuste as opções de início automático e bateria permitidas pelo seu aparelho, se necessário. O aplicativo não altera essas escolhas sozinho.

**Forçar parada**, parar o app no gerenciador do Android, revogar a permissão ou certas restrições do fabricante interrompem o envio. Abra novamente o aplicativo para retomá-lo. Não há funcionamento garantido contra essas ações do sistema.

A fila cifrada comporta até 30 avisos recentes, com prazo de dez minutos. Ao reconectar o leitor, notificações ainda ativas e posteriores à ativação do envio podem ser recuperadas; confirmações cifradas evitam reenvio de avisos já entregues. Notificações removidas durante uma interrupção não podem ser recuperadas. Veja serviço, pendências e último envio no cartão de diagnóstico. A TV precisa estar ligada, com receptor ativo e acessível pela rede.

Avisos secretos, permanentes, resumos e conteúdo sensível identificado pelo filtro são omitidos. Se o Android negar acesso, o espelhamento não funciona; o app respeita essa restrição.

## 5. Home Assistant 2.3.0

Requer Home Assistant 2026.9+. No **HACS → Repositórios personalizados**, adicione `https://github.com/Douglaslopes24/Casanotify-tv-`, categoria **Integração**. Baixe CasaNotify TV e reinicie o HA.

Para instalação manual, extraia **CasaNotify-TV-Integracao-HA-2.3.0.zip**, substitua `/config/custom_components/casanotify_tv` e reinicie. O `manifest.json` deve indicar `"version": "2.3.0"`. Não exclua a integração existente para atualizar.

No primeiro vínculo:

1. Em **Configurações → Dispositivos e serviços**, configure a TV descoberta ou adicione CasaNotify TV pelo IPv4 e porta **8765**.
2. Confira o SHA-256 com a identificação exibida fisicamente na TV.
3. Na TV, gere o código em **Vincular Home Assistant** e digite no HA; a chave em **Ver chave do Home Assistant** é uma alternativa.
4. Confira as 37 entidades e use **Testar aviso**.

Não é necessário criar conta CasaNotify para vincular o HA. Os comandos usam HTTPS na 8766. Atualizações antigas da 2.0.0 podem pedir nova aprovação do certificado; confirme fisicamente e reconfigure a entrada existente para preservar IDs.

## 6. Câmeras RTSP pelo Home Assistant

1. Configure sua câmera RTSP/ONVIF no **Home Assistant**.
2. Na automação do evento, escolha **CasaNotify TV: Mostrar câmera na TV**.
3. Selecione a entidade **Avisos** da TV e a entidade `camera.*` da câmera.
4. Ajuste mensagem, duração, posição, som e áudio do vídeo. Execute a ação para testar.

O Home Assistant resolve a fonte RTSP e envia o aviso diretamente à TV; o celular pode estar fechado. A TV reproduz a câmera, sem gravação, proxy novo ou transcodificação. Ela precisa alcançar o endereço da fonte e suportar seu codec; prefira substream H.264. Câmeras que forneçam somente HLS/WebRTC não atendem a essa ação RTSP.

Na ação, ajuste **Decodificação**, **Transporte RTSP** e **Buffer** se necessário. Automático usa os codecs disponíveis na TV e permite uma tentativa com software após erro de decodificação de vídeo. Software não instala um codec novo. Automações antigas com `camera_id` continuam aceitas pelo HA; perfis anteriores são preservados. [Exemplos de ação e automação](EXEMPLOS.md).

## 7. Câmera manual separada

1. No celular, abra **⋮ → Câmeras RTSP**.
2. Salve nome e endereço `rtsp://...`; as credenciais ficam protegidas na TV e não são retornadas na lista.
3. Selecione a câmera e toque em **Exibir na TV**. A janela contém somente vídeo e **Liberar**; mensagens de conexão/erro aparecem quando necessário.
4. Toque em **Liberar** no celular ou no botão da janela usando o controle físico. O botão Voltar na janela também a fecha.

A janela é independente dos avisos: limpar ou pausar notificações não a fecha. Encerrar o receptor fecha a câmera. A TV deve permitir sobreposição e alcançar a câmera. RTSP pode não ter criptografia; use uma rede confiável. H.264, H.265, H.263, MPEG-4 Visual, VP8 e VP9 têm leitores RTP, mas o perfil de vídeo precisa ser compatível com os codecs da TV; prefira H.264 para maior compatibilidade.

## 8. Personalização e VPN

**⋮ → Personalização** permite editar logo interno e fundo com imagens escolhidas no seletor Android. **Aparência** reúne temas, animação, posições e toques. **Criar aviso** aceita texto e imagem; ative Som para ouvir um dos 16 toques, identificados como Som 1 a Som 16. O ícone instalado no launcher permanece o original.

A configuração de **VPN do CasaNotify está oculta e desativada** nesta versão. VPNs externas do Android não são removidas nem desligadas. O sensor de estado no HA continua existindo para preservar automações; uma VPN externa pode bloquear descoberta/acesso local.

## Se algo não funcionar

- **TV indisponível:** confira receptor, rede e energia. Use **⋮ → Minha TV e conexão → Manual** se mDNS estiver bloqueado. Para a mesma identidade e certificado, o vínculo é reaproveitado.
- **Controle indisponível:** confira o login do CasaNotify, o receptor ativo e a autorização local em **Autorizar controle remoto** na TV. Não há PIN adicional. Algumas telas não oferecem ações compatíveis.
- **Avisos param em segundo plano:** confira acesso às notificações, serviço contínuo ativo, aplicativos escolhidos e ajustes de bateria/início automático do fabricante. Forçar parada exige reabrir o app.
- **Câmera indisponível:** confirme a fonte RTSP da entidade no HA, alcance pela TV, credenciais e codec.
- **Certificado mudou:** compare na TV antes de aprovar novamente.
- **Avisos revogados:** entre na conta e use **Autorizar novamente os avisos**. A revogação não é contornada automaticamente.
- **Play Protect / acesso negado:** veja [Segurança Android](SEGURANCA_ANDROID.md). Mantenha as proteções do sistema; nenhuma edição tem aprovação garantida do Google.

Consulte [a validação](VALIDACAO.md): esta versão ainda precisa de teste nos seus aparelhos físicos.
