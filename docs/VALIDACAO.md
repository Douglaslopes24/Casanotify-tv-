# Validação — CasaNotify TV

Última revisão da integração e dos aplicativos: 10/10/2026.

## Aplicativos 2.4.1 / integração 2.2.1 — segurança de rede

- **82 testes Java aprovados:** TLS local real, adulteração/repetição de comandos, revogação e isolamento de credenciais; novos casos de JSON ambíguo/profundo, compressão, conexões lentas e limites por IP.
- **112 testes Python aprovados:** Home Assistant 2026.9.3/Python 3.14.7, recuperação de IP autenticada, anúncio falso, conflito com reconfiguração, JSON malformado e resposta gzip em aiohttp local real. Ruff aprovado.
- **Dois fluxos DOM aprovados**, incluindo login persistente, personalização, CSRF, revogação e texto seguro.
- Três APKs 2.4.1, versionCode 10, assinatura original, API mínima 26/alvo 35, permissões/componentes e sons conferidos; pacote HA 2.2.1. Mesmas identidades, vínculos e 37 entidades.
- Testes limitados ao código e ao laboratório local. Sem execução em aparelhos físicos, fuzzing contínuo, teste distribuído ou auditoria independente. [Achados, correções e limites](SEGURANCA_2_4_1.md). Registros abaixo são históricos.

## Aplicativos 2.4.0 / integração 2.2.0 — controle, câmeras e continuidade

- **71 testes Java aprovados**, incluindo protocolo do controle remoto, quadros malformados, PIN, recibos persistentes e política de acesso às câmeras, além da suíte anterior de autenticação/TLS local.
- **82 testes Python aprovados** com Home Assistant 2026.9.3/Python 3.14.7; nova ação de câmera por entidade, erros sem credenciais e permissão de leitura. Ruff aprovado.
- **Dois fluxos DOM aprovados**, incluindo ausência de configuração RTSP e VPN no painel do celular.
- Três APKs 2.4.0, versionCode 9, API mínima 26/alvo 35; assinatura original, alinhamento, integridade, permissões e dez MP3s conferidos. Serviço foreground apenas em Celular; VPN desativada em todas as edições.
- Sem execução desta versão em TV/celular físicos ou emulador. Controle nativo, aparência/foco Android, mDNS, restrições de bateria e reprodução RTSP ainda precisam da verificação nos aparelhos. Não há promessa de aprovação do Play Protect.
- [Escopo, proteção e roteiro nos aparelhos](SEGURANCA_2_4.md). Registros abaixo são históricos.

## Aplicativos 2.3.0 / integração 2.1.0 — conexão e segundo plano

- **59 testes Java aprovados**, incluindo autenticação persistente vinculada ao controle e fila com expiração, limite, persistência e isolamento de destino, além dos testes de TLS e ataques locais anteriores.
- **71 testes Python aprovados** com Home Assistant 2026.9.3/Python 3.14.7 e Ruff aprovado. API 3, integração 2.1.0 e 37 entidades preservadas.
- **Dois fluxos DOM aprovados**, painel e transporte nativo simulado, incluindo a opção de manter conectado.
- Três APKs 2.3.0, versionCode 8, API mínima 26 e alvo 35; assinatura original, alinhamento, permissões e os dez MP3s conferidos. Leitor de notificações ausente nas edições TV/Controle.
- Chromium foi baixado, mas o ambiente impediu sua execução ao negar a criação de socket. A interface nativa, o mDNS e o comportamento em segundo plano não foram executados em aparelhos físicos ou emulador. Nenhuma aprovação do Play Protect foi obtida.
- [Escopo, alterações de segurança e roteiro nos aparelhos](SEGURANCA_2_3.md). Os registros abaixo são históricos.

## Aplicativos 2.2.0 / integração 2.1.0 — segurança e sons

- **45 testes Java/JUnit aprovados**, incluindo cenários locais de ataque contra prova, repetição, sessão e parser. TLS real com certificado fixado e recusa de certificado diferente antes do envio de senha.
- **71 testes Python aprovados com Home Assistant 2026.9.3/Python 3.14.7**, incluindo as 37 entidades e HTTPS real local verificando a prova dos comandos GET/POST com texto UTF-8. Análise Ruff aprovada.
- **DOM aprovado nos dois transportes**, incluindo canal nativo simulado sem rede WebView: cadastro, login sem segundo fator, CSRF, limpeza de segredos, 21 ícones, 16 toques, temas, câmera, perfil, histórico, revogação e senha.
- Três APKs: versão 2.2.0, versionCode 7, mínimo API 26 e alvo API 35. Assinatura original v2/v3, alinhamento e permissões inspecionados. TV/Controle não contêm classe/serviço de leitura de notificações.
- Os 10 MP3s foram decodificados para inspeção, têm um stream de áudio cada, duração aproximada de 0,5–3,4 s e são empacotados sem compressão ZIP, com bytes/hashes originais preservados. Reprodução efetiva no MediaPlayer da TV ainda requer aparelho.
- API 3 incompatível com clientes antigos. Certificado, conta, identidade e chave HA preservados; controles antigos precisam de novo vínculo físico inicial. Login continua sem TOTP/SMS.
- Relatório: [revisão e testes de segurança](SEGURANCA_2_2.md). Não houve teste na TV real, emulador ou celular. Nenhuma aprovação do Play Protect foi obtida. Os registros abaixo são históricos.

## Aplicativos 2.1.0 — controle pelo app e login sem TOTP

- **37 testes Java/JUnit aprovados:** protocolo, senha, consentimento, migração de conta TOTP existente sem perder senha ou bloqueio, sessões/revogação, escopos de vínculo, RTSP e filtro de conteúdo. Inclui HTTPS real: certificado correto permite login e certificado diferente impede a chegada da senha ao servidor.
- **DOM aprovado nos dois transportes:** painel e app com canal nativo simulado, sem `fetch` de rede. Cadastro direto, login só com senha, CSRF, câmeras, temas, toques, ícones, perfil, histórico, revogação e troca de senha.
- Três APKs: 2.1.0, versionCode 6, min API 26 e target API 35, compilação com JDK 17/SDK 35, assinatura v2/v3 e alinhamento conferidos. Certificado de assinatura original preservado.
- TV e Controle sem serviço nem classe `NotificationListenerService`; Celular mantém esse serviço explicitamente. Controle e Celular não declaram receptor/boot/sobreposição.
- Sem mudança no contrato Home Assistant/API 2 nem na identidade HTTPS introduzida em 2.0.1.
- O teste visual Chromium não pôde iniciar nesta execução por ausência do executável. Os testes DOM não validam renderização, foco, seletor Android ou comportamento de WebView real.
- **Não validado em aparelho físico/emulador nesta sessão:** instalação/atualização, abertura WebView e canal Android, sobreposição, seleção de imagem, codecs RTSP, VPN, listener e resposta real do Play Protect. Não há afirmação de aprovação do Google ou remoção garantida do bloqueio.

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

Passar nos testes não equivale a auditoria independente de segurança nem a compatibilidade universal. VPN integrada exige servidor próprio e Android 11+ com IPsec. Na 2.1.0, a autenticação do controle usa usuário e senha, sem TOTP ou SMS. Os registros de versões anteriores abaixo/acima descrevem o comportamento daquelas versões. Edição de imagem altera logo interno/fundo, não o ícone instalado no launcher.
