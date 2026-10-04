import asyncio
from unittest.mock import patch

import pytest
from homeassistant.exceptions import ServiceValidationError
from homeassistant.helpers import entity_registry as er

from .conftest import DEVICE_ID


def entity(hass, domain, key):
    return er.async_get(hass).async_get_entity_id(domain, "casanotify_tv", f"{DEVICE_ID}_{key}")


async def test_registers_all_37_entities(hass, entry):
    entries = er.async_entries_for_config_entry(er.async_get(hass), entry.entry_id)
    assert len(entries) == 37
    assert len({e.unique_id for e in entries}) == 37
    assert len({e.device_id for e in entries}) == 1
    for item in entries:
        assert hass.states.get(item.entity_id) is not None
    assert hass.states.get(entity(hass, "number", "opacity")).state == "80"
    assert hass.states.get(entity(hass, "binary_sensor", "connected")).state == "on"


async def test_standard_notify_and_rich_actions(hass, entry, tv):
    target = entity(hass, "notify", "notifications")
    await hass.services.async_call(
        "notify", "send_message", {"entity_id": target, "message": "Olá TV", "title": "Porta"}, blocking=True
    )
    assert tv["commands"][-1] == ("/api/notify", {"message": "Olá TV", "title": "Porta"})
    await hass.services.async_call(
        "casanotify_tv",
        "send_notification",
        {
            "entity_id": target,
            "message": "Campainha",
            "image_url": "http://192.168.1.10/image.jpg",
            "image_refresh": 5,
            "urgent": True,
            "opacity": 0.6,
        },
        blocking=True,
    )
    assert tv["commands"][-1][1]["urgent"] is True
    await hass.services.async_call(
        "casanotify_tv", "clear_notification", {"entity_id": target, "id": "door"}, blocking=True
    )
    assert tv["commands"][-1] == ("/api/clear", {"id": "door"})


async def test_settings_preserve_other_fields_and_theme_clears_colors(hass, entry, tv):
    tv["config"]["defaults"] = {"background": "#ABCDEF", "volume": 72}
    await hass.services.async_call(
        "select",
        "select_option",
        {"entity_id": entity(hass, "select", "theme"), "option": "ocean"},
        blocking=True,
    )
    assert tv["config"]["defaults"] == {"volume": 72, "theme": "ocean"}
    assert hass.states.get(entity(hass, "text", "background")).state == "#122330"
    await asyncio.gather(
        entry.runtime_data.set_value("duration", 25, style=True),
        entry.runtime_data.set_value("sound", True, style=True),
    )
    assert tv["config"]["defaults"] == {"volume": 72, "theme": "ocean", "duration": 25, "sound": True}


@pytest.mark.parametrize(
    "domain,key,service,field,value,expected",
    [
        ("number", "opacity", "set_value", "value", 65, 0.65),
        ("select", "ui_theme", "select_option", "option", "dark", "dark"),
        ("switch", "startup_animation", "turn_off", None, None, False),
        ("select", "tone", "select_option", "option", "doorbell", "doorbell"),
        ("select", "tone", "select_option", "option", "sound_10", "sound_10"),
        ("select", "backdrop", "select_option", "option", "custom", "custom"),
        ("switch", "paused", "turn_on", None, None, True),
        ("text", "accent", "set_value", "value", "#12ABEF", "#12ABEF"),
        ("time", "quiet_start", "set_value", "time", "23:15:00", "23:15"),
    ],
)
async def test_control_platforms(hass, entry, tv, domain, key, service, field, value, expected):
    data = {"entity_id": entity(hass, domain, key)}
    if field:
        data[field] = value
    await hass.services.async_call(domain, service, data, blocking=True)
    config = tv["config"]["defaults"] if key in ("opacity", "accent", "tone", "backdrop") else tv["config"]
    assert config[key] == expected


async def test_buttons_and_suppressed_error(hass, entry, tv):
    await hass.services.async_call(
        "button", "press", {"entity_id": entity(hass, "button", "test")}, blocking=True
    )
    assert tv["commands"][-1][0] == "/api/notify"
    await hass.services.async_call(
        "button", "press", {"entity_id": entity(hass, "button", "clear")}, blocking=True
    )
    assert tv["commands"][-1] == ("/api/clear", {})
    tv["result"] = "suppressed"
    with pytest.raises(ServiceValidationError, match="silenciou"):
        await hass.services.async_call(
            "notify",
            "send_message",
            {"entity_id": entity(hass, "notify", "notifications"), "message": "Test"},
            blocking=True,
        )


async def test_offline_and_recovery(hass, entry, tv):
    from custom_components.casanotify_tv.api import CasaNotifyError

    with patch.object(entry.runtime_data.api, "status", side_effect=CasaNotifyError("Offline")):
        await entry.runtime_data.async_refresh()
    assert hass.states.get(entity(hass, "binary_sensor", "connected")).state == "off"
    assert hass.states.get(entity(hass, "switch", "paused")).state == "unavailable"
    await entry.runtime_data.async_refresh()
    assert hass.states.get(entity(hass, "binary_sensor", "connected")).state == "on"


async def test_diagnostics_redact_credentials_and_identity(hass, entry):
    from custom_components.casanotify_tv.diagnostics import async_get_config_entry_diagnostics

    data = str(await async_get_config_entry_diagnostics(hass, entry))
    for secret in (entry.data["token"], entry.data["host"], DEVICE_ID, "TV da sala"):
        assert secret not in data


async def test_diagnostics_available_before_setup(hass):
    from pytest_homeassistant_custom_component.common import MockConfigEntry

    from custom_components.casanotify_tv.diagnostics import async_get_config_entry_diagnostics

    entry = MockConfigEntry(domain="casanotify_tv", data={"token": "private", "host": "192.168.1.50"})
    entry.add_to_hass(hass)
    data = await async_get_config_entry_diagnostics(hass, entry)
    assert data["available"] is False
    assert data["app_version"] is None
    assert "private" not in str(data)
    assert "192.168.1.50" not in str(data)


async def test_device_name_updates_in_registry(hass, entry, tv):
    from homeassistant.helpers import device_registry as dr

    await hass.services.async_call(
        "text",
        "set_value",
        {"entity_id": entity(hass, "text", "device_name"), "value": "TV do quarto"},
        blocking=True,
    )
    device = dr.async_get(hass).async_get_device_by_identifier(("casanotify_tv", DEVICE_ID), entry.entry_id)
    assert device.name == "TV do quarto"
