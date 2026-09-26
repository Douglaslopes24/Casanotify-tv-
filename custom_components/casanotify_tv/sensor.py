"""CasaNotify TV sensor entities."""

from homeassistant.components.sensor import SensorEntity
from homeassistant.const import EntityCategory

from .entity import CasaNotifyEntity


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(
        CasaNotifySensor(entry.runtime_data, key, name, icon, EntityCategory.DIAGNOSTIC)
        for key, name, icon in [
            ("queued", "Avisos na fila", "mdi:format-list-numbered"),
            ("version", "Versão do aplicativo", "mdi:information-outline"),
        ]
    )


class CasaNotifySensor(CasaNotifyEntity, SensorEntity):
    @property
    def native_value(self):
        if self.key == "queued":
            return self.status["overlay"]["queued"]
        return self.status["version"]
