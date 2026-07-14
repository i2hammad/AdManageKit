package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i2hammad.admanagekit.admob.NativeAdTemplate
import com.i2hammad.admanagekit.admob.NativeBannerMedium
import com.i2hammad.admanagekit.compose.BannerAdCompose
import com.i2hammad.admanagekit.compose.NativeBannerMediumCompose
import com.i2hammad.admanagekit.compose.NativeTemplateCompose
import com.i2hammad.admanagekit.compose.rememberInterstitialAd
import com.i2hammad.admanagekit.core.ad.NativeAdSize

// Google's public test ad unit ids - reused across the sample app's other test activities.
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

/**
 * Sample screen demonstrating the `admanagekit-compose` module: banner, native template
 * (both a built-in [NativeAdTemplate] preset and an app-supplied custom layout via
 * [NativeTemplateCompose]'s `customLayoutResId`), and an interstitial ad.
 */
class ComposeAdsTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ComposeAdsScreen()
                }
            }
        }
    }
}

@Composable
private fun ComposeAdsScreen() {
    var useCustomTemplate by remember { mutableStateOf(false) }

    val showInterstitial = rememberInterstitialAd(
        adUnitId = INTERSTITIAL_AD_UNIT_ID,
        onAdShown = { Log.d("ComposeAdsTest", "Interstitial shown") },
        onAdDismissed = { Log.d("ComposeAdsTest", "Interstitial dismissed") },
        onAdFailedToLoad = { error -> Log.e("ComposeAdsTest", "Interstitial failed to load: $error") }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Compose Ads Sample", style = MaterialTheme.typography.headlineSmall)

        Text("Banner", style = MaterialTheme.typography.titleMedium)
        BannerAdCompose(
            adUnitId = BANNER_AD_UNIT_ID,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Native Template", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use custom layout", modifier = Modifier.weight(1f))
            Switch(checked = useCustomTemplate, onCheckedChange = { useCustomTemplate = it })
        }

        if (useCustomTemplate) {
            // Demonstrates customLayoutResId: a fully custom, app-supplied layout instead
            // of one of the built-in NativeAdTemplate presets. The layout/shimmer live in
            // this sample app's own res/layout, not the library.
            NativeTemplateCompose(
                adUnitId = NATIVE_AD_UNIT_ID,
                customLayoutResId = R.layout.layout_custom_native_ad_demo,
                customShimmerResId = R.layout.layout_custom_native_ad_demo_shimmer,
                customSizeHint = NativeAdSize.MEDIUM,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            NativeTemplateCompose(
                adUnitId = NATIVE_AD_UNIT_ID,
                template = NativeAdTemplate.CARD_MODERN,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text("Native Banner Medium", style = MaterialTheme.typography.titleMedium)

        NativeBannerMediumCompose(
            adUnitId = NATIVE_AD_UNIT_ID,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { showInterstitial() }) {
            Text("Show Interstitial Ad")
        }
    }
}
