"""CasaNotify TV text entities."""

from homeassistant.components.text import TextEntity, TextMode
from homeassistant.const import EntityCategory

from .entity import CasaNotifyEntity

DEFINITIONS = [
    ("device_name", "Nome da TV", "mdi:television", 60, False),
    ("background", "Cor de fundo", "mdi:format-color-fill", 7, True),
    ("text_color", "Cor do texto", "mdi:format-color-text", 7, True),
    ("accent", "Cor de destaque", "mdi:palette", 7, True),
]


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(CasaNotifyText(entry.runtime_data, *item) for item in DEFINITIONS)


class CasaNotifyText(CasaNotifyEntity, TextEntity):
    _attr_mode = TextMode.TEXT
    _attr_native_min = 1

    def __init__(self, coordinator, key, name, icon, maximum, style):
        super().__init__(coordinator, key, name, icon, EntityCategory.CONFIG)
        self._attr_native_max = maximum
        self.is_style = style
        if style:
            self._attr_pattern = "#[0-9a-fA-F]{6}"
            self._attr_native_min = 7

    @property
    def native_value(self):
        return (self.style if self.is_style else self.config)[self.key]

    async def async_set_value(self, value):
        await self.coordinator.set_value(self.key, value, style=self.is_style)
