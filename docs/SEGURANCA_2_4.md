# Revisão 2.4 — controle remoto, câmeras e segundo plano

09/10/2026. Aplicativos 2.4.0, integração Home Assistant 2.2.0. Revisão do código e testes locais autorizados; não houve acesso à TV, ao celular ou às câmeras do usuário.

## Alterações e limites

| Área | Alteração | Limite prático |
|---|---|---|
| Controle da Android TV | Cliente Remote v2 com pareamento Polo, chave RSA no Android Keystore e certificado aprovado fixado | Exige Android TV Remote Service; compatibilidade e volume dependem do aparelho |
| Pareamento | PIN de seis caracteres exibido pela TV; prova vinculada às duas chaves RSA; certificado salvo só após confirmação | Pareamento nativo separado da conta CasaNotify; não é segundo fator de login |
| Transporte remoto | TLS 1.2/1.3, certificado do cliente, quadros de até 64 KiB, prazos e teclas permitidas | A confiança inicial existe somente na porta de pareamento; comandos sempre exigem certificado aprovado |
| Câmeras | Ação HA por entidade, permissão de leitura, prazo para resolver fonte e erro sem credenciais; papel HA obrigatório na API | A TV acessa RTSP diretamente; não torna criptografado o RTSP da câmera nem adiciona transcodificação |
| Avisos | Serviço foreground connectedDevice com notificação/Pausar, START_STICKY, independente da tarefa da tela, rebind e rede | Android e fabricante continuam controlando a execução. Forçar parada não é contornado |
| Entrega | Fila cifrada existente + trava temporária de CPU só durante entrega; até 512 confirmações cifradas por dez minutos | Não garante exatamente uma entrega se a resposta do receptor for perdida |
| Recuperação | Examina até 200 notificações ainda ativas quando o listener reconecta; limita idade e momento da ativação | Não recupera notificações que já foram removidas durante a interrupção |
| VPN | Interface removida e Activity desativada | Não desliga túneis do Android; sensor HA preservado para manter compatibilidade |

Serviço contínuo e leitura de notificações são exclusivos da edição Celular. O envio permanece opcional, com conteúdo oculto inicialmente. O início do foreground service ocorre a partir da tela visível ou dos eventos permitidos de boot/atualização; não se tenta contornar a proibição de iniciar serviços a partir de callbacks comuns em segundo plano. A ação Pausar desativa o envio e apaga pendências. A trava de CPU expira em até 90 segundos e é liberada ao terminar a tentativa; não permanece ativa em repouso.

A conexão do controle remoto só fica aberta durante a tela Remoto. Comandos não são repetidos automaticamente. O vínculo nativo não é revogado ao sair da conta CasaNotify: são autorizações distintas. Esquecer remove a confiança local; a revogação no serviço da TV é feita nos ajustes da própria Android TV. Nenhuma permissão de acessibilidade, ADB, root, SMS ou instalação de pacotes foi acrescentada.

## Verificações executadas

- **71 testes Java/JUnit:** vetores protobuf de pareamento e teclas, prova de PIN/RSA, recusa de erro/versão/ack incorretos, quadros fragmentados, tamanhos abusivos, varints inválidos e campos duplicados; negociação e ping incluindo valor padrão zero; recibos após reinício, limites, expiração, troca de destino e política HA para câmeras. Inclui a suíte anterior de autenticação, repetição, TLS local real e pinning.
- **82 testes Python:** Home Assistant 2026.9.3/Python 3.14.7; 37 entidades preservadas; nova ação de câmera com padrões e ajustes; fontes inválidas/ausentes, erros sem credenciais, domínio incorreto e recusa de usuário sem permissão de leitura. As fontes de câmera são simuladas nesses testes; não houve reprodução de uma câmera física.
- **Ruff aprovado** para integração, testes e ferramentas.
- **Dois fluxos DOM aprovados:** painel e canal nativo simulado; cadastro/login/sessão, CSRF, personalização, imagem, YAML, remoção de câmeras/VPN do painel, histórico e revogação.
- **Três APKs compilados e assinados:** 2.4.0, versionCode 9, min API 26/target 35; assinatura original v2/v3, alinhamento e integridade ZIP conferidos. Permissões/componentes verificados no manifesto binário; leitor e serviço contínuo ausentes em TV/Controle; Activity VPN desativada. Dez MP3s originais preservados e sem compressão ZIP.

O teste de protocolo do controle usa vetores e mensagens simuladas. Não valida uma sessão mTLS Android Keystore com Android TV Remote Service real. Os testes TLS anteriores cobrem o transporte CasaNotify em servidores locais. Nenhum teste de laboratório equivale a certificação, garantia de ausência de vulnerabilidades ou aprovação do Play Protect.

## Verificação necessária nos aparelhos

1. Atualizar TV e celular sem apagar dados; aceitar os termos, conferir login e vínculos preservados.
2. Parear Remoto com PIN da TV, testar cada tecla em outro aplicativo, sair/voltar da tela e confirmar reconexão sem PIN. Alterações no certificado devem impedir conexão automática.
3. Ativar avisos e notificação do serviço; enviar um aviso de aplicativo selecionado. Repetir com tela CasaNotify fechada, removida dos recentes e tela do telefone apagada.
4. Interromper/restaurar a rede e confirmar entrega de pendências com menos de dez minutos para a mesma TV, sem migrar avisos para outro vínculo.
5. Reiniciar o celular desbloqueando-o depois; conferir retomada permitida pelo fabricante. Testar Pausar, permissão revogada e Forçar parada: nenhum deve ser contornado.
6. Executar Mostrar câmera na TV pelo HA com celular fechado; testar substream H.264, duração, silêncio, pausa e fonte inacessível.
7. Confirmar ausência das opções de VPN e de cadastro RTSP no celular. Conferir que túneis externos e IDs das entidades HA permanecem intactos.

## Referências de implementação

- Android, tipos de foreground service: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android, restrições de início em segundo plano: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Android, NotificationListenerService: https://developer.android.com/reference/android/service/notification/NotificationListenerService
- Android, Service/START_STICKY: https://developer.android.com/reference/android/app/Service
- Home Assistant, Android TV Remote: https://www.home-assistant.io/integrations/androidtv_remote/
- Protocolo de interoperabilidade: https://github.com/tronikos/androidtvremote2 ; atribuições e licença em `../THIRD_PARTY_NOTICES.md`.
