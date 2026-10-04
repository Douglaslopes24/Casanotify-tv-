# Avisos do Android e do navegador

## Play Protect ao instalar

A mensagem sobre acesso a dados sensíveis corresponde à proteção descrita pelo Google para aplicativos obtidos pela Internet com permissões sensíveis, entre elas o leitor de notificações. Veja a [orientação oficial para desenvolvedores](https://developers.google.com/android/play-protect/warning-dev-guidance?hl=pt-br).

Na versão 2.2.0, **CasaNotify TV** e **CasaNotify Controle** não incluem esse leitor: a TV recebe mensagens pela rede e o controle faz os ajustes sem ler notificações. **CasaNotify Celular** mantém o leitor declarado porque sua finalidade é encaminhar notificações escolhidas pelo usuário; por isso, o bloqueio ainda pode ocorrer nessa edição. Nenhum código tenta ocultar a permissão, concedê-la automaticamente ou mudar o Play Protect.

Mantenha a proteção ativa. Se o APK correto continuar bloqueado, interrompa a instalação e registre a versão, a edição e o texto do aviso. A documentação do Google contém o caminho para solicitar uma análise/contestação pelo desenvolvedor. Nenhuma contestação ou publicação na Play Store foi enviada nesta entrega; um resultado favorável não é garantido.

## Acesso negado às notificações no celular

É uma autorização especial controlada pelo Android. Sem ela, o espelhamento automático não funciona. A edição Celular explica o acesso, exige escolha dos aplicativos e permite desligar o envio. Se o sistema negar a permissão, o envio fica indisponível; não é um erro de usuário/senha do painel. Consulte a [ajuda oficial sobre configurações restritas](https://support.google.com/android/answer/12623953?hl=pt-BR).

Isso não impede o Home Assistant de enviar avisos diretamente à edição TV, quando ela estiver instalada e o receptor ativo. O botão de teste manual do celular verifica somente o vínculo/rede, não comprova autorização para ler notificações.

## “Não seguro” no navegador

Cadastro e ajustes acontecem no app Controle, que verifica o certificado da TV aprovado no vínculo. A versão 2.2.0 remove o painel e a tela de login servidos pela TV ao navegador. O app exige uma chave vinculada e uma prova por comando, além da senha para os ajustes do proprietário.

A atualização não muda a confiança do Chrome nem instala certificados no computador/celular. O aviso antigo era sobre o certificado próprio da TV e é diferente do Play Protect. Se havia desativado avisos desse site, reative-os e passe a usar o app Controle atualizado. A integração Home Assistant continua podendo ser vinculada diretamente na TV, sem criar conta no controle. [Ajuda do Chrome](https://support.google.com/chrome/answer/95617?hl=pt-BR).

Veja [o escopo dos testes e as correções](SEGURANCA_2_2.md). Não houve acesso remoto à TV do usuário, auditoria independente ou aprovação do Google.
