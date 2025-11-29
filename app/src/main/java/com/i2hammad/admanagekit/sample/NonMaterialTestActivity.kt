package com.i2hammad.admanagekit.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.i2hammad.admanagekit.admob.AdManagerCallback

/**
 * Test activity using non-Material theme (android:Theme.Light.NoTitleBar)
 * Used to verify that AppOpenManager's welcome dialog works without Material theme.
 */
class NonMaterialTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create layout programmatically (no Material components)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val titleText = TextView(this).apply {
            text = "Non-Material Theme Test"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(titleText)

        val descText = TextView(this).apply {
            text = "This activity uses android:Theme.Light.NoTitleBar (non-Material theme).\n\n" +
                    "App Open ads should still work with the welcome dialog.\n\n" +
                    "Press the button below to trigger a force app open ad."
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(descText)

        val triggerButton = Button(this).apply {
            text = "Force Show App Open Ad"
            setOnClickListener {
                triggerAppOpenAd()
            }
        }
        layout.addView(triggerButton)

        val backButton = Button(this).apply {
            text = "Go Back (Trigger on Resume)"
            setOnClickListener {
                // Go to MainActivity and come back - should trigger app open on resume
                Toast.makeText(this@NonMaterialTestActivity, "Navigate away and come back to trigger app open", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        layout.addView(backButton)

        setContentView(layout)
    }

    private fun triggerAppOpenAd() {
        val appOpenManager = (application as MyApplication).appOpenManager
        appOpenManager?.forceShowAdIfAvailable(this, object : AdManagerCallback() {
            override fun onNextAction() {
                Toast.makeText(this@NonMaterialTestActivity, "App Open Ad flow completed", Toast.LENGTH_SHORT).show()
            }

            override fun onAdLoaded() {
                // Ad loaded successfully
            }

            override fun onFailedToLoad(adError: com.google.android.gms.ads.AdError?) {
                Toast.makeText(this@NonMaterialTestActivity, "Ad failed: ${adError?.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
