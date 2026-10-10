"""Home Assistant owns RTSP sources and dispatch, independently of the phone UI."""

from unittest.mock import AsyncMock, patch

import pytest
import voluptuous as vol
from homeassistant.auth.permissions.const import POLICY_READ
from homeassistant.core import Context
from homeassistant.exceptions import HomeAssistantError, Unauthorized

from .test_integration import entity

SOURCE = "rtsp://fixture:private-fixture@192.168.1.80:554/stream"
LOOKUP = "custom_components.casanotify_tv.notify.resolve_camera_source"


async def send_camera(hass, **fields):
    await hass.services.async_call(
        "casanotify_tv",
        "send_camera_notification",
        {
            "entity_id": entity(hass, "notify", "notifications"),
            "camera_entity_id": "camera.entrada",
            **fields,
        },
        blocking=True,
    )


async def test_camera_entity_resolved_in_ha_with_safe_defaults(hass, entry, tv):
    with patch(LOOKUP, new=AsyncMock(return_value=SOURCE)) as source:
        await send_camera(hass)
    source.assert_awaited_once_with(hass, "camera.entrada")
    path, data = tv["commands"][-1]
    assert path == "/api/notify"
    assert data == {
        "title": "Câmera de segurança",
        "message": "Veja o que está acontecendo.",
        "duration": 30,
        "video_muted": True,
        "replace": True,
        "video_url": SOURCE,
        "icon": "camera",
    }
    assert "private-fixture" not in str(hass.states.async_all())


async def test_camera_custom_alert_settings_reach_receiver(hass, entry, tv):
    with patch(LOOKUP, new=AsyncMock(return_value=SOURCE)):
        await send_camera(hass, duration=45, tone="sound_10", sound=True, id="door", urgent=True)
    data = tv["commands"][-1][1]
    assert data["duration"] == 45
    assert data["tone"] == "sound_10"
    assert data["sound"] is True
    assert data["id"] == "door"
    assert data["urgent"] is True


@pytest.mark.parametrize("source", [None, "", "https://camera.test/live", "rtsp://", SOURCE + "\r\n"])
async def test_unusable_camera_source_sends_nothing(hass, entry, tv, source):
    with patch(LOOKUP, new=AsyncMock(return_value=source)):
        with pytest.raises(HomeAssistantError, match="RTSP") as caught:
            await send_camera(hass)
    assert "private-fixture" not in str(caught.value)
    assert tv["commands"] == []


@pytest.mark.parametrize("error", [RuntimeError(SOURCE), TimeoutError(SOURCE)])
async def test_camera_failures_never_reveal_credentials(hass, entry, tv, error):
    with patch(LOOKUP, new=AsyncMock(side_effect=error)):
        with pytest.raises(HomeAssistantError) as caught:
            await send_camera(hass)
    assert SOURCE not in str(caught.value)
    assert caught.value.__suppress_context__ is True
    assert tv["commands"] == []


async def test_wrong_domain_rejected_before_camera_lookup(hass, entry, tv):
    with patch(LOOKUP, new=AsyncMock(return_value=SOURCE)) as source:
        with pytest.raises(vol.Invalid):
            await send_camera(hass, camera_entity_id="sensor.entrada")
    source.assert_not_awaited()
    assert tv["commands"] == []


async def test_camera_read_permission_checked_before_source_lookup(hass, entry, tv, hass_admin_user):
    def permitted(entity_id, policy):
        return not (entity_id == "camera.entrada" and policy == POLICY_READ)

    with (
        patch.object(hass_admin_user.permissions, "check_entity", side_effect=permitted),
        patch(LOOKUP, new=AsyncMock(return_value=SOURCE)) as source,
    ):
        with pytest.raises(Unauthorized):
            await hass.services.async_call(
                "casanotify_tv",
                "send_camera_notification",
                {
                    "entity_id": entity(hass, "notify", "notifications"),
                    "camera_entity_id": "camera.entrada",
                },
                context=Context(user_id=hass_admin_user.id),
                blocking=True,
            )
    source.assert_not_awaited()
    assert tv["commands"] == []
