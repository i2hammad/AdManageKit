package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.i2hammad.admanagekit.sample.R
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback

/**
 * Test Activity demonstrating how to show interstitial ads
 * between fragment transitions.
 *
 * This example shows:
 * - Loading an interstitial ad on activity start
 * - Showing the ad when navigating between fragments
 * - Reloading the ad after it's shown for subsequent transitions
 */
class FragmentTransitionTestActivity : AppCompatActivity(),
    FirstFragment.NavigationListener,
    SecondFragment.NavigationListener {

    companion object {
        private const val TAG = "FragmentTransitionTest"
        private const val TEST_INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712"
    }

    private lateinit var tvStatus: TextView
    private var isShowingFirstFragment = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fragment_transition_test)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)

        // Load the first fragment
        if (savedInstanceState == null) {
            showFirstFragment()
        }

        // Load interstitial ad
        loadInterstitialAd()

        // Handle back press with modern approach
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isShowingFirstFragment) {
                    // Show ad when pressing back from second fragment
                    showAdThenNavigate {
                        supportFragmentManager.popBackStack()
                        isShowingFirstFragment = true
                    }
                } else {
                    // Allow default back behavior (finish activity)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadInterstitialAd() {
        updateStatus("Loading interstitial ad...")

//        AdManager.getInstance().loadInterstitialAd(
//            this,
//            TEST_INTERSTITIAL_AD_UNIT,
//            object : InterstitialAdLoadCallback() {
//                override fun onAdLoaded(ad: InterstitialAd) {
//                    Log.d(TAG, "Interstitial ad loaded successfully")
//                    updateStatus("Ad loaded - Ready for transition")
//                }
//
//                override fun onAdFailedToLoad(error: LoadAdError) {
//                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
//                    updateStatus("Ad failed to load: ${error.code}")
//                }
//            }
//        )
    }

    private fun showFirstFragment() {
        val fragment = FirstFragment.newInstance()
        fragment.setNavigationListener(this)

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commitAllowingStateLoss()

        isShowingFirstFragment = true
    }

    private fun showSecondFragment() {
        val fragment = SecondFragment.newInstance()
        fragment.setNavigationListener(this)

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()

        isShowingFirstFragment = false
    }

    // Called from FirstFragment when user wants to navigate to SecondFragment
    override fun onNavigateToSecond() {
        showAdThenNavigate {
            showSecondFragment()
        }
    }

    // Called from SecondFragment when user wants to go back
    override fun onNavigateBack() {
        showAdThenNavigate {
            supportFragmentManager.popBackStack()
            isShowingFirstFragment = true
        }
    }

    /**
     * Shows an interstitial ad (if available) and then executes the navigation action.
     * If no ad is available, the navigation happens immediately.
     */
    private fun showAdThenNavigate(onComplete: () -> Unit) {
        updateStatus("Showing ad...")

        AdManager.getInstance().forceShowInterstitialWithDialog(
            this,
            object : AdManagerCallback() {
                override fun onNextAction() {
                    Log.d(TAG, "Ad dismissed or not available, proceeding with navigation")
                    updateStatus("Navigating...")

                    // Perform the navigation
                    onComplete()

                    // Reload ad for the next transition
                    loadInterstitialAd()
                }
            }
        )
    }

    private fun updateStatus(status: String) {
        tvStatus.text = "Ad Status: $status"
    }
}
