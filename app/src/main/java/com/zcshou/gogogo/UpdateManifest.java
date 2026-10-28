package com.zcshou.gogogo;

import org.json.JSONObject;
import java.net.URI;

/** Release contract, independent of Android so malformed updates can be tested on the JVM. */
public final class UpdateManifest {
    public static final String REPOSITORY = "slayercjc/GoGoGo";
    public final long versionCode;
    public final String versionName, apk, sha256;
    public final long size;
    public final int minSdk;

    public UpdateManifest(String json) throws Exception {
        JSONObject value = new JSONObject(json);
        versionCode = value.getLong("versionCode");
        versionName = value.getString("versionName");
        apk = value.getString("apk");
        sha256 = value.getString("sha256");
        size = value.getLong("size");
        minSdk = value.getInt("minSdk");
        if (versionCode <= 0 || versionName.isEmpty() || versionName.length() > 64
                || !apk.matches("[A-Za-z0-9._-]+\\.apk") || apk.contains("..")
                || !sha256.matches("[a-fA-F0-9]{64}") || size <= 0 || size > 200L * 1024 * 1024
                || minSdk < 27 || !"com.zcshou.gogogo".equals(value.getString("applicationId"))) {
            throw new IllegalArgumentException("更新描述格式不正确");
        }
    }

    public boolean isNewer(long installedCode) { return versionCode > installedCode; }

    public static boolean isReleaseAsset(String url) {
        try {
            URI uri = new URI(url);
            return "https".equals(uri.getScheme()) && "github.com".equals(uri.getHost())
                    && uri.getUserInfo() == null && uri.getPort() == -1
                    && uri.getQuery() == null && uri.getFragment() == null
                    && uri.getPath().startsWith("/" + REPOSITORY + "/releases/download/")
                    && !uri.getPath().contains("/../") && !uri.getPath().contains("/./");
        } catch (Exception e) { return false; }
    }
}
