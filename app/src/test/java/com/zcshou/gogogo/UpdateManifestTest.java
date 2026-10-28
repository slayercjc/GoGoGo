package com.zcshou.gogogo;

import org.junit.Test;
import static org.junit.Assert.*;

public class UpdateManifestTest {
    private final String valid = "{\"applicationId\":\"com.zcshou.gogogo\",\"versionCode\":1123,\"versionName\":\"1.1\",\"minSdk\":27,\"apk\":\"HuanWei-1.1.apk\",\"size\":16000000,\"sha256\":\"" + "a".repeat(64) + "\"}";
    @Test public void comparesInternalCodeRatherThanDisplayName() throws Exception {
        UpdateManifest manifest = new UpdateManifest(valid);
        assertTrue(manifest.isNewer(1122));
        assertFalse(manifest.isNewer(1123));
        assertFalse(manifest.isNewer(1200));
    }
    @Test public void rejectsUntrustedDownloadUrls() {
        assertTrue(UpdateManifest.isReleaseAsset("https://github.com/slayercjc/GoGoGo/releases/download/v1.1/HuanWei-1.1.apk"));
        for (String url : new String[]{"http://github.com/slayercjc/GoGoGo/releases/download/v1.1/a.apk", "https://github.com.evil.test/slayercjc/GoGoGo/releases/download/a", "https://github.com/ZCShou/GoGoGo/releases/download/v1/a.apk", "https://github.com@evil.test/slayercjc/GoGoGo/releases/download/a", "https://github.com/slayercjc/GoGoGo/releases/download/../a.apk"}) {
            assertFalse(url, UpdateManifest.isReleaseAsset(url));
        }
    }
    @Test public void rejectsMalformedMetadata() {
        for (String bad : new String[]{valid.replace("HuanWei-1.1.apk", "../a.apk"), valid.replace("16000000", "0"), valid.replace("16000000", "999999999"), valid.replace("com.zcshou.gogogo", "other.app"), valid.replace("a".repeat(64), "1234"), "{}"}) {
            try { new UpdateManifest(bad); fail("Accepted invalid update"); }
            catch (Exception expected) { /* Expected rejection. */ }
        }
    }
}
