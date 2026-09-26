"""CasaNotify TV number entities."""

from homeassistant.components.number import NumberEntity, NumberMode
from homeassistant.const import EntityCategory

from .entity import CasaNotifyEntity

DEFINITIONS = [
    ("duration", "Duração", "mdi:timer-outline", 3, 120, 1, "s"),
    ("width", "Largura", "mdi:arrow-expand-horizontal", 260, 800, 1, "dp"),
    ("text_size", "Tamanho do texto", "mdi:format-size", 14, 36, 1, "sp"),
    ("opacity", "Opacidade", "mdi:opacity", 40, 80, 1, "%"),
    ("radius", "Cantos arredondados", "mdi:rounded-corner", 0, 40, 1, "dp"),
    ("margin", "Margem", "mdi:page-layout-body", 0, 100, 1, "dp"),
    ("volume", "Volume do aviso", "mdi:volume-high", 0, 100, 1, "%"),
]


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(CasaNotifyNumber(entry.runtime_data, *item) for item in DEFINITIONS)


class CasaNotifyNumber(CasaNotifyEntity, NumberEntity):
    _attr_mode = NumberMode.BOX

    def __init__(self, coordinator, key, name, icon, minimum, maximum, step, unit):
        super().__init__(coordinator, key, name, icon, EntityCategory.CONFIG)
        self._attr_native_min_value = minimum
        self._attr_native_max_value = maximum
        self._attr_native_step = step
        self._attr_native_unit_of_measurement = unit

    @property
    def native_value(self):
        value = self.style[self.key]
        return round(value * 100) if self.key == "opacity" else value

    async def async_set_native_value(self, value):
        value = value / 100 if self.key == "opacity" else int(value)
        await self.coordinator.set_value(self.key, value, style=True)
