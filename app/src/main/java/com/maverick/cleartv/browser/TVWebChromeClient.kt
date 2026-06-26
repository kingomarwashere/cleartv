package com.maverick.cleartv.browser

import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView

class TVWebChromeClient(
    private val rootView: ViewGroup,
    private val onProgressChanged: (Int) -> Unit,
    private val onTitleReceived: (String) -> Unit,
    private val onFullscreenEnter: (View) -> Unit,
    private val onFullscreenExit: () -> Unit
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        onTitleReceived(title)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        customView = view
        customViewCallback = callback
        onFullscreenEnter(view)
    }

    override fun onHideCustomView() {
        customView = null
        customViewCallback = null
        onFullscreenExit()
    }

    fun isInFullscreen() = customView != null

    fun exitFullscreen() {
        customViewCallback?.onCustomViewHidden()
    }
}
