"""CasaNotify TV switch entities."""

from homeassistant.components.switch import SwitchEntity
from homeassistant.const import EntityCategory

from .entity import CasaNotifyEntity

DEFINITIONS = [
    ("startup_animation", "Animação de inicialização", "mdi:animation-play", False),
    ("paused", "Pausar avisos", "mdi:pause-circle", False),
    ("auto_start", "Iniciar com a TV", "mdi:restart", False),
    ("quiet_enabled", "Horário silencioso", "mdi:moon-waning-crescent", False),
    ("sound", "Som", "mdi:volume-high", True),
    ("speak", "Leitura em voz alta", "mdi:account-voice", True),
    ("progress", "Barra de progresso", "mdi:progress-clock", True),
]


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(CasaNotifySwitch(entry.runtime_data, *item) for item in DEFINITIONS)


class CasaNotifySwitch(CasaNotifyEntity, SwitchEntity):
    def __init__(self, coordinator, key, name, icon, style):
        super().__init__(coordinator, key, name, icon, EntityCategory.CONFIG)
        self.is_style = style

    @property
    def is_on(self):
        return bool((self.style if self.is_style else self.config)[self.key])

    async def async_turn_on(self, **kwargs):
        await self.coordinator.set_value(self.key, True, style=self.is_style)

    async def async_turn_off(self, **kwargs):
        await self.coordinator.set_value(self.key, False, style=self.is_style)
