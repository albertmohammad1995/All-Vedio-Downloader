package com.example.downloader // আপনার আসল প্যাকেজ নেম এখানে দেবেন

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnDownload: Button
    private var videoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        webView = findViewById(R.id.playerWebView)
        btnDownload = findViewById(R.id.btnDownload)

        // MainActivity থেকে পাঠানো ভিডিও লিংক রিসিভ করা
        videoUrl = intent.getStringExtra("VIDEO_URL")

        setupWebView()

        if (!videoUrl.isNullOrEmpty()) {
            // ফেসবুক বা ইউটিউব লিংক সরাসরি প্লেয়ার বা মোবাইল সাইটে লোড করা
            webView.loadUrl(videoUrl!!)
        }

        // ডাউনলোড বাটনে ক্লিক করলে অ্যাড রান এবং টার্মাক্স সার্ভার থেকে ডাউনলোড শুরু হবে
        btnDownload.setOnClickListener {
            showAdAndStartDownload()
        }
    }

    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun showAdAndStartDownload() {
        // ১. প্রথমে এখানে আপনার অ্যাড (Ad) দেখানোর কোড রান হবে (যেমন: Interstitial Ad)
        Toast.sleep(1000) // সিমুলেশন বা অ্যাড লোডিং
        Toast.parseToast(this, "অ্যাড শেষ হচ্ছে, ডাউনলোড শুরু হচ্ছে...")

        // ২. অ্যাড শেষ হওয়ার পর টার্মাক্স লোকাল সার্ভার (127.0.0.1:5000) থেকে ডাইরেক্ট লিংক ফেচ করার লজিক
        triggerTermuxDownload()
    }

    private fun triggerTermuxDownload() {
        if (videoUrl.isNullOrEmpty()) return

        // টার্মাক্স লোকাল সার্ভার এন্ডপয়েন্ট (যেমন: http://127.0.0.1:5000/download?url=...)
        val termuxServerUrl = "http://127.0.0.1:5000/download?url=" + Uri.encode(videoUrl)

        val request = DownloadManager.Request(Uri.parse(termuxServerUrl))
            .setTitle("Video Downloading")
            .setDescription("Termux Server থেকে ডাউনলোড হচ্ছে...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Downloaded_Video.mp4")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(this, "ডাউনলোড ম্যানেজার এ ফাইল যোগ করা হয়েছে!", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
