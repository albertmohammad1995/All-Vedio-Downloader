package com.downloader.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private WebView webView;
    private EditText etUrlBox;
    private ExtendedFloatingActionButton fabDownload;
    private String currentVideoUrl = "";

    private RewardedAd rewardedAd;
    private boolean isLoading = false;
    private FirebaseAnalytics mFirebaseAnalytics;
    private final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"; // আপনার রিয়েল আইডি বসাতে চাইলে এটি বদলে দেবেন: ca-app-pub-3649823459134836/7790678843

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> loadRewardedAd());

        webView = findViewById(R.id.webView);
        etUrlBox = findViewById(R.id.etUrlBox);
        fabDownload = findViewById(R.id.fabDownload);

        Button btnGo = findViewById(R.id.btnGo);
        Button btnFb = findViewById(R.id.btnFb);
        Button btnInsta = findViewById(R.id.btnInsta);
        Button btnYoutube = findViewById(R.id.btnYoutube);

        setupWebView();

        // Default open Facebook
        webView.loadUrl("https://m.facebook.com");

        // Bottom Menu clicks
        btnFb.setOnClickListener(v -> webView.loadUrl("https://m.facebook.com"));
        btnInsta.setOnClickListener(v -> webView.loadUrl("https://www.instagram.com"));
        btnYoutube.setOnClickListener(v -> webView.loadUrl("https://www.youtube.com"));

        // Search Bar Go click
        btnGo.setOnClickListener(v -> loadUrlFromSearch());
        etUrlBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromSearch();
                return true;
            }
            return false;
        });

        // Floating Download Button click -> Show Rewarded Ad first, then download
        fabDownload.setOnClickListener(v -> {
            if (!currentVideoUrl.isEmpty()) {
                showRewardedAd();
            } else {
                Toast.makeText(this, "No video detected yet!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRewardedAd() {
        if (rewardedAd == null && !isLoading) {
            isLoading = true;
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(this, AD_UNIT_ID, adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedAd = ad;
                            isLoading = false;
                            Log.d(TAG, "Ad was loaded.");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            rewardedAd = null;
                            isLoading = false;
                            Log.d(TAG, loadAdError.getMessage());
                        }
                    });
        }
    }

    private void showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd.show(MainActivity.this, rewardItem -> {
                Toast.makeText(MainActivity.this, "Reward Earned! Starting download...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentVideoUrl));
                startActivity(intent);
                loadRewardedAd(); // Load next ad
            });
        } else {
            Toast.makeText(this, "Ad is still loading. Please try again in a moment.", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }

    private void loadUrlFromSearch() {
        String query = etUrlBox.getText().toString().trim();
        if (!query.isEmpty()) {
            if (!query.startsWith("http://") && !query.startsWith("https://")) {
                if (query.contains(".") && !query.contains(" ")) {
                    query = "https://" + query;
                } else {
                    query = "https://www.google.com/search?q=" + Uri.encode(query);
                }
            }
            webView.loadUrl(query);
        }
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                etUrlBox.setText(url);

                view.evaluateJavascript(
                    "(function() {" +
                        "var videos = document.getElementsByTagName('video');" +
                        "if(videos.length > 0) {" +
                            "return videos[0].src;" +
                        "}" +
                        "return '';" +
                    "})();",
                    value -> {
                        if (value != null && !value.equals("null") && !value.isEmpty()) {
                            currentVideoUrl = value.replace("\"", "");
                            fabDownload.setVisibility(View.VISIBLE);
                        } else {
                            if (url.contains("/videos/") || url.contains("/reel/") || url.contains("watch") || url.contains("shorts")) {
                                currentVideoUrl = url;
                                fabDownload.setVisibility(View.VISIBLE);
                            } else {
                                fabDownload.setVisibility(View.GONE);
                            }
                        }
                    }
                );
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
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
