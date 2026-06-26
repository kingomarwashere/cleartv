package com.maverick.cleartv.browser

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.InputDevice
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView

class TVWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {

    init {
        configure()
        // Prevent WebView from ever requesting the soft keyboard
        setOnFocusChangeListener { _, _ ->
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)
                ?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        // Returning null prevents the TV keyboard from ever appearing.
        // Hardware keyboard events still reach the WebView via dispatchKeyEvent.
        return null
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
        // Back key handled by activity
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
