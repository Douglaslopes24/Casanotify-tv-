# Histórico

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
