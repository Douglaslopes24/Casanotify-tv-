# CasaNotify 2.3.0 — vínculo, retomada e entrega

Revisão de 08/10/2026. Aplicativos Android 2.3.0, integração Home Assistant 2.1.0, API 3. O trabalho foi feito no código e em ambientes locais de teste; não houve acesso ou ataque à TV, ao celular, ao roteador ou a serviços externos do usuário.

## O que mudou

- O endereço pode mudar sem perder o vínculo. A busca mDNS é limitada a seis segundos e não aprova certificados. A recuperação exige o UUID e o certificado originais, conexão TLS fixada e prova HMAC da chave existente antes de salvar um IP. A conexão manual à mesma TV também preserva a chave.
- `Manter conectado` cria uma autorização aleatória de 256 bits, cifrada com Android Keystore no celular. Na TV, só seu hash é persistido, associado ao controle. O token não vai para o JavaScript. Senhas e cookies não ficam persistidos no celular. Cada retomada cria uma nova sessão e um novo CSRF.
- Logout revoga as sessões e a autorização daquele controle; trocar a senha revoga todas as autorizações e sessões. Revogar a chave do controle impede a prova necessária à retomada. Desmarcar Manter conectado no login remove a autorização anterior sem encerrar a nova sessão corrente. Não foi reintroduzido segundo fator.
- O primeiro login na edição Celular cria uma chave separada e limitada a avisos, subordinada ao controle. Revogar o controle também remove sua chave de avisos. Chaves legadas independentes continuam válidas. Uma chave de avisos revogada não é recriada automaticamente: há uma ação explícita de reparo, com login do proprietário.
- `/api/hello`, `/auth/resume` e `/api/phones/link` são operações internas do transporte Android; a ponte da WebView as recusa. A interface continua empacotada no APK, sem abrir navegador.
- O limite de 30 avisos por minuto responde HTTP 429; não é tratado como credencial revogada.

## Notificações com a tela fechada

O listener autorizado é gerenciado pelo Android e não depende da Activity. Há tentativa autorizada de rebind, retomada após boot/atualização e reação a mudanças de rede. A entrega usa executor próprio e `JobScheduler` persistente, com rede disponível e espera exponencial. Não foi adicionado serviço em primeiro plano no celular, SMS, acessibilidade ou acesso geral a arquivos.

A fila fica cifrada, limitada a 30 avisos e vinculada à identidade/certificado/chave do destino. Avisos de mais de dez minutos não são enviados e são removidos na próxima execução; o Android pode adiar a limpeza enquanto o processo não executa. Entrega confirmada, desligamento, remoção do vínculo ou retirada do conteúdo limpa os respectivos itens/fila. Permissão, seleção de aplicativos, destino e conteúdo são conferidos antes do envio. Tarefas capturadas antes de uma limpeza são invalidadas. O filtro de conteúdo sensível continua conservador e não identifica todos os segredos.

Uma falha após a TV receber um aviso e antes da confirmação pode causar repetição. Não há garantia de entrega exatamente uma vez, nem de funcionamento com o aplicativo forçado a parar. As regras de bateria, as restrições do fabricante e o acesso a notificações continuam sob controle do Android.

## Validação realizada

- **59 testes Java aprovados:** incluem o conjunto anterior de protocolo, TLS e testes adversariais, mais retomada após reinício, rejeição de token errado/em outro controle, revogação por logout/troca de senha, rotação de autorização, conta legada, identidade da TV e persistência/expiração/limite/destino da fila.
- **71 testes Python aprovados**, Home Assistant 2026.9.3, com HTTPS real local e 37 entidades; Ruff aprovado. A integração não mudou de versão.
- **Dois fluxos DOM aprovados:** painel e ponte nativa simulada com `fetch` bloqueado. Cadastro/login, escolha de manter conectado, sessão, CSRF, personalização, som, câmera, perfil e limpeza de dados. Esses testes simulam o transporte e não executam o Android Keystore.
- **Três APKs assinados:** versionCode 8, mínimo API 26, alvo API 35. Mesma assinatura original v2/v3, alinhamento e limites de permissões verificados. TV/Controle não contêm o leitor de notificações. O job e o receiver de retomada existem somente na edição Celular. Os dez MP3s originais foram conferidos por hash e permanecem sem compressão para reprodução.

A tentativa de teste visual em Chromium não pôde iniciar: o ambiente negou a criação de socket (`Operation not permitted`). Não houve captura visual dessa versão, execução em emulador, teste físico de mDNS, reinício, economia de bateria ou notificações com a tela fechada. Compilação e testes automatizados não substituem essa validação nos aparelhos. Não há aprovação do Play Protect nem alegação de ausência de todas as vulnerabilidades.

## Verificação no aparelho

Atualize primeiro o receptor e depois o celular, sem limpar os dados. Abra a TV, aceite os termos atualizados e ative o receptor. No celular, entre uma vez com Manter conectado. Em Ajustes → Avisos dos aplicativos, aceite os termos, conceda a permissão no Android, escolha os apps e ative o envio.

Confira abertura após encerrar a tela, teste de aviso com o controle fechado, retorno da rede, mudança de IP na mesma rede e reinício dos aparelhos. O cartão Conexão em segundo plano mostra autorização, listener, pendências e último envio. Para um problema concreto, registre versão do Android, fabricante, mensagem do diagnóstico e horário; não publique senha, chave, código ou conteúdo privado.
