"""CasaNotify TV binary sensor entities."""

from homeassistant.components.binary_sensor import BinarySensorDeviceClass, BinarySensorEntity
from homeassistant.const import EntityCategory

from .entity import CasaNotifyEntity

DEFINITIONS = [
    ("vpn_active", "VPN ativa", "mdi:vpn"),
    ("connected", "Receptor conectado", "mdi:lan-connect"),
    ("overlay_permission", "Sobreposição permitida", "mdi:picture-in-picture-top-right"),
    ("notification_active", "Aviso na tela", "mdi:message-text"),
    ("quiet_active", "Silêncio ativo agora", "mdi:moon-waning-crescent"),
]


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(CasaNotifyBinarySensor(entry.runtime_data, *item) for item in DEFINITIONS)


class CasaNotifyBinarySensor(CasaNotifyEntity, BinarySensorEntity):
    def __init__(self, coordinator, key, name, icon):
        super().__init__(coordinator, key, name, icon)
        if key == "connected":
            self._attr_device_class = BinarySensorDeviceClass.CONNECTIVITY
            self._attr_entity_category = EntityCategory.DIAGNOSTIC

    @property
    def available(self):
        return True if self.key == "connected" else super().available

    @property
    def is_on(self):
        if self.key == "connected":
            return self.coordinator.last_update_success
        if self.key == "notification_active":
            return bool(self.status["overlay"]["active"])
        return bool(self.status[self.key])
