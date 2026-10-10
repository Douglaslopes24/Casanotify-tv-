# Exemplos no Home Assistant

Troque `notify.tv_da_sala_avisos` pela entidade **Avisos** criada na sua instalação.

```yaml
action: casanotify_tv.send_notification
target:
  entity_id: notify.tv_da_sala_avisos
data:
  title: "Tem alguém na porta"
  message: "A campainha tocou."
  icon: bell
  sound: true
  tone: doorbell
  duration: 20
  urgent: true
```

Câmera configurada no Home Assistant (integração CasaNotify 2.2.0):

```yaml
action: casanotify_tv.send_camera_notification
target:
  entity_id: notify.tv_da_sala_avisos
data:
  camera_entity_id: camera.entrada
  title: Entrada
  message: Movimento detectado
  video_muted: true
  duration: 30
  sound: true
  tone: doorbell
```

A entidade deve fornecer uma fonte RTSP que a TV consiga alcançar. A configuração da câmera e seu evento ficam no Home Assistant; o celular pode estar fechado. Exemplo de automação, ajustando os três IDs à sua instalação:

```yaml
alias: Entrada na TV
triggers:
  - trigger: state
    entity_id: binary_sensor.movimento_entrada
    to: "on"
actions:
  - action: casanotify_tv.send_camera_notification
    target:
      entity_id: notify.tv_da_sala_avisos
    data:
      camera_entity_id: camera.entrada
      title: Entrada
      message: Movimento detectado
      id: movimento_entrada
      duration: 30
      replace: true
      video_muted: true
mode: single
```

A ação resolve a fonte no HA; não copie usuário/senha para o YAML da automação. O protocolo RTSP da câmera pode trafegar sem criptografia e a TV deve suportar seu codec, preferencialmente H.264. Não há gravação ou transcodificação nova.

Automações antigas `send_notification` com `camera_id` continuam aceitas pelo vínculo do HA. Também é aceito `video_url` nessa ação, mas o uso da entidade evita repetir credenciais. Para foto, use `image_url: "http://IP_DA_CAMERA/imagem.jpg"` e opcional `image_refresh: 5`, sem vídeo no mesmo aviso. URLs de fotos não aceitam credenciais embutidas.

Para fundo personalizado já enviado pelo painel, use `backdrop: custom`. Toques: `soft`, `doorbell`, `chime`, `pulse`, `alarm`, `digital` e `sound_01` até `sound_10`. Ícones: `home`, `bell`, `door`, `camera`, `light`, `check`, `warning`, `info`, `sensor`, `motion`, `temperature`, `humidity`, `phone`, `delivery`, `chat`, `battery`, `wifi`, `lock`, `smoke`, `water`, `alarm`.

```yaml
action: casanotify_tv.clear_notification
target:
  entity_id: notify.tv_da_sala_avisos
data: {}
```

Sem `id`, limpar remove o aviso atual e a fila. A descoberta automática não configura suas automações ou sensores: selecione o evento desejado no editor de automações e inclua a ação do CasaNotify TV.

## Usar os novos sons (TV 2.2.0 e integração 2.1.0)

Ative som e selecione `sound_01` até `sound_10`. Troque a entidade pela entidade Avisos da sua TV:

```yaml
action: casanotify_tv.send_notification
target:
  entity_id: notify.tv_da_sala_avisos
data:
  title: Campainha
  message: Tem alguém na entrada.
  sound: true
  tone: sound_05
  volume: 70
```

Também pode escolher o toque padrão na entidade de seleção **Toque** ou na aba **Aparência** do controle. A duração dos MP3s recebidos varia de aproximadamente 0,5 a 3,4 segundos; o volume final depende da TV. Os nomes originais e hashes estão em `SONS.json`. Esses arquivos foram fornecidos pelo usuário; a licença MIT do código não é uma declaração de licença dos áudios.
