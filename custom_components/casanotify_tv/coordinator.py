"""Shared polling and serialized configuration updates."""

import asyncio
import logging
from datetime import timedelta

from homeassistant.exceptions import ConfigEntryAuthFailed, ServiceValidationError
from homeassistant.helpers import device_registry as dr
from homeassistant.helpers.update_coordinator import DataUpdateCoordinator, UpdateFailed

from .api import CasaNotifyError, InvalidAuth
from .const import DOMAIN

LOGGER = logging.getLogger(__name__)


class CasaNotifyCoordinator(DataUpdateCoordinator):
    def __init__(self, hass, entry, api):
        super().__init__(
            hass,
            LOGGER,
            name=DOMAIN,
            config_entry=entry,
            update_interval=timedelta(seconds=10),
            always_update=False,
        )
        self.api = api
        self.entry = entry
        self.lock = asyncio.Lock()

    async def _snapshot(self):
        status, config = await asyncio.gather(self.api.status(), self.api.config())
        if status["device_id"] != self.entry.unique_id:
            raise CasaNotifyError("A different TV is using this address")
        registry = dr.async_get(self.hass)
        device = registry.async_get_device_by_identifier((DOMAIN, self.entry.unique_id), self.entry.entry_id)
        if device and (device.name != status["device_name"] or device.sw_version != status["version"]):
            registry.async_update_device(device.id, name=status["device_name"], sw_version=status["version"])
        return {"status": status, "config": config}

    async def _async_update_data(self):
        try:
            async with self.lock:
                return await self._snapshot()
        except InvalidAuth as err:
            raise ConfigEntryAuthFailed(
                "Vincule a TV novamente: confira a chave e o certificado na TV"
            ) from err
        except CasaNotifyError as err:
            raise UpdateFailed(str(err)) from err

    async def set_value(self, key, value, *, style=False):
        try:
            async with self.lock:
                # Read fresh values to retain changes made using the phone panel.
                current = await self.api.config()
                if style:
                    defaults = dict(current["defaults"])
                    if key == "theme":
                        for color in ("background", "text_color", "accent"):
                            defaults.pop(color, None)
                    defaults[key] = value
                    patch = {"defaults": defaults}
                else:
                    patch = {key: value}
                await self.api.request("POST", "/api/config", patch)
                self.async_set_updated_data(await self._snapshot())
        except CasaNotifyError as err:
            if isinstance(err, InvalidAuth):
                self.entry.async_start_reauth(self.hass)
            raise ServiceValidationError(str(err)) from err

    async def command(self, path, data):
        try:
            result = await self.api.request("POST", path, data)
        except CasaNotifyError as err:
            if isinstance(err, InvalidAuth):
                self.entry.async_start_reauth(self.hass)
            raise ServiceValidationError(str(err)) from err
        await self.async_request_refresh()
        if result.get("status") == "suppressed":
            raise ServiceValidationError("A TV silenciou o aviso: verifique a pausa e o horário silencioso")
        return result
