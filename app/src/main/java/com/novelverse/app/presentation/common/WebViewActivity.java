package com.novelverse.app.presentation.common;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Generic WebView wrapper.
 * Pass EXTRA_URL and optionally EXTRA_TITLE.
 */
@AndroidEntryPoint
public class WebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL   = "url";
    public static final String EXTRA_TITLE = "title";

    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        String url   = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        webView     = findViewById(R.id.webview);
        progressBar = findViewById(R.id.webview_progress);

        if (title != null) ((TextView) findViewById(R.id.webview_title)).setText(title);

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack(); else finish();
        });
        findViewById(R.id.btn_refresh).setOnClickListener(v -> webView.reload());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String u) {
                ((TextView) findViewById(R.id.webview_title)).setText(v.getTitle());
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                progressBar.setProgress(p);
                progressBar.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }
        });

        if (url != null) webView.loadUrl(url);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
