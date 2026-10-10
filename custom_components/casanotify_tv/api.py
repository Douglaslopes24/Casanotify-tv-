"""Bounded local client with explicit SHA-256 certificate approval. Credentials never cross redirects."""

import asyncio
import hashlib
import hmac
import ipaddress
import json
import math
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


def bounded_json(raw: bytes) -> dict:
    """Bound parser work before decoding; reject ambiguous or non-standard JSON."""
    if len(raw) > 131072:
        raise CasaNotifyError("Response exceeds 128 KiB")
    text = raw.decode("utf-8")
    depth = separators = 0
    quoted = escaped = False
    for char in text:
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
        elif char in "[{":
            depth += 1
            if depth > 17:
                raise ValueError("JSON nesting limit")
        elif char in "]}":
            depth -= 1
        elif char in ",:":
            separators += 1
            if separators > 8192:
                raise ValueError("JSON structure limit")

    def number(value):
        if len(value) > 64:
            raise ValueError("JSON number limit")
        result = float(value) if any(c in value for c in ".eE") else int(value)
        if not math.isfinite(result):
            raise ValueError("Non-finite JSON number")
        return result

    def reject_constant(_):
        raise ValueError("Invalid JSON constant")

    def unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result or len(key) > 128:
                raise ValueError("Duplicate or oversized JSON key")
            result[key] = value
        return result

    result = json.loads(
        text,
        object_pairs_hook=unique_object,
        parse_int=number,
        parse_float=number,
        parse_constant=reject_constant,
    )
    if not isinstance(result, dict):
        raise ValueError("Invalid JSON object")
    pending = [result]
    count = 0
    while pending:
        value = pending.pop()
        count += 1
        if count > 4096:
            raise ValueError("JSON value limit")
        if isinstance(value, dict):
            pending.extend(value.values())
        elif isinstance(value, list):
            pending.extend(value)
    return result


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
        if method not in ("GET", "POST") or not re.fullmatch(r"/(api|media)/[a-z/]+", path):
            raise CasaNotifyError("Invalid operation")
        if not authenticate and (method, path) not in (
            ("GET", "/api/info"),
            ("GET", "/api/challenge"),
            ("POST", "/api/pair"),
        ):
            raise InvalidAuth("Pairing is required")
        body = (
            json.dumps(data, ensure_ascii=False, separators=(",", ":")).encode() if data is not None else None
        )
        headers = {"Accept": "application/json", "Accept-Encoding": "identity"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        if authenticate:
            if not self.token:
                raise InvalidAuth("Pairing is required")
            challenge = (await self.request("GET", "/api/challenge", authenticate=False)).get("challenge", "")
            if not isinstance(challenge, str) or not re.fullmatch(
                r"[0-9]{1,16}\.[A-Za-z0-9_-]{43}\.[0-9a-f]{64}", challenge
            ):
                raise CasaNotifyError("Invalid server challenge; update the TV app")

            def digest(value):
                return hashlib.sha256(value).hexdigest()

            message = "\n".join(
                ("CasaNotify-HMAC-v1", "ha", method, path, challenge, digest(body or b""), digest(b""), "")
            )
            proof = hmac.new(
                digest(self.token.encode()).encode(), message.encode(), hashlib.sha256
            ).hexdigest()
            headers.update(
                {
                    "X-CasaNotify-Client": "ha",
                    "X-CasaNotify-Challenge": challenge,
                    "X-CasaNotify-Proof": proof,
                }
            )
        return await self._send(method, path, body, headers, authenticate)

    async def _send(self, method, path, body, headers, authenticate):
        discovery = path == "/api/info" and method == "GET" and not authenticate
        if not discovery and not re.fullmatch(r"[0-9a-f]{64}", self.fingerprint):
            raise InvalidAuth("Approve the TV certificate before pairing or sending commands")
        tls = None if discovery else aiohttp.Fingerprint(bytes.fromhex(self.fingerprint))
        try:
            async with (
                asyncio.timeout(8),
                self.session.request(
                    method,
                    (self.discovery_url if discovery else self.base_url) + path,
                    ssl=tls,
                    data=body,
                    headers=headers,
                    allow_redirects=False,
                    auto_decompress=False,
                ) as response,
            ):
                if response.status in (401, 403):
                    raise InvalidAuth("Access refused by TV")
                if path == "/api/info" and response.status == 404:
                    raise UnsupportedDevice("Install CasaNotify TV 2.2.0 or later")
                if response.headers.get("Content-Encoding", "identity").lower().strip() != "identity":
                    raise CasaNotifyError("Compressed responses are not supported")
                length = response.headers.get("Content-Length")
                if length is not None and (len(length) > 10 or int(length) > 131072):
                    raise CasaNotifyError("Response exceeds 128 KiB")
                body = bytearray()
                async for part in response.content.iter_chunked(8192):
                    body.extend(part)
                    if len(body) > 131072:
                        raise CasaNotifyError("Response exceeds 128 KiB")
                payload = bounded_json(body)
                if not 200 <= response.status < 300:
                    # No raw server text, notification contents or credentials in logs.
                    raise CasaNotifyError(f"TV returned HTTP {response.status}")
                return payload
        except aiohttp.ServerFingerprintMismatch as err:
            raise InvalidAuth("TV certificate changed: compare it on the TV and pair again") from err
        except (aiohttp.ClientError, TimeoutError, ValueError, RecursionError) as err:
            raise CasaNotifyError("Could not communicate with TV") from err

    async def info(self):
        try:
            info = await self.request("GET", "/api/info", authenticate=False)
        except InvalidAuth as err:
            raise UnsupportedDevice("Install CasaNotify TV 2.2.0 or later") from err
        self.validate_identity(info)
        return info

    @staticmethod
    def validate_identity(info):
        if info.get("app") != "CasaNotify TV" or info.get("api_version") != 3:
            raise UnsupportedDevice("Unsupported app or API")
        try:
            for field, limit in (("device_name", 60), ("version", 40)):
                if not isinstance(info[field], str) or not info[field].strip() or len(info[field]) > limit:
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
