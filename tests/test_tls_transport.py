"""Real HTTPS against an ephemeral localhost certificate, no TV or external network."""

import hashlib
import ssl
from datetime import UTC, datetime, timedelta

import aiohttp
import pytest
from aiohttp import web
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, rsa
from cryptography.x509.oid import NameOID

from custom_components.casanotify_tv.api import CasaNotifyApi, InvalidAuth

from .conftest import DEVICE_ID, TOKEN


@pytest.mark.parametrize("key_type", ["rsa", "ec"])
async def test_pin_prevents_credentials_reaching_changed_certificate(tmp_path, socket_enabled, key_type):
    # The fixture runs after Home Assistant's socket guard; only localhost is allowed.
    key = (
        ec.generate_private_key(ec.SECP256R1())
        if key_type == "ec"
        else rsa.generate_private_key(public_exponent=65537, key_size=2048)
    )
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "CasaNotify test")])
    cert = (
        x509.CertificateBuilder()
        .subject_name(name)
        .issuer_name(name)
        .public_key(key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(datetime.now(UTC) - timedelta(minutes=1))
        .not_valid_after(datetime.now(UTC) + timedelta(days=1))
        .sign(key, hashes.SHA256())
    )
    certificate, private = tmp_path / "cert.pem", tmp_path / "key.pem"
    certificate.write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    private.write_bytes(
        key.private_bytes(
            serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()
        )
    )
    pin = hashlib.sha256(cert.public_bytes(serialization.Encoding.DER)).hexdigest()
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(certificate, private)
    received = []

    async def handler(request):
        received.append((request.path, request.headers.get("Authorization")))
        return web.json_response(
            {
                "app": "CasaNotify TV",
                "api_version": 2,
                "device_name": "TV de teste",
                "version": "2.0.1",
                "device_id": DEVICE_ID,
                "tls_fingerprint": pin,
                "tls_port": site._server.sockets[0].getsockname()[1],
            }
        )

    app = web.Application()
    app.router.add_get("/api/status", handler)
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "127.0.0.1", 0, ssl_context=context)
    await site.start()
    port = site._server.sockets[0].getsockname()[1]
    try:
        async with aiohttp.ClientSession() as session:
            good = CasaNotifyApi(session, "127.0.0.1", 8765, TOKEN, pin, port)
            assert (await good.status())["device_id"] == DEVICE_ID
            assert received == [("/api/status", "Bearer " + TOKEN)]
            changed = CasaNotifyApi(session, "127.0.0.1", 8765, TOKEN, "00" * 32, port)
            with pytest.raises(InvalidAuth, match="certificate changed"):
                await changed.status()
            assert len(received) == 1
    finally:
        await runner.cleanup()
