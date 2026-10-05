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
import android.widget.ImageButton; // ImageButton import করা হয়েছে
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
    
    // আপনার নির্দিষ্ট অ্যাডমব আইডি
    private static final String AD_UNIT_ID = "ca-app-pub-3649023459134036/7790678843";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // AdMob ইনিশিয়ালাইজেশন
        MobileAds.initialize(this, initializationStatus -> loadAdMobAd());

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        etSearch = findViewById(R.id.etSearch);
        webView = findViewById(R.id.webView);

        // লেআউটের সাথে সামঞ্জস্য রেখে ImageButton করা হয়েছে
        ImageButton btnFacebook = findViewById(R.id.btnFacebook);
        ImageButton btnInstagram = findViewById(R.id.btnInstagram);
        ImageButton btnYoutube = findViewById(R.id.btnYoutube);
        ImageButton btnX = findViewById(R.id.btnX);

        // WebView সেটিংস
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
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (url != null) {
                    etSearch.setText(url);
                }
            }
        });

        webView.loadUrl("https://www.facebook.com");

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        // সার্চবার বা ইনপুটে লিংক লিখে সার্চ করলে অ্যাড শো করবে এবং প্রসেস হবে
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

                // ভিডিও লিংক হলে অ্যাড ফ্লো ট্রিগার করা
                if (query.contains("youtube.com") || query.contains("youtu.be") || query.contains("facebook.com") || query.contains("fb.watch")) {
                    triggerDownloadFlow(query);
                }
            }
            return true;
        });

        btnFacebook.setOnClickListener(v -> webView.loadUrl("https://www.facebook.com"));
        btnInstagram.setOnClickListener(v -> webView.loadUrl("https://www.instagram.com"));
        btnYoutube.setOnClickListener(v -> webView.loadUrl("https://www.youtube.com"));
        btnX.setOnClickListener(v -> webView.loadUrl("https://x.com"));
    }

    // অ্যাডমব অ্যাড লোড করার ফাংশন
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

    // অ্যাড দেখানোর ট্রিগার ফ্লো
    private void triggerDownloadFlow(String url) {
        if (url == null || url.isEmpty()) return;
        pendingDownloadUrl = url;
        Toast.makeText(this, "প্রিপারিং ডাউনলোড...", Toast.LENGTH_SHORT).show();

        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        } else {
            executeProcessAndDownload(pendingDownloadUrl);
            loadAdMobAd();
        }
    }

    // ব্যাকগ্রাউন্ডে টার্মাক্স সার্ভার থেকে লিংক ফেচ করা
    private void executeProcessAndDownload(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return;
        Toast.makeText(this, "টার্মাক্স সার্ভার থেকে ভিডিও প্রসেস হচ্ছে...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            String downloadLink = null;
            try {
                String encodedUrl = URLEncoder.encode(videoUrl, "UTF-8");
                String targetUrl = "http://127.0.0.1:5000/get_video?url=" + encodedUrl;
                
                URL url = new URL(targetUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

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
                    Toast.npmToast(MainActivity.this, "ডাইরেক্ট ডাউনলোড লিংক পাওয়া যায়নি!", Toast.LENGTH_SHORT); // Safe fallback
                }
            });
        });
    }

    // অ্যান্ড্রয়েডের নিজস্ব DownloadManager দিয়ে ফাইল নামিয়ে নেওয়া
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
                Toast.makeText(this, "ডাউনলোড সফলভাবে শুরু হয়েছে!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }
}
