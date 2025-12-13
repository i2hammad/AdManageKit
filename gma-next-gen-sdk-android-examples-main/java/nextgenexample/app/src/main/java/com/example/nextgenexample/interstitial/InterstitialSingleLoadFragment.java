/*
 * Copyright 2024 Google LLC
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

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.R;
import com.example.nextgenexample.databinding.FragmentInterstitialBinding;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;

/** An [AdFragment] subclass that loads an interstitial ad. */
public class InterstitialSingleLoadFragment extends AdFragment<FragmentInterstitialBinding> {
  // Default constructor required for fragment instantiation.
  public InterstitialSingleLoadFragment() {}

  // Sample interstitial ad unit ID.
  private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
  private static final long COUNTDOWN_INTERVAL = 50L;
  private static final long GAME_LENGTH_MILLISECONDS = 5000L;
  private InterstitialAd interstitialAd;
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

    binding.txtStatus.setVisibility(View.GONE);

    // Configure the "Play again" button.
    binding.playAgainButton.setVisibility(View.INVISIBLE);
    binding.playAgainButton.setOnClickListener(
        unusedView -> {
          loadAd();
          startGame();
        });

    loadAd();
    startGame();
  }

  /** Load a new interstitial ad if one isn't already loaded. */
  private void loadAd() {
    if (interstitialAd != null) {
      Log.d(Constant.TAG, "Interstitial ad already loaded.");
      return;
    }

    InterstitialAd.load(
        new AdRequest.Builder(AD_UNIT_ID).build(),
        new AdLoadCallback<InterstitialAd>() {
          @Override
          public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
            InterstitialSingleLoadFragment.this.interstitialAd = interstitialAd;
            showToast("Interstitial ad loaded.");
            Log.d(Constant.TAG, "Interstitial ad loaded.");
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            interstitialAd = null;
            showToast("Interstitial ad failed to load.");
            Log.w(Constant.TAG, "Interstitial ad failed to load: " + adError);
          }
        });
  }

  /** Show the ad if it's ready. Otherwise attempt to load an ad and restart the game. */
  private void showInterstitial() {
    if (interstitialAd == null) {
      loadAd();
      startGame();
      Log.d(Constant.TAG, "The interstitial ad wasn't ready yet.");
      return;
    }

    // Listen for ad events.
    interstitialAd.setAdEventCallback(
        new InterstitialAdEventCallback() {
          @Override
          public void onAdShowedFullScreenContent() {
            Log.d(Constant.TAG, "Interstitial ad showed.");
          }

          @Override
          public void onAdDismissedFullScreenContent() {
            interstitialAd = null;
            Log.d(Constant.TAG, "Interstitial ad dismissed.");
          }

          @Override
          public void onAdFailedToShowFullScreenContent(
              @NonNull FullScreenContentError fullScreenContentError) {
            interstitialAd = null;
            showToast("Interstitial ad failed to show.");
            Log.w(Constant.TAG, "Interstitial ad failed to show: " + fullScreenContentError);
          }

          @Override
          public void onAdImpression() {
            Log.d(Constant.TAG, "Interstitial ad recorded an impression.");
          }

          @Override
          public void onAdClicked() {
            Log.d(Constant.TAG, "Interstitial ad clicked.");
          }
        });

    // Show the ad.
    interstitialAd.show(requireActivity());
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
}
