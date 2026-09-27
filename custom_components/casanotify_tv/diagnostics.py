"""Diagnostics exclude keys, IPs, pairing codes and notification contents."""

from .const import INTEGRATION_VERSION


async def async_get_config_entry_diagnostics(hass, entry):
    coordinator = getattr(entry, "runtime_data", None)
    data = (coordinator.data or {}) if coordinator is not None else {}
    status = data.get("status", {})
    config = data.get("config", {})
    return {
        "integration_version": INTEGRATION_VERSION,
        "app_version": status.get("version"),
        "api_version": status.get("api_version"),
        "available": coordinator is not None and coordinator.last_update_success,
        "overlay_permission": status.get("overlay_permission"),
        "paused": config.get("paused"),
        "quiet_enabled": config.get("quiet_enabled"),
        "queued": status.get("overlay", {}).get("queued"),
    }
