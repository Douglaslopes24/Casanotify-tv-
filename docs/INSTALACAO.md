# Instalar CasaNotify 2.4.1

## Atualizar sem perder o vínculo

Instale **CasaNotify-TV-2.4.1.apk** na TV e **CasaNotify-Celular-2.4.1.apk** no telefone, como atualização, sem apagar os dados. Para usar somente o controle, há **CasaNotify-Controle-2.4.1.apk**, sem leitor de notificações. As três edições usam o mesmo identificador e assinatura: uma substitui a outra no mesmo aparelho.

Atualize a integração Home Assistant para **2.2.1** e reinicie o HA, mantendo a integração existente. As 37 entidades, seus IDs, a conta, o certificado e os vínculos das versões 2.2/2.3/2.4 são preservados. Abra os apps. Os termos permanecem na versão de 09/10/2026; quem já os aceitou na 2.4.0 não recebe novo aceite por esta correção. Na TV, deixe o receptor ativo; no celular, confira os ajustes de avisos para ativar o serviço contínuo.

A 2.2.1 verifica a identidade da TV antes de salvar um IP descoberto. Se um anúncio falso tiver alterado o endereço na versão anterior e a recuperação automática não ocorrer, use **Reconfigurar** na integração existente e informe o IP exibido na TV. Confira o certificado na própria TV; não desative sua verificação.

## 1. TV receptora

1. Instale a edição **TV**, abra e aceite os termos.
2. Autorize **Sobreposição** no Android e volte ao CasaNotify.
3. Toque em **Ativar receptor**. Celular, TV e Home Assistant devem conseguir se comunicar pela rede.
4. Se desejar, ative **Iniciar após ligar a TV**.

Permissões e primeira ativação exigem ação na própria TV. Android 8+ é necessário. Samsung Tizen e LG webOS precisam de um aparelho Android externo.

## 2. Primeiro vínculo e login do celular

1. Instale e abra a edição **Celular** ou **Controle**.
2. Aguarde a busca automática e selecione sua TV. Há **Informar IP manualmente** se mDNS estiver bloqueado.
3. Compare o SHA-256 mostrado com **Perfil → Identificação segura da TV** na TV.
4. Gere o código em **Perfil → Vincular controle / criar conta** e digite no celular. Esse vínculo é feito uma vez.
5. Crie sua conta ou entre com usuário e senha existentes. Deixe **Manter conectado neste celular** marcado para acesso automático; a senha não fica salva. Não há segundo fator ou SMS.

Os ajustes ficam no aplicativo: Criar aviso, Aparência, Home Assistant, Minha marca, Conta e conexões e Atividade. A TV não serve painel ao navegador. Fechar a tela ou perder a rede não apaga o vínculo. O IP é recuperado por descoberta autenticada; sair da conta, trocar senha ou revogar o controle exige novo login.

## 3. Controle remoto Android TV

1. Toque em **Remoto**, na barra superior do aplicativo do celular.
2. Toque em **Vincular controle**. Mantenha TV e celular na mesma rede e o receptor CasaNotify ativo.
3. Digite os **seis caracteres** que aparecem na tela da Android TV; podem conter letras de A a F.
4. Use setas, OK, Voltar, Início, volume, Mudo, reprodução e Espera. O controle se reconecta ao reabrir essa tela.

Esse é o pareamento do **Android TV Remote Service**, diferente do código CasaNotify e independente do login da conta. É feito uma vez e fica salvo no celular. Não exige ADB, root ou serviço de acessibilidade. Algumas TV Boxes com Android comum não incluem o serviço; o controle não funciona nelas sem suporte do aparelho. A saída de áudio pode limitar o controle de volume.

**Esquecer este controle** remove o vínculo local. Para revogá-lo também na TV, use os ajustes da própria Android TV. O vínculo nativo é independente da conta e dos avisos CasaNotify.

## 4. Avisos com o aplicativo fechado

Na edição **Celular**:

1. Faça o primeiro login do controle. Ele também cria a chave limitada dos avisos, sem outro IP ou código.
2. Abra **Ajustes → Avisos dos aplicativos** e aceite os termos atualizados.
3. Autorize o acesso às notificações nas configurações do Android e escolha quais aplicativos podem enviar avisos.
4. Ative o envio e permita a notificação do serviço. Por padrão, o conteúdo das mensagens é ocultado.
5. No cartão **Avisos com o app fechado**, toque em **Ativar conexão contínua** se o serviço estiver parado. Confira a notificação **CasaNotify · avisos para a TV**.
6. Feche a tela do CasaNotify ou remova-a dos recentes e envie uma notificação de um aplicativo escolhido. O serviço é independente dessa tela. A notificação permanente tem um botão **Pausar avisos**.

No Xiaomi/POCO/HyperOS e em outros fabricantes, use o botão **Bateria e início automático no Android** e ajuste as opções de início automático e bateria permitidas pelo seu aparelho, se necessário. O aplicativo não altera essas escolhas sozinho.

**Forçar parada**, parar o app no gerenciador do Android, revogar a permissão ou certas restrições do fabricante interrompem o envio. Abra novamente o aplicativo para retomá-lo. Não há funcionamento garantido contra essas ações do sistema.

A fila cifrada comporta até 30 avisos recentes, com prazo de dez minutos. Ao reconectar o leitor, notificações ainda ativas e posteriores à ativação do envio podem ser recuperadas; confirmações cifradas evitam reenvio de avisos já entregues. Notificações removidas durante uma interrupção não podem ser recuperadas. Veja serviço, pendências e último envio no cartão de diagnóstico. A TV precisa estar ligada, com receptor ativo e acessível pela rede.

Avisos secretos, permanentes, resumos e conteúdo sensível identificado pelo filtro são omitidos. Se o Android negar acesso, o espelhamento não funciona; o app respeita essa restrição.

## 5. Home Assistant 2.2.1

Requer Home Assistant 2026.9+. No **HACS → Repositórios personalizados**, adicione `https://github.com/Douglaslopes24/Casanotify-tv-`, categoria **Integração**. Baixe CasaNotify TV e reinicie o HA.

Para instalação manual, extraia **CasaNotify-TV-Integracao-HA-2.2.1.zip**, substitua `/config/custom_components/casanotify_tv` e reinicie. O `manifest.json` deve indicar `"version": "2.2.1"`. Não exclua a integração existente para atualizar.

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

O cadastro e o disparo de RTSP foram retirados do aplicativo do celular. Automações antigas com `camera_id` continuam aceitas pelo HA; perfis anteriores são preservados. [Exemplos de ação e automação](EXEMPLOS.md).

## 7. Personalização e VPN

**Minha marca** permite editar logo interno e fundo com imagens escolhidas no seletor Android. **Aparência** reúne temas, animação, posições e toques. **Criar aviso** aceita texto e imagem; ative Som para ouvir um dos 16 toques. O ícone instalado no launcher permanece o original.

A configuração de **VPN do CasaNotify está oculta e desativada** nesta versão. VPNs externas do Android não são removidas nem desligadas. O sensor de estado no HA continua existindo para preservar automações; uma VPN externa pode bloquear descoberta/acesso local.

## Se algo não funcionar

- **TV indisponível:** confira receptor, rede e energia. Use **Ajustes → Minha TV e conexão → Informar IP manualmente** se mDNS estiver bloqueado. Para a mesma identidade e certificado, o vínculo é reaproveitado.
- **Controle não pareia:** confira Android TV Remote Service na TV, mesma rede e PIN atual. PIN do CasaNotify não funciona no controle nativo.
- **Avisos param em segundo plano:** confira acesso às notificações, serviço contínuo ativo, aplicativos escolhidos e ajustes de bateria/início automático do fabricante. Forçar parada exige reabrir o app.
- **Câmera indisponível:** confirme a fonte RTSP da entidade no HA, alcance pela TV, credenciais e codec.
- **Certificado mudou:** compare na TV antes de aprovar novamente.
- **Avisos revogados:** entre na conta e use **Autorizar novamente os avisos**. A revogação não é contornada automaticamente.
- **Play Protect / acesso negado:** veja [Segurança Android](SEGURANCA_ANDROID.md). Mantenha as proteções do sistema; nenhuma edição tem aprovação garantida do Google.

Consulte [a validação](VALIDACAO.md): esta versão ainda precisa de teste nos seus aparelhos físicos.
