#!/usr/bin/env python3
"""Reproducible native Android build using official SDK tools and JDK 17.
Usage: python3 tools/build_apk.py --sdk /path/to/Android/Sdk --jdk /path/to/jdk
Requires platform 35, build-tools 35.0.0; no runtime third-party dependencies.
"""
import argparse, os, pathlib, secrets, shutil, subprocess, zipfile, xml.etree.ElementTree as ET

def main():
    p=argparse.ArgumentParser();p.add_argument('--sdk',default=os.getenv('ANDROID_HOME'));p.add_argument('--jdk',default=os.getenv('JAVA_HOME'));p.add_argument('--android-jar');p.add_argument('--build-tools');p.add_argument('--output');p.add_argument('--signing-properties')
    a=p.parse_args();root=pathlib.Path(__file__).resolve().parents[1];build=root/'build/manual';build.mkdir(parents=True,exist_ok=True)
    if not a.jdk:p.error('Informe --jdk ou JAVA_HOME.')
    if not a.sdk and not(a.android_jar and a.build_tools):p.error('Informe --sdk ou --android-jar e --build-tools.')
    java=pathlib.Path(a.jdk);sdk=pathlib.Path(a.sdk) if a.sdk else None
    android=pathlib.Path(a.android_jar) if a.android_jar else sdk/'platforms/android-35/android.jar'
    tools=pathlib.Path(a.build_tools) if a.build_tools else sdk/'build-tools/35.0.0'
    env=dict(os.environ,JAVA_HOME=str(java));env['PATH']=str(java/'bin')+os.pathsep+env.get('PATH','')
    def run(*args):subprocess.run([str(x) for x in args],check=True,env=env,cwd=root)
    source=root/'app/src/main';manifest=ET.parse(source/'AndroidManifest.xml');manifest.getroot().set('package','br.com.casanotify.tv');ET.register_namespace('android','http://schemas.android.com/apk/res/android');manifest.write(build/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
    for name in ['gen','classes','dex']:
        path=build/name
        if path.exists():shutil.rmtree(path)
        path.mkdir()
    run(tools/'aapt2','compile','--dir',source/'res','-o',build/'resources.zip')
    run(tools/'aapt2','link','-I',android,'--manifest',build/'AndroidManifest.xml','--java',build/'gen','--min-sdk-version','26','--target-sdk-version','35','--version-code','3','--version-name','2.0.0','-o',build/'resources.apk',build/'resources.zip')
    sources=list((source/'java').rglob('*.java'))+list((build/'gen').rglob('*.java'))
    compiler=[java/'bin/javac'] if (java/'bin/javac').exists() else [java/'bin/java','--module','jdk.compiler/com.sun.tools.javac.Main']
    run(*compiler,'-encoding','UTF-8','-source','8','-target','8','-classpath',android,'-d',build/'classes',*sources)
    with zipfile.ZipFile(build/'classes.jar','w',zipfile.ZIP_DEFLATED) as z:
        for f in (build/'classes').rglob('*.class'):z.write(f,f.relative_to(build/'classes'))
    run(java/'bin/java','-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--release','--lib',android,'--min-api','26','--output',build/'dex',build/'classes.jar')
    shutil.copyfile(build/'resources.apk',build/'unsigned.apk')
    with zipfile.ZipFile(build/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
        for f in (build/'dex').glob('*.dex'):z.write(f,f.name)
        for f in (source/'assets').rglob('*'):
            if f.is_file():z.write(f,'assets/'+str(f.relative_to(source/'assets')))
    run(tools/'zipalign','-f','4',build/'unsigned.apk',build/'aligned.apk')
    props=pathlib.Path(a.signing_properties).resolve() if a.signing_properties else root/'keystore.properties'
    if a.signing_properties and not props.exists():p.error('Arquivo de assinatura não encontrado.')
    if not props.exists():
        (root/'signing').mkdir(exist_ok=True);password=secrets.token_urlsafe(32);env['CASANOTIFY_SIGN_PASS']=password
        run(java/'bin/keytool','-genkeypair','-keystore',root/'signing/casanotify-release.jks','-storetype','PKCS12','-storepass:env','CASANOTIFY_SIGN_PASS','-keypass:env','CASANOTIFY_SIGN_PASS','-alias','casanotify','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=CasaNotify TV, O=Personal Android App, C=BR')
        props.write_text('storeFile=signing/casanotify-release.jks\nstorePassword='+password+'\nkeyAlias=casanotify\nkeyPassword='+password+'\n');props.chmod(0o600)
    settings=dict(line.split('=',1) for line in props.read_text().splitlines() if '=' in line);env['CASANOTIFY_SIGN_PASS']=settings['storePassword']
    output=pathlib.Path(a.output) if a.output else root/'build/CasaNotify-TV-2.0.0.apk';output.parent.mkdir(parents=True,exist_ok=True)
    run(java/'bin/java','-jar',tools/'lib/apksigner.jar','sign','--ks',props.parent/settings['storeFile'],'--ks-key-alias',settings['keyAlias'],'--ks-pass','env:CASANOTIFY_SIGN_PASS','--min-sdk-version','26','--out',output,build/'aligned.apk')
    run(java/'bin/java','-jar',tools/'lib/apksigner.jar','verify','--verbose',output)
    run(tools/'zipalign','-c','4',output)
    print('APK_READY',output,output.stat().st_size)

if __name__=='__main__':main()
