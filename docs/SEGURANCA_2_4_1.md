# Revisão de segurança — CasaNotify 2.4.1 / HA 2.2.1

10/10/2026. Revisão do código e testes ofensivos controlados em processos e servidores de laboratório locais. Nenhuma varredura, exploração ou acesso à rede, à TV, ao celular ou às câmeras do usuário foi realizado. Os cenários não constituem certificação nem auditoria independente.

## Resultado

Foi corrigida uma falha de disponibilidade na descoberta da integração: um anúncio mDNS e uma resposta HTTP falsos, com o UUID público de uma TV cadastrada, podiam trocar o endereço salvo. O teste reproduziu a alteração na versão anterior. Isso podia interromper a integração; não demonstrou obtenção da chave, descriptografia ou execução de comandos. O certificado e a chave aprovados já eram preservados.

Na 2.2.1, a integração conecta ao endereço candidato usando **o certificado, a chave e a porta TLS salvos**, exige autenticação e confere o UUID antes de alterar IP/porta. Falha de certificado, identidade ou comunicação mantém a configuração anterior. Uma reconfiguração manual concluída durante a verificação também é preservada. Atualizar não exige excluir ou refazer a integração.

## Correções e reforços

| Superfície | Risco examinado | Alteração e evidência |
|---|---|---|
| Descoberta HA | Anúncio falso altera endereço; anúncio sugere outra porta/certificado | Atualização condicionada à identidade autenticada. Regressões de anúncio falso, certificado/porta anunciados, UUID diferente, mudança legítima e reconfiguração concorrente |
| Receptor HTTP/TLS | Conexão inicial lenta e escrita sem prazo podem ocupar trabalhadores; não havia cota por IP | Dois sockets por IP por listener, 12 totais, quatro trabalhadores e oito na fila. Prazo absoluto de entrada de 10 s e resposta de 8 s. Sockets reais testam cabeçalho/corpo lentos, handshake parado, cota e clientes que não leem |
| Clientes Android | Timeout por leitura isolado não limita toda a resposta | Prazo absoluto com desconexão; teste TLS real de cabeçalhos enviados aos poucos e recusa de erro na obtenção do desafio |
| JSON das APIs | Parsers permissivos, recursão profunda, campos ambíguos e números excessivos | Objeto raiz, UTF-8 válido, chaves únicas, profundidade/trabalho/tamanho limitados e números finitos. Validação anterior ao parser Android; o receptor reutiliza o objeto em vez de decodificá-lo repetidamente |
| Respostas HA/Android | Descompressão automática poderia expandir dados antes da verificação de tamanho | `Accept-Encoding: identity`, recusa de compressão e descompressão automática desativada no HA. Teste com gzip em servidor aiohttp real de loopback, verificando a opção do transporte |
| Sessão nativa | Uma troca concorrente de TV/chave podia misturar autorizações persistidas | A sessão confere UUID, certificado e chave antes de usar/gravar retomada ou vincular avisos; troca de destino encerra a sessão. Teste da regra de identidade/credencial e inspeção dos pontos de gravação; concorrência da Activity não foi executada em Android físico |

A cota é por listener: as portas 8765 e 8766 mantêm contagens separadas. Não é bloqueio de rede por firewall. Vários clientes atrás do mesmo IP compartilham a cota e podem receber falha transitória se excederem dois sockets simultâneos. Os limites completos, incluindo exceções para upload, imagens e PBKDF2, estão em [API](../android/API.md).

JSON das respostas tem limite de 128 KiB; descoberta no controle, 64 KiB. Imagens privadas mantêm limite próprio de 2 MiB. Pedidos comuns têm até 64 KiB; upload, 1,6 MB. Chaves duplicadas, inclusive escritas com escapes equivalentes, JSON profundo, valores não finitos, números longos, JSON concatenado, corpo GET e compressão não entram no processamento normal. Reforços de parser não implicam que tenha sido demonstrada execução de código na versão anterior.

## Proteções existentes verificadas novamente

- HTTPS com certificado SHA-256 aprovado no vínculo; certificado diferente é rejeitado antes de transmitir a senha em conexões reais locais. Não há fallback para HTTP em comandos nem redirecionamento de credenciais.
- HMAC-SHA256 sobre cliente, método, rota, corpo, cookie, CSRF e desafio de uso único. Testes recusam repetição, adulteração, chave errada/revogada, comando em HTTP, desafio expirado e desafio de outra inicialização.
- Pareamento presencial com código temporário, escopo e limite de tentativas. Chaves de controle, espelhamento e HA separadas; sessão de proprietário vinculada ao controle; RTSP reservado ao HA. A câmera respeita a permissão do usuário no Home Assistant.
- Senha com PBKDF2-HMAC-SHA256, bloqueio de tentativas, sessões com expiração e revogação. Não foi reintroduzido segundo fator/SMS. O login automático usa autorização cifrada, sem armazenar senha.
- Android Keystore para segredos; WebView com interface local, sem rede/navegação externa e sem entrega dos tokens/cookies ao JavaScript. Backup do aplicativo desativado.
- Controle nativo Android TV mantém PIN inicial, chave no Keystore, certificado do serviço aprovado e teclas/quadros limitados. Não usa ADB, acessibilidade ou root.

## Validação executada

- **82 testes Java/JUnit aprovados**, com JDK 17, parser HTTP real, sockets de loopback, HTTPS local, certificado EC temporário e suíte TLS com JDK/Conscrypt. Os testes de prazo usam intervalos menores injetados no mesmo caminho de execução para não manter conexões abusivas por longos períodos.
- **112 testes Python aprovados**, Home Assistant 2026.9.3/Python 3.14.7, incluindo 37 entidades, descoberta, câmera, HMAC e TLS real local com RSA/EC. Ruff aprovado.
- **Dois fluxos DOM aprovados:** interface local e canal nativo simulado, incluindo cadastro/login, persistência de sessão, CSRF, revogação, texto seguro, configurações e câmera/VPN ocultas.
- APKs **2.4.1**, versionCode **10**, mínimo API 26/alvo 35; assinatura original v2/v3. Verificação de alinhamento, integridade, permissões, componentes e hashes dos dez MP3s pelo script `android/tools/verify_apks.py`. Não foram adicionadas permissões.
- Integração **2.2.1**, API 3/control_protocol 2 e 37 entidades estáveis. Certificados, chaves, conta, vínculos e alias do controle remoto preservados. Termos continuam em 09/10/2026.

## Limites e uso na rede

Não houve teste desta versão em Android físico ou emulador. Agendamento de timeouts, energia, atualização sobre uma instalação existente, mDNS real, controle remoto, segundo plano e mídia devem ser conferidos nos aparelhos. O teste de gzip usa carga limitada a 1 MB descomprimido; não foi um ensaio de exaustão de memória do HA. Não houve teste de carga distribuída, fuzzing contínuo ou exploração do sistema Android, roteador, codecs ou dependências internas do HA.

Os limites reduzem ocupação abusiva de recursos do aplicativo, mas não impedem saturação do Wi-Fi, ataques distribuídos, bloqueio de mDNS, ARP malicioso ou desligamento do aparelho. O watchdog fecha conexões; não garante interromper cálculo de um handler já iniciado. A proteção autentica a posse das chaves: um aparelho ou credencial comprometido pode agir com as permissões desse vínculo. Revogue vínculos que não reconhecer e mantenha Android, HA e roteador atualizados.

Não encaminhe 8765/8766 ou 6466/6467 diretamente para a Internet. Restrinja o acesso de dispositivos não confiáveis usando a rede/roteador, quando disponível; o aplicativo não configura o firewall. O primeiro certificado deve ser comparado na TV. Identificação e descoberta permanecem públicas por projeto. O aviso com RTSP pode usar uma conexão sem criptografia entre TV e câmera, independentemente do HTTPS dos comandos. Esta atualização não transforma RTSP em transporte cifrado, não remove restrições do Android e não garante aprovação do Play Protect.

## Atualização

Instale TV 2.4.1 no receptor e Celular 2.4.1 ou Controle 2.4.1 no telefone, **como atualização, sem apagar dados**. Atualize o componente HA para 2.2.1 e reinicie o Home Assistant mantendo a entrada existente. Se uma versão anterior já salvou IP incorreto, use Reconfigurar na entrada atual ou aguarde uma descoberta legítima autenticada. Compare qualquer mudança de certificado na própria TV. [Instruções completas](INSTALACAO.md).

Referências técnicas: [OWASP REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html), [OWASP Code Review](https://cheatsheetseries.owasp.org/cheatsheets/Secure_Code_Review_Cheat_Sheet.html), [aiohttp ClientSession/Fingerprint](https://docs.aiohttp.org/en/stable/client_reference.html), [config flow do HA](https://developers.home-assistant.io/docs/config_entries_config_flow_handler/).
