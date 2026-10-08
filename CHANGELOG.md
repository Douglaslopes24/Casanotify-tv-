# Histórico

## Aplicativos 2.3.0 — 08/10/2026

- Descoberta automática da TV, recuperação autenticada do IP e preservação do primeiro vínculo.
- Manter conectado com autorização cifrada vinculada ao controle, sem armazenar senha. Logout, alteração de senha e revogação impedem a retomada.
- Primeiro login da edição Celular cria a credencial limitada dos avisos; sem segundo pareamento. Credenciais legadas são preservadas.
- Notificações independentes da Activity, rebind autorizado, retomada no boot/atualização, reação à rede e fila cifrada com até 30 itens e prazo de 10 minutos, reagendada pelo Android.
- Ajustes do celular em cartões com busca de aplicativos, diagnóstico e reparo explícito de vínculo. Painel adaptado a telas pequenas.
- Limite de avisos retorna 429, sem confundir congestionamento com revogação.
- Integração HA permanece 2.1.0, API 3 e 37 entidades; assinatura Android original, versionCode 8.


## Aplicativos 2.2.0 e integração 2.1.0 — 03/10/2026

- API 3: prova HMAC-SHA256 com desafio de uso único por comando, dentro de HTTPS fixado. Rejeição de comandos alterados, expirados e repetidos. Sem segredo compartilhado por todos os APKs.
- Cada controle recebe chave própria após autorização física na TV. Login mantém usuário e senha, sem TOTP/SMS. Sessões são vinculadas ao controle; revogação bloqueia o cliente.
- Painel e login retirados do servidor da TV; ajustes exclusivamente pela interface empacotada no controle. Removido acesso administrativo por Bearer isolado.
- Parser recusa UTF-8 malformado e controles nos cabeçalhos/caminho. Tempo de resposta de autenticação ampliado para aparelhos lentos.
- Adicionados os 10 MP3s fornecidos, total de 16 toques, selecionáveis no controle e Home Assistant. Reprodução interrompida ao limpar/substituir aviso.
- Conta, certificado da 2.0.1/2.1.0, identidade e chave HA preservados. Exige atualizar todos os clientes; controle anterior precisa de um novo vínculo inicial.
- Testes de ataque limitados ao laboratório local; relatório em `docs/SEGURANCA_2_2.md`. Assinatura original e versionCode 7.

## Aplicativos 2.1.0 — 28/09/2026

- CasaNotify Controle no celular: cadastro, login, avisos, aparência, câmeras, imagens, conta, Home Assistant e histórico dentro do app, sem abrir navegador.
- Login somente com usuário e senha; migração remove TOTP preservando hash/sal, nome, consentimento e bloqueio de tentativas. Cadastro inicial exige aprovação presencial da TV, sem segundo fator recorrente.
- Interface empacotada localmente, transporte HTTPS com certificado fixado e lista de rotas, sem navegação externa nem aceitação de erros TLS. Sessão somente em memória. Seletor Android de imagens sem acesso geral a arquivos.
- Três edições: TV receptora, Controle sem leitor de notificações e Celular com espelhamento opcional e consentimento. Mantém a identidade e assinatura do aplicativo; não há promessa de aprovação do Play Protect.
- Certificado HTTPS da 2.0.1 e integração Home Assistant 2.0.1/API 2 preservados. As três edições têm versionCode 6 e mantêm os dados quando instaladas como atualização.
- 37 testes Java e testes DOM do painel/canal nativo aprovados; APKs assinados, com permissões inspecionadas. Teste em hardware permanece necessário.

## Integração Home Assistant 2.0.1 — 27/09/2026

- Respostas sem nome ou versão válidos agora produzem um erro tratado, evitando falhas inesperadas na configuração e nas entidades.
- Diagnóstico pode ser consultado antes de uma conexão bem-sucedida, mantendo credenciais e IPs fora do relatório.
- Instruções de vinculação distinguem o código/chave do Home Assistant do login e autenticador do navegador e deixam explícita a porta de descoberta 8765.
- Corrige a configuração do teste de HTTPS no GitHub Actions: a fixture de sockets é ativada após a preparação dos testes do Home Assistant, com conexões restritas a localhost.
- 58 testes aprovados localmente, incluindo as 37 entidades, reconfiguração de IP, confirmação de novo certificado preservando entidades e transporte HTTPS com certificados RSA/EC P-256.
- Pacote ZIP republicado. API 2 e aplicativo Android 2.0.1 preservados.

## Aplicativo 2.0.1 — 26/09/2026

- Corrige a autorização de assinatura da chave HTTPS no Android Keystore: ECDSA P-256 com `DIGEST_NONE`, necessário quando a pilha TLS já calculou o resumo da mensagem.
- Implementa a seleção do certificado para conexões por `SSLSocket` e `SSLEngine` no mesmo gerenciador de chaves.
- Gera uma nova identidade HTTPS local uma única vez ao atualizar da 2.0.0. Exige conferir o novo SHA-256 na TV e aprovar novamente nos clientes; mantém a conta, o autenticador e os demais dados do aplicativo.
- Preserva a assinatura do APK e a API 2. Integração Home Assistant continua em 2.0.0.
- Acrescenta testes com Conscrypt e TLS 1.2/1.3, além de orientações para `ERR_CONNECTION_CLOSED` e avisos do Play Protect. Não representa aprovação pelo Google ou teste em aparelho físico.

## 2.0.0 — 26/09/2026

- HTTPS com certificado por dispositivo e validação por SHA-256 no Home Assistant/celular.
- Cadastro local, senha PBKDF2, autenticação TOTP, sessão/CSRF, bloqueio de tentativas e recuperação presencial.
- Termos, aceite e agradecimento personalizado; edição de nome e senha.
- RTSP nativo com até 12 perfis protegidos; vídeo sem áudio por padrão.
- Seis toques, 21 ícones, logo interno/fundo personalizados e editor de imagens no painel.
- Temas claro/escuro/sistema e animação inicial.
- Espelhamento opcional de aplicativos selecionados no celular, controle de conteúdo e credenciais revogáveis limitadas ao envio.
- Provisionamento/conexão IKEv2/IPsec em Android 11+ compatível e acesso às VPNs do Android.
- Integração com 37 entidades e migração via reautenticação para aprovação do certificado.
- Comandos HTTP antigos desativados. Atualize o APK e a integração juntos.

## 1.1.0

Descoberta mDNS, integração Home Assistant própria e 32 entidades. Transporte HTTP local, substituído na versão 2.0.
