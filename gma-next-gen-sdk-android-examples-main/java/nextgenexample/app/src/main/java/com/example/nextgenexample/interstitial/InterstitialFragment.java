/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nextgenexample.interstitial;

import static com.example.nextgenexample.Constant.TAG;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.R;
import com.example.nextgenexample.databinding.FragmentInterstitialBinding;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader;

/** A [Fragment] subclass that preloads interstitial ads. */
public class InterstitialFragment extends AdFragment<FragmentInterstitialBinding> {

  // Sample interstitial ad unit ID.
  private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
  private static final long COUNTDOWN_INTERVAL = 50L;
  private static final long GAME_LENGTH_MILLISECONDS = 5000L;
  private CountDownTimer countDownTimer;
  private boolean gamePaused;
  private boolean gameOver;
  private long timeLeftMillis;

  @Override
  protected BindingInflater<FragmentInterstitialBinding> getBindingInflater() {
    return FragmentInterstitialBinding::inflate;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    // Start preloading.
    startPreloading();

    startGame();

    // Initialize the UI.
    binding.playAgainButton.setOnClickListener(
        sender -> {
          startGame();
          updateUI();
        });
    updateUI();
  }

  private void startPreloading() {
    PreloadCallback preloadCallback =
        // [Important] Do not call start() or pollAd() within the callback.
        new PreloadCallback() {
          @Override
          public void onAdFailedToPreload(
              @NonNull String preloadId, @NonNull LoadAdError loadAdError) {
            Log.i(TAG, "Interstitial ad failed to preload with error: " + loadAdError.getMessage());
          }

          @Override
          public void onAdsExhausted(@NonNull String preloadId) {
            Log.i(TAG, "Interstitial ads exhausted.");
            showToast("Interstitial ads exhausted.");
            updateUI();
          }

          @Override
          public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
            Log.i(TAG, "Interstitial ad preloaded.");
            showToast("Interstitial ad preloaded.");
            updateUI();
          }
        };
    AdRequest adRequest = new AdRequest.Builder(AD_UNIT_ID).build();
    PreloadConfiguration preloadConfiguration = new PreloadConfiguration(adRequest);
    InterstitialAdPreloader.start(AD_UNIT_ID, preloadConfiguration, preloadCallback);
  }

  private void showInterstitial() {
    // Polling returns the next available ad and loads another ad in the background.
    InterstitialAd ad = InterstitialAdPreloader.pollAd(AD_UNIT_ID);
    if (ad == null) {
      Log.i(TAG, "No preloaded interstitial ads available.");
      return;
    }

    // Interact with the ad object as needed.
    Log.d(TAG, "Interstitial ad response info: " + ad.getResponseInfo());
    ad.setAdEventCallback(
        new InterstitialAdEventCallback() {
          @Override
          public void onAdImpression() {
            Log.d(TAG, "Interstitial ad recorded an impression.");
          }

          @Override
          public void onAdPaid(@NonNull AdValue value) {
            Log.d(
                TAG,
                String.format(
                    "Interstitial ad onAdPaid: %d %s",
                    value.getValueMicros(), value.getCurrencyCode()));
          }
        });

    ad.show(requireActivity());
    updateUI();
  }

  public synchronized void updateUI() {
    runOnUiThread(
        () -> {
          if (InterstitialAdPreloader.isAdAvailable(AD_UNIT_ID)) {
            binding.txtStatus.setText(R.string.available);
          } else {
            binding.txtStatus.setText(R.string.exhausted);
          }
        });
  }

  /**
   * Create the game timer, which counts down to the end of the level and shows the "Play again"
   * button.
   */
  private void createTimer(long milliseconds) {
    if (countDownTimer != null) {
      countDownTimer.cancel();
    }

    countDownTimer =
        new CountDownTimer(milliseconds, COUNTDOWN_INTERVAL) {
          @Override
          public void onTick(long millisUntilFinished) {
            timeLeftMillis = millisUntilFinished;
            // Display countdown start from 5.0 seconds.
            int seconds = (int) (millisUntilFinished / 1000);
            if (millisUntilFinished % 1000 != 0) {
              seconds++;
            }

            binding.timer.setText(
                getResources().getQuantityString(R.plurals.seconds_left, seconds, seconds));
          }

          @Override
          public void onFinish() {
            gameOver = true;
            binding.timer.setText(getString(R.string.you_lose));
            binding.playAgainButton.setVisibility(View.VISIBLE);

            // Show the alert dialog which triggers the interstitial ad.
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder
                .setTitle(R.string.game_over)
                .setMessage("You lasted " + GAME_LENGTH_MILLISECONDS / 1000 + " seconds")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> showInterstitial());

            AlertDialog dialog = builder.create();
            dialog.show();
          }
        };

    countDownTimer.start();
  }

  /** Set the game back to "start". */
  private void startGame() {
    binding.playAgainButton.setVisibility(View.INVISIBLE);
    createTimer(GAME_LENGTH_MILLISECONDS);
    gamePaused = false;
    gameOver = false;
  }

  /** Cancel the timer. */
  private void pauseGame() {
    if (gameOver || gamePaused) {
      return;
    }

    if (countDownTimer != null) {
      countDownTimer.cancel();
    }
    gamePaused = true;
  }

  /** Create timer with the active time remaining. */
  private void resumeGame() {
    if (gameOver || !gamePaused) {
      return;
    }

    createTimer(timeLeftMillis);
    gamePaused = false;
  }

  @Override
  public void onResume() {
    super.onResume();
    resumeGame();
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseGame();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    // To stop preloading ads, call destroy() with a preload ID.
    InterstitialAdPreloader.destroy(AD_UNIT_ID);
  }
}
