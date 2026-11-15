package io.github.ahassan07.carvideo;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * A plain browser on the phone. Cookies are shared with the car screen, so signing in to a site
 * here (where there is a keyboard) also signs it in on the car screen.
 */
public final class PhoneBrowserActivity extends Activity {
    static final String EXTRA_URL = "url";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        WebSetup.configure(webView);
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            String url = getIntent().getStringExtra(EXTRA_URL);
            webView.loadUrl(url != null ? url : Prefs.home(this));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        setContentView(new android.view.View(this));
        webView.destroy();
        super.onDestroy();
    }
}
