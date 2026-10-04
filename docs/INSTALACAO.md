# Instalar CasaNotify 2.2.0

## 1. Preparar a TV

1. Instale **CasaNotify-TV-2.2.0.apk** no Android TV/TV Box (Android 8+), como atualização sobre a versão anterior, sem apagar dados.
2. Abra o app, leia e aceite os termos.
3. Autorize **Sobreposição** nas configurações do Android e volte ao app.
4. Toque em **Ativar receptor** e anote o **IP para o app de controle**.

Permissões do sistema, instalação e ativação do receptor exigem ação na própria TV. Depois, faça os ajustes de avisos pelo celular. TVs Samsung Tizen e LG webOS precisam de um dispositivo Android externo.

## 2. Primeiro acesso e ajustes pelo celular

1. Instale **CasaNotify-Controle-2.2.0.apk** e abra **CasaNotify Controle**.
2. Informe o IP da TV, sem `https://` nem porta. Os aparelhos precisam se alcançar pela rede local ou VPN.
3. Na TV, abra **Perfil → Identificação segura da TV**. Compare todos os grupos SHA-256 com os exibidos no celular. Em **Perfil → Vincular controle / criar conta**, gere o código e digite-o no celular. Ele aprova este controle uma vez e vale dois minutos.
4. Se já tem conta, entre com o usuário e a senha existentes. **Não há segundo fator nem código de autenticador no login.**
5. No primeiro cadastro, informe nome, usuário e senha de 12–128 caracteres, leia e aceite os termos.
6. O app mostra o agradecimento com seu nome e abre os ajustes: **Criar aviso, Aparência, Home Assistant, Câmeras RTSP, Minha marca, Conta e celulares, Atividade**.

Nenhuma dessas etapas abre Chrome ou outro navegador. O menu superior permite trocar a TV, ler termos, configurar a VPN do celular e consultar o espelhamento opcional. Fotos são escolhidas pelo seletor de imagens do Android. O app não pede acesso geral aos arquivos. A câmera RTSP é reproduzida na TV, não na prévia do controle.

A identificação da TV e a chave de vínculo ficam cifradas no celular. A senha não é salva no controle e a sessão fica apenas na memória: ao fechar/recriar o app, pode ser necessário entrar novamente. Cinco falhas de login bloqueiam tentativas por cinco minutos. Se esquecer a senha, redefina a conta presencialmente na TV; os dados de câmeras e o vínculo do Home Assistant permanecem.

### Qual APK instalar

| Edição | Aparelho | Função |
|---|---|---|
| TV | TV/TV Box | Receptor dos avisos e servidor local |
| Controle | Celular | Cadastro, login e todos os ajustes do painel; não lê notificações de outros apps |
| Celular (opcional) | Celular | Tudo do Controle e espelhamento de notificações escolhidas, mediante autorização do Android |

As edições usam o mesmo identificador e a mesma assinatura das versões anteriores: uma substitui a outra no mesmo aparelho. Instale TV na TV e Controle no celular. Se quiser espelhamento, substitua Controle por Celular no telefone; o vínculo de controle é preservado.

### Atualização sem perder a conta

Da versão 2.0.1/2.1.0 para 2.2.0, mantenha os dados: usuário, senha, câmeras, imagens e certificado são preservados. Atualize também a integração Home Assistant para **2.1.0** e o app Controle/Celular para **2.2.0**. Clientes antigos não conseguem enviar comandos à API 3.

Faça o novo vínculo do controle na TV, depois entre com sua senha existente. Quem já tinha Home Assistant vinculado mantém sua chave e as entidades ao atualizar a integração. Vínculos de espelhamento existentes também podem ser reutilizados pelo aplicativo atualizado. O segredo TOTP antigo continua removido automaticamente; não é necessário autenticador.
Quem atualiza da 2.0.0 também recebe a correção de HTTPS introduzida na 2.0.1: o certificado da TV muda uma vez. Confira o novo SHA-256 e reconfigure o vínculo existente do Home Assistant e dos celulares. Não exclua a integração nem crie outra TV; isso ajuda a preservar entidades e automações.

## 3. Home Assistant

Exige Home Assistant 2026.9 ou posterior. **Não é necessário criar conta no app de controle para vincular a TV ao Home Assistant.** O código correto é o gerado em **Vincular Home Assistant** na TV; a alternativa é a chave exibida em **Ver chave do Home Assistant**.

### Pelo HACS

1. HACS → menu → **Repositórios personalizados**.
2. Adicione `https://github.com/Douglaslopes24/Casanotify-tv-`, categoria **Integração**.
3. Procure CasaNotify TV, instale e reinicie o Home Assistant.

Não é necessário que o projeto esteja no catálogo padrão do HACS para adicioná-lo como repositório personalizado.

### Pelo ZIP

1. Extraia **CasaNotify-TV-Integracao-HA-2.1.0.zip**.
2. Copie a pasta `custom_components/casanotify_tv` para `/config/custom_components/casanotify_tv` do Home Assistant, substituindo a versão antiga.
3. Confira que existe `/config/custom_components/casanotify_tv/manifest.json` e reinicie o Home Assistant.

### Atualizar uma integração já instalada

No HACS, abra CasaNotify TV e baixe novamente a versão do repositório. Pela instalação manual, substitua a pasta pelo ZIP 2.1.0. Reinicie o Home Assistant e mantenha a integração existente para preservar entidades e automações. O `manifest.json` atualizado deve indicar `"version": "2.1.0"`.

Se a atualização do APK pedir nova autenticação, confira o certificado na TV e siga a solicitação do Home Assistant. Não apague a integração nem crie uma segunda TV para resolver a troca de certificado.

### Vinculação e atualização da versão 1.x

1. Vá a **Configurações → Dispositivos e serviços**. Configure a TV descoberta ou adicione **CasaNotify TV** manualmente.
2. No modo manual, use o IPv4 mostrado na TV, sem `https://`, e a porta **8765**. Ela é usada só na descoberta; os comandos irão por HTTPS na **8766**.
3. Compare o SHA-256 apresentado com o certificado exibido fisicamente na TV. Confirme que são iguais.
4. Na TV, abra **Vincular Home Assistant** e gere o código de seis números. Digite somente esse código ou, como alternativa, somente a chave do Home Assistant exibida na TV.
5. A integração cria **37 entidades**. Use o botão **Testar aviso**.

Instalações 1.x pedem reautenticação para aprovar o certificado. A identificação da TV é preservada se os dados do app forem mantidos; isso preserva os IDs das entidades antigas. Automações `rest_command` antigas por HTTP devem ser substituídas pelas ações desta integração. O APK 2.2 também recusa comandos com Bearer sem a prova de vínculo; use a integração atualizada.

## 4. Notificações do celular

1. Instale **CasaNotify-Celular-2.2.0.apk** no celular Android, somente se a instalação for permitida pelo sistema. Abra o app e aceite os termos.
2. Na TV: **Perfil → Vincular celular para avisos**.
3. No CasaNotify Celular, abra **Menu → Avisos dos aplicativos**. Informe o IP da TV, confira o certificado com a TV e digite o código de vínculo de celular.
4. Leia a explicação de acesso às notificações pelo botão do app e solicite a autorização do Android. Se o sistema negar a permissão, mantenha o envio desligado; ele não funcionará automaticamente sem esse acesso.
5. Escolha os aplicativos que podem enviar avisos e ative o envio. Se desejar, ative também **conteúdo das mensagens**.

Por padrão, a TV recebe apenas o nome do aplicativo e aviso genérico. Algumas notificações podem ser omitidas por restrições do Android, economia de bateria, mensagens secretas ou palavras sensíveis. A proteção do Android não é contornada. No painel **Conta e celulares**, revogue aparelhos que não devem continuar enviando avisos. O celular e a TV precisam conseguir se comunicar pela rede/VPN.

## 5. Câmeras, sons, imagem e temas

- **Câmeras RTSP:** dê um nome e informe a URL fornecida pela câmera. Use **Testar na TV**. Prefira H.264/substream. A compatibilidade de codec, credenciais e transporte depende do equipamento. Não há gravação. Copie o ID da câmera para automações; a URL protegida não é devolvida pela lista.
- **Minha marca:** envie PNG, JPEG ou WebP; ajuste recorte, posição, rotação e brilho; salve como logo interno ou fundo. O ícone instalado no launcher continua sendo o original.
- **Aparência:** escolha tema claro/escuro/sistema, animação inicial, toque padrão e fundo personalizado nos avisos. Salve.
- **Criar aviso:** selecione uma câmera salva ou imagem. Vídeo e foto são alternativas por aviso. Ative som para ouvir o toque escolhido. São 16 opções: 6 originais e os 10 MP3s recebidos. No Home Assistant, os novos IDs são `sound_01` até `sound_10`.

## 6. VPN

Na TV, abra **VPN e rede privada**. No controle, use **Menu → VPN deste celular**. A autorização e o perfil da VPN de cada aparelho são controlados pelo Android nesse aparelho. Para IKEv2 integrado, exige Android 11+ com suporte IPsec. Informe seu próprio servidor, identidade e usuário/senha ou PSK. Autorize a solicitação do Android. Servidores com certificado privado personalizado podem exigir um cliente externo; o formulário integrado usa a confiança do Android.

O botão **Abrir VPNs do Android** permite gerenciar VPNs já instaladas, conforme o fabricante. Não há servidor, assinatura nem túnel gratuito fornecido pelo CasaNotify. Se o mDNS não atravessar a VPN, use a vinculação manual por IP alcançável. A indicação de VPN ativa é a do Android; não testa o destino do seu túnel.

## Se algo não funcionar

- **“Sem resposta da TV” / `ERR_CONNECTION_CLOSED`:** abra o app Controle 2.2.0, confirme o IP atual mostrado na TV e o receptor ativo. Atualize também TV e integração. O navegador não tem mais painel de configuração; use **Menu → Trocar TV** no controle para refazer o vínculo quando necessário. A captura recebida não permite determinar sozinha a causa: pode envolver conectividade, receptor, protocolo ou demora na resposta. O login agora aceita até 60 segundos de resposta para derivar a senha em aparelhos lentos.
- **VPN ativa e painel inacessível:** confira se a VPN permite acesso à rede local e se celular e TV conseguem alcançar o mesmo endereço. Não exponha as portas no roteador para corrigir esse acesso local.
- **Google Play Protect / acesso negado:** consulte [a explicação dos avisos e das edições](SEGURANCA_ANDROID.md). Mantenha a proteção ativa. As edições TV e Controle não incluem o leitor de notificações; a edição Celular continua dependendo dessa permissão. Ainda é necessário verificar o comportamento de instalação nos seus aparelhos.
- Permissão de sobreposição: confirme em Configurações do Android → Aplicativos → Acesso especial.
- TV não descoberta: confirme receptor ativo, mesma rede e ausência de isolamento de clientes. Tente IP manual.
- Certificado mudou: confira na TV antes de vincular novamente. Não aprove valores desconhecidos.
- Login bloqueado: aguarde cinco minutos. Não é necessário código de autenticador. Um código de vínculo vencido deve ser gerado novamente na TV.
- Câmera não abre: confira URL, acesso à rede, autenticação e perfil H.264 suportado pelo aparelho.
- VPN não conecta: confira os dados do servidor e a autorização do Android.
- Alertas silenciosos: verifique pausa, horário silencioso, volume e saída de áudio da TV. Urgência ignora horário silencioso, mas respeita pausa.

Consulte `docs/VALIDACAO.md`: não houve teste desta versão em seus dispositivos físicos.
