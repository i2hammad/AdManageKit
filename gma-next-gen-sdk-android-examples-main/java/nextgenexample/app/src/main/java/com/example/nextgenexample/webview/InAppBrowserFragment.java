package com.example.nextgenexample.webview;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.databinding.FragmentBrowserBinding;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import java.net.MalformedURLException;
import java.net.URL;

/** An [AdFragment] subclass that provides an interface for interacting with web content. */
public class InAppBrowserFragment extends AdFragment<FragmentBrowserBinding> {
  // Default constructor required for fragment instantiation.
  public InAppBrowserFragment() {}

  private static final String WEB_VIEW_URL = "https://google.github.io/webview-ads/test/";
  private static final String CUSTOM_TABS_URL =
      "https://google.github.io/webview-ads/test/?browser=cct";
  // TODO: Replace this URL with an associated website.
  private static final String ORIGIN = "https://www.google.com";
  private CustomTabsClient customTabsClient;
  private CustomTabsSession customTabsSession;
  private WebView webView;

  @Override
  protected BindingInflater<FragmentBrowserBinding> getBindingInflater() {
    return FragmentBrowserBinding::inflate;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    binding.launchWebviewButton.setOnClickListener(unusedView -> launchWebView());
    binding.launchCustomTabButton.setOnClickListener(unusedView -> launchCustomTab());

    webView = binding.webView;
    return view;
  }

  private void launchWebView() {
    webView.setVisibility(View.VISIBLE);

    webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri url = request.getUrl();

            // Handle null URL or host. Return false to allow the WebView to handle it.
            if (url == null || url.getHost() == null) {
              return false;
            }

            String scheme = url.getScheme();

            // Handle custom URL schemes (e.g., market://, intent://) by attempting to
            // launch an external application.
            if (scheme != null && !scheme.equals("http") && !scheme.equals("https")) {
              Intent intent = new Intent(Intent.ACTION_VIEW, url);
              try {
                requireContext().startActivity(intent);
              } catch (ActivityNotFoundException exception) {
                // Activity cannot be found.
                showToast("Failed to load URL with scheme: " + scheme);
              }
              return true; // URL handled externally.
            }

            String currentDomain;
            try {
              currentDomain = new URL(view.getUrl()).getHost();
            } catch (MalformedURLException e) {
              // Handle malformed URL, e.g., log the error or show a message.
              showToast("Could not load the page. Please check the URL.");
              return false;
            }

            String targetDomain = url.getHost();

            // If the current domain equals the target domain, the
            // assumption is the user is not navigating away from
            // the site. Reload the URL within the existing web view.
            if (currentDomain.equals(targetDomain)) {
              return false;
            }

            // User is navigating away from the site, open the URL in
            // Custom Tabs to preserve the state of the web view.
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
            customTabsIntent.launchUrl(requireContext(), url);
            return true;
          }
        });

    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

    MobileAds.registerWebView(webView);
    webView.loadUrl(WEB_VIEW_URL);
  }

  private void launchCustomTab() {
    // Get the default browser package name, this will be null if
    // the default browser does not provide a CustomTabsService.
    String packageName = CustomTabsClient.getPackageName(requireContext(), null);
    if (packageName == null) {
      // Do nothing as service connection is not supported.
      return;
    }

    // Listen for events from the CustomTabsSession delegated by the Google Mobile Ads SDK.
    CustomTabsCallback customTabsCallback =
        new CustomTabsCallback() {
          @Override
          public void onNavigationEvent(int navigationEvent, Bundle extras) {
            // Called when a navigation event happens.
            Log.i(Constant.TAG, "onNavigationEvent: " + navigationEvent);
          }

          @Override
          public void onMessageChannelReady(Bundle extras) {
            // Called when the channel is ready for sending and receiving messages on both
            // ends. This frequently happens, such as each time the SDK requests a
            // new channel.
            Log.i(Constant.TAG, "onMessageChannelReady");
          }

          @Override
          public void onPostMessage(@NonNull String message, Bundle extras) {
            // Called when a tab controlled by this CustomTabsSession has sent a postMessage.
            Log.i(Constant.TAG, "onPostMessage: " + message);
          }

          @Override
          public void onRelationshipValidationResult(
              int relation, @NonNull Uri requestedOrigin, boolean result, Bundle extras) {
            // Called when a relationship validation result is available.
            Log.i(Constant.TAG, "onRelationshipValidationResult: " + result);
          }

          @Override
          public void onActivityResized(int height, int width, @NonNull Bundle extras) {
            // Called when the tab is resized.
            Log.i(Constant.TAG, "onActivityResized");
          }
        };

    CustomTabsServiceConnection connection =
        new CustomTabsServiceConnection() {
          @Override
          public void onCustomTabsServiceConnected(
              @NonNull ComponentName name, @NonNull CustomTabsClient client) {
            customTabsClient = client;

            // Warm up the browser process.
            customTabsClient.warmup(0L);

            // Create a new browser session using the Google Mobile Ads SDK.
            customTabsSession =
                MobileAds.registerCustomTabsSession(
                    client,
                    // Checks the "Digital Asset Link" to connect the postMessage channel.
                    ORIGIN,
                    // Optional parameter to receive the delegated callbacks.
                    customTabsCallback);

            // Create a new browser session if the Google Mobile Ads SDK is
            // unable to create one.
            if (customTabsSession == null) {
              customTabsSession = client.newSession(customTabsCallback);
            }

            // Pass the custom tabs session into the intent.
            CustomTabsIntent customTabsIntent =
                new CustomTabsIntent.Builder(customTabsSession).build();
            customTabsIntent.launchUrl(requireContext(), Uri.parse(CUSTOM_TABS_URL));
          }

          @Override
          public void onServiceDisconnected(ComponentName componentName) {
            // Remove the custom tabs client and custom tabs session.
            customTabsClient = null;
            customTabsSession = null;
          }
        };

    CustomTabsClient.bindCustomTabsService(requireContext(), packageName, connection);
  }
}
