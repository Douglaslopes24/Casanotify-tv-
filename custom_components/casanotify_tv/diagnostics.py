"""Diagnostics exclude keys, IPs, pairing codes and notification contents."""


async def async_get_config_entry_diagnostics(hass, entry):
    coordinator = entry.runtime_data
    status = coordinator.data["status"]
    config = coordinator.data["config"]
    return {
        "integration_version": "2.0.0",
        "app_version": status["version"],
        "api_version": status["api_version"],
        "available": coordinator.last_update_success,
        "overlay_permission": status["overlay_permission"],
        "paused": config["paused"],
        "quiet_enabled": config["quiet_enabled"],
        "queued": status["overlay"]["queued"],
    }
