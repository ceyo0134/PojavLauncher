Ceyo Launcher build kit v7

1) Fork'unda (PojavLauncher v3_openjdk tabanli) bu zip'in icindekilerini kok dizine yukle (ayni yollarin ustune yazar)
2) Actions -> "Ceyo Launcher APK" -> Run workflow
3) "CeyoLauncher-apk" artifact'ini indir

Ekran:
- layout/fragment_launcher.xml        : yatay / genis ekran, 3 sutun (kenar cubugu | banner + hizli islemler | profil + surum + oyunu baslat)
- layout-port/fragment_launcher.xml   : dikey ekran icin tek sutun
- Hesap secici ve ayarlar dugmesi Pojav'in ust cubugunda (activity layout'una dokunulmadi)
- MainMenuFragment'in bagladigi id'lerin hepsi korundu: news_button, discord_button, custom_control_button,
  install_jar_button, share_logs_button, open_files_button, mc_version_spinner, edit_profile_button, play_button
- v6: sabit profil karti (kafa + isim) kaldirildi; hesap adi/kafasi Pojav'in ust cubugundaki hesap seciciden dinamik gelir
- v7: Wiki ve Discord gorunmez yapildi (id'ler Pojav kodu icin korunuyor)
- v8: "Mod Yükle" kartı eklendi (Forge/Fabric/Quilt/OptiFine/modpack ekranına gider; uzun basınca mods klasörünü açar).
- v8: Oyun klasörü Belgeler/.minecraft oldu. Android 11+ için "tüm dosyalara erişim" izni istenir; verilmezse eski klasör kullanılır (çökmez).
- v8: Eski klasör varsa izin sonrası otomatik Belgeler/.minecraft içine taşınır (aynı diskte rename).
- ÖNEMLİ: Java/manifest değişiklikleri scripts/ceyo_patch.py ile build sırasında uygulanır (workflow otomatik çalıştırır). Bu zip'i fork köküne yüklerken scripts/ ve app_pojavlauncher/ klasörlerini de dahil et.
