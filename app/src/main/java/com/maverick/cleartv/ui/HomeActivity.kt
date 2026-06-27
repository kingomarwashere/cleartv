package com.maverick.cleartv.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maverick.cleartv.ClearTVApp
import com.maverick.cleartv.R
import com.maverick.cleartv.browser.BrowserActivity
import com.maverick.cleartv.settings.SettingsActivity

class HomeActivity : Activity() {

    data class QuickTile(val label: String, val url: String, val emoji: String)

    private val quickTiles = listOf(
        QuickTile("YouTube", "https://m.youtube.com", "▶"),
        QuickTile("Google", "https://www.google.com", "G"),
        QuickTile("Reddit", "https://old.reddit.com", "R"),
        QuickTile("Twitch", "https://m.twitch.tv", "T"),
        QuickTile("Wikipedia", "https://en.wikipedia.org", "W"),
        QuickTile("GitHub", "https://github.com", "⌥"),
        QuickTile("Hacker News", "https://news.ycombinator.com", "Y"),
        QuickTile("Archive.org", "https://archive.org/details/movies", "A")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val searchBar = findViewById<TextView>(R.id.search_bar)
        val tilesGrid = findViewById<RecyclerView>(R.id.quick_tiles)
        val shieldStatus = findViewById<TextView>(R.id.shield_status)

        // Shield status
        val app = application as ClearTVApp
        shieldStatus.text = "🛡 AdBlock Active — ${app.adBlockEngine.ruleCount.takeIf { it > 0 }?.let { "$it rules" } ?: "Loading..."}"

        // Search bar focus → show address bar
        searchBar.setOnClickListener { launchAddressBar() }
        searchBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) launchAddressBar()
        }

        // Quick tiles
        tilesGrid.layoutManager = GridLayoutManager(this, 4)
        tilesGrid.adapter = TileAdapter(quickTiles) { tile ->
            BrowserActivity.start(this, tile.url)
        }

        // Settings
        findViewById<View>(R.id.settings_btn).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun launchAddressBar() {
        // Go straight to browser in edit mode — no dialog needed
        BrowserActivity.startInEditMode(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_MENU) {
            launchAddressBar()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    inner class TileAdapter(
        private val tiles: List<QuickTile>,
        private val onClick: (QuickTile) -> Unit
    ) : RecyclerView.Adapter<TileAdapter.TileVH>() {

        inner class TileVH(val view: View) : RecyclerView.ViewHolder(view) {
            val emoji: TextView = view.findViewById(R.id.tile_emoji)
            val label: TextView = view.findViewById(R.id.tile_label)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
            TileVH(layoutInflater.inflate(R.layout.item_tile, parent, false))

        override fun onBindViewHolder(holder: TileVH, position: Int) {
            val tile = tiles[position]
            holder.emoji.text = tile.emoji
            holder.label.text = tile.label
            holder.view.setOnClickListener { onClick(tile) }
            holder.view.setOnFocusChangeListener { v, hasFocus ->
                v.scaleX = if (hasFocus) 1.08f else 1f
                v.scaleY = if (hasFocus) 1.08f else 1f
            }
        }

        override fun getItemCount() = tiles.size
    }
}
