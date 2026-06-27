package com.maverick.cleartv.browser

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView

class TVWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {

    init {
        configure()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 10; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 ClearTV/1.0"
        }
        isFocusable = true
        isFocusableInTouchMode = true
        scrollBarStyle = SCROLLBARS_OUTSIDE_OVERLAY
        isScrollbarFadingEnabled = true
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return false
        return super.onKeyDown(keyCode, event)
    }

    fun loadSmart(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$")) -> "https://$input"
            else -> "https://www.google.com/search?q=${android.net.Uri.encode(input)}&safe=off"
        }
        loadUrl(url)
    }
}
