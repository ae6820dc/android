"""End-to-end check on a clean Android emulator, using only its built-in apps."""
import pathlib, re, subprocess, time, xml.etree.ElementTree as ET

PKG = 'io.github.ae6820dc.sajatmenu'
OUT = pathlib.Path('smoke-output')
OUT.mkdir(exist_ok=True)
def adb(*args):
    return subprocess.check_output(['adb', *args], text=True)
def nodes():
    adb('shell','uiautomator','dump','/sdcard/window.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/window.xml')).iter('node'))
def find(label, timeout=15):
    deadline=time.time()+timeout
    while time.time()<deadline:
        for n in nodes():
            if (n.get('text')==label or n.get('content-desc')==label) and (n.get('class')!='android.widget.EditText' or label.startswith('Keresés')): return n
        time.sleep(.3)
    capture('failure')
    (OUT/'failure.xml').write_text(adb('shell','cat','/sdcard/window.xml'))
    raise AssertionError('Missing UI: '+label)
def tap(label):
    n=find(label)
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    time.sleep(.35)
def capture(name):
    with (OUT/(name+'.png')).open('wb') as f:
        subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
def start():
    adb('shell','am','start','-W','-n',PKG+'/.MainActivity')
    find('Saját Menü')
def add(label):
    tap('Menü'); tap('Alkalmazás hozzáadása')
    tap('Keresés az alkalmazások között')
    adb('shell','input','text',label)
    tap(label)

adb('shell','pm','clear',PKG)
start()
find('0 alkalmazás a saját menüdben')
capture('01-empty')
add('Settings')
find('1 alkalmazás a saját menüdben')
tap('Settings')
deadline=time.time()+15
while time.time()<deadline:
    state=adb('shell','dumpsys','activity','activities')
    if any('com.android.settings/' in line for line in state.splitlines() if 'mResumedActivity' in line or 'topResumedActivity' in line): break
    time.sleep(.5)
else: raise AssertionError('Settings did not launch')
adb('shell','input','keyevent','4')
start()
add('Contacts')
find('2 alkalmazás a saját menüdben')
tap('Menü'); tap('Átrendezés'); tap('Contacts'); tap('1. hely')
labels=[n.get('text') for n in nodes() if n.get('text') in ('Settings','Contacts')]
assert labels==['Contacts','Settings'], labels
adb('shell','am','force-stop',PKG)
start(); find('2 alkalmazás a saját menüdben')
labels=[n.get('text') for n in nodes() if n.get('text') in ('Settings','Contacts')]
assert labels==['Contacts','Settings'], labels
capture('02-custom-menu')
tap('Menü'); capture('03-overflow-menu'); tap('Eltávolítás a menüből'); tap('Settings'); tap('Eltávolítás')
find('1 alkalmazás a saját menüdben')
assert any(n.get('text')=='Contacts' for n in nodes())
assert not any(n.get('text')=='Settings' for n in nodes())
assert 'package:com.android.settings' in adb('shell','pm','list','packages','com.android.settings')
capture('04-removed-from-menu')
print('SMOKE_OK: empty start, manual add, search, launch, reorder, persistence, menu-only removal')
