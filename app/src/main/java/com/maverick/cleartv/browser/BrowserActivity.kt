package com.maverick.cleartv.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
    private lateinit var urlInputDisplay: TextView
    private lateinit var blockedCountView: TextView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var browserContainer: FrameLayout
    private lateinit var toolbarBrowse: LinearLayout
    private lateinit var toolbarEdit: LinearLayout

    private var blockedCount = 0
    private var isEditMode = false

    private val inputBuffer = StringBuilder()
    private val cursorHandler = Handler(Looper.getMainLooper())
    private var cursorOn = true
    private val cursorBlink = object : Runnable {
        override fun run() {
            if (isEditMode) {
                cursorOn = !cursorOn
                updateInputDisplay()
                cursorHandler.postDelayed(this, 500)
            }
        }
    }

    // Steals InputConnection from WebView so hardware keyboard goes to us
    private lateinit var hiddenInput: EditText
    private var textWatcher: TextWatcher? = null

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_EDIT_MODE = "edit_mode"
        fun start(activity: Activity, url: String) {
            activity.startActivity(Intent(activity, BrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            })
        }
        fun startInEditMode(activity: Activity) {
            activity.startActivity(Intent(activity, BrowserActivity::class.java).apply {
                putExtra(EXTRA_EDIT_MODE, true)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Never let the TV keyboard pop up — all input is handled manually
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )
        setContentView(R.layout.activity_browser)

        fullscreenContainer = findViewById(R.id.fullscreen_container)
        browserContainer = findViewById(R.id.browser_container)
        progressBar = findViewById(R.id.progress_bar)
        urlBar = findViewById(R.id.url_bar)
        urlInputDisplay = findViewById(R.id.url_input_display)
        blockedCountView = findViewById(R.id.blocked_count)
        toolbarBrowse = findViewById(R.id.toolbar_browse)
        toolbarEdit = findViewById(R.id.toolbar_edit)

        hiddenInput = findViewById(R.id.hidden_input)

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

        if (intent.getBooleanExtra(EXTRA_EDIT_MODE, false)) {
            webView.post { enterEditMode() }
        }
    }

    private fun setupToolbar() {
        findViewById<TextView>(R.id.btn_home).setOnClickListener { goHome() }
        findViewById<TextView>(R.id.btn_back).setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        findViewById<TextView>(R.id.btn_forward).setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        urlBar.setOnClickListener { enterEditMode() }
        findViewById<TextView>(R.id.btn_cancel).setOnClickListener { exitEditMode() }
        findViewById<TextView>(R.id.btn_go).setOnClickListener { commitUrl() }
    }

    private fun enterEditMode() {
        isEditMode = true
        inputBuffer.clear()
        toolbarBrowse.visibility = View.GONE
        toolbarEdit.visibility = View.VISIBLE
        updateInputDisplay()
        cursorOn = true
        cursorHandler.postDelayed(cursorBlink, 500)

        // Steal InputConnection from WebView — hardware keyboard now types here
        hiddenInput.setText("")
        hiddenInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        // SHOW_FORCED binds the InputConnection even though keyboard stays hidden
        imm.showSoftInput(hiddenInput, InputMethodManager.SHOW_FORCED)
        // Now immediately hide the visual keyboard (TV/phone) but keep the binding
        hiddenInput.post { imm.hideSoftInputFromWindow(hiddenInput.windowToken, 0) }

        // TextWatcher receives all typed characters via InputConnection
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val newText = s.toString()
                if (newText != inputBuffer.toString()) {
                    inputBuffer.clear()
                    inputBuffer.append(newText)
                    updateInputDisplay()
                }
            }
        }
        hiddenInput.addTextChangedListener(textWatcher)
    }

    private fun exitEditMode() {
        isEditMode = false
        cursorHandler.removeCallbacks(cursorBlink)
        textWatcher?.let { hiddenInput.removeTextChangedListener(it) }
        textWatcher = null
        hiddenInput.setText("")
        toolbarEdit.visibility = View.GONE
        toolbarBrowse.visibility = View.VISIBLE
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(hiddenInput.windowToken, 0)
        webView.requestFocus()
    }

    private fun commitUrl() {
        val input = inputBuffer.toString().trim()
        if (input.isNotEmpty()) webView.loadSmart(input)
        exitEditMode()
    }

    private fun updateInputDisplay() {
        val cursor = if (isEditMode && cursorOn) "|" else ""
        urlInputDisplay.text = "${inputBuffer}$cursor"
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
        // In edit mode: only intercept control keys.
        // Printable characters are handled by TextWatcher via InputConnection.
        if (event.action == KeyEvent.ACTION_DOWN && isEditMode) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                    commitUrl(); return true
                }
                KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
                    exitEditMode(); return true
                }
                // DEL handled by EditText itself — don't intercept
            }
        }

        if (event.action == KeyEvent.ACTION_DOWN && !isEditMode) {
            val toolbarFocused = toolbarBrowse.findFocus() != null
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (!toolbarFocused) {
                        findViewById<TextView>(R.id.btn_home).requestFocus(); return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (toolbarFocused) { webView.requestFocus(); return true }
                }
                KeyEvent.KEYCODE_L -> {
                    if (event.isMetaPressed || event.isCtrlPressed) { enterEditMode(); return true }
                }
                KeyEvent.KEYCODE_DEL -> {
                    if (!toolbarFocused && webView.canGoBack()) { webView.goBack(); return true }
                }
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    val cc = webView.webChromeClient as? TVWebChromeClient
                    if (cc?.isInFullscreen() == true) { cc.exitFullscreen(); return true }
                    if (webView.canGoBack()) { webView.goBack(); return true }
                }
                KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SEARCH -> {
                    enterEditMode(); return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onPause() { super.onPause(); webView.onPause(); cursorHandler.removeCallbacks(cursorBlink) }
    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onDestroy() { webView.destroy(); super.onDestroy() }
}
