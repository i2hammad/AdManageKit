package com.i2hammad.admanagekit.billing

import android.app.Activity
import android.os.Looper
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsFactory
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.QueryProductDetailsResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * The case 4.4.13 fixed: Play answers the product-details query with ERROR ("An internal
 * error occurred."), prices never load, and every Buy tap fails with "Billing not initialized".
 *
 * AppPurchase builds its own BillingClient, so BillingClient.newBuilder is mocked to hand back
 * a client whose setup and query callbacks the test answers by hand. Retry delays run on the
 * main looper and are advanced in Robolectric's virtual time. Only a subscription product is
 * configured, so every captured query is the SUBS one.
 */
@RunWith(RobolectricTestRunner::class)
class AppPurchaseRetryTest {

    private val client = mockk<BillingClient>(relaxed = true)
    private val setups = mutableListOf<BillingClientStateListener>()
    private val queries = mutableListOf<ProductDetailsResponseListener>()
    private val purchaseListener = mockk<PurchaseListener>(relaxed = true)
    private val detailsListener = mockk<ProductDetailsListener>(relaxed = true)
    private lateinit var billing: AppPurchase

    private val monthly = ProductDetailsFactory.subscription(
        PRODUCT,
        listOf(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_monthly",
                phases = listOf(
                    ProductDetailsFactory.Phase(
                        priceMicros = 4_990_000,
                        formattedPrice = "$4.99",
                        billingPeriod = "P1M",
                        recurrenceMode = 1,
                    ),
                ),
            ),
        ),
    )

    @Before
    fun setUp() {
        resetSingleton()
        val builder = mockk<BillingClient.Builder>()
        mockkStatic(BillingClient::class)
        every { BillingClient.newBuilder(any()) } returns builder
        every { builder.setListener(any()) } returns builder
        every { builder.enablePendingPurchases(any()) } returns builder
        every { builder.enableAutoServiceReconnection() } returns builder
        every { builder.build() } returns client
        every { client.isReady } returns true
        every { client.startConnection(any()) } answers { setups += firstArg<BillingClientStateListener>() }
        every { client.queryProductDetailsAsync(any(), any()) } answers {
            queries += secondArg<ProductDetailsResponseListener>()
        }
        every { client.launchBillingFlow(any(), any()) } returns result(BillingClient.BillingResponseCode.OK)

        billing = AppPurchase.getInstance()
        billing.setPurchaseListener(purchaseListener)
        billing.setProductDetailsListener(detailsListener)
        billing.initBilling(
            RuntimeEnvironment.getApplication(),
            listOf(PurchaseItem(PRODUCT, AppPurchase.TYPE_IAP.SUBSCRIPTION)),
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        resetSingleton()
    }

    @Test
    fun `a failed query is retried after 2, 5 and 15 seconds, then left alone`() {
        finishSetup(BillingClient.BillingResponseCode.OK)
        assertEquals(1, queries.size)

        failLastQuery(BillingClient.BillingResponseCode.ERROR)
        advance(1_999)
        assertEquals("no retry before 2 s", 1, queries.size)
        advance(1)
        assertEquals(2, queries.size)

        failLastQuery(BillingClient.BillingResponseCode.ERROR)
        advance(5_000)
        assertEquals(3, queries.size)

        failLastQuery(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
        advance(15_000)
        assertEquals(4, queries.size)

        failLastQuery(BillingClient.BillingResponseCode.ERROR)
        advance(120_000)
        assertEquals("three retries, then stop", 4, queries.size)

        verify(exactly = 4) { detailsListener.onProductDetailsFailed(BillingClient.ProductType.SUBS, any(), any()) }
        verify(exactly = 0) { purchaseListener.displayErrorMessage(any()) }
    }

    @Test
    fun `a retry that succeeds loads the prices`() {
        finishSetup(BillingClient.BillingResponseCode.OK)
        failLastQuery(BillingClient.BillingResponseCode.ERROR)
        assertFalse(billing.isProductDetailsLoaded(PRODUCT))

        advance(2_000)
        succeedLastQuery()

        assertTrue(billing.isProductDetailsLoaded(PRODUCT))
        assertTrue(billing.areAllProductDetailsLoaded())
        verify { detailsListener.onProductDetailsLoaded(BillingClient.ProductType.SUBS, any(), any()) }
        advance(120_000)
        assertEquals("no retries after success", 2, queries.size)
    }

    @Test
    fun `answers that will not change by waiting are not retried`() {
        finishSetup(BillingClient.BillingResponseCode.OK)
        failLastQuery(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
        advance(120_000)
        assertEquals(1, queries.size)
    }

    @Test
    fun `a failed first setup reconnects`() {
        assertEquals(1, setups.size)
        finishSetup(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
        assertEquals(0, queries.size)

        advance(2_000)
        assertEquals("reconnect after 2 s", 2, setups.size)
        finishSetup(BillingClient.BillingResponseCode.OK)
        assertEquals("a good setup queries the products", 1, queries.size)
    }

    @Test
    fun `query on a disconnected client connects instead of doing nothing`() {
        every { client.isReady } returns false
        billing.queryProductDetails(listOf(PRODUCT), BillingClient.ProductType.SUBS)
        assertEquals(2, setups.size)
        assertEquals(0, queries.size)
    }

    @Test
    fun `Buy before prices fails once, re-queries, and the next Buy opens Play`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        finishSetup(BillingClient.BillingResponseCode.OK)
        failLastQuery(BillingClient.BillingResponseCode.ERROR)

        assertEquals("Billing not initialized", billing.subscribe(activity, PRODUCT))
        idle()
        verify { purchaseListener.displayErrorMessage("Billing not initialized") }
        assertEquals("the failed Buy asks Play again", 2, queries.size)

        succeedLastQuery()
        assertEquals("Billing flow started", billing.subscribe(activity, PRODUCT))
        verify(exactly = 1) { client.launchBillingFlow(activity, any()) }
    }

    @Test
    fun `refreshProductDetails only queries what is missing`() {
        finishSetup(BillingClient.BillingResponseCode.OK)
        succeedLastQuery()
        billing.refreshProductDetails()
        assertEquals(1, queries.size)
    }

    private fun finishSetup(code: Int) {
        setups.last().onBillingSetupFinished(result(code))
        idle()
    }

    private fun failLastQuery(code: Int) {
        queries.last().onProductDetailsResponse(result(code), mockk<QueryProductDetailsResult>(relaxed = true))
        idle()
    }

    private fun succeedLastQuery() {
        val response = mockk<QueryProductDetailsResult>()
        every { response.productDetailsList } returns listOf(monthly)
        every { response.unfetchedProductList } returns emptyList()
        queries.last().onProductDetailsResponse(result(BillingClient.BillingResponseCode.OK), response)
        idle()
    }

    private fun result(code: Int) = BillingResult.newBuilder().setResponseCode(code).setDebugMessage("test").build()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun resetSingleton() {
        AppPurchase::class.java.getDeclaredField("appPurchase").apply { isAccessible = true }.set(null, null)
    }

    private companion object {
        const val PRODUCT = "sub_monthly"
    }
}
