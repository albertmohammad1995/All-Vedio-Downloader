package com.downloader.app;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etSearch;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabDownload;

    private InterstitialAd mInterstitialAd;

    private String pendingDownloadUrl = "";

    private static final String AD_UNIT_ID =
            "ca-app-pub-3649023459134036/7790678843";

    /*
     * তোমার বর্তমান API endpoint।
     * সার্ভারটি চালু ও সঠিকভাবে deploy করা আছে কি না,
     * সেটি আলাদাভাবে যাচাই করতে হবে।
     */
    private static final String API_BASE_URL =
            "https://all-vedio-downloaders-aq2p.vercel.app/api/download";

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        etSearch = findViewById(R.id.etSearch);
        webView = findViewById(R.id.webView);

        ImageButton btnFacebook = findViewById(R.id.btnFacebook);
        ImageButton btnInstagram = findViewById(R.id.btnInstagram);
        ImageButton btnYoutube = findViewById(R.id.btnYoutube);
        ImageButton btnX = findViewById(R.id.btnX);

        setupDownloadButton();
        setupWebView();
        setupSearch();
        setupPlatformButtons(
                btnFacebook,
                btnInstagram,
                btnYoutube,
                btnX
        );

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        MobileAds.initialize(
                this,
                initializationStatus -> mainHandler.post(this::loadAdMobAd)
        );

        webView.loadUrl("https://www.facebook.com");
    }

    private void setupDownloadButton() {

        fabDownload = new FloatingActionButton(this);

        fabDownload.setImageResource(android.R.drawable.ic_menu_save);

        fabDownload.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#FF0000")
                )
        );

        fabDownload.setColorFilter(Color.WHITE);
        fabDownload.setContentDescription("Download video");
        fabDownload.setVisibility(View.GONE);

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(16, 16, 48, 48);

        ViewGroup rootLayout = findViewById(android.R.id.content);
        rootLayout.addView(fabDownload, params);

        fabDownload.setOnClickListener(v -> {

            String currentUrl = webView.getUrl();

            if (currentUrl == null || !isTargetVideoUrl(currentUrl)) {
                showToast("Please open a supported video page first.");
                return;
            }

            triggerDownloadFlow(currentUrl);
        });
    }

    private void setupWebView() {

        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {

                String url = request.getUrl().toString();

                if (url.startsWith("http://")
                        || url.startsWith("https://")) {

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
    }

    private void setupSearch() {

        etSearch.setOnEditorActionListener((v, actionId, event) -> {

            String query = etSearch.getText().toString().trim();

            if (query.isEmpty()) {
                showToast("Enter a website or search term.");
                return true;
            }

            etSearch.clearFocus();

            String finalUrl;

            if (query.startsWith("https://")
                    || query.startsWith("http://")) {

                finalUrl = query;

            } else {

                try {
                    finalUrl = "https://www.google.com/search?q="
                            + URLEncoder.encode(query, "UTF-8");
                } catch (Exception e) {
                    showToast("Could not process your search.");
                    return true;
                }
            }

            webView.loadUrl(finalUrl);

            return true;
        });
    }

    private void setupPlatformButtons(
            ImageButton btnFacebook,
            ImageButton btnInstagram,
            ImageButton btnYoutube,
            ImageButton btnX
    ) {

        btnFacebook.setOnClickListener(
                v -> webView.loadUrl("https://www.facebook.com")
        );

        btnInstagram.setOnClickListener(
                v -> webView.loadUrl("https://www.instagram.com")
        );

        btnYoutube.setOnClickListener(
                v -> webView.loadUrl("https://www.youtube.com")
        );

        btnX.setOnClickListener(
                v -> webView.loadUrl("https://x.com")
        );
    }

    private void checkAndToggleDownloadButton(String url) {

        if (isTargetVideoUrl(url)) {

            pendingDownloadUrl = url;
            fabDownload.setVisibility(View.VISIBLE);

        } else {

            fabDownload.setVisibility(View.GONE);
        }
    }

    private boolean isTargetVideoUrl(String url) {

        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lower = url.toLowerCase(Locale.ROOT);

        return lower.contains("youtube.com/watch")
                || lower.contains("youtube.com/shorts/")
                || lower.contains("youtu.be/")
                || lower.contains("facebook.com/watch")
                || lower.contains("fb.watch/")
                || lower.contains("/videos/")
                || lower.contains("/reel/")
                || lower.matches(".*\\.(mp4|webm|mov|m4v)(\\?.*)?$");
    }

    private void loadAdMobAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(
                this,
                AD_UNIT_ID,
                adRequest,
                new InterstitialAdLoadCallback() {

                    @Override
                    public void onAdLoaded(
                            @NonNull InterstitialAd interstitialAd
                    ) {

                        mInterstitialAd = interstitialAd;

                        mInterstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {

                                    @Override
                                    public void onAdDismissedFullScreenContent() {

                                        mInterstitialAd = null;

                                        executeProcessAndDownload(
                                                pendingDownloadUrl
                                        );

                                        loadAdMobAd();
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(
                                            @NonNull AdError adError
                                    ) {

                                        mInterstitialAd = null;

                                        executeProcessAndDownload(
                                                pendingDownloadUrl
                                        );

                                        loadAdMobAd();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onAdFailedToLoad(
                            @NonNull LoadAdError loadAdError
                    ) {
                        mInterstitialAd = null;
                    }
                }
        );
    }

    private void triggerDownloadFlow(String url) {

        if (url == null || url.trim().isEmpty()) {
            showToast("Video URL is missing.");
            return;
        }

        pendingDownloadUrl = url;

        showToast("Preparing video...");

        if (mInterstitialAd != null) {

            mInterstitialAd.show(MainActivity.this);

        } else {

            executeProcessAndDownload(pendingDownloadUrl);
            loadAdMobAd();
        }
    }

    private void executeProcessAndDownload(String videoUrl) {

        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            showToast("Video URL is missing.");
            return;
        }

        showToast("Connecting to download server...");

        executorService.execute(() -> {

            String downloadLink = null;
            String errorMessage = "Could not get a valid video link.";

            HttpURLConnection connection = null;

            try {

                String encodedUrl =
                        URLEncoder.encode(videoUrl, "UTF-8");

                String targetUrl =
                        API_BASE_URL + "?url=" + encodedUrl;

                URL url = new URL(targetUrl);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(20000);

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int statusCode = connection.getResponseCode();

                InputStream inputStream;

                if (statusCode >= 200 && statusCode < 300) {

                    inputStream = connection.getInputStream();

                } else {

                    inputStream = connection.getErrorStream();
                    errorMessage = "Server returned HTTP " + statusCode;
                }

                StringBuilder responseString = new StringBuilder();

                if (inputStream != null) {

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            )
                    );

                    String line;

                    while ((line = reader.readLine()) != null) {
                        responseString.append(line);
                    }

                    reader.close();
                    inputStream.close();
                }

                if (statusCode >= 200 && statusCode < 300) {

                    JSONObject jsonObject =
                            new JSONObject(responseString.toString());

                    if (jsonObject.optBoolean("success", false)) {

                        String candidate =
                                jsonObject.optString(
                                        "download_url",
                                        ""
                                ).trim();

                        Uri parsedUri = Uri.parse(candidate);

                        String scheme = parsedUri.getScheme();

                        if (!candidate.isEmpty()
                                && parsedUri.getHost() != null
                                && ("https".equalsIgnoreCase(scheme)
                                || "http".equalsIgnoreCase(scheme))) {

                            downloadLink = candidate;

                        } else {

                            errorMessage =
                                    "The server returned an invalid video link.";
                        }

                    } else {

                        errorMessage = jsonObject.optString(
                                "error",
                                "The server could not process this video."
                        );
                    }
                }

            } catch (Exception e) {

                android.util.Log.e(
                        "VideoDownloader",
                        "API request failed",
                        e
                );

                errorMessage =
                        "Could not connect to the server. Please try again.";

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

            final String finalLink = downloadLink;
            final String finalError = errorMessage;

            mainHandler.post(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (finalLink == null || finalLink.trim().isEmpty()) {

                    showToast(finalError);
                    return;
                }

                Intent intent = new Intent(
                        MainActivity.this,
                        PlayerActivity.class
                );

                intent.putExtra("video_url", videoUrl);
                intent.putExtra("download_url", finalLink);

                startActivity(intent);
            });
        });
    }

    private void showToast(String message) {

        Toast.makeText(
                MainActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.destroy();
        }

        executorService.shutdownNow();

        super.onDestroy();
    }
}
