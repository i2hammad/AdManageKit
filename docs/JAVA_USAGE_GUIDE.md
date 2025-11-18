# Java Usage Guide - InterstitialAdBuilder

## Overview

The `InterstitialAdBuilder` is **fully compatible with Java**. This guide provides complete Java examples for all features.

---

## 🚀 Quick Start (Java)

### Simple Usage
```java
// Import
import com.i2hammad.admanagekit.admob.InterstitialAdBuilder;
import com.i2hammad.admanagekit.admob.OnAdCompleteListener;

// In your Activity
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .show(new OnAdCompleteListener() {
        @Override
        public void onComplete() {
            // Continue to next screen
            startNextActivity();
        }
    });
```

---

## 📚 Complete Java Examples

### Example 1: Basic Navigation
```java
public class GameOverActivity extends AppCompatActivity {

    private void playAgain() {
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_ad_unit))
            .show(new OnAdCompleteListener() {
                @Override
                public void onComplete() {
                    startActivity(new Intent(GameOverActivity.this, GameActivity.class));
                    finish();
                }
            });
    }
}
```

### Example 2: With Fallback
```java
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/primary")
    .fallback("ca-app-pub-xxxxx/backup")
    .show(new OnAdCompleteListener() {
        @Override
        public void onComplete() {
            goToNextScreen();
        }
    });
```

### Example 3: Frequency Control - Every 3rd Time
```java
public class GameActivity extends AppCompatActivity {

    private InterstitialAdBuilder adBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Initialize builder
        adBuilder = InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_game))
            .everyNthTime(3)  // Show on 3rd, 6th, 9th level
            .debug();
    }

    private void onLevelComplete() {
        adBuilder.show(new OnAdCompleteListener() {
            @Override
            public void onComplete() {
                loadNextLevel();
            }
        });
    }
}
```

### Example 4: With All Callbacks
```java
import com.i2hammad.admanagekit.admob.InterstitialAdCallback;
import com.google.android.gms.ads.LoadAdError;

InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .show(new InterstitialAdCallback() {
        @Override
        public void onAdLoaded() {
            Log.d("Ads", "Ad loaded successfully");
        }

        @Override
        public void onAdShown() {
            Log.d("Ads", "Ad shown to user");
            analytics.logEvent("ad_shown", null);
        }

        @Override
        public void onAdFailed(LoadAdError error) {
            Log.e("Ads", "Ad failed: " + error.getMessage());
        }

        @Override
        public void onComplete() {
            proceedToNextScreen();
        }
    });
```

### Example 5: Individual Callbacks
```java
import com.i2hammad.admanagekit.admob.OnAdLoadedListener;
import com.i2hammad.admanagekit.admob.OnAdShownListener;
import com.i2hammad.admanagekit.admob.OnAdFailedListener;

InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .onAdLoaded(new OnAdLoadedListener() {
        @Override
        public void onAdLoaded() {
            Log.d("Ads", "Ad loaded");
        }
    })
    .onAdShown(new OnAdShownListener() {
        @Override
        public void onAdShown() {
            FirebaseAnalytics.getInstance(MainActivity.this)
                .logEvent("interstitial_shown", null);
        }
    })
    .onFailed(new OnAdFailedListener() {
        @Override
        public void onAdFailed(LoadAdError error) {
            Log.e("Ads", "Failed: " + error.getMessage());
        }
    })
    .show(new OnAdCompleteListener() {
        @Override
        public void onComplete() {
            startActivity(new Intent(MainActivity.this, NextActivity.class));
        }
    });
```

### Example 6: Frequency Control
```java
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .everyNthTime(2)         // Every 2nd call
    .maxShows(15)            // Max 15 times total
    .minIntervalSeconds(45)  // 45 seconds minimum
    .show(new OnAdCompleteListener() {
        @Override
        public void onComplete() {
            continueFlow();
        }
    });
```

### Example 7: With Loading Dialog
```java
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .withLoadingDialog()  // Show "Please wait..."
    .show(new OnAdCompleteListener() {
        @Override
        public void onComplete() {
            goNext();
        }
    });
```

### Example 8: Force Show
```java
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .force()  // Ignore time interval
    .show(new OnAdCompleteListener() {
        @Override
        public void onComplete() {
            finish();
        }
    });
```

### Example 9: Complete Configuration
```java
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/primary")
    .fallback("ca-app-pub-xxxxx/backup-1")
    .fallback("ca-app-pub-xxxxx/backup-2")
    .everyNthTime(3)
    .maxShows(20)
    .minIntervalSeconds(60)
    .force()
    .withLoadingDialog()
    .autoReload(true)
    .debug()
    .show(new InterstitialAdCallback() {
        @Override
        public void onAdLoaded() {
            Log.d("Ads", "Loaded");
        }

        @Override
        public void onAdShown() {
            Log.d("Ads", "Shown");
        }

        @Override
        public void onAdFailed(LoadAdError error) {
            Log.e("Ads", "Failed: " + error.getMessage());
        }

        @Override
        public void onComplete() {
            startActivity(new Intent(MainActivity.this, NextActivity.class));
            finish();
        }
    });
```

### Example 10: Preload Pattern
```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Preload ad early
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_ad_unit))
            .preload();
    }

    private void showAdBeforeExit() {
        // Ad already preloaded, shows instantly
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_ad_unit))
            .show(new OnAdCompleteListener() {
                @Override
                public void onComplete() {
                    finish();
                }
            });
    }
}
```

---

## 🎯 Java Callback Interfaces

### OnAdCompleteListener
```java
public interface OnAdCompleteListener {
    void onComplete();
}
```

**Usage:**
```java
.show(new OnAdCompleteListener() {
    @Override
    public void onComplete() {
        // Continue your flow
    }
});
```

### OnAdLoadedListener
```java
public interface OnAdLoadedListener {
    void onAdLoaded();
}
```

**Usage:**
```java
.onAdLoaded(new OnAdLoadedListener() {
    @Override
    public void onAdLoaded() {
        // Ad loaded successfully
    }
})
```

### OnAdShownListener
```java
public interface OnAdShownListener {
    void onAdShown();
}
```

**Usage:**
```java
.onAdShown(new OnAdShownListener() {
    @Override
    public void onAdShown() {
        // Ad shown to user
    }
})
```

### OnAdFailedListener
```java
public interface OnAdFailedListener {
    void onAdFailed(LoadAdError error);
}
```

**Usage:**
```java
.onFailed(new OnAdFailedListener() {
    @Override
    public void onAdFailed(LoadAdError error) {
        // Handle failure
        Log.e("Ads", error.getMessage());
    }
})
```

### InterstitialAdCallback (Combined)
```java
public interface InterstitialAdCallback {
    void onAdLoaded();
    void onAdShown();
    void onAdFailed(LoadAdError error);
    void onComplete();
}
```

**Usage:**
```java
.show(new InterstitialAdCallback() {
    @Override
    public void onAdLoaded() { }

    @Override
    public void onAdShown() { }

    @Override
    public void onAdFailed(LoadAdError error) { }

    @Override
    public void onComplete() { }
});
```

---

## 🎨 Common Patterns (Java)

### Pattern 1: Game - Every 3 Levels
```java
public class GameActivity extends AppCompatActivity {

    private InterstitialAdBuilder levelCompleteAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        levelCompleteAd = InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_game))
            .everyNthTime(3)
            .debug();
    }

    private void onLevelComplete() {
        levelCompleteAd.show(new OnAdCompleteListener() {
            @Override
            public void onComplete() {
                loadNextLevel();
            }
        });
    }
}
```

### Pattern 2: Limited Exposure
```java
public class MainActivity extends AppCompatActivity {

    private void showWelcomeAd() {
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_welcome))
            .maxShows(3)  // Only first 3 times
            .show(new OnAdCompleteListener() {
                @Override
                public void onComplete() {
                    showMainContent();
                }
            });
    }
}
```

### Pattern 3: Time-Controlled
```java
public class ArticleActivity extends AppCompatActivity {

    private void showArticleAd() {
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_article))
            .minIntervalSeconds(120)  // 2 minutes minimum
            .show(new OnAdCompleteListener() {
                @Override
                public void onComplete() {
                    loadNextArticle();
                }
            });
    }
}
```

### Pattern 4: Combined Controls
```java
public class AppActivity extends AppCompatActivity {

    private void showSmartAd() {
        InterstitialAdBuilder.with(this)
            .adUnit("ca-app-pub-xxxxx/yyyyy")
            .everyNthTime(2)
            .maxShows(10)
            .minIntervalSeconds(30)
            .debug()
            .show(new OnAdCompleteListener() {
                @Override
                public void onComplete() {
                    continueFlow();
                }
            });
    }
}
```

---

## 🔧 Advanced Java Usage

### Reusable Callback Classes
```java
public class MyAdCallback implements InterstitialAdCallback {

    private Activity activity;

    public MyAdCallback(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onAdLoaded() {
        Log.d("MyApp", "Ad loaded");
    }

    @Override
    public void onAdShown() {
        FirebaseAnalytics.getInstance(activity)
            .logEvent("ad_interstitial_shown", null);
    }

    @Override
    public void onAdFailed(LoadAdError error) {
        Log.e("MyApp", "Ad failed: " + error.getMessage());
        Bundle bundle = new Bundle();
        bundle.putInt("error_code", error.getCode());
        bundle.putString("error_message", error.getMessage());
        FirebaseAnalytics.getInstance(activity)
            .logEvent("ad_failed", bundle);
    }

    @Override
    public void onComplete() {
        // Continue flow
    }
}

// Usage
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit-id")
    .show(new MyAdCallback(this));
```

### Base Activity Pattern
```java
public abstract class AdEnabledActivity extends AppCompatActivity {

    protected void showInterstitialAndProceed(Runnable onComplete) {
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_ad_unit))
            .everyNthTime(3)
            .minIntervalSeconds(60)
            .show(new OnAdCompleteListener() {
                @Override
                public void onComplete() {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
    }
}

// Usage in child activity
public class GameActivity extends AdEnabledActivity {

    private void onGameOver() {
        showInterstitialAndProceed(new Runnable() {
            @Override
            public void run() {
                restartGame();
            }
        });
    }
}
```

### Application-Level Setup
```java
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize AdMob
        MobileAds.initialize(this);

        // Enable aggressive loading
        AdManager.getInstance().enableAggressiveAdLoading();

        // Preload first ad
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                InterstitialAdBuilder.with(activity)
                    .adUnit("ca-app-pub-xxxxx/yyyyy")
                    .preload();
                unregisterActivityLifecycleCallbacks(this);
            }

            @Override
            public void onActivityStarted(Activity activity) { }
            @Override
            public void onActivityResumed(Activity activity) { }
            @Override
            public void onActivityPaused(Activity activity) { }
            @Override
            public void onActivityStopped(Activity activity) { }
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override
            public void onActivityDestroyed(Activity activity) { }
        });
    }
}
```

---

## 📊 Java vs Kotlin Comparison

### Simple Show

#### Kotlin
```kotlin
showInterstitialAd("ad-unit") { next() }
```

#### Java
```java
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .show(new OnAdCompleteListener() {
        @Override
        public void onComplete() {
            next();
        }
    });
```

### With Callbacks

#### Kotlin
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .onAdShown { analytics.log("shown") }
    .onFailed { error -> Log.e("Ad", error.message) }
    .show { next() }
```

#### Java
```java
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .onAdShown(new OnAdShownListener() {
        @Override
        public void onAdShown() {
            analytics.log("shown");
        }
    })
    .onFailed(new OnAdFailedListener() {
        @Override
        public void onAdFailed(LoadAdError error) {
            Log.e("Ad", error.getMessage());
        }
    })
    .show(new OnAdCompleteListener() {
        @Override
        public void onComplete() {
            next();
        }
    });
```

---

## 🎯 Best Practices for Java

### 1. Reuse Builders
```java
// ✅ GOOD: Create once, reuse
private InterstitialAdBuilder adBuilder;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    adBuilder = InterstitialAdBuilder.with(this)
        .adUnit("ad-unit")
        .everyNthTime(3);
}

private void showAd() {
    adBuilder.show(new OnAdCompleteListener() {
        @Override
        public void onComplete() { next(); }
    });
}

// ❌ BAD: Create every time
private void showAd() {
    InterstitialAdBuilder.with(this)
        .adUnit("ad-unit")
        .everyNthTime(3)
        .show(...);
}
```

### 2. Use Combined Callback
```java
// ✅ GOOD: Single callback class
.show(new InterstitialAdCallback() {
    @Override public void onAdLoaded() { }
    @Override public void onAdShown() { }
    @Override public void onAdFailed(LoadAdError error) { }
    @Override public void onComplete() { }
});

// ❌ WORSE: Multiple separate callbacks
.onAdLoaded(...).onAdShown(...).onFailed(...).show(...)
```

### 3. Extract Reusable Callbacks
```java
// ✅ GOOD: Reusable
public class AdCallbacks {
    public static class GameOver implements OnAdCompleteListener {
        private Activity activity;

        public GameOver(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onComplete() {
            activity.startActivity(new Intent(activity, GameActivity.class));
        }
    }
}

// Usage
.show(new AdCallbacks.GameOver(this));
```

---

## 🔍 Troubleshooting (Java)

### Issue: Callbacks Not Working

**Problem:**
```java
// ❌ WRONG: Doesn't implement interface
.show(new Object() {
    public void onComplete() { }
});
```

**Solution:**
```java
// ✅ CORRECT: Implement interface
.show(new OnAdCompleteListener() {
    @Override
    public void onComplete() { }
});
```

### Issue: Memory Leaks

**Problem:**
```java
// ❌ WRONG: Inner class holds activity reference
.show(new OnAdCompleteListener() {
    @Override
    public void onComplete() {
        MyActivity.this.finish();  // Can leak
    }
});
```

**Solution:**
```java
// ✅ CORRECT: Use WeakReference or static class
static class CompleteListener implements OnAdCompleteListener {
    private WeakReference<Activity> activityRef;

    CompleteListener(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void onComplete() {
        Activity activity = activityRef.get();
        if (activity != null && !activity.isFinishing()) {
            activity.finish();
        }
    }
}
```

---

## 📝 Summary

### Java Compatibility Features
- ✅ **`@JvmStatic`** - Companion object methods accessible
- ✅ **Interface callbacks** - No lambda required
- ✅ **Method overloading** - Supports both Kotlin and Java patterns
- ✅ **All features available** - 100% feature parity with Kotlin

### Available Interfaces
- ✅ `OnAdCompleteListener` - Main completion callback
- ✅ `OnAdLoadedListener` - Ad loaded callback
- ✅ `OnAdShownListener` - Ad shown callback
- ✅ `OnAdFailedListener` - Failure callback
- ✅ `InterstitialAdCallback` - Combined callback

### Key Benefits for Java Developers
- ✅ **Fluent API** - Same builder pattern as Kotlin
- ✅ **Type-safe** - All callbacks strongly typed
- ✅ **No lambdas required** - Interface-based callbacks
- ✅ **Full feature set** - All functionality available
- ✅ **Well documented** - Complete Java examples

---

**InterstitialAdBuilder is fully production-ready for Java projects!** ☕
