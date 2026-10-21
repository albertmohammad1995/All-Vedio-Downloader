package com.downloader.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PlayerActivity extends AppCompatActivity {

    private WebView playerWebView;
    private Button btnDownload;
    private String pageUrl = "";
    private String capturedVideoUrl = "";

    // আপনার সঠিক Vercel লাইভ প্রজেক্টের ডোমেন
    private static final String VERCEL_API_BASE = "https://all-vedio-downloaders-aq2p.vercel.app/api/download?url=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerWebView = findViewById(R.id.playerWebView);
        btnDownload = findViewById(R.id.btnDownload);

        // Intent থেকে ভিডিও বা পেজ লিংক গ্রহণ
        pageUrl = getIntent().getStringExtra("video_url");
        String initialDownloadUrl = getIntent().getStringExtra("download_url");
        if (initialDownloadUrl != null && !initialDownloadUrl.isEmpty()) {
            capturedVideoUrl = initialDownloadUrl;
        }

        WebSettings webSettings = playerWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // ভিডিও মিডিয়া ফাইল Intercept/Grab করা
        playerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                view.loadUrl(url);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                if (url.contains(".mp4") || url.contains(".m3u8") || url.contains("video/") || url.contains("googlevideo.com") || url.contains("fbcdn")) {
                    Log.d("VideoGrabber", "Captured Video Link: " + url);
                    if (url.length() > 30) {
                        capturedVideoUrl = url;
                        runOnUiThread(() -> {
                            btnDownload.setText("Download Ready (Stream Found)");
                        });
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        // WebView ডাউনলোড লিসেনার
        playerWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url != null && !url.isEmpty()) {
                startDownloadManager(url);
            } else {
                Toast.makeText(PlayerActivity.this, "Download link is not supported", Toast.LENGTH_SHORT).show();
            }
        });

        if (pageUrl != null && !pageUrl.isEmpty()) {
            playerWebView.loadUrl(pageUrl);
        }

        // ডাউনলোড বাটনে ক্লিক করলে মাল্টি-লেয়ার প্রসেসিং শুরু হবে
        btnDownload.setOnClickListener(v -> {
            String currentWebUrl = playerWebView.getUrl();
            
            if (capturedVideoUrl != null && !capturedVideoUrl.isEmpty()) {
                // ১. যদি WebView কোনো স্ট্রিম লিংক ধরে থাকে, তা দিয়ে ডাউনলোড শুরু হবে
                startDownloadManager(capturedVideoUrl);
            } else if (currentWebUrl != null && !currentWebUrl.isEmpty()) {
                // ২. সার্ভার থেকে লিংক ফেচ করার চেষ্টা করবে
                Toast.makeText(this, "Fetching direct link from server...", Toast.LENGTH_SHORT).show();
                fetchDirectLinkFromServer(currentWebUrl);
            } else if (pageUrl != null && !pageUrl.isEmpty()) {
                Toast.makeText(this, "Fetching direct link from server...", Toast.LENGTH_SHORT).show();
                fetchDirectLinkFromServer(pageUrl);
            } else {
                Toast.makeText(this, "Please play the video first!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchDirectLinkFromServer(String targetUrl) {
        new Thread(() -> {
            try {
                String apiCallUrl = VERCEL_API_BASE + Uri.encode(targetUrl);
                URL url = new URL(apiCallUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(result.toString());
                boolean success = jsonObject.optBoolean("success", false);

                if (success) {
                    String directDownloadUrl = jsonObject.optString("download_url");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (directDownloadUrl != null && !directDownloadUrl.isEmpty()) {
                            startDownloadManager(directDownloadUrl);
                        } else {
                            // সার্ভার থেকে লিংক না পেলে ফলব্যাক হিসেবে মূল পেজ বা ভিডিও ইউআরএল ট্রাই করবে
                            startDownloadManager(targetUrl);
                        }
                    });
                } else {
                    // সার্ভার রেসপন্স ফেইল করলেও যেন ডাউনলোড আটকে না যায়, তাই মূল লিংক পাস করবে
                    new Handler(Looper.getMainLooper()).post(() -> startDownloadManager(targetUrl));
                }

            } catch (Exception e) {
                Log.e("ApiError", "Error fetching link", e);
                // কোনো রকম এক্সেপশন বা এরর আসলেও ফলব্যাক হিসেবে মূল লিংক দিয়ে ডাউনলোড শুরু করার চেষ্টা করবে
                new Handler(Looper.getMainLooper()).post(() -> startDownloadManager(targetUrl));
            }
        }).start();
    }

    private void startDownloadManager(String url) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Downloading Video");
            request.setDescription("Saving media file securely...");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Video_" + System.currentTimeMillis() + ".mp4");

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Download started successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
