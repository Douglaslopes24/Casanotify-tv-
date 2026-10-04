# Revisão de segurança — CasaNotify TV 2.2.0

Data: 03/10/2026. Integração Home Assistant 2.1.0, API 3. Revisão do código e testes automatizados com processos e servidores locais de teste. Nenhum ataque foi executado contra a TV, o roteador, câmeras ou serviços externos do usuário.

## Mudanças e motivos

| Situação anterior ou risco | Correção nesta entrega |
|---|---|
| Painel e login acessíveis pelo servidor HTTPS ao navegador | Servidor deixa de entregar a interface. Cadastro e ajustes usam a interface empacotada no app Controle |
| Cookie ou credencial Bearer era suficiente para certos comandos | Comandos exigem prova HMAC-SHA256 da chave vinculada, com corpo, método, rota, cookie e CSRF autenticados |
| Uma cópia de uma solicitação autenticada poderia ser repetida | Desafio autenticado pelo servidor, válido 60 s, aceito somente uma vez; reinício invalida desafios pendentes |
| Sessão do proprietário não era associada a um controle vinculado | Chave própria por controle e sessão vinculada a esse cliente; outro cliente não reaproveita a sessão |
| Caracteres de controle/UTF-8 malformado podiam ser interpretados de forma ambígua | Parser rejeita esses pedidos com HTTP 400 antes de chamar a API |
| Redefinição de conta poderia deixar controles anteriores autorizados ao novo cadastro | Redefinição presencial também revoga todas as chaves de controle |
| Tempo curto no transporte podia interromper login em aparelho lento | Limite de resposta para autenticação ampliado para 60 segundos no transporte Android |

Esses itens descrevem revisão e redução de superfície, não uma comprovação de que houve invasão. A captura “Sem resposta da TV” não determina a causa do erro; conectividade, VPN, receptor/protocolo e desempenho precisam ser verificados no aparelho.

## Como funciona a proteção

- TLS 1.2/1.3 cifra o transporte. O app e o Home Assistant fixam o SHA-256 do certificado após comparação física na TV; não há fallback para HTTP ou certificado desconhecido.
- HMAC-SHA256 autentica os comandos com chave individual do controle/espelhamento ou chave própria do Home Assistant. Não é uma nova cifra e não substitui TLS.
- Desafios usam aleatoriedade criptográfica, chave temporária do servidor e tempo monotônico para expiração. O registro de uso tem limite de 2.048 e não descarta desafios ainda válidos para aceitar mais comandos.
- O cadastro/login mantém usuário e senha, sem TOTP/SMS. O código mostrado fisicamente na TV autoriza o primeiro vínculo de cada cliente; não é solicitado a cada login.
- Senhas: PBKDF2-HMAC-SHA256, sal individual, 600.000 iterações. Cinco falhas bloqueiam por cinco minutos, com bloqueio persistido. Segredos no Android são cifrados com AES-GCM e chave do Android Keystore.
- Espelhamento tem apenas envio limitado de título/mensagem; configurações exigem Home Assistant ou chave de controle com sessão do proprietário. A revogação do vínculo impede novos comandos.
- Identificação pública (`/api/info`), obtenção de desafio e pareamento inicial são exceções necessárias ao vínculo. Eles não permitem consultar configurações ou emitir avisos sem autorização.

## Testes executados

45 testes Java/JUnit aprovados. A suíte de ataque verifica comando válido seguido de repetição, alteração de corpo/método/rota/cookie/CSRF, prova ausente/incorreta, transporte sem TLS, expiração, reinício, troca de ID do cliente, revogação, limite do registro antirrepetição, sessão usada por outro cliente e pedidos inválidos em socket local. A suíte também testa senhas, bloqueios, consentimento, migração, certificados, TLS 1.2/1.3, parser e escopos de código.

71 testes Python aprovados com Home Assistant 2026.9.3. Cobrem as 37 entidades, configuração, descoberta, recuperação, esquema dos 10 sons novos e HTTPS real local com certificados RSA e EC. O servidor de teste verifica a prova do Home Assistant, inclusive em corpo UTF-8, sem header Bearer. Certificado diferente é recusado antes de receber o comando.

Interface: testes DOM no transporte de teste e no canal nativo simulado, com `fetch` de rede bloqueado nesse segundo modo. Não são testes de renderização ou execução da WebView em Android real. Compilação, permissões, assinatura e áudio empacotado são conferidos nos APKs; detalhes em [VALIDACAO.md](VALIDACAO.md).

## Limites explícitos

A chave autoriza seu portador. Não há atestação que torne impossível um outro programa usar uma chave extraída de um aparelho comprometido. Mantenha os aparelhos e o Home Assistant protegidos e revogue vínculos desconhecidos. Não exponha as portas do receptor diretamente à Internet.

Os testes exercitam componentes de autenticação, parser e clientes em laboratório. Não substituem auditoria independente, fuzzing exaustivo ou teste integral da API em uma TV física. Não houve prova de resistência a negação de serviço distribuída, extração de chaves em aparelho com root ou comprometimento do sistema operacional.

A proteção descrita é da comunicação de controle com a TV. URLs externas de imagens e câmeras seguem seus próprios protocolos; RTSP e HTTP podem não ser cifrados. VPN, áudio, sobreposição, câmera, WebView, Android Keystore real e Play Protect dependem de verificação nos dispositivos. Não há alegação de aplicativo invulnerável ou aprovado pelo Google.

Referências técnicas: [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [criptografia Android](https://developer.android.com/privacy-and-security/cryptography), [OWASP REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html), [OWASP TLS](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Security_Cheat_Sheet.html).
