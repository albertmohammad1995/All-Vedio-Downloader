package com.downloader.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etSearch;
    private SwipeRefreshLayout swipeRefreshLayout;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabDownload;

    private InterstitialAd mInterstitialAd;
    private String pendingDownloadUrl = "";
    
    private static final String AD_UNIT_ID = "ca-app-pub-3649023459134036/7790678843";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> loadAdMobAd());

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        etSearch = findViewById(R.id.etSearch);
        webView = findViewById(R.id.webView);

        ImageButton btnFacebook = findViewById(R.id.btnFacebook);
        ImageButton btnInstagram = findViewById(R.id.btnInstagram);
        ImageButton btnYoutube = findViewById(R.id.btnYoutube);
        ImageButton btnX = findViewById(R.id.btnX);

        // ব্রাউজ করার সময় স্ক্রিনের নিচে ডান কোণায় ফ্লোটিং ডাউনলোড বাটন তৈরি করা
        fabDownload = new com.google.android.material.floatingactionbutton.FloatingActionButton(this);
        fabDownload.setImageResource(android.R.drawable.ic_menu_save);
        fabDownload.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF0000")));
        fabDownload.setVisibility(View.GONE); // শুরুতে লুকিয়ে থাকবে, ভিডিও পেজ আসলে ভেসে উঠবে

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0, 0, 48, 48);
        
        ViewGroup rootLayout = findViewById(android.R.id.content);
        rootLayout.addView(fabDownload, params);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                checkAndToggleDownloadButton(url);
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                    return true;
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                checkAndToggleDownloadButton(url);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        webView.loadUrl("https://www.facebook.com");

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                etSearch.clearFocus();
                String finalUrl;
                if (query.startsWith("http://") || query.startsWith("https://")) {
                    finalUrl = query;
                } else {
                    finalUrl = "https://www.google.com/search?q=" + query;
                }
                webView.loadUrl(finalUrl);
                checkAndToggleDownloadButton(finalUrl);
            }
            return true;
        });

        fabDownload.setOnClickListener(v -> {
            String currentUrl = webView.getUrl();
            if (currentUrl != null && isTargetVideoUrl(currentUrl)) {
                triggerDownloadFlow(currentUrl);
            } else {
                Toast.makeText(this, "Please open a video first!", Toast.LENGTH_SHORT).show();
            }
        });

        btnFacebook.setOnClickListener(v -> webView.loadUrl("https://www.facebook.com"));
        btnInstagram.setOnClickListener(v -> webView.loadUrl("https://www.instagram.com"));
        btnYoutube.setOnClickListener(v -> webView.loadUrl("https://www.youtube.com"));
        btnX.setOnClickListener(v -> webView.loadUrl("https://x.com"));
    }

    private void checkAndToggleDownloadButton(String url) {
        if (isTargetVideoUrl(url)) {
            pendingDownloadUrl = url;
            fabDownload.setVisibility(View.VISIBLE); // ভিডিও পেজ পেলে ফ্লোটিং বাটন শো করবে
        } else {
            fabDownload.setVisibility(View.GONE);
        }
    }

    // স্ক্রিনশট অনুযায়ী আপডেট করা শক্তিশালী ভিডিও ইউআরএল ডিটেক্টর
    private boolean isTargetVideoUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("youtube.com/watch") || 
               lower.contains("youtu.be/") || 
               lower.contains("facebook.com/watch") || 
               lower.contains("fb.watch") || 
               lower.contains("/videos/") || 
               lower.contains("/reel/") ||
               lower.matches(".*\\.(mp4|webm|mov|m4v)(\\?.*)?$");
    }

    private void loadAdMobAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, AD_UNIT_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        executeProcessAndDownload(pendingDownloadUrl);
                        loadAdMobAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        executeProcessAndDownload(pendingDownloadUrl);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
            }
        });
    }

    private void triggerDownloadFlow(String url) {
        if (url == null || url.isEmpty()) return;
        pendingDownloadUrl = url;
        Toast.makeText(this, "Preparing download...", Toast.LENGTH_SHORT).show();

        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        } else {
            executeProcessAndDownload(pendingDownloadUrl);
            loadAdMobAd();
        }
    }

    private void executeProcessAndDownload(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return;
        Toast.makeText(this, "Processing video from server...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            String downloadLink = null;
            try {
                String encodedUrl = URLEncoder.encode(videoUrl, "UTF-8");
                String targetUrl = "https://all-vedio-downloaders-aq2p.vercel.app/api/download?url=" + encodedUrl;
                
                URL url = new URL(targetUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder responseString = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseString.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(responseString.toString());
                    if (jsonObject.optBoolean("success", false)) {
                        downloadLink = jsonObject.optString("download_url");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String finalLink = downloadLink;
            mainHandler.post(() -> {
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("video_url", videoUrl);
                intent.putExtra("download_url", (finalLink != null && !finalLink.isEmpty()) ? finalLink : videoUrl);
                startActivity(intent);
            });
        });
    }
}
