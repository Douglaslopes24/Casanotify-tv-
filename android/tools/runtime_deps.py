"""Fetch locked runtime artifacts and extract Android library resources/classes."""
import hashlib
import json
import pathlib
import urllib.request
import xml.etree.ElementTree as ET
import zipfile


def prepare(root, build, cache):
    cache = pathlib.Path(cache)
    cache.mkdir(parents=True, exist_ok=True)
    jars, resources, packages = [], [], []
    for item in json.loads((root / 'runtime-deps.lock.json').read_text()):
        target = cache / item['file']
        def valid():
            return target.is_file() and target.stat().st_size == item['bytes'] and hashlib.sha256(target.read_bytes()).hexdigest() == item['sha256']
        if not valid():
            data = urllib.request.urlopen(item['url'], timeout=60).read()
            if len(data) != item['bytes'] or hashlib.sha256(data).hexdigest() != item['sha256']:
                raise ValueError('Runtime dependency integrity mismatch: ' + item['file'])
            target.write_bytes(data)
        if target.suffix == '.jar':
            jars.append(target)
            continue
        directory = build / 'runtime' / target.stem
        directory.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(target) as archive:
            jar = directory / 'classes.jar'
            jar.write_bytes(archive.read('classes.jar'))
            jars.append(jar)
            manifest = ET.fromstring(archive.read('AndroidManifest.xml'))
            if manifest.get('package'):
                packages.append(manifest.get('package'))
            for name in archive.namelist():
                if name.startswith('res/') and not name.endswith('/'):
                    path = directory / name
                    if not path.resolve().is_relative_to(directory.resolve()):
                        raise ValueError('Invalid library path')
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(archive.read(name))
        if (directory / 'res').exists():
            resources.append(directory / 'res')
    return jars, resources, packages
