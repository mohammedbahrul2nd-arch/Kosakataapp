package com.kosakata.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 1, REQ_SAVE = 2;
    private static final String URL = "https://appassets.androidplatform.net/assets/index.html";
    // Tombol "Cadangkan progres" di halaman membuat unduhan blob; WebView tidak menanganinya,
    // jadi diteruskan ke Android.save() yang membuka dialog "Simpan sebagai".
    private static final String HOOK =
        "(function(){if(window.__kh)return;window.__kh=1;var c=HTMLAnchorElement.prototype.click;"
        + "HTMLAnchorElement.prototype.click=function(){var a=this;"
        + "if(a.download&&String(a.href).indexOf('blob:')===0){var n=a.download;"
        + "fetch(a.href).then(function(r){return r.text()}).then(function(t){Android.save(n,t)});return}"
        + "return c.call(a)}})();";

    private WebView wv;
    private ValueCallback<Uri[]> fileCb;
    private String pendingText = "";

    private class Bridge {
        @JavascriptInterface
        public void save(final String name, final String text) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    pendingText = text;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("application/json");
                    i.putExtra(Intent.EXTRA_TITLE, name);
                    startActivityForResult(i, REQ_SAVE);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        wv = new WebView(this);
        setContentView(wv);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        wv.addJavascriptInterface(new Bridge(), "Android");
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override
            public void onPageFinished(WebView v, String url) {
                v.evaluateJavascript(HOOK, null);
            }
        });
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView v, String url, String msg, final JsResult r) {
                new AlertDialog.Builder(MainActivity.this).setMessage(msg)
                    .setPositiveButton(android.R.string.ok, (d, w) -> r.confirm())
                    .setOnCancelListener(d -> r.cancel()).show();
                return true;
            }
            @Override
            public boolean onJsConfirm(WebView v, String url, String msg, final JsResult r) {
                new AlertDialog.Builder(MainActivity.this).setMessage(msg)
                    .setPositiveButton(android.R.string.ok, (d, w) -> r.confirm())
                    .setNegativeButton(android.R.string.cancel, (d, w) -> r.cancel())
                    .setOnCancelListener(d -> r.cancel()).show();
                return true;
            }
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Pilih file cadangan"), REQ_FILE);
                } catch (Exception e) {
                    fileCb = null;
                    cb.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });
        if (b == null) wv.loadUrl(URL); else wv.restoreState(b);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_FILE) {
            if (fileCb != null) {
                fileCb.onReceiveValue(res == RESULT_OK && data != null
                    ? WebChromeClient.FileChooserParams.parseResult(res, data) : null);
                fileCb = null;
            }
        } else if (req == REQ_SAVE && res == RESULT_OK && data != null && data.getData() != null) {
            try (OutputStream o = getContentResolver().openOutputStream(data.getData())) {
                o.write(pendingText.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "Cadangan tersimpan", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            pendingText = "";
        }
    }

    @Override protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); wv.saveState(b); }
    @Override protected void onPause() { super.onPause(); wv.onPause(); }
    @Override protected void onResume() { super.onResume(); wv.onResume(); }
}
