"""Local attack regressions; no user's TV or external target is contacted."""

import asyncio
import gzip
from unittest.mock import patch

import aiohttp
import pytest
from aiohttp import web
from homeassistant import config_entries

from custom_components.casanotify_tv.api import CasaNotifyApi, CasaNotifyError, InvalidAuth, bounded_json

from .conftest import DEVICE_ID, FINGERPRINT, TOKEN
from .test_config_flow import discovery


async def test_forged_rediscovery_cannot_move_saved_address(hass, entry, tv):
    old = dict(entry.data)
    with patch.object(CasaNotifyApi, "status", side_effect=InvalidAuth("Untrusted certificate")):
        result = await hass.config_entries.flow.async_init(
            "casanotify_tv",
            context={"source": config_entries.SOURCE_ZEROCONF},
            data=discovery("192.168.1.99"),
        )
    assert result["type"] == "abort"
    assert dict(entry.data) == old


async def test_discovery_cannot_replace_pin_or_tls_port(hass, entry, tv):
    old = dict(entry.data)
    tv["info"]["tls_fingerprint"] = "cd" * 32
    tv["info"]["tls_port"] = 443

    async def authenticate(client):
        assert client.fingerprint == FINGERPRINT
        assert client.tls_port == 8766
        assert client.token == TOKEN
        assert client.host == "192.168.1.99"
        raise InvalidAuth("Untrusted certificate")

    with patch.object(CasaNotifyApi, "status", authenticate):
        await hass.config_entries.flow.async_init(
            "casanotify_tv",
            context={"source": config_entries.SOURCE_ZEROCONF},
            data=discovery("192.168.1.99"),
        )
    assert dict(entry.data) == old
    assert entry.data["tls_fingerprint"] == FINGERPRINT


async def test_rediscovery_must_confirm_saved_uuid_through_pinned_connection(hass, entry, tv):
    old = dict(entry.data)
    with patch.object(CasaNotifyApi, "status", return_value={"device_id": "other-tv"}):
        await hass.config_entries.flow.async_init(
            "casanotify_tv",
            context={"source": config_entries.SOURCE_ZEROCONF},
            data=discovery("192.168.1.99"),
        )
    assert dict(entry.data) == old


async def test_rediscovery_cannot_overwrite_concurrent_manual_reconfiguration(hass, entry, tv):
    async def reconfigure(client):
        hass.config_entries.async_update_entry(entry, data=dict(entry.data) | {"host": "192.168.1.70"})
        return {"device_id": DEVICE_ID}

    with patch.object(CasaNotifyApi, "status", reconfigure):
        await hass.config_entries.flow.async_init(
            "casanotify_tv",
            context={"source": config_entries.SOURCE_ZEROCONF},
            data=discovery("192.168.1.99"),
        )
    await hass.async_block_till_done()
    assert entry.data["host"] == "192.168.1.70"


BAD_JSON = [
    b'{"a":1,"a":2}',
    b'{"a":1,"\\u0061":2}',
    b'{"a":NaN}',
    b'{"a":Infinity}',
    b'{"a":1e99999}',
    b'{"a":' + b"9" * 65 + b"}",
    b"{}{}",
    b'{"a":"\xc0\xaf"}',
    b'{"x":' + b"[" * 10000 + b"0" + b"]" * 10000 + b"}",
    b'{"x":[' + b"0," * 5000 + b"0]}",
    b'{"' + b"x" * 129 + b'":0}',
    b"[]",
]


@pytest.mark.parametrize("body", BAD_JSON)
def test_wire_json_rejects_ambiguity_depth_work_and_invalid_utf8(body):
    with pytest.raises((ValueError, CasaNotifyError)):
        bounded_json(body)


def test_wire_json_keeps_legitimate_portuguese_and_nested_settings():
    assert bounded_json('{"nome":"Câmera da sala","n":-1.25e2,"config":{"lista":[true,null]}}'.encode()) == {
        "nome": "Câmera da sala",
        "n": -125,
        "config": {"lista": [True, None]},
    }


@pytest.mark.parametrize("body", BAD_JSON)
async def test_bad_discovery_responses_fail_with_controlled_error(aioclient_mock, body):
    aioclient_mock.get("http://192.168.1.50:8765/api/info", content=body)
    async with aioclient_mock.create_session(asyncio.get_running_loop()) as session:
        with pytest.raises(CasaNotifyError):
            await CasaNotifyApi(session, "192.168.1.50", 8765).info()


async def test_gzip_bomb_rejected_before_decompression_with_real_aiohttp(socket_enabled):
    compressed = gzip.compress(b'{"data":"' + b"x" * 1_000_000 + b'"}')
    seen = []

    async def response(request):
        seen.append(request.headers.get("Accept-Encoding"))
        return web.Response(body=compressed, headers={"Content-Encoding": "gzip"})

    app = web.Application()
    app.router.add_get("/api/info", response)
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "127.0.0.1", 0)
    await site.start()
    port = site._server.sockets[0].getsockname()[1]
    try:
        async with aiohttp.ClientSession() as session:
            with patch.object(aiohttp.ClientSession, "request", wraps=session.request) as call:
                with pytest.raises(CasaNotifyError, match="Compressed"):
                    await CasaNotifyApi(session, "127.0.0.1", port).info()
                assert call.call_args.kwargs["auto_decompress"] is False
                assert call.call_args.kwargs["allow_redirects"] is False
        assert seen == ["identity"]
    finally:
        await runner.cleanup()
