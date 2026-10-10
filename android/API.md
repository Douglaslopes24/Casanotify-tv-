# API local 3 · Aplicativos 2.4.0 · Integração HA 2.2.0

Base protegida: `https://IP_DA_TV:8766`. Host IPv4 e porta obrigatórios. JSON UTF-8, `Content-Type: application/json`, `Content-Length`. Sem CORS ou redirecionamento de credenciais. TLS 1.2/1.3 com certificado da TV fixado pelo cliente após comparação física do SHA-256. Não use `verify_ssl: false`.

## Descoberta e vínculo

`http://IP_DA_TV:8765/api/info` é descoberta pública. Retorna `app`, `version`, `api_version: 3`, UUID, nome, `control_protocol: 2`, `auth_mode: "password"`, `tls_port` e `tls_fingerprint`. Outros pedidos HTTP recebem 426. A TV não serve HTML, JavaScript, tela de login ou painel pelo navegador.

Após aprovar o certificado, `POST /api/pair` por HTTPS recebe `code`, `client` (`ha`, `control` ou `phone`) e `name` opcional. O código precisa ter o mesmo escopo emitido fisicamente na TV: seis dígitos, dois minutos, uma vez, cinco tentativas. A resposta traz `token`; o código não é segundo fator de login. Há até 20 vínculos de controle/espelhamento, além da chave do Home Assistant.

## Prova em cada comando

HTTPS fornece a criptografia de transporte. A prova HMAC-SHA256 fornece autenticação por posse de uma chave vinculada. Não há segredo universal embutido no APK e não há alegação de atestação da identidade binária do cliente.

1. Busque `GET /api/challenge` por HTTPS fixado. A resposta pública contém `challenge`, um valor opaco autenticado pelo servidor, válido por 60 segundos. O cliente não precisa sincronizar seu relógio.
2. Use `client_id = "ha"` no Home Assistant. Para controles/espelhamento, use `SHA256(SHA256(token).hexdigest().encode()).hexdigest()`.
3. Construa o texto canônico abaixo, unindo as oito linhas com `\n`, sem adicionar outra quebra. SHA-256 é hexadecimal minúsculo; strings e corpo usam UTF-8. Os valores ausentes são strings vazias.

```text
CasaNotify-HMAC-v1
client_id
METHOD
/path
challenge
SHA256(corpo exato)
SHA256(Cookie exato ou vazio)
X-CasaNotify-CSRF ou vazio
```

4. A chave de HMAC é **o texto hexadecimal ASCII** de `SHA256(token UTF-8)`. Calcule `HMAC-SHA256(chave, texto_canônico UTF-8)` em hexadecimal minúsculo.
5. Envie `X-CasaNotify-Client`, `X-CasaNotify-Challenge` e `X-CasaNotify-Proof`, com os mesmos corpo, método, caminho, cookie e CSRF usados no cálculo. A chave bruta não é enviada em cada comando. `Authorization: Bearer` não é aceito.

O desafio só pode autorizar um comando. Alterar corpo/rota/método/sessão invalida a prova. Reiniciar o receptor invalida desafios pendentes. O servidor limita o registro de desafios usados a 2.048 e não remove entradas ainda válidas para abrir espaço; ao atingir o limite, novos comandos são recusados até expirar uma entrada. O controle não repete POST após falha de rede, pois ele pode ter sido executado. A fila de espelhamento tenta novamente avisos sem confirmação, com o mesmo ID; em caso de perda da resposta pode haver repetição. Não há promessa de entrega exatamente uma vez.

Implementações: `RequestAuth.java`, `ControlClient.java` e `custom_components/casanotify_tv/api.py`.

## Papéis e conta

- **Controle:** exige chave de vínculo para `/auth/state`, `/auth/register` e `/auth/login`. Cadastro recebe usuário, nome, senha, aceite e versão dos termos. Login recebe usuário, senha e `remember` booleano opcional (padrão falso). Não há TOTP nem SMS.
- Com `remember: true`, cadastro/login também retorna `resume_token`, que o transporte nativo remove antes de entregar a resposta à WebView e guarda cifrado. O servidor guarda apenas seu hash, vinculado ao `client_id`. `POST /auth/resume` recebe esse token e exige a prova HMAC do mesmo controle; emite uma nova sessão/CSRF. A autorização dura até sair da conta, trocar senha, revogar o controle ou redefinir a conta. O cadastro/login retorna perfil, CSRF e cookie `__Host-casanotify` Secure/HttpOnly/SameSite=Strict. A sessão fica vinculada ao cliente que entrou. POSTs autenticados por sessão exigem `X-CasaNotify-CSRF`, incluído na prova HMAC. Expiração: 30 minutos de inatividade e 12 horas absolutas.
- `/auth/session` restaura perfil/CSRF; `/auth/logout` encerra as sessões e a autorização persistente daquele controle; `/auth/password` exige `current` e `password`; `/auth/profile` altera `display_name`. Somente um controle com sessão do proprietário acessa esses ajustes.
- **Home Assistant:** chave de administração separada, sem precisar cadastrar conta de controle. Tem acesso à API, mas não aos endpoints `/auth/`.
- **Espelhamento:** `GET /api/hello` para conferir a identidade e `POST /api/notify`, limitado a título/mensagem/ID, até 30 avisos/minuto por vínculo (HTTP 429 ao atingir o limite; não revoga a chave). Não acessa configurações, imagens privadas, câmeras, perfil ou histórico.
- Revogar um vínculo bloqueia novos comandos imediatamente. Redefinir a conta presencialmente revoga os controles, revogando também os avisos associados a esses controles; vínculos legados independentes de espelhamento, Home Assistant e câmeras são mantidos.

## Rotas protegidas

| Método/rota | Uso |
|---|---|
| GET `/api/hello` | Identidade do receptor, após prova de qualquer vínculo válido; sem sessão do proprietário |
| POST `/api/phones/link` | Controle com sessão/CSRF cria uma chave limitada de avisos associada ao seu `client_id`; substitui a anterior desse controle |
| GET `/api/status` | Identidade, sobreposição, pausa, silêncio, fila e VPN |
| GET/POST `/api/config` | Nome, padrões, rotina, tema do app e animação inicial |
| POST `/api/notify` | Aviso; `camera_id` e `video_url` exigem papel `ha`; `image_url` continua permitido ao controle |
| POST `/api/clear` | Limpar por ID ou tudo |
| GET `/api/history` | Até 30 registros temporários |
| GET `/api/cameras` | Somente HA; IDs e nomes legados, sem URLs ou credenciais |
| POST `/api/cameras/save` | Somente HA; compatibilidade legada, limite 12 |
| POST `/api/cameras/delete`, `/api/cameras/test` | Somente HA; ID legado |
| GET `/api/phones` | Controles/espelhamento, sem tokens/hashes |
| POST `/api/phones/revoke` | ID; vazio revoga todos os controles/espelhamento |
| POST `/api/media` | `kind`: logo/background, `data`: base64, até 1 MiB decodificado |
| GET `/media/logo`, `/media/background` | PNG privado reprocessado |
| POST `/api/media/remove` | Remover por `kind` |

Pedidos até 64 KiB; upload até 1,6 MB. Cabeçalhos até 16 KiB, trabalhadores/fila limitados e prazos de leitura. UTF-8 malformado, caracteres de controle, cabeçalhos duplicados, transferência chunked, Host ou origem não autorizados são recusados.

Toques: `soft`, `doorbell`, `chime`, `pulse`, `alarm`, `digital` e `sound_01` até `sound_10`. Nomes e hashes dos arquivos recebidos em [SONS.json](../docs/SONS.json). Campos e limites completos em `Notice.java` e no esquema do Home Assistant.

## Aplicativo de controle e atualização

Interface empacotada na origem `https://app.casanotify.local`, sem navegação externa, acesso a arquivos/content URLs ou rede WebView. Um canal `WebMessagePort` entregue à origem exata usa o transporte Android com métodos/rotas permitidos. Cookies ficam somente em memória e não são entregues ao JavaScript. A chave de vínculo, o certificado aprovado e a autorização opcional de retomada ficam cifrados com Android Keystore. `/auth/resume`, `/api/hello` e `/api/phones/link` são usados internamente pelo código nativo, fora da ponte JavaScript.

A API 3 exige atualizar TV, Controle/Celular e integração HA. A chave HA, os vínculos antigos de espelhamento e os IDs da TV são preservados. O controle anterior não tinha chave própria: exige um novo vínculo inicial na TV. A descoberta mDNS continua `_casanotify._tcp.local.` na porta 8765. Não exponha as portas diretamente à Internet.


## Descoberta e entrega em segundo plano (2.4)

O receptor anuncia `_casanotify._tcp.` com `id`, `name`, `version` e `api`. Os companheiros fazem busca NSD limitada a seis segundos, até 16 serviços e oito resultados IPv4; não varrem sub-redes. Anúncios são apenas candidatos. Ao recuperar o IP, o cliente mantém UUID, certificado e chave originais e exige resposta autenticada de `/api/hello` antes de salvar o novo endereço. Uma mudança no certificado exige nova aprovação física.

O listener da edição Celular é gerenciado pelo Android. `requestRebind` é usado somente com permissão e envio autorizados; boot e atualização do pacote solicitam retomada. `MirrorConnectionService` é um foreground service `connectedDevice`, iniciado a partir da tela visível ou dos eventos permitidos de boot/atualização. Requer envio e acesso às notificações autorizados; mostra notificação com ação Pausar, usa START_STICKY e não para ao remover a tarefa dos recentes. A cada 30 segundos solicita retomada do listener e envio da fila. `JobScheduler`, com rede disponível e retentativa exponencial, complementa as tentativas. Uma trava de CPU limitada a 90 segundos cobre apenas o escoamento de avisos pendentes. Não é criada em repouso. Uma callback de rede aciona nova tentativa enquanto o listener estiver ativo. A fila AES-GCM contém até 30 avisos, com prazo de dez minutos para entrega; a exclusão dos expirados ocorre quando o processo executar novamente. Os itens ficam vinculados ao UUID, certificado e chave da TV; trocar de destino ou chave descarta os antigos. Permissão, seleção de aplicativos e preferência de conteúdo são conferidas novamente antes do envio. Desligar envio/conteúdo ou remover vínculo limpa a fila. Restrições do sistema podem adiar ou suspender captura e entrega.

Quando o listener reconecta, até 200 notificações ainda ativas são examinadas. Só são capturadas as posteriores à ativação do envio e com menos de dez minutos, respeitando os mesmos filtros. Até 512 identificadores de entrega, vinculados à TV/chave, ficam cifrados por dez minutos; não contêm título ou texto. Evitam repetir entregas confirmadas após reinício. Perda da confirmação ainda pode causar repetição. Forçar parada ou parada pelo gerenciador do Android não é contornada.

## Câmeras comandadas pelo HA

`casanotify_tv.send_camera_notification` recebe `camera_entity_id`, título/mensagem opcionais, duração, áudio, som/toque, posição, urgência, substituição e ID do aviso. O HA verifica permissão de leitura da câmera no contexto do usuário, resolve `async_get_stream_source` com prazo de 15 segundos, exige RTSP e envia pelo cliente autenticado. O endereço não é colocado no estado da entidade nem em mensagens de erro. A fonte continua sendo acessada diretamente pela TV; não há transcodificação ou proxy novo. Câmera e codec devem ser acessíveis/suportados pela TV. O restante da integração carrega mesmo sem o domínio camera instalado.

## Controle nativo da Android TV

É independente da API CasaNotify: Polo em TLS/mTLS na porta 6467 para parear; Remote v2 na 6466 para comandos, com a chave privada RSA 2048 do cliente no Android Keystore. O PIN hexadecimal exibido pela TV comprova o vínculo dos certificados; somente após confirmação do servidor o certificado é salvo cifrado. Conexões de comandos sempre exigem esse certificado. Uma mudança exige novo pareamento explícito; falhas nunca provocam aceitação automática de certificado.

Quadros protobuf limitados a 64 KiB e lista restrita de teclas. Ping e negociação não dependem da WebView. A conexão termina ao sair da tela Remoto e se refaz automaticamente ao voltar. Comandos não são reenviados após falhas. O serviço exige Android TV Remote Service; não usa ADB, acessibilidade, root ou injeção privilegiada. Esquecer remove a confiança local; revogação na TV é feita nas configurações da própria Android TV. Fontes/licenças em `../THIRD_PARTY_NOTICES.md`.
