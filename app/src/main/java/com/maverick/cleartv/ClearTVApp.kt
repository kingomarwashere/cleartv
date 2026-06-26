package com.maverick.cleartv

import android.app.Application
import android.util.Log
import com.maverick.cleartv.adblock.AdBlockEngine
import com.maverick.cleartv.adblock.FilterListManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClearTVApp : Application() {

    val adBlockEngine = AdBlockEngine()
    lateinit var filterListManager: FilterListManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        filterListManager = FilterListManager(this)

        appScope.launch(Dispatchers.IO) {
            filterListManager.loadAll(adBlockEngine)
            Log.d("ClearTVApp", "AdBlock engine ready: ${adBlockEngine.ruleCount} rules")

            // Auto-update filter lists if older than 24 hours
            val lastUpdated = filterListManager.getLastUpdated()
            val oneDayMs = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastUpdated > oneDayMs) {
                filterListManager.updateAll(adBlockEngine) { status ->
                    Log.d("ClearTVApp", "Filter update: $status")
                }
            }
        }
    }
}
