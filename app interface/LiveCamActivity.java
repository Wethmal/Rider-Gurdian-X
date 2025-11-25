package com.example.ridergurdianx;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LiveCamActivity extends AppCompatActivity {

    // 🔁 CHANGE THIS TO YOUR ESP32-CAM URL
    // Example default stream from ESP32-CAM webserver:
    // "http://192.168.1.50" or "http://192.168.1.50:81/stream"
    private static final String CAMERA_URL = "http://192.168.8.108/";

    private WebView webViewCam;
    private ProgressBar progressLoading;
    private TextView tvCamStatus, tvUrl;
    private ImageView btnBack;
    private TextView btnReload;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_cam);

        webViewCam = findViewById(R.id.webViewCam);
        progressLoading = findViewById(R.id.progressLoading);
        tvCamStatus = findViewById(R.id.tvCamStatus);
        tvUrl = findViewById(R.id.tvUrl);
        btnBack = findViewById(R.id.btnBack);
        btnReload = findViewById(R.id.btnReload);

        tvUrl.setText(CAMERA_URL);

        WebSettings settings = webViewCam.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);

        webViewCam.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressLoading.setVisibility(View.GONE);
                tvCamStatus.setText("Live");
            }
        });

        webViewCam.setWebChromeClient(new WebChromeClient());

        loadCameraStream();

        btnBack.setOnClickListener(v -> onBackPressed());

        btnReload.setOnClickListener(v -> {
            tvCamStatus.setText("Connecting...");
            progressLoading.setVisibility(View.VISIBLE);
            loadCameraStream();
        });
    }

    private void loadCameraStream() {
        progressLoading.setVisibility(View.VISIBLE);
        webViewCam.loadUrl(CAMERA_URL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webViewCam.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webViewCam.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webViewCam != null) {
            webViewCam.destroy();
        }
        super.onDestroy();
    }
}
