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

Câmera cadastrada no painel (copie o ID em **Câmeras RTSP**):

```yaml
action: casanotify_tv.send_notification
target:
  entity_id: notify.tv_da_sala_avisos
data:
  title: "Entrada"
  message: "Movimento detectado"
  icon: camera
  camera_id: "COLE_O_ID_DA_CAMERA"
  video_muted: true
  duration: 30
```

Também é aceito `video_url: "rtsp://IP_DA_CAMERA:554/stream"`. Prefira câmera salva quando houver senha, para não deixá-la no YAML. Para foto, use `image_url: "http://IP_DA_CAMERA/imagem.jpg"` e opcional `image_refresh: 5`, sem vídeo no mesmo aviso. URLs de fotos não aceitam credenciais embutidas.

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
