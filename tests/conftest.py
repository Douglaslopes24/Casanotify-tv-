"""Exercise real Home Assistant platforms against a simulated receiver."""

from copy import deepcopy
from unittest.mock import patch

import pytest
from pytest_homeassistant_custom_component.common import MockConfigEntry

from custom_components.casanotify_tv.api import CasaNotifyApi

DEVICE_ID = "ddc54cd0-78c8-4de1-b5b8-3c10a8b3e78a"
TOKEN = "a" * 48
FINGERPRINT = "ab" * 32


@pytest.fixture(autouse=True)
def custom_integrations(enable_custom_integrations):
    yield


@pytest.fixture
def tv():
    state = {
        "info": {
            "app": "CasaNotify TV",
            "api_version": 2,
            "version": "2.0.0",
            "tls_fingerprint": FINGERPRINT,
            "tls_port": 8766,
            "device_id": DEVICE_ID,
            "device_name": "TV da sala",
        },
        "config": {
            "device_name": "TV da sala",
            "defaults": {},
            "paused": False,
            "ui_theme": "system",
            "startup_animation": True,
            "auto_start": False,
            "quiet_enabled": False,
            "quiet_start": "22:00",
            "quiet_end": "07:00",
        },
        "overlay": {"active": None, "queued": 0},
        "commands": [],
        "result": "displayed",
    }

    async def info(self):
        return deepcopy(state["info"])

    async def status(self):
        return deepcopy(
            state["info"]
            | {
                "overlay_permission": True,
                "quiet_active": False,
                "vpn_active": False,
                "overlay": state["overlay"],
                "paused": state["config"]["paused"],
            }
        )

    async def config(self):
        return deepcopy(state["config"])

    async def pair(self, code):
        from custom_components.casanotify_tv.api import InvalidAuth

        if code != "123456":
            raise InvalidAuth()
        return TOKEN

    async def request(self, method, path, data=None, **kwargs):
        state["commands"].append((path, deepcopy(data)))
        if path == "/api/config":
            state["config"].update(deepcopy(data))
            state["info"]["device_name"] = state["config"]["device_name"]
            return deepcopy(state["config"])
        if path == "/api/clear":
            state["overlay"] = {"active": None, "queued": 0}
            return {"status": "cleared"}
        return {"status": state["result"], "id": "test"}

    with (
        patch.object(CasaNotifyApi, "info", info),
        patch.object(CasaNotifyApi, "status", status),
        patch.object(CasaNotifyApi, "config", config),
        patch.object(CasaNotifyApi, "pair", pair),
        patch.object(CasaNotifyApi, "request", request),
    ):
        yield state


@pytest.fixture
async def entry(hass, tv, mock_async_zeroconf):
    entry = MockConfigEntry(
        domain="casanotify_tv",
        title="TV da sala",
        unique_id=DEVICE_ID,
        data={
            "host": "192.168.1.50",
            "port": 8765,
            "token": TOKEN,
            "tls_fingerprint": FINGERPRINT,
            "tls_port": 8766,
        },
    )
    entry.add_to_hass(hass)
    assert await hass.config_entries.async_setup(entry.entry_id)
    await hass.async_block_till_done()
    yield entry
    await hass.config_entries.async_unload(entry.entry_id)
    await hass.async_block_till_done()
