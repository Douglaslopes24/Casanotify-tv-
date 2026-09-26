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

Para fundo personalizado já enviado pelo painel, use `backdrop: custom`. Toques: `soft`, `doorbell`, `chime`, `pulse`, `alarm`, `digital`. Ícones: `home`, `bell`, `door`, `camera`, `light`, `check`, `warning`, `info`, `sensor`, `motion`, `temperature`, `humidity`, `phone`, `delivery`, `chat`, `battery`, `wifi`, `lock`, `smoke`, `water`, `alarm`.

```yaml
action: casanotify_tv.clear_notification
target:
  entity_id: notify.tv_da_sala_avisos
data: {}
```

Sem `id`, limpar remove o aviso atual e a fila. A descoberta automática não configura suas automações ou sensores: selecione o evento desejado no editor de automações e inclua a ação do CasaNotify TV.
