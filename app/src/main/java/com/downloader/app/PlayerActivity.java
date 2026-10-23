package com.downloader.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class PlayerActivity extends AppCompatActivity {

    private WebView playerWebView;
    private Button btnDownload;

    private String pageUrl = "";
    private String downloadUrl = "";

    private boolean isProcessing = false;

    private static final String API_BASE =
            "https://all-vedio-downloaders-aq2p.vercel.app/api/download";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerWebView = findViewById(R.id.playerWebView);
        btnDownload = findViewById(R.id.btnDownload);

        pageUrl = getIntent().getStringExtra("video_url");

        String receivedDownloadUrl =
                getIntent().getStringExtra("download_url");

        if (isValidHttpUrl(receivedDownloadUrl)
                && !isSamePageAsSource(receivedDownloadUrl, pageUrl)) {
            downloadUrl = receivedDownloadUrl;
        }

        setupWebView();

        if (pageUrl != null && isValidHttpUrl(pageUrl)) {
            playerWebView.loadUrl(pageUrl);
        }

        updateButton();

        btnDownload.setOnClickListener(v -> {
            if (isProcessing) {
                showToast("Please wait...");
                return;
            }

            if (isValidHttpUrl(downloadUrl)
                    && !isSamePageAsSource(downloadUrl, pageUrl)) {

                startDownload(downloadUrl);

            } else if (isValidHttpUrl(pageUrl)) {

                fetchDirectLink(pageUrl);

            } else {

                showToast("A valid video page is required.");
            }
        });
    }

    private void setupWebView() {

        WebSettings settings = playerWebView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        playerWebView.setWebViewClient(new WebViewClient());
    }

    private void updateButton() {

        if (btnDownload == null) {
            return;
        }

        if (isProcessing) {
            btnDownload.setText("Please wait...");
        } else if (isValidHttpUrl(downloadUrl)) {
            btnDownload.setText("Download Video");
        } else {
            btnDownload.setText("Get Video Link");
        }
    }

    private boolean isValidHttpUrl(String value) {

        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            Uri uri = Uri.parse(value.trim());
            String scheme = uri.getScheme();

            return uri.getHost() != null
                    && ("https".equalsIgnoreCase(scheme)
                    || "http".equalsIgnoreCase(scheme));

        } catch (Exception e) {
            return false;
        }
    }

    /*
     * Prevent the original social-media page from being
     * mistaken for a direct video file URL.
     */
    private boolean isSamePageAsSource(String candidate, String source) {

        if (!isValidHttpUrl(candidate) || !isValidHttpUrl(source)) {
            return false;
        }

        try {
            Uri candidateUri = Uri.parse(candidate);
            Uri sourceUri = Uri.parse(source);

            String candidateHost = candidateUri.getHost();
            String sourceHost = sourceUri.getHost();

            if (candidateHost == null || sourceHost == null) {
                return false;
            }

            return candidate.equals(source)
                    || candidateHost.equalsIgnoreCase(sourceHost)
                    && candidateUri.getPath() != null
                    && candidateUri.getPath().equals(sourceUri.getPath());

        } catch (Exception e) {
            return false;
        }
    }

    private void fetchDirectLink(String targetUrl) {

        if (!isValidHttpUrl(targetUrl)) {
            showToast("Invalid video page URL.");
            return;
        }

        isProcessing = true;
        updateButton();
        showToast("Requesting video link...");

        executor.execute(() -> {

            String resultUrl = null;
            String error = "The server could not find a video link.";

            HttpURLConnection connection = null;

            try {
                String encoded =
                        URLEncoder.encode(targetUrl, "UTF-8");

                URL requestUrl = new URL(API_BASE + "?url=" + encoded);

                connection =
                        (HttpURLConnection) requestUrl.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int status = connection.getResponseCode();

                InputStream stream;

                if (status >= 200 && status < 300) {
                    stream = connection.getInputStream();
                } else {
                    stream = connection.getErrorStream();
                    error = "Server error: HTTP " + status;
                }

                StringBuilder response = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 stream,
                                                 StandardCharsets.UTF_8
                                         )
                                 )) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                if (status >= 200 && status < 300) {

                    JSONObject json = new JSONObject(
                            response.toString()
                    );

                    if (json.optBoolean("success", false)) {

                        String candidate =
                                json.optString("download_url", "").trim();

                        if (isValidHttpUrl(candidate)
                                && !isSamePageAsSource(
                                        candidate,
                                        targetUrl
                                )) {

                            resultUrl = candidate;

                        } else {

                            error = "Server returned an invalid video URL.";
                        }

                    } else {

                        error = json.optString(
                                "error",
                                "Video link could not be created."
                        );
                    }
                }

            } catch (Exception e) {

                android.util.Log.e(
                        "PlayerActivity",
                        "API request failed",
                        e
                );

                error = "Could not connect to the server.";

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

            final String finalUrl = resultUrl;
            final String finalError = error;

            runOnUiThread(() -> {

                isProcessing = false;

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (finalUrl == null || finalUrl.isEmpty()) {

                    updateButton();
                    showToast(finalError);
                    return;
                }

                downloadUrl = finalUrl;

                updateButton();

                showToast("Video link received. Tap Download Video.");
            });
        });
    }

    private void startDownload(String url) {

        if (!isValidHttpUrl(url)) {
            showToast("Invalid download URL.");
            return;
        }

        if (isSamePageAsSource(url, pageUrl)) {
            showToast("This is a webpage, not a direct video link.");
            return;
        }

        Uri uri = Uri.parse(url);

        String path = uri.getPath();

        if (path == null) {
            path = "";
        }

        String lowerPath = path.toLowerCase(Locale.ROOT);

        if (lowerPath.endsWith(".m3u8")
                || lowerPath.endsWith(".mpd")) {

            showToast(
                    "This is a streaming playlist, not a direct video file."
            );
            return;
        }

        /*
         * DownloadManager can download a direct media URL.
         * It cannot turn a webpage or playlist into an MP4.
         */
        try {

            DownloadManager manager =
                    (DownloadManager) getSystemService(
                            Context.DOWNLOAD_SERVICE
                    );

            if (manager == null) {
                showToast("Android Download Manager is unavailable.");
                return;
            }

            String extension = getFileExtension(lowerPath);

            String fileName =
                    "Video_" + System.currentTimeMillis() + extension;

            DownloadManager.Request request =
                    new DownloadManager.Request(uri);

            request.setTitle("Video Download");
            request.setDescription("Downloading video...");
            request.setNotificationVisibility(
                    DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
            );

            long downloadId = manager.enqueue(request);

            if (downloadId > 0) {
                showToast(
                        "Download queued. Check your Downloads folder."
                );
            } else {
                showToast("Could not start the download.");
            }

        } catch (Exception e) {

            android.util.Log.e(
                    "PlayerActivity",
                    "Download failed",
                    e
            );

            showToast("Download failed. Check the video link.");
        }
    }

    private String getFileExtension(String path) {

        if (path.endsWith(".webm")) {
            return ".webm";
        }

        if (path.endsWith(".mov")) {
            return ".mov";
        }

        if (path.endsWith(".m4v")) {
            return ".m4v";
        }

        if (path.endsWith(".mp4")) {
            return ".mp4";
        }

        /*
         * Do not claim every unknown URL is an MP4.
         */
        return ".video";
    }

    private void showToast(String message) {

        runOnUiThread(() -> Toast.makeText(
                PlayerActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show());
    }

    @Override
    protected void onDestroy() {

        if (playerWebView != null) {
            playerWebView.stopLoading();
            playerWebView.destroy();
        }

        executor.shutdownNow();

        super.onDestroy();
    }
}
