package com.downloader.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etSearch;
    private SwipeRefreshLayout swipeRefreshLayout;

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

        // WebViewClient আপডেট করা হয়েছে যাতে কাস্টম স্কিম (fb:// বা অ্যাপ লিংক) ক্র্যাশ না করে এবং ভিডিও লিংক ধরলে ডাউনলোড ট্রিগার হয়
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return handleUrlLoading(view, url);
            }

            @Overrideআপনার দেওয়া কোডটি একদম নিখুঁত আছে এবং এতে অ্যাডমোব, ওয়েবভিউ এবং থ্রেড হ্যান্ডলিং সুন্দরভাবে সাজানো আছে। মূলত সমস্যাটি হচ্ছে—যখন আপনি ওয়েবভিউ-এর ভেতরে ব্রাউজ করে কোনো ভিডিওর পেজে যান, তখন শুধু সার্চবারের এডিটটেক থেকে লিংক চেক করায় ব্যাকএন্ডে রিকোয়েস্ট যায় না। এছাড়া ফেসবুক বা অন্যান্য সাইটের নিজস্ব নেটিভ অ্যাপ স্কিমগুলো (যেমন `fb://` বা কাস্টম প্রোটোকল) ওয়েবভিউতে ক্র্যাশ বা লোড ফেইল করতে পারে।

আপনার আগের সব ফিচার, ডিজাইন এবং অ্যাডমব ইন্টিগ্রেশন হুবহু অক্ষত রেখে শুধু ওয়েবভিউ থেকে সরাসরি ভিডিও পেজ ডিটেক্ট করা এবং কাস্টম স্কিম হ্যান্ডেল করার লজিকসহ আপডেট করা সম্পূর্ণ `MainActivity.java` ফাইলটি নিচে দেওয়া হলো:

```java
package com.downloader.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etSearch;
    private SwipeRefreshLayout swipeRefreshLayout;

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
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // কাস্টম স্কিম (যেমন fb://) ক্র্যাশ রোধ করতে এবং শুধু http/https এলাউ করতে
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    view.loadUrl(url);
                    
                    // ওয়েবভিউতে ব্রাউজ করার সময় যদি সরাসরি কোনো ভিডিও লিংক বা পেজ ওপেন হয়, তা স্বয়ংক্রিয়ভাবে ডিটেক্ট করবে
                    if (isTargetVideoUrl(url)) {
                        etSearch.setText(url);
                        triggerDownloadFlow(url);
                    }
                    return true;
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                // পেজ লোড শেষ হলেও কারেন্ট ইউআরএল চেক করে ডাউনলোড ট্রিগার করতে পারে
                if (url != null && isTargetVideoUrl(url)) {
                    if (!url.equals(etSearch.getText().toString())) {
                        etSearch.setText(url);
                    }
                }
            }
        });

        webView.loadUrl("[https://www.facebook.com](https://www.facebook.com)");

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                etSearch.clearFocus();
                String finalUrl;
                if (query.startsWith("http://") || query.startsWith("https://")) {
                    finalUrl = query;
                } else {
                    finalUrl = "[https://www.google.com/search?q=](https://www.google.com/search?q=)" + query;
                }
                webView.loadUrl(finalUrl);

                if (isTargetVideoUrl(query)) {
                    triggerDownloadFlow(query);
                }
            }
            return true;
        });

        btnFacebook.setOnClickListener(v -> webView.loadUrl("[https://www.facebook.com](https://www.facebook.com)"));
        btnInstagram.setOnClickListener(v -> webView.loadUrl("[https://www.instagram.com](https://www.instagram.com)"));
        btnYoutube.setOnClickListener(v -> webView.loadUrl("[https://www.youtube.com](https://www.youtube.com)"));
        btnX.setOnClickListener(v -> webView.loadUrl("[https://x.com](https://x.com)"));
    }

    // ভিডিও লিংক ফিল্টার করার মেথড
    private boolean isTargetVideoUrl(String url) {
        if (url == null) return false;
        return url.contains("youtube.com") || 
               url.contains("youtu.be") || 
               url.contains("facebook.com") || 
               url.contains("fb.watch") || 
               url.contains("/videos/") || 
               url.contains("/reel/");
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
            mInterstitial`Ad.show(MainActivity.this);
        } else {
            executeProcessAndDownload(pendingDownloadUrl);
            loadAdMobAd();
        }
    }

    private void executeProcessAndDownload(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return;
        Toast.makeText(this, "Processing video from Termux server...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            String downloadLink = null;
            try {
                String encodedUrl = URLEncoder.encode(videoUrl, "UTF-8");
                String targetUrl = "[http://127.0.0.1:5000/get_video?url=](http://127.0.0.1:5000/get_video?url=)" + encodedUrl;
                
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
                if (finalLink != null && !finalLink.isEmpty()) {
                    startDownloadManager(finalLink);
                } else {
                    Toast.makeText(MainActivity.this, "Direct download link not found!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void startDownloadManager(String url) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Downloading Video");
            request.setDescription("Saving file securely...");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Video_" + System.currentTimeMillis() + ".mp4");

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Download started successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }
}
