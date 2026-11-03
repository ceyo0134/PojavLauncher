package net.kdt.pojavlaunch.modloaders.modpacks.api;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cy Launcher: secili profilin mods klasorunu, Minecraft surumunu ve mod yukleyicisini (loader) bulur.
 * Modrinth'ten tek mod indirirken dogru klasore ve uyumlu surume yonlendirmek icin kullanilir.
 */
public class CyModTarget {
    // "1.20.1", "1.21" gibi surumler; basinda/sonunda rakam veya nokta olmamali (loader surumlerini yutmasin)
    private static final Pattern MC_VERSION = Pattern.compile("(?<![\\d.])1\\.\\d{1,2}(?:\\.\\d{1,2})?(?![\\d.])");

    public final File modsDir;
    /** Ornek: "1.20.1". Bilinmiyorsa null (filtre uygulanmaz) */
    @Nullable public final String mcVersion;
    /** Modrinth loader adlari: "fabric", "quilt", "forge", "neoforge". Bilinmiyorsa null */
    @Nullable public final String[] loaders;

    private CyModTarget(File modsDir, @Nullable String mcVersion, @Nullable String[] loaders) {
        this.modsDir = modsDir;
        this.mcVersion = mcVersion;
        this.loaders = loaders;
    }

    public boolean hasFilter() {
        return mcVersion != null || loaders != null;
    }

    /** ["fabric","quilt"] biciminde JSON dizisi (Modrinth sorgusu icin) */
    @Nullable public String loadersJson() {
        return toJsonArray(loaders);
    }

    @Nullable public String mcVersionJson() {
        return mcVersion == null ? null : toJsonArray(new String[]{mcVersion});
    }

    private static String toJsonArray(@Nullable String[] values) {
        if(values == null || values.length == 0) return null;
        StringBuilder sb = new StringBuilder("[");
        for(int i = 0; i < values.length; i++) {
            if(i > 0) sb.append(',');
            sb.append('"').append(values[i]).append('"');
        }
        return sb.append(']').toString();
    }

    /** Arka planda veya UI thread'de cagrilabilir (kucuk bir json okur) */
    public static CyModTarget current() {
        File gameDir = new File(Tools.DIR_GAME_NEW);
        String versionId = null;
        try {
            String currentProfile = LauncherPreferences.DEFAULT_PREF
                    .getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
            if(Tools.isValidString(currentProfile)) {
                LauncherProfiles.load();
                MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
                if(profile != null) {
                    gameDir = Tools.getGameDirPath(profile);
                    versionId = profile.lastVersionId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // profil okunamazsa varsayilan klasore dus
        }
        return new CyModTarget(new File(gameDir, "mods"), parseMcVersion(versionId), parseLoaders(versionId));
    }

    @Nullable
    static String parseMcVersion(@Nullable String versionId) {
        if(versionId == null) return null;
        Matcher m = MC_VERSION.matcher(versionId);
        return m.find() ? m.group() : null;
    }

    @Nullable
    static String[] parseLoaders(@Nullable String versionId) {
        if(versionId == null) return null;
        String id = versionId.toLowerCase(Locale.ROOT);
        if(id.contains("neoforge")) return new String[]{"neoforge"};
        if(id.contains("forge")) return new String[]{"forge"};
        // Quilt, Fabric modlarini da yukleyebilir
        if(id.contains("quilt")) return new String[]{"quilt", "fabric"};
        if(id.contains("fabric")) return new String[]{"fabric"};
        return null;
    }
}
