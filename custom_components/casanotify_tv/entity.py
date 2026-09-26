"""Entities share a stable TV device and a single polling coordinator."""

from homeassistant.helpers.device_registry import DeviceInfo
from homeassistant.helpers.update_coordinator import CoordinatorEntity

from .const import DOMAIN, resolved_defaults


class CasaNotifyEntity(CoordinatorEntity):
    _attr_has_entity_name = True

    def __init__(self, coordinator, key, name, icon, category=None):
        super().__init__(coordinator)
        self.key = key
        self._attr_unique_id = f"{coordinator.entry.unique_id}_{key}"
        self._attr_name = name
        self._attr_icon = icon
        self._attr_entity_category = category

    @property
    def device_info(self):
        status = self.coordinator.data["status"]
        return DeviceInfo(
            identifiers={(DOMAIN, self.coordinator.entry.unique_id)},
            name=status["device_name"],
            manufacturer="CasaNotify TV",
            model="Receptor Android TV",
            sw_version=status["version"],
            configuration_url=self.coordinator.api.base_url,
        )

    @property
    def config(self):
        return self.coordinator.data["config"]

    @property
    def style(self):
        return resolved_defaults(self.config)

    @property
    def status(self):
        return self.coordinator.data["status"]
