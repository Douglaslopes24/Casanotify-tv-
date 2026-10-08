# CasaNotify TV

Notificações personalizadas sobre a tela do Android TV, com integração própria para o Home Assistant e envio opcional de notificações do celular.

**Aplicativos 2.3.0 · Integração 2.1.0 · Android 8+ · Home Assistant 2026.9+ · 37 entidades por TV**

## Downloads

- [CasaNotify TV 2.3.0 — instalar na TV/receptor](downloads/CasaNotify-TV-2.3.0.apk)
- [CasaNotify Controle 2.3.0 — cadastro, login e ajustes no celular](downloads/CasaNotify-Controle-2.3.0.apk)
- [CasaNotify Celular 2.3.0 — controle com espelhamento opcional](downloads/CasaNotify-Celular-2.3.0.apk)
- [Integração Home Assistant 2.1.0](downloads/CasaNotify-TV-Integracao-HA-2.1.0.zip)
- [Código completo](https://github.com/Douglaslopes24/Casanotify-tv-/archive/refs/heads/main.zip)
- [Instalação passo a passo](docs/INSTALACAO.md)

Instale a edição TV no receptor e **Celular** no telefone para controlar a TV e espelhar avisos. A edição Controle é uma alternativa sem leitura de notificações. As três edições mantêm o identificador e a assinatura originais; uma substitui a outra no mesmo aparelho. Atualize sem apagar os dados. Certificado HTTPS, senha, câmeras, identidade e vínculos existentes são preservados ao atualizar da 2.2.0, sem apagar dados.

**A integração permanece em 2.1.0; se já usa essa versão, não precisa reinstalá-la.** A API 3 exige prova criptográfica da chave vinculada em cada comando. Os clientes antigos deixam de comandar a TV. No app Controle, faça um vínculo inicial em **Perfil → Vincular controle / criar conta** na TV; depois use seu usuário e senha existentes e mantenha **Manter conectado** marcado. Os próximos acessos restauram a sessão automaticamente, sem segundo fator ou SMS. O Home Assistant atualizado reaproveita sua chave existente.

O primeiro cadastro, login e todos os ajustes ficam dentro do aplicativo de controle. A TV **não serve mais painel nem tela de login ao navegador**. Os 10 MP3s enviados foram adicionados aos 6 toques existentes.
TV e Controle não incluem leitor de notificações de outros aplicativos. A edição Celular adiciona o espelhamento opcional e continua sujeita às restrições do Android/Play Protect. Nenhuma edição tem aprovação garantida do Google. [Entenda os avisos das capturas](docs/SEGURANCA_ANDROID.md).

**Integração 2.1.0:** conexão HTTPS com certificado aprovado, comandos autenticados com desafio de uso único e 16 opções de toque. As 37 entidades e seus identificadores permanecem estáveis.
## Novidades da 2.3.0

- Busca automática da TV por mDNS e recuperação do IP após mudanças na rede, conferindo o certificado e a identidade já aprovados.
- Primeiro vínculo preservado e acesso automático com autorização cifrada. A senha não é salva.
- O primeiro login na edição Celular vincula também os avisos, sem outro IP ou código. Permissão e seleção dos aplicativos continuam sob seu controle.
- Entrega independente da tela aberta, com fila cifrada de até 30 avisos, expiração de 10 minutos e novas tentativas por `JobScheduler`. Respeita restrições e permissões do Android.
- Ajustes do celular organizados em Minha TV, Avisos, Conexão em segundo plano, Aplicativos autorizados e Seu espaço; busca por aplicativo e diagnóstico de pendências.
- Painel com cartões, campos maiores e navegação adaptada ao celular.

## Recursos

- Aplicativo de controle com cadastro e login por usuário/senha, conectado por HTTPS à TV. Sem autenticação de dois fatores nem SMS.
- Cadastro com aceite dos termos e agradecimento com o nome escolhido; alteração de nome e senha; recuperação presencial pela TV.
- Câmeras RTSP ao vivo, até 12 perfis protegidos, vídeo sem áudio por padrão. Compatibilidade depende do aparelho, codec e câmera; prefira H.264.
- Dezesseis toques, 21 ícones, cinco paletas, nove posições e ajuste de cores, transparência, tamanho, duração, voz e volume.
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

Use `notify.send_message` para mensagens simples ou `casanotify_tv.send_notification` para aparência, câmera e som. Selecione a entidade **Avisos** da sua TV. [Exemplos](docs/EXEMPLOS.md).

## Segurança e limites

O controle usa a porta HTTPS `8766` e valida o certificado da TV aprovado no primeiro vínculo. A tela é empacotada no APK, em uma WebView sem navegação externa; somente o transporte Android se comunica com a TV. Redirecionamentos e identidades diferentes são recusados. A porta `8765` identifica publicamente a TV, sem receber senhas. Os comandos da API 3 exigem HMAC-SHA256 com uma chave vinculada e um desafio de uso único. O HTTP não redireciona nem recebe comandos. Apenas identificação, emissão de desafio e pareamento inicial são públicos; comandos, login, dados e imagens exigem vínculo.

A conta é de um proprietário local por TV. Senha usa PBKDF2-HMAC-SHA256 com 600.000 iterações; segredos, credenciais e câmeras ficam cifrados com Android Keystore. Sessões usam cookies Secure/HttpOnly/SameSite, proteção CSRF, 30 minutos de inatividade e limite absoluto de 12 horas. O app guarda a sessão somente na memória; se Manter conectado foi autorizado, uma credencial cifrada e exclusiva desse controle restaura o acesso após fechar ou reiniciar. Sair da conta, trocar a senha ou revogar o controle cancela esse acesso. A atualização remove os segredos TOTP antigos, mantendo o hash da senha e o bloqueio de tentativas. Cinco falhas bloqueiam login por cinco minutos. Não exponha o receptor diretamente à Internet.

O espelhamento recebe credenciais limitadas ao envio de avisos. Cada controle tem sua própria chave, além da sessão do proprietário vinculada a esse controle. Os vínculos podem ser revogados. A proteção comprova posse da chave; não identifica de forma infalível o programa que a usa caso um aparelho ou chave seja comprometido. O filtro adicional de palavras sensíveis é conservador e não garante detectar todo segredo; mantenha o conteúdo desativado quando não quiser mensagens na TV. RTSP pode trafegar sem criptografia: use rede confiável ou VPN. VPN não fornece servidor e pode impedir descoberta local. A TV precisa estar ligada, conectada e permitir sobreposição. Samsung Tizen e LG webOS não executam o APK diretamente.

## Desenvolvimento e validação

Código Android em `android/`; integração em `custom_components/casanotify_tv/`. Não há chaves privadas de assinatura no repositório. Veja [compilação](android/README.md), [API](android/API.md), [testes e limites da validação](docs/VALIDACAO.md) e [atualizações](CHANGELOG.md).

Consulte [o relatório da versão 2.3](docs/SEGURANCA_2_3.md) e [os resultados de validação](docs/VALIDACAO.md). Os testes de ataque foram feitos em processos e servidores de teste locais, sem acessar a TV do usuário. Ainda é necessário testar instalação, áudio, sobreposição, câmera e VPN nos aparelhos físicos. Não há garantia de aprovação pelo Play Protect.
