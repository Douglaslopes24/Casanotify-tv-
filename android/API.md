# API local 2.0

Base autenticada: `https://IP_DA_TV:8766`. Host numérico e porta obrigatórios. JSON UTF-8, `Content-Type: application/json`, `Content-Length`. Sem CORS nem redirecionamento de credenciais.

`http://IP_DA_TV:8765/api/info` é somente descoberta pública. Retorna `app`, `version`, `api_version: 2`, UUID, nome, `tls_port` e `tls_fingerprint` SHA-256. Compare fisicamente o certificado antes de enviar código/chave por HTTPS. O HTTP não aceita comandos; retorna 426. Não use `verify_ssl: false` como substituto da fixação do certificado.

## Autenticação

- Navegador: `/auth/state`, `/auth/register` com código físico de escopo `setup`, nome, usuário, senha, aceite e versão dos termos. `/auth/confirm` recebe `pending_id` e primeiro TOTP. `/auth/login` recebe usuário, senha e TOTP.
- Login retorna perfil e token CSRF, com cookie `__Host-casanotify` Secure/HttpOnly/SameSite=Strict. POSTs de sessão exigem `X-CasaNotify-CSRF`. `/auth/session` restaura perfil/CSRF; `/auth/logout` revoga a sessão; `/auth/password` exige senha atual, nova senha e TOTP novo; `/auth/profile` altera o nome.
- Home Assistant: `/api/pair` com `code` e `client: ha` retorna credencial de administração. Use `Authorization: Bearer CHAVE` somente por HTTPS com certificado aprovado.
- Celular: `/api/pair` com código de escopo `phone`, `client: phone` e nome. Credencial separada, somente para enviar título/mensagem. Até 10 celulares e 30 avisos/minuto por celular. Revogação pelo proprietário.
- Códigos de pareamento: seis dígitos, dois minutos, uma vez, cinco tentativas. Não substituem códigos TOTP de login.

## Rotas protegidas

| Método/rota | Uso |
|---|---|
| GET `/api/status` | Identidade, sobreposição, pausa, silêncio, estado da fila e VPN |
| GET/POST `/api/config` | Nome, padrões do aviso, rotina, `ui_theme` e `startup_animation` |
| POST `/api/notify` | Enviar aviso; aceita `camera_id` ou `video_url` RTSP ou `image_url` |
| POST `/api/clear` | Limpar por ID ou limpar tudo |
| GET `/api/history` | Até 30 registros temporários |
| GET `/api/cameras` | Lista de IDs e nomes, sem URLs/credenciais |
| POST `/api/cameras/save` | `id` vazio para adicionar, `name` e URL RTSP; limite 12 |
| POST `/api/cameras/delete`, `/api/cameras/test` | ID da câmera |
| GET `/api/phones` | Celulares, sem tokens/hashes |
| POST `/api/phones/revoke` | ID; vazio revoga todos |
| POST `/api/media` | `kind`: logo/background, `data`: imagem base64, até 1 MiB decodificado |
| GET `/media/logo`, `/media/background` | Imagens privadas reprocessadas em PNG |
| POST `/api/media/remove` | Remover por `kind` |

Limite de pedidos: 64 KiB; upload `/api/media` permite corpo de até 1,6 MB. Imagens são redimensionadas e recodificadas, sem metadados originais. Cabeçalhos até 16 KiB, trabalhadores/fila limitados e prazos de leitura.

Campos de aviso e limites estão em `Notice.java` e no esquema da ação do Home Assistant. Título até 160 e mensagem até 1.200 caracteres; duração 3–120 segundos; largura 260–800 dp; fonte 14–36 sp; opacidade 0,4–0,8; cantos 0–40 dp; margem/volume 0–100; foto até 2.048 caracteres e atualização 0 ou 5–60 segundos. Vídeo silenciado por padrão. Pausa bloqueia inclusive urgentes; urgência ignora somente horário silencioso. `id` repetido substitui o aviso correspondente.

## Descoberta

Serviço `_casanotify._tcp.local.` na porta 8765, TXT `id`, `api`, `version`. A identidade, o certificado local e o vínculo são preservados em atualização sem apagar dados. Não publique a porta na Internet. O SHA público anunciado não é confiável por si só: precisa de aprovação presencial no primeiro vínculo.
