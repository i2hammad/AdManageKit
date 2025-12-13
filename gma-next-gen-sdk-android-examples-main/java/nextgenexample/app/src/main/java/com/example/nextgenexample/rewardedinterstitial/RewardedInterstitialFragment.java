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

package com.example.nextgenexample.rewardedinterstitial;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.R;
import com.example.nextgenexample.databinding.FragmentRewardedInterstitialBinding;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback;
import java.util.Locale;

/** An [AdFragment] subclass that loads a rewarded interstitial ad. */
public class RewardedInterstitialFragment extends AdFragment<FragmentRewardedInterstitialBinding> {
  // Default constructor required for fragment instantiation.
  public RewardedInterstitialFragment() {}

  // Sample rewarded interstitial ad unit ID.
  private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379";
  private static final long COUNTDOWN_INTERVAL = 50L;
  private static final long GAME_LENGTH_MILLISECONDS = 5000L;
  private static final int GAME_OVER_REWARD = 1;
  private RewardedInterstitialAd rewardedInterstitialAd;
  private CountDownTimer countDownTimer;
  private boolean gamePaused;
  private boolean gameOver;
  private long timeLeftMillis;
  private int coinCount;

  @Override
  protected BindingInflater<FragmentRewardedInterstitialBinding> getBindingInflater() {
    return FragmentRewardedInterstitialBinding::inflate;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Configure the "Play again" button.
    binding.playAgainButton.setVisibility(View.INVISIBLE);
    binding.playAgainButton.setOnClickListener(
        unusedView -> {
          loadAd();
          startGame();
        });

    // Display the current coin count.
    binding.coins.setText(getString(R.string.coins, coinCount));

    loadAd();
    startGame();
  }

  /** Load a new rewarded interstitial ad if one isn't already loaded. */
  private void loadAd() {
    if (rewardedInterstitialAd != null) {
      Log.d(Constant.TAG, "Rewarded interstitial ad already loaded.");
      return;
    }

    RewardedInterstitialAd.load(
        new AdRequest.Builder(AD_UNIT_ID).build(),
        new AdLoadCallback<RewardedInterstitialAd>() {
          @Override
          public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
            RewardedInterstitialFragment.this.rewardedInterstitialAd = rewardedInterstitialAd;
            showToast("Rewarded interstitial ad loaded.");
            Log.d(Constant.TAG, "Rewarded interstitial ad loaded.");
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            rewardedInterstitialAd = null;
            showToast("Rewarded interstitial ad failed to load.");
            Log.w(Constant.TAG, "Rewarded interstitial ad failed to load: " + adError);
          }
        });
  }

  /** Show the ad if it's ready. Otherwise attempt to load an ad and restart the game. */
  private void showRewardedInterstitialAd() {
    if (rewardedInterstitialAd == null) {
      loadAd();
      startGame();
      Log.d(Constant.TAG, "The rewarded interstitial ad wasn't ready yet.");
      return;
    }

    // Listen for ad events.
    rewardedInterstitialAd.setAdEventCallback(
        new RewardedInterstitialAdEventCallback() {
          @Override
          public void onAdShowedFullScreenContent() {
            Log.d(Constant.TAG, "Rewarded interstitial ad showed.");
          }

          @Override
          public void onAdDismissedFullScreenContent() {
            rewardedInterstitialAd = null;
            Log.d(Constant.TAG, "Rewarded interstitial ad dismissed.");
          }

          @Override
          public void onAdFailedToShowFullScreenContent(
              @NonNull FullScreenContentError fullScreenContentError) {
            rewardedInterstitialAd = null;
            showToast("Rewarded interstitial ad failed to show.");
            Log.w(
                Constant.TAG, "Rewarded interstitial ad failed to show: " + fullScreenContentError);
          }

          @Override
          public void onAdImpression() {
            Log.d(Constant.TAG, "Rewarded interstitial ad recorded an impression.");
          }

          @Override
          public void onAdClicked() {
            Log.d(Constant.TAG, "Rewarded interstitial ad recorded a click.");
          }
        });

    // Show the ad.
    rewardedInterstitialAd.show(
        requireActivity(),
        rewardItem -> {
          addCoins(rewardItem.getAmount());
          showToast(
              String.format(
                  Locale.ENGLISH,
                  "Reward earned: %d %s",
                  rewardItem.getAmount(),
                  rewardItem.getType()));
        });
  }

  private void addCoins(int coins) {
    coinCount += coins;
    requireActivity()
        .runOnUiThread(() -> binding.coins.setText(getString(R.string.coins, coinCount)));
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
            addCoins(GAME_OVER_REWARD);
            // Set up a listener to handle the "adDialogCountdownComplete" result, triggering the
            // display of a rewarded interstitial ad.
            getChildFragmentManager()
                .setFragmentResultListener(
                    "adDialogCountdownComplete",
                    RewardedInterstitialFragment.this,
                    (requestKey, bundle) -> showRewardedInterstitialAd());
            new AdDialogFragment().show(getChildFragmentManager(), Constant.TAG);
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
