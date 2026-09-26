"""Standard notify entity plus an action for rich TV overlays."""

from urllib.parse import urlsplit

import voluptuous as vol
from homeassistant.components.notify import NotifyEntity, NotifyEntityFeature
from homeassistant.helpers import config_validation as cv
from homeassistant.helpers import entity_platform

from .const import ANIMATIONS, BACKDROPS, ICONS, POSITIONS, THEMES, TONES
from .entity import CasaNotifyEntity


def image_url(value):
    value = vol.All(cv.string, vol.Length(max=2048))(value)
    if value:
        try:
            url = urlsplit(value)
            if (
                url.scheme not in ("http", "https")
                or not url.hostname
                or url.username
                or url.password
                or url.fragment
            ):
                raise ValueError
        except ValueError as err:
            raise vol.Invalid("Use HTTP/HTTPS without embedded credentials") from err
    return value


def video_url(value):
    value = vol.All(cv.string, vol.Length(max=2048))(value)
    if value:
        try:
            url = urlsplit(value)
            if url.scheme != "rtsp" or not url.hostname or url.fragment or "\r" in value or "\n" in value:
                raise ValueError
            if url.port is not None and not 1 <= url.port <= 65535:
                raise ValueError
        except ValueError as err:
            raise vol.Invalid("Use a valid RTSP camera address") from err
    return value


def integer(minimum, maximum):
    return vol.All(
        vol.Coerce(float),
        vol.Range(min=minimum, max=maximum),
        lambda value: int(value) if value.is_integer() else _invalid_integer(),
    )


def _invalid_integer():
    raise vol.Invalid("Use an integer")


SEND_SCHEMA = {
    vol.Required("message"): vol.All(cv.string, vol.Length(max=1200)),
    vol.Optional("title"): vol.All(cv.string, vol.Length(max=160)),
    vol.Optional("id"): vol.All(cv.string, vol.Length(min=1, max=80)),
    vol.Optional("camera_id"): vol.All(cv.string, vol.Length(min=1, max=80)),
    vol.Optional("video_url"): video_url,
    vol.Optional("video_muted"): cv.boolean,
    vol.Optional("tone"): vol.In(TONES),
    vol.Optional("backdrop"): vol.In(BACKDROPS),
    vol.Optional("image_url"): image_url,
    vol.Optional("image_refresh"): vol.Any(0, integer(5, 60)),
    vol.Optional("theme"): vol.In(THEMES),
    vol.Optional("position"): vol.In(POSITIONS),
    vol.Optional("icon"): vol.In(ICONS),
    vol.Optional("animation"): vol.In(ANIMATIONS),
    vol.Optional("duration"): integer(3, 120),
    vol.Optional("width"): integer(260, 800),
    vol.Optional("text_size"): integer(14, 36),
    vol.Optional("radius"): integer(0, 40),
    vol.Optional("margin"): integer(0, 100),
    vol.Optional("volume"): integer(0, 100),
    vol.Optional("opacity"): vol.All(vol.Coerce(float), vol.Range(min=0.4, max=0.8)),
    **{
        vol.Optional(key): vol.All(cv.string, vol.Match(r"^#[0-9a-fA-F]{6}$"))
        for key in ("background", "text_color", "accent")
    },
    **{vol.Optional(key): cv.boolean for key in ("sound", "speak", "urgent", "replace", "progress")},
}


async def async_setup_entry(hass, entry, async_add_entities):
    async_add_entities(
        [CasaNotifyNotifier(entry.runtime_data, "notifications", "Avisos", "mdi:television-speaker")]
    )
    platform = entity_platform.async_get_current_platform()
    platform.async_register_entity_service("send_notification", SEND_SCHEMA, "async_send_advanced")
    platform.async_register_entity_service(
        "clear_notification",
        {
            vol.Optional("id"): vol.All(cv.string, vol.Length(max=80)),
        },
        "async_clear_notification",
    )


class CasaNotifyNotifier(CasaNotifyEntity, NotifyEntity):
    _attr_supported_features = NotifyEntityFeature.TITLE

    async def async_send_message(self, message, title=None):
        data = {"message": message}
        if title is not None:
            data["title"] = title
        await self.coordinator.command("/api/notify", vol.Schema(SEND_SCHEMA)(data))

    async def async_send_advanced(self, **kwargs):
        await self.coordinator.command("/api/notify", kwargs)
        self._async_record_notification()

    async def async_clear_notification(self, **kwargs):
        await self.coordinator.command("/api/clear", kwargs)
