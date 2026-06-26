package com.maverick.cleartv.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.maverick.cleartv.ClearTVApp
import com.maverick.cleartv.R
import com.maverick.cleartv.ui.AddressBarDialog

class BrowserActivity : Activity() {

    private lateinit var webView: TVWebView
    private lateinit var progressBar: ProgressBar
    private lateinit var urlBar: TextView
    private lateinit var blockedCountView: TextView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var browserContainer: FrameLayout

    private var blockedCount = 0

    companion object {
        const val EXTRA_URL = "url"
        fun start(activity: Activity, url: String) {
            val intent = Intent(activity, BrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_browser)

        fullscreenContainer = findViewById(R.id.fullscreen_container)
        browserContainer = findViewById(R.id.browser_container)
        progressBar = findViewById(R.id.progress_bar)
        urlBar = findViewById(R.id.url_bar)
        blockedCountView = findViewById(R.id.blocked_count)
        webView = TVWebView(this)
        browserContainer.addView(webView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        setupWebView()

        val url = intent.getStringExtra(EXTRA_URL) ?: "https://www.google.com"
        webView.loadSmart(url)
    }

    private fun setupWebView() {
        val app = application as ClearTVApp

        webView.webViewClient = TVWebViewClient(
            adBlockEngine = app.adBlockEngine,
            onPageStarted = { url ->
                runOnUiThread {
                    progressBar.visibility = View.VISIBLE
                    blockedCount = 0
                    updateBlockedCount()
                    urlBar.text = url
                }
            },
            onPageFinished = { url, title ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    urlBar.text = title?.ifEmpty { url } ?: url
                }
            },
            onProgressChanged = { progress ->
                runOnUiThread { progressBar.progress = progress }
            },
            onRequestBlocked = {
                blockedCount++
                runOnUiThread { updateBlockedCount() }
            }
        )

        webView.webChromeClient = TVWebChromeClient(
            rootView = fullscreenContainer,
            onProgressChanged = { progress ->
                runOnUiThread { progressBar.progress = progress }
            },
            onTitleReceived = { title ->
                runOnUiThread { urlBar.text = title }
            },
            onFullscreenEnter = { view ->
                runOnUiThread {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    browserContainer.visibility = View.GONE
                    fullscreenContainer.addView(view, ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                    fullscreenContainer.visibility = View.VISIBLE
                }
            },
            onFullscreenExit = {
                runOnUiThread {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    fullscreenContainer.removeAllViews()
                    fullscreenContainer.visibility = View.GONE
                    browserContainer.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun updateBlockedCount() {
        blockedCountView.text = if (blockedCount > 0) "🛡 $blockedCount blocked" else ""
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val chromeClient = webView.webChromeClient as? TVWebChromeClient

        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (chromeClient?.isInFullscreen() == true) {
                    chromeClient.exitFullscreen()
                    return true
                }
                if (webView.canGoBack()) {
                    webView.goBack()
                    return true
                }
                return false
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SEARCH -> {
                showAddressBar()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (event.isLongPress) {
                    showAddressBar()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showAddressBar() {
        AddressBarDialog.show(this, webView.url ?: "") { input ->
            webView.loadSmart(input)
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
