#!/usr/bin/env python3
"""Ceyo Launcher: Pojav kaynagina kucuk, guvenli yamalar uygular.
Her yama once 'zaten uygulanmis mi' diye bakar (tekrar calistirmak zararsiz).
Bir capa (anchor) bulunamazsa build'i DURDURUR ve hangi dosyada oldugunu soyler."""
import sys, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
JAVA = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch"
MANIFEST = ROOT / "app_pojavlauncher/src/main/AndroidManifest.xml"

def patch(path, steps):
    s = path.read_text(encoding="utf-8")
    for marker, old, new in steps:
        if marker in s:
            print(f"  [atla] {path.name}: {marker!r} zaten var")
            continue
        if s.count(old) < 1:
            sys.exit(f"HATA: {path.name} icinde capa bulunamadi:\n{old}\n(Pojav surumu degismis olabilir)")
        s = s.replace(old, new, 1)
        print(f"  [ok]   {path.name}: {marker!r}")
    path.write_text(s, encoding="utf-8")

# ---- Tools.java: oyun klasoru Belgeler/.minecraft ----
patch(JAVA / "Tools.java", [
    ("DIR_GAME_MAIN;",
     "    public static String DIR_GAME_NEW;\n",
     "    public static String DIR_GAME_NEW;\n    public static String DIR_GAME_MAIN; // Ceyo: Belgeler/.minecraft (izin yoksa eski klasor)\n"),
    ("DIR_GAME_MAIN = CeyoStorage",
     "        DIR_GAME_HOME = getPojavStorageRoot(ctx).getAbsolutePath();\n        DIR_GAME_NEW = DIR_GAME_HOME + \"/.minecraft\";\n",
     "        DIR_GAME_HOME = getPojavStorageRoot(ctx).getAbsolutePath();\n        DIR_GAME_MAIN = CeyoStorage.resolveGameDir(ctx, DIR_GAME_HOME);\n        DIR_GAME_NEW = DIR_GAME_MAIN;\n"),
    ("DIR_GAME_MAIN != null",
     "        } else {\n            DIR_GAME_NEW = DIR_GAME_HOME + \"/.minecraft\";\n        }\n",
     "        } else {\n            DIR_GAME_NEW = DIR_GAME_MAIN != null ? DIR_GAME_MAIN : DIR_GAME_HOME + \"/.minecraft\";\n        }\n"),
])

# ---- MainMenuFragment.java: Mod Yukle karti + Dizini Ac ----
patch(JAVA / "fragments/MainMenuFragment.java", [
    ("ceyo_mod_button",
     "        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);\n",
     "        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);\n"
     "        View mModButton = view.findViewById(R.id.ceyo_mod_button);\n"
     "        if(mModButton != null) {\n"
     "            // Pojav'in mevcut profil/modloader ekrani: Forge, Fabric, Quilt, OptiFine, modpack\n"
     "            mModButton.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null));\n"
     "            // Uzun basinca mods klasorunu ac\n"
     "            mModButton.setOnLongClickListener(v -> {\n"
     "                File modsDir = new File(getCurrentProfileDirectory(), \"mods\");\n"
     "                modsDir.mkdirs();\n"
     "                net.kdt.pojavlaunch.CeyoStorage.openFolder(v.getContext(), modsDir);\n"
     "                return true;\n"
     "            });\n"
     "        }\n"),
    ("CeyoStorage.openFolder(v.getContext(), getCurrentProfileDirectory())",
     "            openPath(v.getContext(), getCurrentProfileDirectory(), false);\n",
     "            net.kdt.pojavlaunch.CeyoStorage.openFolder(v.getContext(), getCurrentProfileDirectory());\n"),
])

# ---- MainMenuFragment.java: Modrinth'ten mod indirme karti ----
patch(JAVA / "fragments/MainMenuFragment.java", [
    ("ceyo_modrinth_button",
     "        mNewsButton.setOnLongClickListener((v)->{\n",
     "        View mModrinthButton = view.findViewById(R.id.ceyo_modrinth_button);\n"
     "        if(mModrinthButton != null) {\n"
     "            // Dogrudan Modrinth tek mod aramasi (secili profilin mods klasorune indirir)\n"
     "            mModrinthButton.setOnClickListener(v -> {\n"
     "                Bundle modsArgs = new Bundle();\n"
     "                modsArgs.putBoolean(SearchModFragment.ARG_MODS_MODE, true);\n"
     "                Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, modsArgs);\n"
     "            });\n"
     "        }\n\n"
     "        mNewsButton.setOnLongClickListener((v)->{\n"),
])

# ---- TestStorageActivity.java: izin akisi + tasima ----
patch(JAVA / "TestStorageActivity.java", [
    ("mAllFilesState",
     "    private final int REQUEST_STORAGE_REQUEST_CODE = 1;\n",
     "    private final int REQUEST_STORAGE_REQUEST_CODE = 1;\n    private int mAllFilesState = 0; // 0: yok, 1: istendi (ilk onResume'u atla), 2: kullanici donusu bekleniyor\n"),
    ("SDK_INT < 30",
     "        if(Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29 && !isStorageAllowed(this)) requestStoragePermission();\n        else exit();\n",
     "        if(Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 30 && !isStorageAllowed(this)) requestStoragePermission();\n        else askAllFilesOrExit();\n"),
    ("askAllFilesOrExit() {",
     "    private void exit() {\n",
     "    private void askAllFilesOrExit() {\n"
     "        if(CeyoStorage.needsAllFilesAccess(this)) {\n"
     "            mAllFilesState = 1;\n"
     "            CeyoStorage.askAllFilesAccess(this);\n"
     "        } else exit();\n"
     "    }\n\n"
     "    @Override\n"
     "    protected void onResume() {\n"
     "        super.onResume();\n"
     "        if(mAllFilesState == 1) { mAllFilesState = 2; return; }\n"
     "        if(mAllFilesState != 2) return;\n"
     "        mAllFilesState = 0;\n"
     "        if(CeyoStorage.hasAllFilesAccess(this)) { exit(); return; }\n"
     "        CeyoStorage.showPermissionDialog(this,\n"
     "                () -> { mAllFilesState = 1; CeyoStorage.askAllFilesAccess(this); },\n"
     "                this::exit);\n"
     "    }\n\n"
     "    private void exit() {\n"),
    ("CeyoStorage.migrateLegacy",
     "        LauncherPreferences.loadPreferences(this);\n",
     "        CeyoStorage.migrateLegacy(this);\n        LauncherPreferences.loadPreferences(this);\n"),
])
# izin verilince (Android <= 10) exit() yerine askAllFilesOrExit() zinciri gerekmez; exit() dogrudan kalir.

# ---- ProfileTypeSelectFragment.java: Forge/Fabric/Quilt/OptiFine kurulumunu demo/yerel hesap kilidinden cikar ----
patch(JAVA / "fragments/ProfileTypeSelectFragment.java", [
    ("// Ceyo: demo kilidi kaldirildi",
     "        if(Tools.isLocalProfile(requireContext()) || Tools.isDemoProfile(requireContext())){\n"
     "            Toast.makeText(requireContext(), R.string.toast_not_available_demo, Toast.LENGTH_LONG).show();\n"
     "        } else {\n"
     "            Tools.swapFragment(requireActivity(), fragmentClass, tag, null);\n"
     "        }\n",
     "        // Ceyo: demo kilidi kaldirildi\n"
     "        Tools.swapFragment(requireActivity(), fragmentClass, tag, null);\n"),
])

# ---- Uygulama adi: tum dillerde CyLauncher ----
import re
RES = ROOT / "app_pojavlauncher/src/main/res"
changed = 0
for f in RES.glob("values*/strings.xml"):
    t = f.read_text(encoding="utf-8")
    n = re.sub(r'(<string name="app_name"[^>]*>)[^<]*(</string>)', r'\g<1>Cy Launcher\g<2>', t)
    n = re.sub(r'(<string name="app_short_name"[^>]*>)[^<]*(</string>)', r'\g<1>Cy Launcher\g<2>', n)
    if n != t:
        f.write_text(n, encoding="utf-8"); changed += 1
print(f"  [ok]   uygulama adi {changed} dil dosyasinda guncellendi")

# ---- AndroidManifest.xml ----
patch(MANIFEST, [
    ("MANAGE_EXTERNAL_STORAGE",
     "    <uses-permission android:name=\"android.permission.INTERNET\" />\n",
     "    <uses-permission android:name=\"android.permission.INTERNET\" />\n    <uses-permission android:name=\"android.permission.MANAGE_EXTERNAL_STORAGE\" tools:ignore=\"ScopedStorage\" />\n"),
    ("requestLegacyExternalStorage",
     "        android:hasFragileUserData=\"true\"\n",
     "        android:hasFragileUserData=\"true\"\n        android:requestLegacyExternalStorage=\"true\"\n"),
])
txt = MANIFEST.read_text(encoding="utf-8")
# Android 10 (API 29) icin de klasik depolama izni
txt = txt.replace('android:name="android.permission.WRITE_EXTERNAL_STORAGE"\n        android:maxSdkVersion="28"', 'android:name="android.permission.WRITE_EXTERNAL_STORAGE"\n        android:maxSdkVersion="29"')
txt = txt.replace('android:name="android.permission.READ_EXTERNAL_STORAGE"\n        android:maxSdkVersion="28"', 'android:name="android.permission.READ_EXTERNAL_STORAGE"\n        android:maxSdkVersion="29"')
MANIFEST.write_text(txt, encoding="utf-8")
print("Ceyo yamalari tamam.")
