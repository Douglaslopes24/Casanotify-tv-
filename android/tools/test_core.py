#!/usr/bin/env python3
"""Run Java protocol/authentication tests and a real local TLS handshake (JDK 17)."""
import argparse
import pathlib
import subprocess
import tempfile

p = argparse.ArgumentParser()
p.add_argument('--jdk', required=True)
p.add_argument('--libs', required=True, help='Folder containing junit.jar, hamcrest.jar, json.jar and conscrypt.jar')
a = p.parse_args()
root = pathlib.Path(__file__).resolve().parents[1]
java = pathlib.Path(a.jdk) / 'bin'
libs = pathlib.Path(a.libs).resolve()
classpath = ':'.join(str(libs / name) for name in ['junit.jar', 'hamcrest.jar', 'json.jar', 'conscrypt.jar'])
with tempfile.TemporaryDirectory(prefix='casanotify-tests-') as work:
    output = pathlib.Path(work)
    key = output / 'tls-fixture.p12'
    subprocess.run([str(java / 'keytool'), '-genkeypair', '-keystore', str(key), '-storetype', 'PKCS12', '-storepass', 'test-fixture-only', '-keypass', 'test-fixture-only', '-alias', 'local', '-keyalg', 'EC', '-groupname', 'secp256r1', '-validity', '1', '-dname', 'CN=localhost', '-ext', 'SAN=IP:127.0.0.1,DNS:localhost'], check=True, capture_output=True)
    package = root / 'app/src/main/java/br/com/casanotify/tv'
    sources = [package / (name + '.java') for name in ['Notice', 'QuietHours', 'Pairing', 'LanServer', 'AuthCrypto', 'AccountManager', 'MirrorFilter', 'DeviceKeyManager', 'CertificatePin', 'RequestAuth']]
    sources += [root/'app/src/companion/java/br/com/casanotify/tv/ControlClient.java']
    sources += sorted((root / 'app/src/test/java').rglob('*.java'))
    sources += sorted((root / 'app/src/testCompanion/java').rglob('*.java'))
    compiler = [java / 'javac'] if (java / 'javac').exists() else [java / 'java', '--module', 'jdk.compiler/com.sun.tools.javac.Main']
    subprocess.run([str(x) for x in compiler] + ['-encoding', 'UTF-8', '-classpath', classpath, '-d', str(output)] + [str(x) for x in sources], check=True)
    subprocess.run([str(java / 'java'), '--add-opens=java.base/java.net=ALL-UNNAMED', '-Dcasanotify.testKeystore=' + str(key), '-cp', str(output) + ':' + classpath, 'org.junit.runner.JUnitCore', 'br.com.casanotify.tv.CoreTest', 'br.com.casanotify.tv.SecurityTest', 'br.com.casanotify.tv.TlsTest', 'br.com.casanotify.tv.ControlClientTest', 'br.com.casanotify.tv.AttackTest'], check=True)
