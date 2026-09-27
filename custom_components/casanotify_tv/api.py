"""Bounded local client with explicit SHA-256 certificate approval. Credentials never cross redirects."""

import asyncio
import ipaddress
import json
import re
import uuid

import aiohttp


class CasaNotifyError(Exception):
    """Protocol or connectivity failure."""


class InvalidAuth(CasaNotifyError):
    """Invalid API key or expired pairing code."""


class UnsupportedDevice(CasaNotifyError):
    """Another device or older APK."""


def normalize_host(host: str) -> str:
    """The TV intentionally accepts literal addresses only."""
    address = ipaddress.IPv4Address(host.strip())
    if address.is_unspecified or address.is_multicast:
        raise ValueError("Use the IPv4 address displayed on the TV")
    return str(address)


class CasaNotifyApi:
    def __init__(
        self,
        session: aiohttp.ClientSession,
        host: str,
        port: int,
        token: str = "",
        fingerprint: str = "",
        tls_port: int = 8766,
    ) -> None:
        self.session = session
        self.host = normalize_host(host)
        self.port = int(port)
        if not 1 <= self.port <= 65535:
            raise ValueError("Invalid port")
        self.token = token
        self.fingerprint = fingerprint.lower().replace(":", "")
        self.tls_port = int(tls_port)
        if not 1 <= self.tls_port <= 65535:
            raise ValueError("Invalid TLS port")
        self.discovery_url = f"http://{self.host}:{self.port}"
        self.base_url = f"https://{self.host}:{self.tls_port}"

    async def request(self, method, path, data=None, *, authenticate=True):
        discovery = path == "/api/info" and method == "GET" and not authenticate
        if not discovery and not re.fullmatch(r"[0-9a-f]{64}", self.fingerprint):
            raise InvalidAuth("Approve the TV certificate before pairing or sending commands")
        tls = None if discovery else aiohttp.Fingerprint(bytes.fromhex(self.fingerprint))
        headers = {"Accept": "application/json"}
        if authenticate:
            headers["Authorization"] = f"Bearer {self.token}"
        try:
            async with (
                asyncio.timeout(8),
                self.session.request(
                    method,
                    (self.discovery_url if discovery else self.base_url) + path,
                    ssl=tls,
                    json=data,
                    headers=headers,
                    allow_redirects=False,
                ) as response,
            ):
                if response.status in (401, 403):
                    raise InvalidAuth("Access refused by TV")
                if path == "/api/info" and response.status == 404:
                    raise UnsupportedDevice("Install CasaNotify TV 2.0.0 or later")
                body = bytearray()
                async for part in response.content.iter_chunked(8192):
                    body.extend(part)
                    if len(body) > 131072:
                        raise CasaNotifyError("Response exceeds 128 KiB")
                payload = json.loads(body)
                if not isinstance(payload, dict):
                    raise CasaNotifyError("Invalid JSON object")
                if not 200 <= response.status < 300:
                    # No raw server text, notification contents or credentials in logs.
                    raise CasaNotifyError(f"TV returned HTTP {response.status}")
                return payload
        except aiohttp.ServerFingerprintMismatch as err:
            raise InvalidAuth("TV certificate changed: compare it on the TV and pair again") from err
        except (aiohttp.ClientError, TimeoutError, ValueError) as err:
            raise CasaNotifyError("Could not communicate with TV") from err

    async def info(self):
        try:
            info = await self.request("GET", "/api/info", authenticate=False)
        except InvalidAuth as err:
            raise UnsupportedDevice("Install CasaNotify TV 2.0.0 or later") from err
        self.validate_identity(info)
        return info

    @staticmethod
    def validate_identity(info):
        if info.get("app") != "CasaNotify TV" or info.get("api_version") != 2:
            raise UnsupportedDevice("Unsupported app or API")
        try:
            for field in ("device_name", "version"):
                if not isinstance(info[field], str) or not info[field].strip():
                    raise ValueError("Missing device name or version")
            uuid.UUID(info["device_id"])
            if not re.fullmatch(r"[0-9a-fA-F]{64}", info["tls_fingerprint"]):
                raise ValueError("Missing TLS identity")
            if type(info["tls_port"]) is not int or not 1 <= info["tls_port"] <= 65535:
                raise ValueError("Invalid TLS port")
        except (ValueError, KeyError, TypeError, AttributeError) as err:
            raise UnsupportedDevice("Device identity missing") from err

    async def pair(self, code):
        result = await self.request("POST", "/api/pair", {"code": code, "client": "ha"}, authenticate=False)
        token = result.get("token")
        if not isinstance(token, str) or len(token) != 48:
            raise CasaNotifyError("Invalid pairing response")
        self.token = token
        return token

    async def status(self):
        status = await self.request("GET", "/api/status")
        self.validate_identity(status)
        return status

    async def config(self):
        config = await self.request("GET", "/api/config")
        if not isinstance(config.get("defaults"), dict):
            raise CasaNotifyError("Invalid configuration response")
        return config
