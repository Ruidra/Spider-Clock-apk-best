package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OriginalSpiderClockView(
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .testTag("original_spider_clock_webview"),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    loadWithOverviewMode = false
                    useWideViewPort = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }

                webViewClient = object : WebViewClient() {}
                loadUrl("file:///android_asset/spider_clock/index.html")
            }
        },
        update = { webView ->
            // Running smoothly
        }
    )
}
