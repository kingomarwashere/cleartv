package com.maverick.cleartv.browser

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.maverick.cleartv.adblock.AdBlockEngine
import java.io.ByteArrayInputStream

class TVWebViewClient(
    private val adBlockEngine: AdBlockEngine,
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String, String?) -> Unit,
    private val onProgressChanged: (Int) -> Unit
) : WebViewClient() {

    private val cosmeticInjectionJs = """
(function() {
    var selectors = %SELECTORS%;
    if (selectors.length === 0) return;
    var style = document.createElement('style');
    style.textContent = selectors.join(', ') + ' { display: none !important; visibility: hidden !important; }';
    (document.head || document.documentElement).appendChild(style);
})();
    """.trimIndent()

    // YouTube-specific ad skip script
    private val youtubeAdScript = """
(function() {
    var observer = new MutationObserver(function() {
        var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button');
        if (skipBtn) skipBtn.click();
        var adOverlay = document.querySelector('.ad-showing');
        if (adOverlay) {
            var video = document.querySelector('video');
            if (video) { video.currentTime = video.duration; }
        }
    });
    observer.observe(document.body || document.documentElement, { childList: true, subtree: true });

    // Block ad requests via XHR
    var origOpen = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function(method, url) {
        if (url && (url.includes('googlevideo.com/videoplayback') && url.includes('adformat'))) {
            return;
        }
        return origOpen.apply(this, arguments);
    };
})();
    """.trimIndent()

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        val host = request.url.host ?: ""

        if (adBlockEngine.shouldBlock(url, host)) {
            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
        }
        return null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        val host = try { java.net.URI(url).host ?: "" } catch (e: Exception) { "" }

        // Inject cosmetic filters (element hiding CSS)
        val selectors = adBlockEngine.getCosmeticSelectors(host)
        if (selectors.isNotEmpty()) {
            val selectorsJson = selectors.take(500).joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
            val js = cosmeticInjectionJs.replace("%SELECTORS%", "[$selectorsJson]")
            view.evaluateJavascript(js, null)
        }

        // Inject YouTube ad skipper on YouTube pages
        if (host.contains("youtube.com") || host.contains("youtu.be")) {
            view.evaluateJavascript(youtubeAdScript, null)
        }

        onPageFinished(url, view.title)
    }
}
