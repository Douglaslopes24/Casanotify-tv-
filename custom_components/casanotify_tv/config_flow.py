"""Zeroconf discovery, manual pairing, reauthentication and IP changes."""

import re

import voluptuous as vol
from homeassistant import config_entries
from homeassistant.const import CONF_HOST, CONF_PORT, CONF_TOKEN
from homeassistant.helpers import selector
from homeassistant.helpers.aiohttp_client import async_get_clientsession
from homeassistant.helpers.service_info.zeroconf import ZeroconfServiceInfo

from .api import CasaNotifyApi, CasaNotifyError, InvalidAuth, UnsupportedDevice, normalize_host
from .const import DOMAIN, PORT


class CasaNotifyConfigFlow(config_entries.ConfigFlow, domain=DOMAIN):
    VERSION = 1

    def __init__(self):
        self.host = ""
        self.port = PORT
        self.info = {}
        self.existing = None

    def client(self, token=""):
        return CasaNotifyApi(
            async_get_clientsession(self.hass),
            self.host,
            self.port,
            token,
            self.info.get("tls_fingerprint", ""),
            self.info.get("tls_port", 8766),
        )

    async def async_step_user(self, user_input=None):
        errors = {}
        if user_input:
            try:
                self.host = normalize_host(user_input[CONF_HOST])
                self.port = user_input[CONF_PORT]
                self.info = await self.client().info()
                if self.existing and self.existing.unique_id != self.info["device_id"]:
                    return self.async_abort(reason="wrong_device")
                await self.async_set_unique_id(self.info["device_id"])
                if not self.existing:
                    self._abort_if_unique_id_configured()
                self.context["title_placeholders"] = {"name": self.info["device_name"]}
                return await self.async_step_pair()
            except UnsupportedDevice:
                errors["base"] = "unsupported_device"
            except ValueError:
                errors[CONF_HOST] = "invalid_host"
            except CasaNotifyError:
                errors["base"] = "cannot_connect"
        return self.async_show_form(
            step_id="user",
            data_schema=vol.Schema(
                {
                    vol.Required(CONF_HOST, default=self.host): str,
                    vol.Required(CONF_PORT, default=self.port): vol.All(
                        vol.Coerce(int), vol.Range(min=1, max=65535)
                    ),
                }
            ),
            errors=errors,
        )

    async def async_step_zeroconf(self, discovery_info: ZeroconfServiceInfo):
        try:
            self.host = normalize_host(discovery_info.host)
            self.port = discovery_info.port
            self.info = await self.client().info()
            if discovery_info.properties.get("id") != self.info["device_id"]:
                return self.async_abort(reason="wrong_device")
        except ValueError, CasaNotifyError:
            return self.async_abort(reason="cannot_connect")
        await self.async_set_unique_id(self.info["device_id"])
        existing = next(
            (entry for entry in self._async_current_entries() if entry.unique_id == self.unique_id),
            None,
        )
        if existing:
            saved = dict(existing.data)
            if (saved[CONF_HOST], saved[CONF_PORT]) == (self.host, self.port):
                return self.async_abort(reason="already_configured")
            # mDNS and /api/info are hints, never proof of a receiver's identity.
            # Authenticate the candidate with the OLD certificate, port and key.
            try:
                trusted = CasaNotifyApi(
                    async_get_clientsession(self.hass),
                    self.host,
                    self.port,
                    saved[CONF_TOKEN],
                    saved.get("tls_fingerprint", ""),
                    saved.get("tls_port", 8766),
                )
                status = await trusted.status()
                if status.get("device_id") != existing.unique_id:
                    return self.async_abort(reason="wrong_device")
            except ValueError, CasaNotifyError:
                return self.async_abort(reason="cannot_connect")
            if dict(existing.data) != saved:
                return self.async_abort(reason="already_configured")
        self._abort_if_unique_id_configured(updates={CONF_HOST: self.host, CONF_PORT: self.port})
        self.context["title_placeholders"] = {"name": self.info["device_name"]}
        return await self.async_step_pair()

    async def async_step_pair(self, user_input=None):
        errors = {}
        if user_input is not None:
            code = user_input.get("code", "").strip().replace(" ", "")
            token = user_input.get(CONF_TOKEN, "").strip()
            if not user_input.get("confirm_fingerprint", False):
                errors["base"] = "confirm_fingerprint"
            elif bool(code) == bool(token) or (code and not re.fullmatch(r"[0-9]{6}", code)):
                errors["base"] = "choose_credential"
            else:
                try:
                    api = self.client(token)
                    if code:
                        token = await api.pair(code)
                    status = await api.status()
                    if status["device_id"] != self.info["device_id"]:
                        return self.async_abort(reason="wrong_device")
                    data = {
                        CONF_HOST: self.host,
                        CONF_PORT: self.port,
                        CONF_TOKEN: token,
                        "tls_fingerprint": self.info["tls_fingerprint"],
                        "tls_port": self.info["tls_port"],
                    }
                    if self.existing:
                        return self.async_update_reload_and_abort(self.existing, data_updates=data)
                    return self.async_create_entry(title=status["device_name"], data=data)
                except InvalidAuth:
                    errors["base"] = "invalid_auth"
                except CasaNotifyError:
                    errors["base"] = "cannot_connect"
        return self.async_show_form(
            step_id="pair",
            data_schema=vol.Schema(
                {
                    vol.Required("confirm_fingerprint", default=False): bool,
                    vol.Optional("code"): selector.TextSelector(
                        selector.TextSelectorConfig(type=selector.TextSelectorType.PASSWORD)
                    ),
                    vol.Optional(CONF_TOKEN): selector.TextSelector(
                        selector.TextSelectorConfig(type=selector.TextSelectorType.PASSWORD)
                    ),
                }
            ),
            errors=errors,
            description_placeholders={
                "name": self.info.get("device_name", "TV"),
                "host": self.host,
                "fingerprint": self.info.get("tls_fingerprint", ""),
            },
        )

    async def async_step_reauth(self, entry_data):
        self.existing = self._get_reauth_entry()
        self.host, self.port = entry_data[CONF_HOST], entry_data[CONF_PORT]
        return await self.async_step_user()

    async def async_step_reconfigure(self, user_input=None):
        self.existing = self._get_reconfigure_entry()
        self.host, self.port = self.existing.data[CONF_HOST], self.existing.data[CONF_PORT]
        return await self.async_step_user()
