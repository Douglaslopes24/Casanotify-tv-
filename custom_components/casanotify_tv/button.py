"""CasaNotify TV button entities."""

from homeassistant.components.button import ButtonEntity

from .entity import CasaNotifyEntity


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(
        CasaNotifyButton(entry.runtime_data, key, name, icon)
        for key, name, icon in [
            ("test", "Testar aviso", "mdi:message-check"),
            ("clear", "Limpar avisos", "mdi:notification-clear-all"),
        ]
    )


class CasaNotifyButton(CasaNotifyEntity, ButtonEntity):
    async def async_press(self):
        if self.key == "test":
            await self.coordinator.command(
                "/api/notify",
                {
                    "title": "Home Assistant conectado!",
                    "message": "CasaNotify TV pronto para receber seus avisos.",
                    "icon": "check",
                    "replace": True,
                },
            )
        else:
            await self.coordinator.command("/api/clear", {})
