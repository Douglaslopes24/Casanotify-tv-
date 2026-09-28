#!/usr/bin/env python3
"""Inspect the signed artifacts to enforce the TV/phone permission boundary."""

import argparse
import hashlib
import json
import pathlib
import re
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build-tools', required=True)
    parser.add_argument('--jdk', required=True)
    parser.add_argument('--tv', required=True)
    parser.add_argument('--phone', required=True)
    parser.add_argument('--control', required=True)
    parser.add_argument('--expected-certificate', required=True)
    args = parser.parse_args()
    tools = pathlib.Path(args.build_tools)
    java = pathlib.Path(args.jdk) / 'bin/java'

    def run(*command):
        return subprocess.check_output([str(x) for x in command], text=True)

    common = {'android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE'}
    receiver = {
        'android.permission.SYSTEM_ALERT_WINDOW',
        'android.permission.FOREGROUND_SERVICE',
        'android.permission.FOREGROUND_SERVICE_SPECIAL_USE',
        'android.permission.POST_NOTIFICATIONS',
        'android.permission.RECEIVE_BOOT_COMPLETED',
    }
    results = []
    for edition, filename in [('tv', args.tv), ('control', args.control), ('phone', args.phone)]:
        apk = pathlib.Path(filename)
        badging = run(tools / 'aapt', 'dump', 'badging', apk)
        manifest = run(tools / 'aapt', 'dump', 'xmltree', apk, 'AndroidManifest.xml')
        permissions = set(re.findall(r"uses-permission: name='([^']+)'", badging))
        assert permissions == (common | receiver if edition == 'tv' else common), permissions
        assert "name='br.com.casanotify.tv' versionCode='6' versionName='2.1.0'" in badging
        assert "sdkVersion:'26'" in badging and "targetSdkVersion:'35'" in badging
        with zipfile.ZipFile(apk) as archive:
            assert archive.testzip() is None
            dex = b''.join(archive.read(n) for n in archive.namelist() if n.endswith('.dex'))
        if edition == 'tv':
            assert 'BIND_NOTIFICATION_LISTENER_SERVICE' not in manifest
            assert 'PhoneActivity' not in manifest and 'PhoneNotificationService' not in manifest
            for component in ('PhoneActivity', 'PhoneApi', 'PhoneNotificationService'):
                assert f'Lbr/com/casanotify/tv/{component};'.encode() not in dex
            assert b'Landroid/service/notification/NotificationListenerService;' not in dex
            assert 'NotifyService' in manifest and 'MainActivity' in manifest
        elif edition == 'control':
            assert 'BIND_NOTIFICATION_LISTENER_SERVICE' not in manifest
            assert 'PhoneNotificationService' not in manifest
            assert b'Landroid/service/notification/NotificationListenerService;' not in dex
            assert 'ControlActivity' in manifest and 'MainActivity' not in manifest
            assert 'BootReceiver' not in manifest and '.NotifyService' not in manifest
        else:
            assert 'ControlActivity' in manifest
            assert 'BIND_NOTIFICATION_LISTENER_SERVICE' in manifest
            assert 'PhoneActivity' in manifest and 'PhoneNotificationService' in manifest
            assert 'MainActivity' not in manifest and 'BootReceiver' not in manifest
            assert '.NotifyService' not in manifest
        signature = run(java, '-jar', tools / 'lib/apksigner.jar', 'verify', '--print-certs', apk)
        certificate = re.search(r'certificate SHA-256 digest: ([0-9a-f]+)', signature).group(1)
        assert certificate == args.expected_certificate.lower(), 'APK signing identity changed'
        run(tools / 'zipalign', '-c', '4', apk)
        results.append({
            'edition': edition,
            'bytes': apk.stat().st_size,
            'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
            'permissions': sorted(permissions),
            'signature_matches': True,
        })
    print(json.dumps(results, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
