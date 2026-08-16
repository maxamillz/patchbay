package com.maxamillz.patchbay;

public final class PatchbayConfig {
    private PatchbayConfig() {}
    // ponytail: static LAN address avoids mDNS failures on the Echo Show.
    // ponytail: Echo's WebView rejects the local TLS handshake; native capture keeps this LAN HTTP page functional.
    public static final String WEB_URL = "http://192.168.68.81:8771/?ws=ws%3A%2F%2F192.168.68.81%3A8765";
}
