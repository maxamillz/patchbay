package com.maxamillz.patchbay;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Base64;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class MainActivity extends Activity {
    private WebView webView;
    private AudioRecord nativeMic;
    private volatile boolean nativeMicRunning;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.addJavascriptInterface(this, "AndroidPatchbay");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !url.startsWith("http://192.168.68.81:8771/");
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                            && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                        return;
                    }
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                            && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                        return;
                    }
                }
                request.deny();
            }
        });
        setContentView(webView);
        if (hasMediaPermissions()) loadPatchbay();
        else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 1);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 1 && hasMediaPermissions()) loadPatchbay();
    }

    private boolean hasMediaPermissions() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadPatchbay() { webView.loadUrl(PatchbayConfig.WEB_URL); }

    @android.webkit.JavascriptInterface public synchronized boolean startMic() {
        if (nativeMicRunning || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return nativeMicRunning;
        int min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) return false;
        nativeMic = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 1280));
        if (nativeMic.getState() != AudioRecord.STATE_INITIALIZED) { nativeMic.release(); nativeMic = null; return false; }
        nativeMic.startRecording();
        nativeMicRunning = true;
        new Thread(() -> {
            byte[] pcm = new byte[640];
            while (nativeMicRunning) {
                int n = nativeMic.read(pcm, 0, pcm.length);
                if (n > 0) {
                    String b64 = Base64.encodeToString(pcm, 0, n, Base64.NO_WRAP);
                    webView.post(() -> webView.evaluateJavascript("window.__patchbayNativePcm&&window.__patchbayNativePcm('" + b64 + "')", null));
                }
            }
        }, "patchbay-mic").start();
        return true;
    }

    @android.webkit.JavascriptInterface public synchronized void stopMic() {
        nativeMicRunning = false;
        if (nativeMic != null) { nativeMic.stop(); nativeMic.release(); nativeMic = null; }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
