# Validação — CasaNotify TV

Última revisão da integração: 27/09/2026. Validação do aplicativo: 26/09/2026.

## Integração Home Assistant 2.0.1

- **58 testes Python aprovados com Home Assistant 2026.9.3 e Python 3.14.7**; análise Ruff aprovada.
- Verificadas as 37 entidades, ações simples/avançadas, controles, descoberta, reconfiguração e recuperação de indisponibilidade com receptor simulado.
- Novo certificado exige confirmação presencial; após a confirmação, os IDs das entidades são preservados. Alterar o IP não cria uma segunda entrada. Outra identidade de TV é recusada na reautenticação.
- HTTPS real em localhost com certificados RSA e EC P-256. Um certificado diferente é recusado antes do envio da credencial.
- Respostas sem nome/versão válidos geram erro controlado. Diagnóstico funciona antes da primeira conexão e não expõe IP, chave ou identidade.
- Corrigida a falha do teste HTTPS no GitHub Actions das publicações anteriores: o marcador era aplicado antes de o plugin do Home Assistant bloquear sockets. O teste agora usa a fixture oficial `socket_enabled`, mantendo a lista de destinos locais permitidos. Não há acesso à TV real nesses testes.

Comandos executados na raiz: `python -m pytest -o addopts= -q` e `python -m ruff check custom_components tests tools`. O APK não foi modificado nesta revisão; o ZIP da integração foi atualizado para 2.0.1.

## Correção do aplicativo 2.0.1

- **33 testes Java/JUnit aprovados**, incluindo o gerenciador `DeviceKeyManager` usado pelo app, seleção de certificado em `SSLEngine`, conexões HTTPS reais em localhost com os provedores JDK e Conscrypt, TLS 1.2 e TLS 1.3, cookies seguros e recusa de origem incorreta.
- Compilação nativa com JDK 17 e SDK 35: versionCode 4, versionName 2.0.1, Android mínimo API 26 e target API 35. Assinatura v2/v3 e alinhamento verificados; certificado de assinatura igual ao das versões anteriores.
- Migração planejada da identidade HTTPS para chave EC P-256 no Android Keystore, autorizando assinatura de resumo (`DIGEST_NONE`). Seleção de certificado implementada para socket e engine. Referências: [Android KeyGenParameterSpec.Builder](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder#setDigests(java.lang.String...)) e [X509ExtendedKeyManager](https://developer.android.com/reference/javax/net/ssl/X509ExtendedKeyManager).
- Integração Home Assistant e protocolo permanecem em 2.0.0/API 2. Os resultados Python e DOM abaixo são da validação anterior.

Os testes usam uma chave de teste em software; não exercitam o Android Keystore da TV do usuário. Ainda é necessário validar a instalação, a migração do certificado e a conexão no aparelho. Não houve análise ou aprovação do APK pelo Play Protect. A captura recebida mostra `ERR_CONNECTION_CLOSED` no navegador; não contém a mensagem do Play Protect.

## Versão 2.0.0 — executado com sucesso

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
