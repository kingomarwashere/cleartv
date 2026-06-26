package com.maverick.cleartv.settings

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.maverick.cleartv.ClearTVApp
import com.maverick.cleartv.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val app = application as ClearTVApp

        val adBlockSwitch = findViewById<Switch>(R.id.switch_adblock)
        val ruleCountView = findViewById<TextView>(R.id.rule_count)
        val lastUpdatedView = findViewById<TextView>(R.id.last_updated)
        val updateBtn = findViewById<Button>(R.id.btn_update_lists)
        val clearDataBtn = findViewById<Button>(R.id.btn_clear_data)
        val statusView = findViewById<TextView>(R.id.update_status)

        val prefs = getSharedPreferences("cleartv", MODE_PRIVATE)
        adBlockSwitch.isChecked = prefs.getBoolean("adblock_enabled", true)
        adBlockSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("adblock_enabled", checked).apply()
        }

        fun refreshStats() {
            ruleCountView.text = "Active rules: ${app.adBlockEngine.ruleCount}"
            val ts = app.filterListManager.getLastUpdated()
            lastUpdatedView.text = if (ts == 0L) "Never updated"
            else "Last updated: ${SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(ts))}"
        }
        refreshStats()

        updateBtn.setOnClickListener {
            updateBtn.isEnabled = false
            statusView.visibility = View.VISIBLE
            statusView.text = "Starting update..."
            scope.launch {
                withContext(Dispatchers.IO) {
                    app.filterListManager.updateAll(app.adBlockEngine) { status ->
                        runOnUiThread { statusView.text = status }
                    }
                }
                updateBtn.isEnabled = true
                refreshStats()
                Toast.makeText(this@SettingsActivity, "Filter lists updated", Toast.LENGTH_SHORT).show()
            }
        }

        clearDataBtn.setOnClickListener {
            android.webkit.WebStorage.getInstance().deleteAllData()
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show()
        }
    }
}
