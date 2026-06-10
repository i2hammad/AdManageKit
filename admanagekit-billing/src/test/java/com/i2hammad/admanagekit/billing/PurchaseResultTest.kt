package com.i2hammad.admanagekit.billing

import com.android.billingclient.api.BillingClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [PurchaseResult] subscription-state contract.
 *
 * Pins the fixed behavior: setProductType() drives isSubscription() and all
 * subscription-state APIs. Only productType == "subs"
 * (BillingClient.ProductType.SUBS) makes a result a subscription.
 */
class PurchaseResultTest {

    private fun subscriptionResult(
        purchaseState: Int = PurchaseResult.State.PURCHASED,
        acknowledged: Boolean = true,
        autoRenewing: Boolean = true
    ): PurchaseResult = PurchaseResult().apply {
        setProductType(BillingClient.ProductType.SUBS)
        setPurchaseState(purchaseState)
        setAcknowledged(acknowledged)
        setAutoRenewing(autoRenewing)
    }

    // ==================== isSubscription / isInApp ====================

    @Test
    fun `isSubscription is false when productType is unset`() {
        val result = PurchaseResult()
        assertFalse(result.isSubscription)
        assertFalse(result.isInApp)
    }

    @Test
    fun `isSubscription is true only for subs product type`() {
        val subs = PurchaseResult().apply { setProductType(BillingClient.ProductType.SUBS) }
        assertEquals("subs", BillingClient.ProductType.SUBS)
        assertTrue(subs.isSubscription)
        assertFalse(subs.isInApp)
    }

    @Test
    fun `isSubscription is false for inapp product type`() {
        val inApp = PurchaseResult().apply { setProductType(BillingClient.ProductType.INAPP) }
        assertFalse(inApp.isSubscription)
        assertTrue(inApp.isInApp)
    }

    @Test
    fun `isSubscription is false for arbitrary product type`() {
        val other = PurchaseResult().apply { setProductType("something_else") }
        assertFalse(other.isSubscription)
        assertFalse(other.isInApp)
    }

    // ==================== getSubscriptionState ====================

    @Test
    fun `subscription state is NOT_SUBSCRIPTION when productType unset`() {
        val result = PurchaseResult().apply {
            // Even a purchased, acknowledged, auto-renewing purchase is not a
            // subscription without productType == "subs"
            setPurchaseState(PurchaseResult.State.PURCHASED)
            setAcknowledged(true)
            setAutoRenewing(true)
        }
        assertEquals(PurchaseResult.SubscriptionState.NOT_SUBSCRIPTION, result.subscriptionState)
    }

    @Test
    fun `subscription state is NOT_SUBSCRIPTION for inapp purchases`() {
        val result = PurchaseResult().apply {
            setProductType(BillingClient.ProductType.INAPP)
            setPurchaseState(PurchaseResult.State.PURCHASED)
            setAcknowledged(true)
            setAutoRenewing(true)
        }
        assertEquals(PurchaseResult.SubscriptionState.NOT_SUBSCRIPTION, result.subscriptionState)
    }

    @Test
    fun `subs purchased acknowledged autoRenewing is ACTIVE`() {
        val result = subscriptionResult(autoRenewing = true)
        assertEquals(PurchaseResult.SubscriptionState.ACTIVE, result.subscriptionState)
    }

    @Test
    fun `subs purchased acknowledged not autoRenewing is CANCELLED`() {
        val result = subscriptionResult(autoRenewing = false)
        assertEquals(PurchaseResult.SubscriptionState.CANCELLED, result.subscriptionState)
    }

    @Test
    fun `pending subscription is treated as ACTIVE`() {
        val result = subscriptionResult(
            purchaseState = PurchaseResult.State.PENDING,
            acknowledged = false,
            autoRenewing = false
        )
        assertEquals(PurchaseResult.SubscriptionState.ACTIVE, result.subscriptionState)
    }

    @Test
    fun `purchased but unacknowledged subscription is EXPIRED`() {
        val result = subscriptionResult(acknowledged = false, autoRenewing = true)
        assertEquals(PurchaseResult.SubscriptionState.EXPIRED, result.subscriptionState)
    }

    // ==================== isSubscriptionActive ====================

    @Test
    fun `isSubscriptionActive true for ACTIVE state`() {
        assertTrue(subscriptionResult(autoRenewing = true).isSubscriptionActive)
    }

    @Test
    fun `isSubscriptionActive true for CANCELLED state - access until expiration`() {
        assertTrue(subscriptionResult(autoRenewing = false).isSubscriptionActive)
    }

    @Test
    fun `isSubscriptionActive false when not a subscription`() {
        val inApp = PurchaseResult().apply {
            setProductType(BillingClient.ProductType.INAPP)
            setPurchaseState(PurchaseResult.State.PURCHASED)
            setAcknowledged(true)
        }
        assertFalse(inApp.isSubscriptionActive)
        assertFalse(PurchaseResult().isSubscriptionActive)
    }

    @Test
    fun `isSubscriptionActive false for expired subscription`() {
        assertFalse(subscriptionResult(acknowledged = false).isSubscriptionActive)
    }

    // ==================== isSubscriptionCancelled / willSubscriptionRenew ====================

    @Test
    fun `isSubscriptionCancelled true only when subs purchased and not autoRenewing`() {
        assertTrue(subscriptionResult(autoRenewing = false).isSubscriptionCancelled)
        assertFalse(subscriptionResult(autoRenewing = true).isSubscriptionCancelled)
        assertFalse(PurchaseResult().isSubscriptionCancelled)
    }

    @Test
    fun `willSubscriptionRenew requires subs type and autoRenewing`() {
        assertTrue(subscriptionResult(autoRenewing = true).willSubscriptionRenew())
        assertFalse(subscriptionResult(autoRenewing = false).willSubscriptionRenew())

        // autoRenewing alone is not enough without productType == "subs"
        val inApp = PurchaseResult().apply {
            setProductType(BillingClient.ProductType.INAPP)
            setAutoRenewing(true)
        }
        assertFalse(inApp.willSubscriptionRenew())
    }

    // ==================== purchase state helpers ====================

    @Test
    fun `isPurchased and isPending reflect purchase state`() {
        val purchased = PurchaseResult().apply { setPurchaseState(PurchaseResult.State.PURCHASED) }
        assertTrue(purchased.isPurchased)
        assertFalse(purchased.isPending)

        val pending = PurchaseResult().apply { setPurchaseState(PurchaseResult.State.PENDING) }
        assertFalse(pending.isPurchased)
        assertTrue(pending.isPending)
    }
}
