#!/usr/bin/env python3
"""Emulator'de uygulamayi acar, menuleri gezer ve ekran goruntusu alir (screens/ klasorune)."""
import subprocess, time, re, pathlib, sys
import xml.etree.ElementTree as ET

PKG = "cylauncher.com"
OUT = pathlib.Path("screens"); OUT.mkdir(exist_ok=True)

def adb(*args, out=False):
    r = subprocess.run(["adb", *args], capture_output=True, text=True)
    return r.stdout if out else r.returncode

def shot(name):
    with open(OUT / f"{name}.png", "wb") as f:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=f)
    print("  [foto]", name, flush=True)

def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/u.xml")
    adb("pull", "/sdcard/u.xml", "/tmp/u.xml")
    try:
        return list(ET.parse("/tmp/u.xml").getroot().iter("node"))
    except Exception:
        return []

def center(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2

def find(rid=None, texts=None):
    for n in nodes():
        if rid and n.get("resource-id", "").endswith(":id/" + rid):
            return n
        if texts and n.get("text", "").strip().lower() in [t.lower() for t in texts]:
            return n
    return None

def tap(n):
    x, y = center(n); adb("shell", "input", "tap", str(x), str(y)); print("  [tikla]", n.get("resource-id") or n.get("text"), flush=True)

def wait_tap(rid=None, texts=None, timeout=40):
    end = time.time() + timeout
    while time.time() < end:
        n = find(rid, texts)
        if n is not None:
            tap(n); return True
        time.sleep(2)
    print("  [yok]", rid or texts, flush=True)
    return False

def wait_for(rid, timeout=40):
    end = time.time() + timeout
    while time.time() < end:
        if find(rid) is not None: return True
        time.sleep(2)
    return False

def back():
    adb("shell", "input", "keyevent", "KEYCODE_BACK"); time.sleep(2)

# ---- kurulum ----
print("APK kuruluyor", flush=True)
adb("install", "-r", "-g", "apk/CeyoLauncher-debug.apk")
adb("shell", "appops", "set", PKG, "MANAGE_EXTERNAL_STORAGE", "allow")
adb("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS")
adb("logcat", "-c")
adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
time.sleep(8)
shot("01_acilis")

# izin/uyari penceresi cikarsa gec
for _ in range(3):
    n = find(texts=["Skip for now", "Şimdilik atla", "Allow", "İzin ver"])
    if n is None: break
    tap(n); time.sleep(3)

# ---- giris (yerel hesap) ----
if find("button_local_authentication") is not None or find("login_menu") is not None:
    shot("02_giris")
    if wait_tap("button_local_authentication", timeout=10):
        if wait_for("login_edit_email", 15):
            tap(find("login_edit_email")); time.sleep(1)
            adb("shell", "input", "text", "cyoyun")
            adb("shell", "input", "keyevent", "KEYCODE_BACK")  # klavyeyi kapat
            time.sleep(1)
            shot("03_yerel_hesap")
            wait_tap("login_button", timeout=10)
            time.sleep(6)

# ---- ana ekran ----
if wait_for("ceyo_mod_button", 60):
    time.sleep(3); shot("04_ana_ekran")

    # Mod Indir (Modrinth)
    if wait_tap("ceyo_modrinth_button", timeout=10):
        wait_for("search_mod_list", 20); time.sleep(10)
        shot("05_mod_indir_modrinth")
        back()

    # Mod Yukle -> profil turu
    if wait_tap("ceyo_mod_button", timeout=10):
        wait_for("vanilla_profile", 20); time.sleep(2)
        shot("06_profil_turu")
        # Vanilla -> profil duzenleme
        if wait_tap("vanilla_profile", timeout=10):
            wait_for("vprof_editor_profile_name", 20); time.sleep(2)
            shot("07_profil_duzenle")
            adb("shell", "input", "swipe", "540", "1800", "540", "700", "400"); time.sleep(1)
            shot("08_profil_duzenle_alt")
            back()
        back()
    # profil secici
    if wait_tap("mc_version_spinner", timeout=10):
        time.sleep(2); shot("09_profil_secici"); back()
else:
    print("Ana ekran bulunamadi", flush=True)
    shot("99_ana_ekran_yok")

shot("10_son")
open(OUT / "logcat.txt", "w").write(adb("logcat", "-d", "-t", "400", "*:E", out=True))
