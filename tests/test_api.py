import asyncio

import pytest
import voluptuous as vol

from custom_components.casanotify_tv.api import CasaNotifyApi, CasaNotifyError, InvalidAuth, UnsupportedDevice
from custom_components.casanotify_tv.notify import SEND_SCHEMA

from .conftest import DEVICE_ID, FINGERPRINT, TOKEN

BASE = "https://192.168.1.50:8766"
DISCOVERY = "http://192.168.1.50:8765"
IDENTITY = {
    "app": "CasaNotify TV",
    "api_version": 2,
    "device_id": DEVICE_ID,
    "tls_fingerprint": FINGERPRINT,
    "tls_port": 8766,
}


async def test_https_pair_and_http_discovery(aioclient_mock):
    aioclient_mock.get(DISCOVERY + "/api/info", json=IDENTITY)
    aioclient_mock.post(BASE + "/api/pair", json={"token": TOKEN})
    aioclient_mock.get(BASE + "/api/status", json=IDENTITY)
    async with aioclient_mock.create_session(asyncio.get_running_loop()) as session:
        client = CasaNotifyApi(session, "192.168.1.50", 8765, fingerprint=FINGERPRINT)
        assert (await client.info())["device_id"] == DEVICE_ID
        assert await client.pair("123456") == TOKEN
        assert client.token == TOKEN
        await client.status()
    assert aioclient_mock.call_count == 3


async def test_authentication_error(aioclient_mock):
    aioclient_mock.get(BASE + "/api/status", status=401, json={"error": "invalid"})
    async with aioclient_mock.create_session(asyncio.get_running_loop()) as session:
        with pytest.raises(InvalidAuth):
            await CasaNotifyApi(session, "192.168.1.50", 8765, TOKEN, FINGERPRINT).status()


@pytest.mark.parametrize("status", [401, 404])
async def test_old_app_needs_update(aioclient_mock, status):
    aioclient_mock.get(DISCOVERY + "/api/info", status=status, json={"error": "missing"})
    async with aioclient_mock.create_session(asyncio.get_running_loop()) as session:
        with pytest.raises(UnsupportedDevice):
            await CasaNotifyApi(session, "192.168.1.50", 8765).info()


async def test_redirect_does_not_forward_credentials(aioclient_mock):
    aioclient_mock.get(
        BASE + "/api/status", status=302, headers={"Location": "http://192.168.1.99/capture"}, json={}
    )
    async with aioclient_mock.create_session(asyncio.get_running_loop()) as session:
        with pytest.raises(CasaNotifyError):
            await CasaNotifyApi(session, "192.168.1.50", 8765, TOKEN, FINGERPRINT).status()
    assert aioclient_mock.call_count == 1


async def test_large_response_rejected(aioclient_mock):
    aioclient_mock.get(BASE + "/api/config", text='{"data":"' + "x" * 131073 + '"}')
    async with aioclient_mock.create_session(asyncio.get_running_loop()) as session:
        with pytest.raises(CasaNotifyError, match="128 KiB"):
            await CasaNotifyApi(session, "192.168.1.50", 8765, TOKEN, FINGERPRINT).config()


@pytest.mark.parametrize(
    "patch",
    [
        {"image_refresh": 1},
        {"duration": 2},
        {"duration": 4.5},
        {"opacity": 1},
        {"image_url": "rtsp://192.168.1.1/stream"},
        {"image_url": "http://user:pass@192.168.1.1/a"},
        {"accent": "lime"},
        {"theme": "invalid"},
        {"unknown": True},
        {"video_url": "http://192.168.1.1/stream"},
        {"video_url": "rtsp://camera:70000/stream"},
        {"video_url": "rtsp://camera/\nstream"},
        {"tone": "missing"},
        {"backdrop": "invalid"},
    ],
)
def test_rich_action_rejects_invalid_values(patch):
    with pytest.raises(vol.Invalid):
        vol.Schema(SEND_SCHEMA)({"message": "test"} | patch)


async def test_no_credential_transmission_without_approved_certificate(aioclient_mock):
    async with aioclient_mock.create_session(asyncio.get_running_loop()) as session:
        client = CasaNotifyApi(session, "192.168.1.50", 8765, TOKEN)
        with pytest.raises(InvalidAuth, match="Approve"):
            await client.pair("123456")
        with pytest.raises(InvalidAuth, match="Approve"):
            await client.status()
    assert aioclient_mock.call_count == 0


def test_rtsp_and_new_options():
    result = vol.Schema(SEND_SCHEMA)(
        {
            "message": "Entrada",
            "video_url": "rtsp://user:pass@192.168.1.10:554/live",
            "video_muted": True,
            "tone": "digital",
            "icon": "sensor",
            "backdrop": "custom",
        }
    )
    assert result["video_muted"] is True
