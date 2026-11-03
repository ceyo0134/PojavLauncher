package net.kdt.pojavlaunch.modloaders.modpacks.api;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModrinthIndex;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipFile;

public class ModrinthApi implements ModpackApi{
    private final ApiHandler mApiHandler;
    /** Cy Launcher: surum dosyasi url'si -> o surumun gerekli (required) bagimlilik proje id'leri */
    private final Map<String, List<String>> mRequiredDependencies = new ConcurrentHashMap<>();
    public ModrinthApi(){
        mApiHandler = new ApiHandler("https://api.modrinth.com/v2");
    }

    @Override
    public SearchResult searchMod(SearchFilters searchFilters, SearchResult previousPageResult) {
        ModrinthSearchResult modrinthSearchResult = (ModrinthSearchResult) previousPageResult;

        // Fixes an issue where the offset being equal or greater than total_hits is ignored
        if (modrinthSearchResult != null && modrinthSearchResult.previousOffset >= modrinthSearchResult.totalResultCount) {
            ModrinthSearchResult emptyResult = new ModrinthSearchResult();
            emptyResult.results = new ModItem[0];
            emptyResult.totalResultCount = modrinthSearchResult.totalResultCount;
            emptyResult.previousOffset = modrinthSearchResult.previousOffset;
            return emptyResult;
        }


        // Build the facets filters
        HashMap<String, Object> params = new HashMap<>();
        StringBuilder facetString = new StringBuilder();
        facetString.append("[");
        facetString.append(String.format("[\"project_type:%s\"]", searchFilters.isModpack ? "modpack" : "mod"));
        if(searchFilters.mcVersion != null && !searchFilters.mcVersion.isEmpty())
            facetString.append(String.format(",[\"versions:%s\"]", searchFilters.mcVersion));
        if(!searchFilters.isModpack) {
            // Cy Launcher: secili profilin mod yukleyicisine (Fabric/Forge/...) uyan modlari goster
            String[] loaders = CyModTarget.current().loaders;
            if(loaders != null) {
                facetString.append(",[");
                for(int i = 0; i < loaders.length; i++) {
                    if(i > 0) facetString.append(",");
                    facetString.append("\"categories:").append(loaders[i]).append("\"");
                }
                facetString.append("]");
            }
        }
        facetString.append("]");
        params.put("facets", facetString.toString());
        params.put("query", searchFilters.name);
        params.put("limit", 50);
        params.put("index", "relevance");
        if(modrinthSearchResult != null)
            params.put("offset", modrinthSearchResult.previousOffset);

        JsonObject response = mApiHandler.get("search", params, JsonObject.class);
        if(response == null) return null;
        JsonArray responseHits = response.getAsJsonArray("hits");
        if(responseHits == null) return null;

        ModItem[] items = new ModItem[responseHits.size()];
        for(int i=0; i<responseHits.size(); ++i){
            JsonObject hit = responseHits.get(i).getAsJsonObject();
            items[i] = new ModItem(
                    Constants.SOURCE_MODRINTH,
                    hit.get("project_type").getAsString().equals("modpack"),
                    hit.get("project_id").getAsString(),
                    hit.get("title").getAsString(),
                    optString(hit, "description"),
                    optString(hit, "icon_url") // ikonu olmayan modlarda null patlamasin
            );
        }
        if(modrinthSearchResult == null) modrinthSearchResult = new ModrinthSearchResult();
        modrinthSearchResult.previousOffset += responseHits.size();
        modrinthSearchResult.results = items;
        modrinthSearchResult.totalResultCount = response.get("total_hits").getAsInt();
        return modrinthSearchResult;
    }

    @Override
    public ModDetail getModDetails(ModItem item) {
        // Tek mod ise secili profilin MC surumu + loader'ina gore filtrele; sonuc bos cikarsa filtresiz listeye don
        CyModTarget target = item.isModpack ? null : CyModTarget.current();
        JsonArray response = null;
        if(target != null && target.hasFilter()) {
            response = fetchVersions(item.id, target);
        }
        if(response == null || response.size() == 0) {
            response = mApiHandler.get(String.format("project/%s/version", item.id), JsonArray.class);
        }
        if(response == null) return null;

        List<String> names = new ArrayList<>();
        List<String> mcNames = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        List<String> hashes = new ArrayList<>();

        for (int i=0; i<response.size(); ++i) {
            JsonObject version = response.get(i).getAsJsonObject();
            JsonObject file = pickPrimaryFile(version);
            if(file == null) continue; // dosyasi olmayan surumu atla

            String url = file.get("url").getAsString();
            names.add(version.get("name").getAsString());
            mcNames.add(pickMcVersion(version, target));
            urls.add(url);

            // Assume there may not be hashes, in case the API changes
            JsonObject hashesMap = file.has("hashes") && file.get("hashes").isJsonObject()
                    ? file.getAsJsonObject("hashes") : null;
            hashes.add(hashesMap == null || hashesMap.get("sha1") == null ? null : hashesMap.get("sha1").getAsString());

            if(!item.isModpack) rememberRequiredDependencies(url, version);
        }
        if(names.isEmpty()) return null;

        return new ModDetail(item, names.toArray(new String[0]), mcNames.toArray(new String[0]),
                urls.toArray(new String[0]), hashes.toArray(new String[0]));
    }

    @Override
    public ModLoader installMod(ModDetail modDetail, int selectedVersion) throws IOException{
        if(!modDetail.isModpack) {
            installSingleMod(modDetail, selectedVersion);
            return null; // modpack degil: ayri bir modloader kurulumu yok
        }
        return ModpackInstaller.installModpack(modDetail, selectedVersion, this::installMrpack);
    }

    // ---------------------------------------------------------------- Cy Launcher: tek mod indirme

    private JsonArray fetchVersions(String projectId, CyModTarget target) {
        HashMap<String, Object> params = new HashMap<>();
        if(target.mcVersionJson() != null) params.put("game_versions", target.mcVersionJson());
        if(target.loadersJson() != null) params.put("loaders", target.loadersJson());
        return mApiHandler.get(String.format("project/%s/version", projectId), params, JsonArray.class);
    }

    /** primary:true olan dosyayi, yoksa ilk dosyayi dondurur */
    private static JsonObject pickPrimaryFile(JsonObject version) {
        JsonArray files = version.getAsJsonArray("files");
        if(files == null || files.size() == 0) return null;
        for(JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();
            JsonElement primary = file.get("primary");
            if(primary != null && primary.isJsonPrimitive() && primary.getAsBoolean()) return file;
        }
        return files.get(0).getAsJsonObject();
    }

    private static String pickMcVersion(JsonObject version, CyModTarget target) {
        JsonArray gameVersions = version.getAsJsonArray("game_versions");
        if(gameVersions == null || gameVersions.size() == 0) return "?";
        if(target != null && target.mcVersion != null) {
            for(JsonElement element : gameVersions) {
                if(element.getAsString().equals(target.mcVersion)) return target.mcVersion;
            }
        }
        return gameVersions.get(0).getAsString();
    }

    private void rememberRequiredDependencies(String versionUrl, JsonObject version) {
        JsonArray dependencies = version.getAsJsonArray("dependencies");
        if(dependencies == null) return;
        List<String> required = new ArrayList<>();
        for(JsonElement element : dependencies) {
            JsonObject dependency = element.getAsJsonObject();
            JsonElement type = dependency.get("dependency_type");
            JsonElement projectId = dependency.get("project_id");
            if(type == null || projectId == null || projectId.isJsonNull()) continue;
            if("required".equals(type.getAsString())) required.add(projectId.getAsString());
        }
        if(!required.isEmpty()) mRequiredDependencies.put(versionUrl, required);
    }

    private void installSingleMod(ModDetail modDetail, int selectedVersion) throws IOException {
        CyModTarget target = CyModTarget.current();
        File modsDir = target.modsDir;
        if(!modsDir.isDirectory() && !modsDir.mkdirs()) {
            throw new IOException("mods klasoru olusturulamadi: " + modsDir);
        }
        String url = modDetail.versionUrls[selectedVersion];
        try {
            downloadModFile(url, modDetail.versionHashes[selectedVersion], modsDir);

            // Gerekli bagimliliklar (ornegin Fabric API) - basarisiz olursa asil indirmeyi bozmasin
            List<String> dependencies = mRequiredDependencies.get(url);
            if(dependencies != null) {
                for(String projectId : dependencies) {
                    try {
                        installDependency(projectId, target, modsDir);
                    } catch (Exception e) {
                        Log.w("ModrinthApi", "Bagimlilik indirilemedi: " + projectId, e);
                    }
                }
            }
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
        }
    }

    private void installDependency(String projectId, CyModTarget target, File modsDir) throws IOException {
        JsonArray versions = target.hasFilter() ? fetchVersions(projectId, target) : null;
        if(versions == null || versions.size() == 0) return; // uyumlu surum yoksa tahmin yurutme
        JsonObject version = versions.get(0).getAsJsonObject(); // en yeni uyumlu surum
        JsonObject file = pickPrimaryFile(version);
        if(file == null) return;
        JsonObject hashes = file.has("hashes") && file.get("hashes").isJsonObject() ? file.getAsJsonObject("hashes") : null;
        String sha1 = hashes != null && hashes.get("sha1") != null ? hashes.get("sha1").getAsString() : null;
        downloadModFile(file.get("url").getAsString(), sha1, modsDir);
    }

    private static void downloadModFile(String url, String sha1, File modsDir) throws IOException {
        File destination = new File(modsDir, fileNameFromUrl(url));
        byte[] buffer = new byte[8192];
        DownloadUtils.ensureSha1(destination, sha1, (Callable<Void>) () -> {
            DownloadUtils.downloadFileMonitored(url, destination, buffer,
                    new DownloaderProgressWrapper(R.string.modpack_download_downloading_mods,
                            ProgressLayout.INSTALL_MODPACK));
            return null;
        });
    }

    private static String fileNameFromUrl(String url) {
        String name = url.substring(url.lastIndexOf('/') + 1);
        int query = name.indexOf('?');
        if(query >= 0) name = name.substring(0, query);
        try {
            name = URLDecoder.decode(name.replace("+", "%2B"), "UTF-8");
        } catch (Exception ignored) {}
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return name.isEmpty() ? "mod.jar" : name;
    }

    private static String optString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static ModLoader createInfo(ModrinthIndex modrinthIndex) {
        if(modrinthIndex == null) return null;
        Map<String, String> dependencies = modrinthIndex.dependencies;
        String mcVersion = dependencies.get("minecraft");
        if(mcVersion == null) return null;
        String modLoaderVersion;
        if((modLoaderVersion = dependencies.get("forge")) != null) {
            return new ModLoader(ModLoader.MOD_LOADER_FORGE, modLoaderVersion, mcVersion);
        }
        if((modLoaderVersion = dependencies.get("fabric-loader")) != null) {
            return new ModLoader(ModLoader.MOD_LOADER_FABRIC, modLoaderVersion, mcVersion);
        }
        if((modLoaderVersion = dependencies.get("quilt-loader")) != null) {
            return new ModLoader(ModLoader.MOD_LOADER_QUILT, modLoaderVersion, mcVersion);
        }
        return null;
    }

    private ModLoader installMrpack(File mrpackFile, File instanceDestination) throws IOException {
        try (ZipFile modpackZipFile = new ZipFile(mrpackFile)){
            ModrinthIndex modrinthIndex = Tools.GLOBAL_GSON.fromJson(
                    Tools.read(ZipUtils.getEntryStream(modpackZipFile, "modrinth.index.json")),
                    ModrinthIndex.class);
            
            ModDownloader modDownloader = new ModDownloader(instanceDestination);
            for(ModrinthIndex.ModrinthIndexFile indexFile : modrinthIndex.files) {
                modDownloader.submitDownload(indexFile.fileSize, indexFile.path, indexFile.hashes.sha1, indexFile.downloads);
            }
            modDownloader.awaitFinish(new DownloaderProgressWrapper(R.string.modpack_download_downloading_mods, ProgressLayout.INSTALL_MODPACK));
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.modpack_download_applying_overrides, 1, 2);
            ZipUtils.zipExtract(modpackZipFile, "overrides/", instanceDestination);
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 50, R.string.modpack_download_applying_overrides, 2, 2);
            ZipUtils.zipExtract(modpackZipFile, "client-overrides/", instanceDestination);
            return createInfo(modrinthIndex);
        }
    }

    class ModrinthSearchResult extends SearchResult {
        int previousOffset;
    }
}
