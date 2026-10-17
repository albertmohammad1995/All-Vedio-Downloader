package com.downloader.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PlayerActivity extends AppCompatActivity {

    private WebView playerWebView;
    private Button btnDownload;
    private String videoUrl = "";
    private String downloadUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerWebView = findViewById(R.id.playerWebView);
        btnDownload = findViewById(R.id.btnDownload);

        // MainActivity থেকে পাঠানো ভিডিও লিংক এবং ডাউনলোড লিংক রিসিভ করা
        videoUrl = getIntent().getStringExtra("video_url");
        downloadUrl = getIntent().getStringExtra("download_url");

        WebSettings webSettings = playerWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        playerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });

        // WebView-এ ভিডিও পেজটি লোড করা যাতে ইউজার দেখতে পারে
        if (videoUrl != null && !videoUrl.isEmpty()) {
            playerWebView.loadUrl(videoUrl);
        }

        // ডাউনলোড বাটনে ক্লিক করলে সরাসরি ডাউনলোড ম্যানেজার দিয়ে ফাইল সেভ হবে
        btnDownload.setOnClickListener(v -> {
            String targetLink = (downloadUrl != null && !downloadUrl.isEmpty()) ? downloadUrl : videoUrl;
            if (targetLink != null && !targetLink.isEmpty()) {
                startDownloadManager(targetLink);
            } else {
                Toast.makeText(this, "Download link not found!", Toast.LENGTH_SHORT).show();
            }
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
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
