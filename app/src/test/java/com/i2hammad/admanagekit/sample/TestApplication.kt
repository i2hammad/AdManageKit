package com.i2hammad.admanagekit.sample

import android.app.Application

/**
 * Plain Application used by Robolectric tests instead of [MyApplication],
 * which initializes the Yandex SDK, Google Play Billing, and AppOpenManager
 * in onCreate() — none of which should run in JVM unit tests.
 */
class TestApplication : Application()
