from ipaddress import ip_address
from unittest.mock import patch

from homeassistant import config_entries
from homeassistant.data_entry_flow import FlowResultType
from homeassistant.helpers.service_info.zeroconf import ZeroconfServiceInfo

from .conftest import DEVICE_ID, FINGERPRINT, TOKEN


def discovery(host="192.168.1.50", identity=DEVICE_ID):
    address = ip_address(host)
    return ZeroconfServiceInfo(
        ip_address=address,
        ip_addresses=[address],
        port=8765,
        hostname="casanotify.local.",
        type="_casanotify._tcp.local.",
        name="CasaNotify-test._casanotify._tcp.local.",
        properties={"id": identity},
    )


async def test_manual_pair_and_complete_setup(hass, tv, mock_async_zeroconf):
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv", context={"source": config_entries.SOURCE_USER}
    )
    assert result["step_id"] == "user"
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"host": "192.168.1.50", "port": 8765}
    )
    assert result["step_id"] == "pair"
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"code": "123456", "confirm_fingerprint": True}
    )
    assert result["type"] is FlowResultType.CREATE_ENTRY
    assert result["data"]["token"] == TOKEN
    assert result["data"]["tls_fingerprint"] == FINGERPRINT
    assert result["result"].unique_id == DEVICE_ID
    await hass.async_block_till_done()


async def test_discovery_requires_pairing_and_rejects_bad_code(hass, tv, mock_async_zeroconf):
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv", context={"source": config_entries.SOURCE_ZEROCONF}, data=discovery()
    )
    assert result["type"] is FlowResultType.FORM
    assert not hass.config_entries.async_entries("casanotify_tv")
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"code": "999999", "confirm_fingerprint": True}
    )
    assert result["errors"] == {"base": "invalid_auth"}
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"code": "123456", "token": TOKEN, "confirm_fingerprint": True}
    )
    assert result["errors"] == {"base": "choose_credential"}
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"token": TOKEN, "confirm_fingerprint": True}
    )
    assert result["type"] is FlowResultType.CREATE_ENTRY
    await hass.async_block_till_done()


async def test_rediscovery_updates_ip_without_duplicates(hass, entry, tv):
    tv["info"]["tls_fingerprint"] = "cd" * 32
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv", context={"source": config_entries.SOURCE_ZEROCONF}, data=discovery("192.168.1.60")
    )
    assert result["reason"] == "already_configured"
    assert entry.data["host"] == "192.168.1.60"
    assert entry.data["tls_fingerprint"] == FINGERPRINT
    assert len(hass.config_entries.async_entries("casanotify_tv")) == 1
    await hass.async_block_till_done()
    assert entry.runtime_data.api.host == "192.168.1.60"


async def test_wrong_discovered_identity(hass, tv, mock_async_zeroconf):
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv",
        context={"source": config_entries.SOURCE_ZEROCONF},
        data=discovery(identity="different"),
    )
    assert result["reason"] == "wrong_device"


async def test_invalid_ip_and_old_app(hass, tv, mock_async_zeroconf):
    from custom_components.casanotify_tv.api import CasaNotifyApi, UnsupportedDevice

    result = await hass.config_entries.flow.async_init(
        "casanotify_tv", context={"source": config_entries.SOURCE_USER}
    )
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"host": "http://192.168.1.50:8765", "port": 8765}
    )
    assert result["errors"] == {"host": "invalid_host"}
    with patch.object(CasaNotifyApi, "info", side_effect=UnsupportedDevice()):
        result = await hass.config_entries.flow.async_configure(
            result["flow_id"], {"host": "192.168.1.50", "port": 8765}
        )
    assert result["errors"] == {"base": "unsupported_device"}


async def test_reauthentication(hass, entry, tv):
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv",
        context={"source": config_entries.SOURCE_REAUTH, "entry_id": entry.entry_id},
        data=entry.data,
    )
    assert result["step_id"] == "user"
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"host": "192.168.1.50", "port": 8765}
    )
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"code": "123456", "confirm_fingerprint": True}
    )
    assert result["reason"] == "reauth_successful"
    await hass.async_block_till_done()


async def test_pair_requires_physical_fingerprint_confirmation(hass, tv, mock_async_zeroconf):
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv", context={"source": config_entries.SOURCE_ZEROCONF}, data=discovery()
    )
    assert result["description_placeholders"]["fingerprint"] == FINGERPRINT
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"code": "123456", "confirm_fingerprint": False}
    )
    assert result["errors"] == {"base": "confirm_fingerprint"}
    assert not hass.config_entries.async_entries("casanotify_tv")


async def test_legacy_entry_requires_certificate_reapproval(hass, tv, mock_async_zeroconf):
    from homeassistant.config_entries import ConfigEntryState
    from pytest_homeassistant_custom_component.common import MockConfigEntry

    old = MockConfigEntry(
        domain="casanotify_tv",
        title="TV antiga",
        unique_id=DEVICE_ID,
        data={"host": "192.168.1.50", "port": 8765, "token": TOKEN},
    )
    old.add_to_hass(hass)
    assert not await hass.config_entries.async_setup(old.entry_id)
    await hass.async_block_till_done()
    assert old.state is ConfigEntryState.SETUP_ERROR
    flows = hass.config_entries.flow.async_progress()
    assert any(f["context"]["source"] == config_entries.SOURCE_REAUTH for f in flows)


async def test_new_certificate_needs_confirmation_and_preserves_entities(hass, entry, tv):
    from homeassistant.helpers import entity_registry as er

    old_ids = {e.entity_id for e in er.async_entries_for_config_entry(er.async_get(hass), entry.entry_id)}
    new_pin = "cd" * 32
    tv["info"]["tls_fingerprint"] = new_pin
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv",
        context={"source": config_entries.SOURCE_REAUTH, "entry_id": entry.entry_id},
        data=entry.data,
    )
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"host": "192.168.1.50", "port": 8765}
    )
    assert result["description_placeholders"]["fingerprint"] == new_pin
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"code": "123456", "confirm_fingerprint": False}
    )
    assert result["errors"] == {"base": "confirm_fingerprint"}
    assert entry.data["tls_fingerprint"] == FINGERPRINT
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"code": "123456", "confirm_fingerprint": True}
    )
    assert result["reason"] == "reauth_successful"
    await hass.async_block_till_done()
    assert entry.data["tls_fingerprint"] == new_pin
    assert entry.runtime_data.api.fingerprint == new_pin
    assert old_ids == {
        e.entity_id for e in er.async_entries_for_config_entry(er.async_get(hass), entry.entry_id)
    }


async def test_reconfigure_changes_address_without_duplicate_device(hass, entry, tv):
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv",
        context={"source": config_entries.SOURCE_RECONFIGURE, "entry_id": entry.entry_id},
    )
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"host": "192.168.1.80", "port": 8765}
    )
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"token": TOKEN, "confirm_fingerprint": True}
    )
    assert result["reason"] == "reconfigure_successful"
    await hass.async_block_till_done()
    assert entry.data["host"] == "192.168.1.80"
    assert entry.runtime_data.api.host == "192.168.1.80"
    assert len(hass.config_entries.async_entries("casanotify_tv")) == 1


async def test_reauthentication_rejects_different_tv(hass, entry, tv):
    tv["info"]["device_id"] = "3737dc1e-385e-41f1-acb7-b10eaa4e2aec"
    old_data = dict(entry.data)
    result = await hass.config_entries.flow.async_init(
        "casanotify_tv",
        context={"source": config_entries.SOURCE_REAUTH, "entry_id": entry.entry_id},
        data=entry.data,
    )
    result = await hass.config_entries.flow.async_configure(
        result["flow_id"], {"host": "192.168.1.80", "port": 8765}
    )
    assert result["reason"] == "wrong_device"
    assert dict(entry.data) == old_data
