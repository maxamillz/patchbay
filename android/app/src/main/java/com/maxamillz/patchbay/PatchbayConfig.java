package com.maxamillz.patchbay;

public final class PatchbayConfig {
    private PatchbayConfig() {}
    // ponytail: static LAN address avoids mDNS failures on the Echo Show.
    // ponytail: LAN-only HTTP bypasses the Echo WebView TLS defect; do not expose this listener outside LAN.
    public static final String WEB_URL = "http://192.168.68.81:8771/?ws=ws%3A%2F%2F192.168.68.81%3A8765";
}
