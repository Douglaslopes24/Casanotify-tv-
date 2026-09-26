"""CasaNotify TV select entities."""

from homeassistant.components.select import SelectEntity
from homeassistant.const import EntityCategory

from .const import ANIMATIONS, BACKDROPS, ICONS, POSITIONS, THEMES, TONES, UI_THEMES
from .entity import CasaNotifyEntity

DEFINITIONS = [
    ("tone", "Toque", "mdi:music-note", TONES),
    ("backdrop", "Fundo do aviso", "mdi:image", BACKDROPS),
    ("ui_theme", "Tema do aplicativo", "mdi:theme-light-dark", UI_THEMES),
    ("theme", "Tema", "mdi:palette", THEMES),
    ("position", "Posição", "mdi:page-layout-header-footer", POSITIONS),
    ("icon", "Ícone", "mdi:emoticon-outline", ICONS),
    ("animation", "Animação", "mdi:animation", ANIMATIONS),
]


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(CasaNotifySelect(entry.runtime_data, *item) for item in DEFINITIONS)


class CasaNotifySelect(CasaNotifyEntity, SelectEntity):
    def __init__(self, coordinator, key, name, icon, options):
        super().__init__(coordinator, key, name, icon, EntityCategory.CONFIG)
        self._attr_options = options

    @property
    def current_option(self):
        return (self.config if self.key == "ui_theme" else self.style)[self.key]

    async def async_select_option(self, option):
        await self.coordinator.set_value(self.key, option, style=self.key != "ui_theme")
