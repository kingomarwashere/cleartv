package com.maverick.cleartv.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.maverick.cleartv.ClearTVApp
import com.maverick.cleartv.R
import com.maverick.cleartv.ui.HomeActivity

class BrowserActivity : Activity() {

    private lateinit var webView: TVWebView
    private lateinit var progressBar: ProgressBar
    private lateinit var urlBar: TextView
    private lateinit var urlInput: EditText
    private lateinit var blockedCountView: TextView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var browserContainer: FrameLayout
    private lateinit var toolbarBrowse: LinearLayout
    private lateinit var toolbarEdit: LinearLayout

    private var blockedCount = 0
    private var isEditMode = false

    companion object {
        const val EXTRA_URL = "url"
        fun start(activity: Activity, url: String) {
            activity.startActivity(Intent(activity, BrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Never let the system resize the layout for the keyboard
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        setContentView(R.layout.activity_browser)

        fullscreenContainer = findViewById(R.id.fullscreen_container)
        browserContainer = findViewById(R.id.browser_container)
        progressBar = findViewById(R.id.progress_bar)
        urlBar = findViewById(R.id.url_bar)
        urlInput = findViewById(R.id.url_input)
        blockedCountView = findViewById(R.id.blocked_count)
        toolbarBrowse = findViewById(R.id.toolbar_browse)
        toolbarEdit = findViewById(R.id.toolbar_edit)

        webView = TVWebView(this)
        browserContainer.addView(webView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        setupWebView()
        setupToolbar()

        val url = intent.getStringExtra(EXTRA_URL) ?: "https://www.google.com"
        webView.loadSmart(url)
        webView.requestFocus()
    }

    private fun setupToolbar() {
        findViewById<TextView>(R.id.btn_home).setOnClickListener { goHome() }
        findViewById<TextView>(R.id.btn_back).setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        findViewById<TextView>(R.id.btn_forward).setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        urlBar.setOnClickListener { enterEditMode() }
        urlBar.setOnFocusChangeListener { _, focused -> if (focused) enterEditMode() }

        findViewById<TextView>(R.id.btn_cancel).setOnClickListener { exitEditMode() }
        findViewById<TextView>(R.id.btn_go).setOnClickListener { commitUrl() }

        urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                commitUrl(); true
            } else false
        }

        urlInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> { commitUrl(); true }
                    KeyEvent.KEYCODE_ESCAPE -> { exitEditMode(); true }
                    else -> false
                }
            } else false
        }
    }

    private fun enterEditMode() {
        isEditMode = true
        toolbarBrowse.visibility = View.GONE
        toolbarEdit.visibility = View.VISIBLE
        urlInput.setText(webView.url ?: "")
        urlInput.selectAll()
        urlInput.requestFocus()
        // Hide IME — Mac keyboard types directly, no TV keyboard needed
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
    }

    private fun exitEditMode() {
        isEditMode = false
        toolbarEdit.visibility = View.GONE
        toolbarBrowse.visibility = View.VISIBLE
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
        webView.requestFocus()
    }

    private fun commitUrl() {
        val input = urlInput.text.toString().trim()
        if (input.isNotEmpty()) {
            webView.loadSmart(input)
        }
        exitEditMode()
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
                runOnUiThread { if (!isEditMode) urlBar.text = title }
            },
            onFullscreenEnter = { view ->
                runOnUiThread {
                    toolbarBrowse.visibility = View.GONE
                    toolbarEdit.visibility = View.GONE
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
                    fullscreenContainer.removeAllViews()
                    fullscreenContainer.visibility = View.GONE
                    browserContainer.visibility = View.VISIBLE
                    toolbarBrowse.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun updateBlockedCount() {
        blockedCountView.text = if (blockedCount > 0) "🛡 $blockedCount" else ""
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val toolbarFocused = toolbarBrowse.findFocus() != null || toolbarEdit.findFocus() != null
            val chromeClient = webView.webChromeClient as? TVWebChromeClient

            when (event.keyCode) {
                // D-pad down → focus toolbar (unless already there)
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (!isEditMode && !toolbarFocused) {
                        findViewById<TextView>(R.id.btn_home).requestFocus()
                        return true
                    }
                }
                // D-pad up from toolbar → back to page
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (toolbarFocused) {
                        webView.requestFocus()
                        return true
                    }
                }
                // Cmd+L or Ctrl+L → focus URL bar (like every desktop browser)
                KeyEvent.KEYCODE_L -> {
                    if (event.isMetaPressed || event.isCtrlPressed) {
                        enterEditMode(); return true
                    }
                }
                // Backspace = go back when not editing
                KeyEvent.KEYCODE_DEL -> {
                    if (!isEditMode && !toolbarFocused) {
                        if (webView.canGoBack()) { webView.goBack(); return true }
                    }
                }
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    if (isEditMode) { exitEditMode(); return true }
                    if (chromeClient?.isInFullscreen() == true) { chromeClient.exitFullscreen(); return true }
                    if (webView.canGoBack()) { webView.goBack(); return true }
                    return false
                }
                KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SEARCH -> {
                    if (!isEditMode) enterEditMode(); return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onPause() { super.onPause(); webView.onPause() }
    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onDestroy() { webView.destroy(); super.onDestroy() }
}
