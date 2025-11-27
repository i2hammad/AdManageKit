package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.NativeAdTemplate
import com.i2hammad.admanagekit.admob.NativeTemplateView

/**
 * Test activity to preview all NativeTemplateView templates.
 * Allows switching between templates dynamically.
 */
class NativeTemplateTestActivity : AppCompatActivity() {

    private lateinit var nativeTemplateView: NativeTemplateView
    private lateinit var spinnerTemplate: Spinner
    private lateinit var btnLoadAd: Button
    private lateinit var tvCurrentTemplate: TextView
    private lateinit var tvTemplateInfo: TextView

    private val adUnitId = "ca-app-pub-3940256099942544/2247696110" // Test ad unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_template_test)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupSpinner()
        setupButtons()
    }

    private fun initViews() {
        nativeTemplateView = findViewById(R.id.nativeTemplateView)
        spinnerTemplate = findViewById(R.id.spinnerTemplate)
        btnLoadAd = findViewById(R.id.btnLoadAd)
        tvCurrentTemplate = findViewById(R.id.tvCurrentTemplate)
        tvTemplateInfo = findViewById(R.id.tvTemplateInfo)
    }

    private fun setupSpinner() {
        // Get all template names
        val templates = NativeAdTemplate.entries.map { it.displayName }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, templates)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTemplate.adapter = adapter

        spinnerTemplate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTemplate = NativeAdTemplate.fromIndex(position)
                updateTemplate(selectedTemplate)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        btnLoadAd.setOnClickListener {
            loadAd()
        }

        findViewById<Button>(R.id.btnNextTemplate).setOnClickListener {
            val currentPosition = spinnerTemplate.selectedItemPosition
            val nextPosition = (currentPosition + 1) % NativeAdTemplate.entries.size
            spinnerTemplate.setSelection(nextPosition)
        }

        findViewById<Button>(R.id.btnPrevTemplate).setOnClickListener {
            val currentPosition = spinnerTemplate.selectedItemPosition
            val prevPosition = if (currentPosition == 0) NativeAdTemplate.entries.size - 1 else currentPosition - 1
            spinnerTemplate.setSelection(prevPosition)
        }
    }

    private fun updateTemplate(template: NativeAdTemplate) {
        Log.d(TAG, "Switching to template: ${template.name}")

        // Update the template
        nativeTemplateView.setTemplate(template)

        // Update UI
        tvCurrentTemplate.text = "Template: ${template.displayName}"
        tvTemplateInfo.text = getTemplateInfo(template)

        // Auto-load the ad when switching templates
        loadAd()
    }

    private fun loadAd() {
        val currentTemplate = nativeTemplateView.getTemplate()
        Log.d(TAG, "Loading ad for template: ${currentTemplate.name}")

        btnLoadAd.isEnabled = false
        btnLoadAd.text = "Loading..."

        nativeTemplateView.loadNativeAd(this, adUnitId, object : AdLoadCallback() {
            override fun onAdLoaded() {
                Log.d(TAG, "Ad loaded successfully for ${currentTemplate.name}")
                runOnUiThread {
                    btnLoadAd.isEnabled = true
                    btnLoadAd.text = "Reload Ad"
                    Toast.makeText(this@NativeTemplateTestActivity, "Ad Loaded!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailedToLoad(error: AdError?) {
                Log.e(TAG, "Ad failed to load: ${error?.message}")
                runOnUiThread {
                    btnLoadAd.isEnabled = true
                    btnLoadAd.text = "Retry Load"
                    Toast.makeText(this@NativeTemplateTestActivity, "Failed: ${error?.code}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAdImpression() {
                Log.d(TAG, "Ad impression recorded for ${currentTemplate.name}")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Ad clicked for ${currentTemplate.name}")
            }
        })
    }

    private fun getTemplateInfo(template: NativeAdTemplate): String {
        return when (template) {
            NativeAdTemplate.CARD_MODERN -> "Modern card with media, icon, rating & Material button"
            NativeAdTemplate.MATERIAL3 -> "Material Design 3 compliant with stroke border"
            NativeAdTemplate.MINIMAL -> "Clean minimalist design with divider"
            NativeAdTemplate.COMPACT_HORIZONTAL -> "Horizontal layout, great for lists"
            NativeAdTemplate.STORY_STYLE -> "Story-inspired with large media (300dp)"
            NativeAdTemplate.FULL_WIDTH_BANNER -> "Horizontal banner, icon + text + CTA in row"
            NativeAdTemplate.GRID_CARD -> "Square card, perfect for grid layouts"
            NativeAdTemplate.LIST_ITEM -> "Optimized for RecyclerView integration"
            NativeAdTemplate.FEATURED -> "Prominent card with FEATURED badge"
            NativeAdTemplate.OVERLAY_DARK -> "Dark theme with overlay effect"
            NativeAdTemplate.MAGAZINE -> "Editorial style with serif headline"
            NativeAdTemplate.VIDEO_SMALL -> "Compact video player (160dp)"
            NativeAdTemplate.VIDEO_MEDIUM -> "Medium video player (280dp, 16:9)"
            NativeAdTemplate.VIDEO_LARGE -> "Large video player (360dp) with overlay"
            NativeAdTemplate.VIDEO_SQUARE -> "Square video format (1:1)"
            NativeAdTemplate.VIDEO_VERTICAL -> "Portrait video (9:16, Stories style)"
            NativeAdTemplate.VIDEO_FULLSCREEN -> "Edge-to-edge video experience"
        }
    }

    companion object {
        private const val TAG = "NativeTemplateTest"
    }
}
