package com.downloader.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PlayerActivity extends AppCompatActivity {

    private WebView playerWebView;
    private Button btnDownload;
    private String pageUrl = "";
    private String capturedVideoUrl = "";

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

        // 1DM এর মত ভিডিও মিডিয়া ফাইল Intercept/Grab করা
        playerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // .mp4, .m3u8, video/ ইত্যাদির মত ডিরেক্ট মিডিয়া স্ট্রিম রিকোয়েস্ট ফিল্টার
                if (url.contains(".mp4") || url.contains(".m3u8") || url.contains("video/") || url.contains("googlevideo.com")) {
                    Log.d("VideoGrabber", "Captured Video Link: " + url);
                    capturedVideoUrl = url;
                    
                    runOnUiThread(() -> {
                        btnDownload.setText("Download Video (Link Found)");
                    });
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        if (pageUrl != null && !pageUrl.isEmpty()) {
            playerWebView.loadUrl(pageUrl);
        }

        // ডাউনলোড বাটনে ক্লিক করলে ক্যাচ করা লিঙ্ক থেকে ভিডিও ডাউনলোড শুরু হবে
        btnDownload.setOnClickListener(v -> {
            String finalUrl = (capturedVideoUrl != null && !capturedVideoUrl.isEmpty()) ? capturedVideoUrl : pageUrl;
            if (finalUrl != null && !finalUrl.isEmpty()) {
                startDownloadManager(finalUrl);
            } else {
                Toast.makeText(this, "No downloadable video link detected yet!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startDownloadManager(String url) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Downloading Video");
            request.setDescription("Saving media file...");
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
