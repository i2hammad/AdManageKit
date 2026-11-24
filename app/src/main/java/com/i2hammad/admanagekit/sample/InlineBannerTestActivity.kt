package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.compose.InlineBannerAdCompose
import com.i2hammad.admanagekit.config.BannerAdSize

// Google's public adaptive banner test unit; it also serves inline adaptive banners.
private const val INLINE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
private const val TAG = "InlineBannerTest"

/** Positions in the card list where the two inline banners sit. */
private const val COMPOSE_BANNER_POSITION = 3
private const val VIEW_BANNER_POSITION = 10
private const val CARD_COUNT = 20

/**
 * Sample screen for [BannerAdSize.INLINE_ADAPTIVE]: inline adaptive banners inside a
 * scrolling list of cards. One uses the Compose API ([InlineBannerAdCompose]), the other the
 * View API ([BannerAdView] with `INLINE_ADAPTIVE`). Both take no space until the ad loads,
 * then open to the ad's height (at most the max height).
 */
class InlineBannerTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InlineBannerListScreen()
                }
            }
        }
    }
}

@Composable
private fun InlineBannerListScreen() {
    var composeStatus by remember { mutableStateOf("loading…") }
    var viewStatus by remember { mutableStateOf("loading…") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Inline Adaptive Banner", style = MaterialTheme.typography.headlineSmall)
                Text("Compose banner: $composeStatus", style = MaterialTheme.typography.bodySmall)
                Text("View banner: $viewStatus", style = MaterialTheme.typography.bodySmall)
            }
        }
        items(CARD_COUNT) { index ->
            when (index) {
                COMPOSE_BANNER_POSITION -> AdCard(label = "Sponsored · Compose (max 250dp)") {
                    InlineBannerAdCompose(
                        adUnitId = INLINE_BANNER_AD_UNIT_ID,
                        maxHeight = 250.dp,
                        showShimmer = true,
                        onAdLoaded = { heightDp ->
                            composeStatus = "loaded, ${heightDp}dp"
                            Log.d(TAG, "Compose inline banner loaded: ${heightDp}dp")
                        },
                        onAdFailedToLoad = { error ->
                            composeStatus = "failed: ${error?.message}"
                            Log.e(TAG, "Compose inline banner failed: ${error?.message}")
                        }
                    )
                }

                VIEW_BANNER_POSITION -> AdCard(label = "Sponsored · View (max 150dp)") {
                    InlineBannerAsView(onStatus = { viewStatus = it })
                }

                else -> ContentCard(index)
            }
        }
    }
}

/** The View API: a [BannerAdView] loading `INLINE_ADAPTIVE`, wrapping the ad's height. */
@Composable
private fun InlineBannerAsView(onStatus: (String) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            BannerAdView(context).apply {
                setInlineMaxHeight(150)
                onInlineAdHeightChanged = { heightDp ->
                    onStatus("loaded, ${heightDp}dp")
                    Log.d(TAG, "View inline banner height: ${heightDp}dp")
                }
                loadBanner(
                    context as ComponentActivity,
                    INLINE_BANNER_AD_UNIT_ID,
                    BannerAdSize.INLINE_ADAPTIVE,
                    object : AdLoadCallback() {
                        override fun onFailedToLoad(error: com.i2hammad.admanagekit.admob.AdKitError?) {
                            onStatus("failed: ${error?.message}")
                        }
                    }
                )
            }
        },
        // LazyColumn disposes off-screen items: release the AdView with them
        onRelease = { it.destroyAd() }
    )
}

@Composable
private fun AdCard(label: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            content()
        }
    }
}

@Composable
private fun ContentCard(index: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Article ${index + 1}", style = MaterialTheme.typography.titleMedium)
            Text(
                "Sample content card. Scroll the list to see inline adaptive banners " +
                    "sized to the card width, opening to the height AdMob picks.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
