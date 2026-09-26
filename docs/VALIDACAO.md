# Validação — CasaNotify TV 2.0.0

Data: 26/09/2026.

## Executado com sucesso

- APK nativo compilado com JDK 17 e ferramentas oficiais Android SDK 35; versionCode 3; versionName 2.0.0; Android mínimo 8/API 26; target API 35.
- Assinatura APK v2/v3 e alinhamento verificados. Mesmo certificado da entrega 1.1.0.
- **31 testes Java/JUnit:** protocolo, limites, origem/host, pareamento e separação de escopos, senhas PBKDF2, vetores oficiais RFC 6238, cadastro/consentimento, confirmação TOTP, bloqueio persistente, prevenção de repetição, expiração de sessões, troca de senha/revogação, RTSP e filtro de notificações.
- Teste Java com conexão TLS real em localhost, certificado de teste confiado explicitamente, cookies seguros e recusa de origem incorreta.
- **46 testes Python**, com Home Assistant 2026.9.3: inicialização das plataformas, 37 entidades, ações e controles, descoberta, reautenticação, atualização de IP, diagnósticos, esquemas e limites. Os dispositivos são simulados.
- Teste Python com servidor HTTPS real em localhost: certificado correto recebe a credencial; certificado diferente é recusado antes de receber o pedido autenticado.
- Painel testado em DOM com respostas simuladas: cadastro, confirmação, login, restauração de sessão, CSRF, limpeza de segredos, texto sem execução de HTML, ícones/toques, temas, câmera, YAML, perfil, revogação de celular, histórico e troca de senha.
- Análise estática Ruff e sintaxe JavaScript.

## Limites

Não houve execução do APK em TV física, emulador Android ou celular. Android Keystore, MediaPlayer RTSP, permissões de fabricante, notificações de aplicativos reais, anúncio mDNS pela rede física, consentimento/conexão VPN e apresentação visual nativa precisam ser verificados no aparelho.

O teste visual Chromium foi preparado, mas não executou: o ambiente recusou a criação do socket exigido para iniciar o navegador. Portanto, não há aprovação visual por captura de tela, nem teste real do editor de imagem em navegador nesta entrega. O script opcional está em `android/tests/panel.browser.cjs` para execução em um computador com Chromium suportado.

Passar nos testes não equivale a auditoria independente de segurança nem a compatibilidade universal. VPN integrada exige servidor próprio e Android 11+ com IPsec. Autenticação móvel implementada é TOTP, não SMS. Edição de imagem altera logo interno/fundo, não o ícone instalado no launcher.
