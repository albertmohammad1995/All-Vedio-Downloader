// app/java/com/downloader/app/MainActivity.java
package com.downloader.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private EditText etSearch;
    private WebView webView;
    private Button btnFacebook, btnInstagram, btnYoutube, btnX;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private InterstitialAd mInterstitialAd;
    private String pendingDownloadUrl = "";

    private static final String AD_UNIT_ID = "ca-app-pub-3649023459134036/7790678843";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> loadAdMobAd());

        etSearch = findViewById(R.id.etSearch);
        webView = findViewById(R.id.webView);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnInstagram = findViewById(R.id.btnInstagram);
        btnYoutube = findViewById(R.id.btnYoutube);
        btnX = findViewById(R.id.btnX);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setSupportMultipleWindows(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidDownloader");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("fb://") || url.startsWith("instagram://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        return true;
                    }
                }
                etSearch.setText("");
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                etSearch.setText("");
                injectSmartDownloadScript(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                WebSettings settings = newWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, String url) {
                        webView.loadUrl(url);
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.loadUrl("https://www.facebook.com");

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        // স্মার্ট সার্চ বার লজিক: ফেসবুক বা ইউটিউব লিংক আলাদাভাবে হ্যান্ডেল করার ব্যবস্থা
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    etSearch.clearFocus();
                    if (query.contains("facebook.com") || query.contains("fb.watch")) {
                        Toast.init(MainActivity.this, "Processing Facebook Link...", Toast.LENGTH_SHORT).show();
                        webView.loadUrl(query);
                    } else if (query.contains("youtube.com") || query.contains("youtu.be")) {
                        Toast.makeText(MainActivity.this, "Processing YouTube Link...", Toast.LENGTH_SHORT).show();
                        webView.loadUrl(query);
                    } else if (query.startsWith("http://") || query.startsWith("https://")) {
                        webView.loadUrl(query);
                    } else if (query.contains(".")) {
                        webView.loadUrl("https://" + query);
                    } else {
                        webView.loadUrl("https://www.google.com/search?q=" + Uri.encode(query));
                    }
                    etSearch.setText("");
                }
                return true;
            }
            return false;
        });

        btnFacebook.setOnClickListener(v -> {
            webView.loadUrl("https://www.facebook.com");
            etSearch.setText("");
            etSearch.clearFocus();
        });
        btnInstagram.setOnClickListener(v -> {
            webView.loadUrl("https://www.instagram.com");
            etSearch.setText("");
            etSearch.clearFocus();
        });
        btnYoutube.setOnClickListener(v -> {
            webView.loadUrl("https://www.youtube.com");
            etSearch.setText("");
            etSearch.clearFocus();
        });
        btnX.setOnClickListener(v -> {
            webView.loadUrl("https://twitter.com");
            etSearch.setText("");
            etSearch.clearFocus();
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> triggerDownloadFlow(url));
    }

    public class WebAppInterface {
        Context mContext;
        WebAppInterface(Context c) { mContext = c; }

        @JavascriptInterface
        public void downloadVideo(String videoUrl) {
            runOnUiThread(() -> triggerDownloadFlow(videoUrl));
        }
    }

    private void triggerDownloadFlow(String url) {
        if (url == null || url.isEmpty()) return;
        pendingDownloadUrl = url;
        Toast.makeText(MainActivity.this, "Please wait, opening ad...", Toast.LENGTH_SHORT).show();

        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        } else {
            executeDownload(pendingDownloadUrl);
            loadAdMobAd();
        }
    }

    private void injectSmartDownloadScript(WebView view) {
        String jsCode = "javascript:(function() {" +
                "  if (window.smartDownloaderLoaded) return;" +
                "  window.smartDownloaderLoaded = true;" +
                "  setInterval(function() {" +
                "    var videos = document.querySelectorAll('video');" +
                "    videos.forEach(function(video) {" +
                "      if (!video.dataset.downloadInjected) {" +
                "        video.dataset.downloadInjected = 'true';" +
                "        var container = video.parentElement;" +
                "        if (container) {" +
                "          container.style.position = 'relative';" +
                "          var btn = document.createElement('button');" +
                "          btn.innerHTML = '⬇ Download';" +
                "          btn.style.position = 'absolute';" +
                "          btn.style.top = '10px';" +
                "          btn.style.right = '10px';" +
                "          btn.style.zIndex = '999999';" +
                "          btn.style.background = '#ff0000';" +
                "          btn.style.color = '#ffffff';" +
                "          btn.style.border = '2px solid #fff';" +
                "          btn.style.padding = '6px 10px';" +
                "          btn.style.borderRadius = '6px';" +
                "          btn.style.fontSize = '12px';" +
                "          btn.style.fontWeight = 'bold';" +
                "          btn.style.cursor = 'pointer';" +
                "          btn.onclick = function(e) {" +
                "            e.stopPropagation();" +
                "            var src = video.src || (video.querySelector('source') ? video.querySelector('source').src : '');" +
                "            if(!src) {" +
                "               var sourceTag = video.closest('div') ? video.closest('div').querySelector('source') : null;" +
                "               if(sourceTag) src = sourceTag.src;" +
                "            }" +
                "            if(src && !src.startsWith('blob:')) {" +
                "              AndroidDownloader.downloadVideo(src);" +
                "            } else {" +
                "              AndroidDownloader.downloadVideo(window.location.href);" +
                "            }" +
                "          };" +
                "          container.appendChild(btn);" +
                "        }" +
                "      }" +
                "    });" +
                "  }, 1000);" +
                "})();";
        view.evaluateJavascript(jsCode, null);
    }

    private void loadAdMobAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                executeDownload(pendingDownloadUrl);
                                loadAdMobAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                executeDownload(pendingDownloadUrl);
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
    }

    private void executeDownload(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Downloading Video");
            request.setDescription("Downloading file from Video Downloader App");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Video_" + System.currentTimeMillis() + ".mp4");

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(MainActivity.this, "Download started successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
