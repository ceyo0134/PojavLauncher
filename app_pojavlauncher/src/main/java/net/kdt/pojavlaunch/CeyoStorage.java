package net.kdt.pojavlaunch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Ceyo Launcher: oyun klasoru Belgeler/.minecraft ve "tum dosyalara erisim" izni.
 * Tum mantik burada; Pojav dosyalarina sadece kucuk yamalar yapilir (scripts/ceyo_patch.py).
 */
public final class CeyoStorage {
    private static final String TAG = "CeyoStorage";

    private CeyoStorage() {}

    /** Belgeler/.minecraft icin gereken izin verilmis mi? */
    public static boolean hasAllFilesAccess(Context ctx) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        if (Build.VERSION.SDK_INT >= 23) return TestStorageActivity.isStorageAllowed(ctx);
        return true;
    }

    /** Android 11+ icin ozel "tum dosyalara erisim" ekrani gerekiyor mu? */
    public static boolean needsAllFilesAccess(Context ctx) {
        return Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager();
    }

    public static File getDocumentsDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    }

    /** Hedef klasor: Belgeler/.minecraft */
    public static File getTargetDir() {
        return new File(getDocumentsDir(), ".minecraft");
    }

    /** Pojav'in eski (uygulamaya ozel) .minecraft klasoru */
    public static File getLegacyDir(Context ctx) {
        File root;
        if (Build.VERSION.SDK_INT >= 29) root = ctx.getExternalFilesDir(null);
        else root = new File(Environment.getExternalStorageDirectory(), "games/PojavLauncher");
        return root == null ? null : new File(root, ".minecraft");
    }

    /**
     * Oyun klasorunu belirler. Izin varsa Belgeler/.minecraft, yoksa eski klasor (cokmesin diye).
     * @param fallbackHome Tools.DIR_GAME_HOME
     */
    public static String resolveGameDir(Context ctx, String fallbackHome) {
        try {
            if (hasAllFilesAccess(ctx)) {
                File dir = getTargetDir();
                if (dir.isDirectory() || dir.mkdirs()) {
                    if (dir.canWrite()) return dir.getAbsolutePath();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Documents/.minecraft kullanilamadi, eski klasore donuluyor", t);
        }
        return fallbackHome + "/.minecraft";
    }

    /** Eski klasor varsa ve yenisi bossa tasir (ayni diskte rename; basarisiz olursa dokunmaz). */
    public static void migrateLegacy(Context ctx) {
        try {
            if (!hasAllFilesAccess(ctx)) return;
            File old = getLegacyDir(ctx);
            File target = getTargetDir();
            if (old == null || !old.isDirectory()) return;
            if (!new File(old, "launcher_profiles.json").exists() && !new File(old, "versions").exists()) return;
            if (new File(target, "launcher_profiles.json").exists()) return;
            File parent = target.getParentFile();
            if (parent != null) parent.mkdirs();
            if (old.renameTo(target)) {
                Log.i(TAG, "Eski klasor tasindi: " + old + " -> " + target);
                Toast.makeText(ctx, R.string.ceyo_migrated, Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "Tasima basarisiz (rename), eski klasor yerinde duruyor: " + old);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Tasima hatasi", t);
        }
    }

    /** Android 11+: sistemin "tum dosyalara erisim" ekranini ac. */
    public static void askAllFilesAccess(Activity act) {
        if (Build.VERSION.SDK_INT < 30) return;
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + act.getPackageName()));
            act.startActivity(i);
        } catch (Throwable t) {
            try {
                act.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            } catch (Throwable t2) {
                Log.e(TAG, "Izin ekrani acilamadi", t2);
            }
        }
    }

    /** Izin verilmediyse aciklama + tekrar sor / atla diyalogu. */
    public static void showPermissionDialog(Activity act, Runnable onAllow, Runnable onSkip) {
        new AlertDialog.Builder(act)
                .setTitle(R.string.ceyo_perm_title)
                .setMessage(R.string.ceyo_perm_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.ceyo_perm_allow, (d, w) -> onAllow.run())
                .setNegativeButton(R.string.ceyo_perm_skip, (d, w) -> onSkip.run())
                .show();
    }

    /**
     * Klasoru dosya yoneticisinde acar. Belgeler altindaysa sistemin Files uygulamasini dener,
     * olmazsa Pojav'in eski yontemine (Tools.openPath) doner.
     */
    public static void openFolder(Context ctx, File dir) {
        try {
            dir.mkdirs();
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            String path = dir.getAbsolutePath();
            if (path.startsWith(root + "/")) {
                String docId = "primary:" + path.substring(root.length() + 1);
                Uri uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docId);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ctx.startActivity(intent);
                Toast.makeText(ctx, R.string.ceyo_hidden_hint, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Files uygulamasi acilamadi, Pojav yontemine donuluyor", e);
        } catch (Throwable t) {
            Log.w(TAG, "openFolder hatasi", t);
        }
        Tools.openPath(ctx, dir, false);
    }
}
