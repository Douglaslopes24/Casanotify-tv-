"""CasaNotify TV time entities."""

from datetime import time

from homeassistant.components.time import TimeEntity
from homeassistant.const import EntityCategory

from .entity import CasaNotifyEntity


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(
        CasaNotifyTime(entry.runtime_data, key, name, "mdi:clock-outline", EntityCategory.CONFIG)
        for key, name in [("quiet_start", "Início do silêncio"), ("quiet_end", "Fim do silêncio")]
    )


class CasaNotifyTime(CasaNotifyEntity, TimeEntity):
    @property
    def native_value(self):
        return time.fromisoformat(self.config[self.key])

    async def async_set_value(self, value):
        await self.coordinator.set_value(self.key, value.strftime("%H:%M"))
