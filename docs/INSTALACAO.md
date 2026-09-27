# Instalar CasaNotify TV 2.0.1

## 1. Aplicativo na TV

1. Baixe **CasaNotify-TV-2.0.1.apk** em `downloads` do repositório e transfira para seu Android TV/TV Box. Exige Android 8 ou posterior.
2. Autorize a instalação desse arquivo no Android. Instale sobre a versão anterior, sem desinstalar nem apagar os dados.
3. Abra CasaNotify TV e leia/aceite os termos.
4. Toque em **Autorizar sobreposição**, conceda a permissão do Android e volte.
5. Toque em **Ativar receptor**. Mantenha a TV conectada à rede.

O mesmo APK serve para TV e celular. No celular usado apenas para enviar notificações, não é necessário ativar o receptor nem a sobreposição.

### Atualizar da 2.0.0

A versão 2.0.1 corrige a configuração da chave HTTPS e cria um novo certificado local. A assinatura do APK continua a mesma; instale como atualização, sem apagar os dados. Conta, senha, autenticador, câmeras e personalizações continuam salvos.

Após atualizar, ative o receptor e confira o novo SHA-256 em **Perfil → Segurança do painel / certificado**. No navegador, examine o novo certificado antes de aceitá-lo. No Home Assistant, conclua a reautenticação solicitada; se necessário, use **Reconfigurar** na integração existente, confirme o novo certificado e gere outro código na TV. Não exclua a integração. Nos celulares que enviam avisos, repita a vinculação e a conferência do certificado. Use o pacote revisado da integração Home Assistant 2.0.1.

## 2. Criar login seguro no navegador

1. Na TV, abra **Perfil, tema, imagens e segurança → Segurança do painel** e consulte o SHA-256 do certificado.
2. No celular ou computador da mesma rede, abra exatamente o endereço `https://IP_DA_TV:8766` mostrado no app.
3. Por ser um certificado local, o navegador pode mostrar um aviso. Veja os detalhes do certificado e compare o SHA-256 completo com o da TV. Aceite a exceção somente se for igual. Se seu navegador não permitir conferir ou aceitar o certificado, use outro navegador que ofereça essa opção. Não desative globalmente a validação de certificados.
4. Na TV, gere o **código para criar conta**. Ele vale por dois minutos, uma vez, e é diferente do código de Home Assistant/celular.
5. No painel, informe o código, seu nome, usuário e uma senha de 12 a 128 caracteres. Leia e aceite os termos.
6. No aplicativo autenticador de sua preferência, adicione a chave mostrada, com código baseado em tempo: TOTP, seis dígitos, 30 segundos.
7. Digite o código gerado para concluir. O agradecimento usa o nome escolhido.

A conta fica na TV. Não há cadastro em nuvem nem SMS. Um código usado não pode ser reutilizado: aguarde o próximo. Mantenha data/hora automáticas na TV e no celular. Se perder senha ou autenticador, redefina a conta presencialmente em Perfil na TV e faça outro cadastro. Isso encerra sessões do navegador. Não muda automaticamente a chave do Home Assistant nem revoga celulares.

## 3. Home Assistant

Exige Home Assistant 2026.9 ou posterior. **Não é necessário criar usuário, senha ou autenticador no navegador para vincular a TV ao Home Assistant.** O código correto é o gerado em **Vincular Home Assistant** na TV; a alternativa é a chave exibida em **Ver chave do Home Assistant**.

### Pelo HACS

1. HACS → menu → **Repositórios personalizados**.
2. Adicione `https://github.com/Douglaslopes24/Casanotify-tv-`, categoria **Integração**.
3. Procure CasaNotify TV, instale e reinicie o Home Assistant.

Não é necessário que o projeto esteja no catálogo padrão do HACS para adicioná-lo como repositório personalizado.

### Pelo ZIP

1. Extraia **CasaNotify-TV-Integracao-HA-2.0.1.zip**.
2. Copie a pasta `custom_components/casanotify_tv` para `/config/custom_components/casanotify_tv` do Home Assistant, substituindo a versão antiga.
3. Confira que existe `/config/custom_components/casanotify_tv/manifest.json` e reinicie o Home Assistant.

### Atualizar uma integração já instalada

No HACS, abra CasaNotify TV e baixe novamente a versão do repositório. Pela instalação manual, substitua a pasta pelo ZIP 2.0.1. Reinicie o Home Assistant e mantenha a integração existente para preservar entidades e automações. O `manifest.json` atualizado deve indicar `"version": "2.0.1"`.

Se a atualização do APK pedir nova autenticação, confira o certificado na TV e siga a solicitação do Home Assistant. Não apague a integração nem crie uma segunda TV para resolver a troca de certificado.

### Vinculação e atualização da versão 1.x

1. Vá a **Configurações → Dispositivos e serviços**. Configure a TV descoberta ou adicione **CasaNotify TV** manualmente.
2. No modo manual, use o IPv4 mostrado na TV, sem `https://`, e a porta **8765**. Ela é usada só na descoberta; os comandos irão por HTTPS na **8766**.
3. Compare o SHA-256 apresentado com o certificado exibido fisicamente na TV. Confirme que são iguais.
4. Na TV, abra **Vincular Home Assistant** e gere o código de seis números. Digite somente esse código ou, como alternativa, somente a chave do Home Assistant exibida na TV.
5. A integração cria **37 entidades**. Use o botão **Testar aviso**.

Instalações 1.x pedem reautenticação para aprovar o certificado. A identificação da TV é preservada se os dados do app forem mantidos; isso preserva os IDs das entidades antigas. Automações `rest_command` antigas por HTTP devem ser substituídas pelas ações desta integração. O APK 2.0 recusa comandos HTTP.

## 4. Notificações do celular

1. Instale o mesmo APK no celular Android. Abra o app e aceite os termos.
2. Na TV: **Perfil → Vincular celular para avisos**.
3. No celular: **Celular: enviar notificações**. Informe o IP da TV, confira o certificado com a TV e digite o código de vínculo de celular.
4. Abra a permissão de acesso às notificações pelo botão do app e autorize CasaNotify TV no Android.
5. Escolha os aplicativos que podem enviar avisos e ative o envio. Se desejar, ative também **conteúdo das mensagens**.

Por padrão, a TV recebe apenas o nome do aplicativo e aviso genérico. Algumas notificações podem ser omitidas por restrições do Android, economia de bateria, mensagens secretas ou palavras sensíveis. A proteção do Android não é contornada. No painel **Conta e celulares**, revogue aparelhos que não devem continuar enviando avisos. O celular e a TV precisam conseguir se comunicar pela rede/VPN.

## 5. Câmeras, sons, imagem e temas

- **Câmeras RTSP:** dê um nome e informe a URL fornecida pela câmera. Use **Testar na TV**. Prefira H.264/substream. A compatibilidade de codec, credenciais e transporte depende do equipamento. Não há gravação. Copie o ID da câmera para automações; a URL protegida não é devolvida pela lista.
- **Minha marca:** envie PNG, JPEG ou WebP; ajuste recorte, posição, rotação e brilho; salve como logo interno ou fundo. O ícone instalado no launcher continua sendo o original.
- **Aparência:** escolha tema claro/escuro/sistema, animação inicial, toque padrão e fundo personalizado nos avisos. Salve.
- **Criar aviso:** selecione uma câmera salva ou imagem. Vídeo e foto são alternativas por aviso. Ative som para ouvir o toque escolhido.

## 6. VPN

Na TV ou celular, abra **VPN e rede privada**. Para IKEv2 integrado, exige Android 11+ com suporte IPsec. Informe seu próprio servidor, identidade e usuário/senha ou PSK. Autorize a solicitação do Android. Servidores com certificado privado personalizado podem exigir um cliente externo; o formulário integrado usa a confiança do Android.

O botão **Abrir VPNs do Android** permite gerenciar VPNs já instaladas, conforme o fabricante. Não há servidor, assinatura nem túnel gratuito fornecido pelo CasaNotify. Se o mDNS não atravessar a VPN, use a vinculação manual por IP alcançável. A indicação de VPN ativa é a do Android; não testa o destino do seu túnel.

## Se algo não funcionar

- **`ERR_CONNECTION_CLOSED` no navegador:** confirme APK 2.0.1 e receptor ativo. Digite o endereço completo, por exemplo `https://192.168.0.20:8766`, usando o IP atual mostrado na TV. A porta 8766 exige HTTPS; abrir `http://` nessa porta pode encerrar a conexão. Esse erro do navegador, sozinho, não identifica um bloqueio do Play Protect.
- **VPN ativa e painel inacessível:** confira se a VPN permite acesso à rede local e se celular e TV conseguem alcançar o mesmo endereço. Não exponha as portas no roteador para corrigir esse acesso local.
- **Google Play Protect:** mantenha a proteção ativa e registre a mensagem exata, indicando se aparece na TV ou no celular. O Google distingue pedido de verificação de um bloqueio de instalação; permissões sensíveis, incluindo acesso às notificações, podem motivar bloqueios em APKs baixados pela Internet. Se oferecer verificação, solicite a análise. Se indicar aplicativo nocivo ou bloqueado para proteção, interrompa a instalação e envie a captura para investigação. A assinatura do APK, esta correção HTTPS e o código publicado não comprovam aprovação pelo Google. [Orientação oficial](https://developers.google.com/android/play-protect/warning-dev-guidance?hl=pt-br).
- Permissão de sobreposição: confirme em Configurações do Android → Aplicativos → Acesso especial.
- TV não descoberta: confirme receptor ativo, mesma rede e ausência de isolamento de clientes. Tente IP manual.
- Certificado mudou: confira na TV antes de vincular novamente. Não aprove valores desconhecidos.
- Login bloqueado: aguarde cinco minutos. Para código inválido, confira relógios e espere novo código.
- Câmera não abre: confira URL, acesso à rede, autenticação e perfil H.264 suportado pelo aparelho.
- VPN não conecta: confira os dados do servidor e a autorização do Android.
- Alertas silenciosos: verifique pausa, horário silencioso, volume e saída de áudio da TV. Urgência ignora horário silencioso, mas respeita pausa.

Consulte `docs/VALIDACAO.md`: não houve teste desta versão em seus dispositivos físicos.
