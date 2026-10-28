package com.zcshou.gogogo;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Foreground UI, persistent system download, and verified self-update installation. */
public final class AppUpdater {
    private final AppCompatActivity activity;
    private final SharedPreferences state, options;
    private final DownloadManager downloads;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS).followSslRedirects(false).build();
    private boolean active, destroyed, checking, verifying, offered, permissionRequested;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!active || destroyed) return;
            inspectDownload(false);
            main.postDelayed(this, 1500);
        }
    };

    public AppUpdater(AppCompatActivity activity) {
        this.activity = activity;
        state = activity.getSharedPreferences("huanwei_update", 0);
        options = PreferenceManager.getDefaultSharedPreferences(activity);
        downloads = (DownloadManager) activity.getSystemService(android.content.Context.DOWNLOAD_SERVICE);
    }

    public void resume() {
        active = true;
        main.removeCallbacks(poll);
        main.post(poll);
        if (permissionRequested) {
            permissionRequested = false;
            if (activity.getPackageManager().canRequestPackageInstalls()) {
                offered = false;
                inspectDownload(true);
            } else toast("未允许安装。可再次点击检查更新继续安装。");
        }
        if (options.getBoolean("update_auto_check", true)
                && System.currentTimeMillis() - state.getLong("last_check", 0) > 24 * 60 * 60 * 1000L) {
            check(false);
        }
    }

    public void pause() { active = false; main.removeCallbacks(poll); }
    public void close() {
        destroyed = true;
        pause();
        client.dispatcher().cancelAll();
        worker.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }

    public void check(boolean manual) {
        if (state.getLong("download_id", 0) > 0) {
            if (manual) inspectDownload(true);
            return;
        }
        if (checking) { if (manual) toast("正在检查更新…"); return; }
        checking = true;
        if (manual) toast("正在检查新版…");
        // Bound background checks, including network failures, to once per day.
        state.edit().putLong("last_check", System.currentTimeMillis()).apply();
        worker.execute(() -> {
            try {
                String releaseText = fetch("https://api.github.com/repos/" + UpdateManifest.REPOSITORY + "/releases/latest", true);
                if (releaseText == null) { deliver(() -> { if (manual) toast("尚未发布正式版本，请稍后再试"); }); return; }
                JSONObject release = new JSONObject(releaseText);
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) throw new IOException("没有正式版本");
                JSONArray assets = release.getJSONArray("assets");
                JSONObject metadataAsset = asset(assets, "update.json");
                if (metadataAsset == null) throw new IOException("此版本缺少 update.json，请在发布页下载安装包");
                String metadataUrl = metadataAsset.getString("browser_download_url");
                requireAssetUrl(metadataUrl);
                String json = fetch(metadataUrl, false);
                UpdateManifest manifest = new UpdateManifest(json);
                if (!manifest.isNewer(BuildConfig.VERSION_CODE)) {
                    deliver(() -> { if (manual) toast("已是最新版本（" + BuildConfig.VERSION_NAME + "）"); });
                    return;
                }
                if (Build.VERSION.SDK_INT < manifest.minSdk) throw new IOException("新版需要更高的 Android 系统版本");
                JSONObject apkAsset = asset(assets, manifest.apk);
                if (apkAsset == null || apkAsset.getLong("size") != manifest.size) throw new IOException("安装包与更新描述不一致");
                String url = apkAsset.getString("browser_download_url");
                requireAssetUrl(url);
                String notes = release.optString("body", "体验改进与问题修复");
                String summary = notes.length() > 4000 ? notes.substring(0, 4000) + "…" : notes;
                deliver(() -> {
                    if (!manual && options.getBoolean("update_wifi_download", false)) {
                        download(manifest, json, url, true);
                    } else {
                        new MaterialAlertDialogBuilder(activity)
                                .setTitle("发现幻位 " + manifest.versionName)
                                .setMessage(summary + "\n\n下载大小：" + String.format(java.util.Locale.CHINA, "%.1f MB", manifest.size / 1048576.0)
                                        + "\n安装时将由系统确认，正在运行的定位服务会中断。")
                                .setPositiveButton("下载更新", (d, w) -> download(manifest, json, url, false))
                                .setNegativeButton("稍后", null).show();
                    }
                });
            } catch (Exception e) {
                deliver(() -> { if (manual) new MaterialAlertDialogBuilder(activity).setTitle("暂时无法更新")
                        .setMessage("请检查网络或稍后重试。\n" + e.getMessage())
                        .setPositiveButton("查看发布页", (d, w) -> openReleases()).setNegativeButton("关闭", null).show(); });
            } finally { main.post(() -> checking = false); }
        });
    }

    private String fetch(String url, boolean allowMissing) throws Exception {
        Request request = new Request.Builder().url(url).header("User-Agent", "HuanWei/" + BuildConfig.VERSION_NAME)
                .header("Accept", url.startsWith("https://api.github.com/") ? "application/vnd.github+json" : "application/octet-stream").build();
        try (Response response = client.newCall(request).execute()) {
            if (allowMissing && response.code() == 404) return null;
            if (!response.isSuccessful() || response.body() == null) throw new IOException("服务器返回 " + response.code());
            if (response.body().contentLength() > 1024 * 1024) throw new IOException("更新描述过大");
            java.io.InputStream input = response.body().byteStream();
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int n;
            while ((n = input.read(buffer)) != -1) {
                if (output.size() + n > 1024 * 1024) throw new IOException("更新描述过大");
                output.write(buffer, 0, n);
            }
            return output.toString("UTF-8");
        }
    }

    private static JSONObject asset(JSONArray assets, String name) throws Exception {
        for (int i = 0; i < assets.length(); i++) {
            JSONObject item = assets.getJSONObject(i);
            if (name.equals(item.optString("name"))) return item;
        }
        return null;
    }
    private static void requireAssetUrl(String url) throws IOException {
        if (!UpdateManifest.isReleaseAsset(url)) throw new IOException("更新地址不属于本项目");
    }

    private File apkFile(long code) {
        File directory = activity.getExternalFilesDir("Updates");
        if (directory == null) throw new IllegalStateException("存储空间不可用");
        return new File(directory, "huanwei-" + code + ".apk");
    }

    private void download(UpdateManifest manifest, String json, String url, boolean wifiOnly) {
        try {
            if (downloads == null || state.getLong("download_id", 0) > 0) return;
            File file = apkFile(manifest.versionCode);
            if (file.exists() && !file.delete()) throw new IOException("无法清理旧安装包");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle("幻位 " + manifest.versionName)
                    .setDescription("下载完成后返回幻位安装")
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverRoaming(false).setDestinationUri(Uri.fromFile(file));
            if (wifiOnly) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            long id = downloads.enqueue(request);
            state.edit().putLong("download_id", id).putString("manifest", json).apply();
            offered = false;
            toast(wifiOnly ? "将在 Wi-Fi 下下载，可在通知栏查看进度" : "开始下载，可在通知栏查看进度");
        } catch (Exception e) { toast("无法开始下载：" + e.getMessage()); }
    }

    private void inspectDownload(boolean manual) {
        long id = state.getLong("download_id", 0);
        if (id <= 0 || downloads == null || verifying) return;
        try (Cursor cursor = downloads.query(new DownloadManager.Query().setFilterById(id))) {
            if (cursor == null || !cursor.moveToFirst()) { clearDownload(id); if (manual) toast("下载已取消，请重新检查更新"); return; }
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_FAILED) {
                clearDownload(id); toast("更新下载失败，请检查网络和空间后重试"); return;
            }
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                UpdateManifest manifest = new UpdateManifest(state.getString("manifest", "{}"));
                if (!manifest.isNewer(BuildConfig.VERSION_CODE)) { clearDownload(id); return; }
                if (manual || !offered) verify(id, manifest);
            } else if (manual) {
                long done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                String progress = total > 0 ? (done * 100 / total) + "%" : "等待连接";
                new MaterialAlertDialogBuilder(activity).setTitle("更新下载中 · " + progress)
                        .setMessage(status == DownloadManager.STATUS_PAUSED ? "下载已暂停，将在网络或 Wi-Fi 可用后继续。" : "下载任务由系统管理，退出页面不会取消下载。")
                        .setPositiveButton("继续下载", null).setNegativeButton("取消下载", (d, w) -> clearDownload(id)).show();
            }
        } catch (Exception e) { clearDownload(id); toast("更新任务已失效，请重新检查更新"); }
    }

    private void verify(long id, UpdateManifest manifest) {
        verifying = true;
        worker.execute(() -> {
            try {
                File file = apkFile(manifest.versionCode);
                if (file.length() != manifest.size) throw new IOException("文件大小不一致");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (FileInputStream input = new FileInputStream(file)) {
                    byte[] buffer = new byte[65536]; int n;
                    while ((n = input.read(buffer)) != -1) digest.update(buffer, 0, n);
                }
                StringBuilder hex = new StringBuilder();
                for (byte b : digest.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
                if (!hex.toString().equalsIgnoreCase(manifest.sha256)) throw new IOException("文件校验失败");
                PackageManager pm = activity.getPackageManager();
                int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
                PackageInfo archive = pm.getPackageArchiveInfo(file.getAbsolutePath(), flags);
                PackageInfo installed = pm.getPackageInfo(activity.getPackageName(), flags);
                if (archive == null || !activity.getPackageName().equals(archive.packageName)
                        || PackageInfoCompat.getLongVersionCode(archive) != manifest.versionCode
                        || !manifest.isNewer(PackageInfoCompat.getLongVersionCode(installed))
                        || !manifest.versionName.equals(archive.versionName)
                        || !signers(archive).equals(signers(installed)) || signers(archive).isEmpty()) {
                    throw new IOException("包名、版本或签名不匹配，已拒绝安装");
                }
                deliver(() -> {
                    offered = true;
                    if (permissionRequested) return;
                    new MaterialAlertDialogBuilder(activity).setTitle("新版已准备好")
                            .setMessage("幻位 " + manifest.versionName + " 已通过完整性与签名校验。安装会中断当前定位服务。")
                            .setPositiveButton("安装", (d, w) -> install(file))
                            .setNegativeButton("稍后", null).show();
                });
            } catch (Exception e) {
                clearDownload(id);
                deliver(() -> toast("无法安装：" + e.getMessage()));
            } finally { main.post(() -> verifying = false); }
        });
    }

    @SuppressWarnings("deprecation")
    private static Set<String> signers(PackageInfo info) {
        Signature[] signatures = Build.VERSION.SDK_INT >= 28 && info.signingInfo != null
                ? info.signingInfo.getApkContentsSigners() : info.signatures;
        Set<String> result = new HashSet<>();
        if (signatures != null) for (Signature signature : signatures) result.add(signature.toCharsString());
        return result;
    }

    private void install(File file) {
        try {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                new MaterialAlertDialogBuilder(activity).setTitle("允许安装更新")
                        .setMessage("请在系统页面允许幻位安装应用，返回后可继续安装。")
                        .setPositiveButton("去设置", (d, w) -> {
                            try {
                                permissionRequested = true;
                                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:" + activity.getPackageName())));
                            } catch (Exception e) { permissionRequested = false; toast("请在系统设置中允许安装未知应用"); }
                        }).setNegativeButton("取消", null).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileProvider", file);
            activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) { toast("无法打开系统安装器，请从发布页手动安装"); }
    }

    private void clearDownload(long id) {
        if (downloads != null) downloads.remove(id);
        state.edit().remove("download_id").remove("manifest").apply();
        offered = false;
    }
    private void deliver(Runnable action) { main.post(() -> { if (active && !destroyed && !activity.isFinishing()) action.run(); }); }
    private void toast(String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
    private void openReleases() {
        try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/" + UpdateManifest.REPOSITORY + "/releases"))); }
        catch (Exception e) { toast("未找到浏览器"); }
    }
}
