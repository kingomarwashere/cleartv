package com.maverick.cleartv.adblock

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class FilterList(
    val name: String,
    val url: String,
    val cacheFile: String,
    val enabled: Boolean = true
)

class FilterListManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.filesDir, "filter_lists").also { it.mkdirs() }

    val filterLists = listOf(
        FilterList(
            name = "EasyList",
            url = "https://easylist.to/easylist/easylist.txt",
            cacheFile = "easylist.txt"
        ),
        FilterList(
            name = "EasyPrivacy",
            url = "https://easylist.to/easylist/easyprivacy.txt",
            cacheFile = "easyprivacy.txt"
        ),
        FilterList(
            name = "AdGuard Base",
            url = "https://filters.adtidy.org/extension/chromium/filters/2.txt",
            cacheFile = "adguard_base.txt"
        ),
        FilterList(
            name = "AdGuard Tracking Protection",
            url = "https://filters.adtidy.org/extension/chromium/filters/3.txt",
            cacheFile = "adguard_tracking.txt"
        ),
        FilterList(
            name = "uBlock Origin Filters",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            cacheFile = "ublock_filters.txt"
        ),
        FilterList(
            name = "uBlock Origin Annoyances",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt",
            cacheFile = "ublock_annoyances.txt"
        ),
        FilterList(
            name = "Peter Lowe's Ad & Tracker List",
            url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0",
            cacheFile = "peter_lowe.txt"
        )
    )

    suspend fun loadAll(engine: AdBlockEngine) = withContext(Dispatchers.IO) {
        // Always load bundled list first — works offline immediately
        val bundled = loadBundledRules()
        engine.loadRules(bundled)

        // Then layer in cached filter lists
        for (list in filterLists) {
            val cached = File(cacheDir, list.cacheFile)
            if (cached.exists() && cached.length() > 0) {
                try {
                    engine.loadRules(cached.readLines())
                    Log.d("FilterListManager", "Loaded cached: ${list.name}")
                } catch (e: Exception) {
                    Log.e("FilterListManager", "Failed to load cached ${list.name}: ${e.message}")
                }
            }
        }
        Log.d("FilterListManager", "Total rules loaded: ${engine.ruleCount}")
    }

    suspend fun updateAll(engine: AdBlockEngine, onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        for (list in filterLists) {
            if (!list.enabled) continue
            try {
                onProgress("Updating ${list.name}...")
                val content = fetchList(list.url)
                if (content != null) {
                    val cacheFile = File(cacheDir, list.cacheFile)
                    cacheFile.writeText(content)
                    Log.d("FilterListManager", "Updated ${list.name} (${content.lines().size} lines)")
                }
            } catch (e: Exception) {
                Log.e("FilterListManager", "Failed to update ${list.name}: ${e.message}")
            }
        }
        // Reload engine with fresh lists
        engine.clear()
        loadAll(engine)
        onProgress("Done — ${engine.ruleCount} rules active")
    }

    fun getLastUpdated(): Long {
        return filterLists.mapNotNull { list ->
            val f = File(cacheDir, list.cacheFile)
            if (f.exists()) f.lastModified() else null
        }.minOrNull() ?: 0L
    }

    private fun fetchList(url: String): String? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun loadBundledRules(): List<String> {
        // Bundled top-tier ad/tracker domains — works offline on first launch
        return BUNDLED_DOMAINS.lines().filter { it.isNotBlank() }
    }

    companion object {
        // Top ad/tracker domains bundled for immediate offline protection
        private val BUNDLED_DOMAINS = """
||doubleclick.net^
||googleadservices.com^
||googlesyndication.com^
||google-analytics.com^
||googletagmanager.com^
||googletagservices.com^
||amazon-adsystem.com^
||adnxs.com^
||scorecardresearch.com^
||moatads.com^
||krxd.net^
||quantserve.com^
||taboola.com^
||outbrain.com^
||revcontent.com^
||spotxchange.com^
||openx.net^
||openx.com^
||pubmatic.com^
||rubiconproject.com^
||rubiconproject.net^
||indexexchange.com^
||smartadserver.com^
||yieldmo.com^
||sharethrough.com^
||casalemedia.com^
||lijit.com^
||advertising.com^
||adtech.de^
||33across.com^
||bidswitch.net^
||doubleverify.com^
||adroll.com^
||criteo.com^
||criteo.net^
||rlcdn.com^
||demdex.net^
||bluekai.com^
||turn.com^
||mathtag.com^
||tidaltv.com^
||rfihub.com^
||adsrvr.org^
||adform.net^
||adform.com^
||adgrx.com^
||nexac.com^
||flashtalking.com^
||sizmek.com^
||serving-sys.com^
||tribalfusion.com^
||undertone.com^
||brightroll.com^
||yieldlab.net^
||yieldlab.de^
||smaato.net^
||inneractive.com^
||spotxchange.com^
||contextweb.com^
||appnexus.com^
||adtech.de^
||adsafeprotected.com^
||integral-marketing.com^
||mopub.com^
||flurry.com^
||inmobi.com^
||inmobi.com^
||verizonmedia.com^
||oath.com^
||aolcdn.com^
||brealtime.com^
||tremorvideo.com^
||unrulymedia.com^
||springserve.com^
||videohub.tv^
||spotx.tv^
||freewheel.tv^
||fwmrm.net^
||lijit.com^
||sovrn.com^
||zergnet.com^
||mgid.com^
||popads.net^
||popadstop.com^
||propellerads.com^
||exoclick.com^
||trafficjunky.net^
||trafficfactory.biz^
||juicyads.com^
||etahub.com^
||go.socdm.com^
||pagefair.com^
||pagefair.net^
||syndication.twitter.com^
||ads.twitter.com^
||ads-twitter.com^
||ads.linkedin.com^
||ads.facebook.com^
||an.facebook.com^
||pixel.facebook.com^
||pixel.wp.com^
||analytics.twitter.com^
||bat.bing.com^
||ads.yahoo.com^
||media.net^
||media.net^
||adservice.google.com^
||adservice.google.co.uk^
||ads.reddit.com^
||redditads.com^
||analytics.google.com^
||stats.g.doubleclick.net^
||cm.g.doubleclick.net^
||ad.doubleclick.net^
||googleads.g.doubleclick.net^
||pubads.g.doubleclick.net^
||securepubads.g.doubleclick.net^
||pagead2.googlesyndication.com^
||tpc.googlesyndication.com^
||imasdk.googleapis.com^
||s0.2mdn.net^
||pagead.googlesyndication.com^
||cdn.auditude.com^
||static.auditude.com^
||amazon.com^
@@||s3.amazonaws.com^
@@||cloudfront.net^
@@||images-amazon.com^
        """.trimIndent()
    }
}
