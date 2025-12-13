package com.example.nextgenexample.inlinebanner;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.R;
import com.example.nextgenexample.databinding.FragmentInlineBannerBinding;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** An [AdFragment] subclass that loads inline banner ads. */
public final class InlineBannerFragment extends AdFragment<FragmentInlineBannerBinding> {
  // Sample banner ad unit ID.
  private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741";
  // A banner ad is placed after every 8 menu items in the list.
  private static final int ITEMS_PER_AD = 8;

  // List of banner ads and menu items populated in the RecyclerView.
  private final List<Object> recyclerViewItems = new ArrayList<>();

  // Default constructor required for fragment instantiation.
  public InlineBannerFragment() {}

  @Override
  protected BindingInflater<FragmentInlineBannerBinding> getBindingInflater() {
    return FragmentInlineBannerBinding::inflate;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    RecyclerView recyclerView = binding.recyclerView;
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

    // Specify an adapter.
    RecyclerViewAdapter adapter =
        RecyclerViewAdapter.newInstance(requireActivity(), recyclerViewItems);
    recyclerView.setAdapter(adapter);

    // Update the RecyclerView item's list with menu items and banner ads.
    addMenuItemsFromJson();

    // Wait for the RecyclerView to be laid out.
    recyclerView.post(() -> {
      if (recyclerView.getWidth() > 0) {
        // Load banner ads now that the RecyclerView has a width.
        loadBannerAds(adapter);
      } else {
        Log.w(Constant.TAG, "RecyclerView width is still 0. Cannot load ads.");
      }
    });
  }

  private void loadBannerAds(RecyclerViewAdapter adapter) {
    // Get the ad size based on the recyclerViewWidth width.
    RecyclerView recyclerView = binding.recyclerView;
    int recyclerViewWidthPixels = recyclerView.getWidth();
    float density = getResources().getDisplayMetrics().density;
    // Set width to 320 width if density is 0.
    int adWidth =
        (density != 0) ? (int) (recyclerViewWidthPixels / density) : 320;

    AdSize adSize =
        AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(requireContext(), adWidth);

    for (int i = ITEMS_PER_AD; i < recyclerViewItems.size(); i += ITEMS_PER_AD + 1) {
      final int adIndex = i;

      // Add the banner item.
      BannerItem bannerItem = new BannerItem();
      recyclerViewItems.add(adIndex, bannerItem);
      adapter.notifyItemInserted(adIndex);

      BannerAd.load(
          new BannerAdRequest.Builder(AD_UNIT_ID, adSize).build(),
        new AdLoadCallback<BannerAd>() {
          @Override
          public void onAdLoaded(@NonNull BannerAd bannerAd) {
            Log.d(Constant.TAG, "Banner ad loaded.");
            if (getActivity() != null) {
              getActivity().runOnUiThread(() -> {
                // Update the banner item with the loaded banner ad.
                recyclerViewItems.set(adIndex, bannerItem);
                bannerItem.bannerAd = bannerAd;
                adapter.notifyItemChanged(adIndex);
              });
            }
            bannerAd.setAdEventCallback(new BannerAdEventCallback() {
              @Override
              public void onAdImpression() {
                Log.d(Constant.TAG, "Banner ad recorded an impression.");
              }

              @Override
              public void onAdClicked() {
                Log.d(Constant.TAG, "Banner ad clicked.");
              }

              @Override
              public void onAdShowedFullScreenContent() {
                Log.d(Constant.TAG, "Banner ad showed.");
              }

              @Override
              public void onAdDismissedFullScreenContent() {
                Log.d(Constant.TAG, "Banner ad dismissed.");
              }

              @Override
              public void onAdFailedToShowFullScreenContent(
                  @NonNull FullScreenContentError fullScreenContentError) {
                Log.w(Constant.TAG, "Banner ad failed to show: " + fullScreenContentError);
              }
            });
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            Log.w(Constant.TAG, "Banner ad failed to load: " + adError);
          }
        });
    }
  }

  /** Adds [InlineMenuItem]'s from a JSON file. */
  private void addMenuItemsFromJson() {
    try {
      String jsonDataString = readJsonDataFromFile();
      JSONArray menuItemsJsonArray = new JSONArray(jsonDataString);
      for (int i = 0; i < menuItemsJsonArray.length(); i++) {
        JSONObject menuItemObject = menuItemsJsonArray.getJSONObject(i);
        String menuItemName = menuItemObject.getString("name");
        String menuItemDescription = menuItemObject.getString("description");
        String menuItemPrice = menuItemObject.getString("price");
        String menuItemCategory = menuItemObject.getString("category");
        String menuItemImageName = menuItemObject.getString("photo");
        FoodMenuItem menuItem = new FoodMenuItem(
            menuItemName,
            menuItemDescription,
            menuItemPrice,
            menuItemCategory,
            menuItemImageName

        );
        recyclerViewItems.add(menuItem);
      }
    } catch (IOException | JSONException exception) {
      Log.e(Constant.TAG, "Unable to parse JSON file.", exception);
    }
  }

  /**
   * Reads the JSON file and converts the JSON data to a [String].
   *
   * @return A [String] representation of the JSON data.
   * @throws IOException if unable to read the JSON file.
   */
  private String readJsonDataFromFile() throws IOException {
    StringBuilder builder = new StringBuilder();
    try (InputStream inputStream = getResources().openRawResource(R.raw.menu_items);
        BufferedReader bufferedReader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String jsonDataString;
      while ((jsonDataString = bufferedReader.readLine()) != null) {
        builder.append(jsonDataString);
      }
    }
    return builder.toString();
  }
}
