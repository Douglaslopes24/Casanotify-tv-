"""CasaNotify TV local integration."""

from homeassistant.const import CONF_HOST, CONF_PORT, CONF_TOKEN
from homeassistant.exceptions import ConfigEntryAuthFailed
from homeassistant.helpers.aiohttp_client import async_get_clientsession

from .api import CasaNotifyApi
from .const import PLATFORMS
from .coordinator import CasaNotifyCoordinator


async def async_setup_entry(hass, entry):
    if not entry.data.get("tls_fingerprint"):
        raise ConfigEntryAuthFailed(
            "Atualize o APK para 2.0 e vincule novamente para aprovar o certificado HTTPS"
        )
    api = CasaNotifyApi(
        async_get_clientsession(hass),
        entry.data[CONF_HOST],
        entry.data[CONF_PORT],
        entry.data[CONF_TOKEN],
        entry.data["tls_fingerprint"],
        entry.data.get("tls_port", 8766),
    )
    coordinator = CasaNotifyCoordinator(hass, entry, api)
    await coordinator.async_config_entry_first_refresh()
    entry.runtime_data = coordinator
    entry.async_on_unload(entry.add_update_listener(async_reload_entry))
    await hass.config_entries.async_forward_entry_setups(entry, PLATFORMS)
    return True


async def async_reload_entry(hass, entry):
    await hass.config_entries.async_reload(entry.entry_id)


async def async_unload_entry(hass, entry):
    return await hass.config_entries.async_unload_platforms(entry, PLATFORMS)
