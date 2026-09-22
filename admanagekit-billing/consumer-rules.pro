# Consumer rules for ad-manage-kit-billing.
#
# Deliberately empty of -keep rules.
#
# `AppPurchase` and its listeners are called directly by the consuming app, and
# the value types (`PurchaseItem`, `PurchaseResult`, `OfferInfo`,
# `OneTimeOfferInfo`, `BillingPeriod`, `SubscriptionState`) are ordinary Kotlin
# classes built in our own code rather than deserialized into.
#
# The JSON that Play returns is parsed by the Play Billing Library into its own
# `ProductDetails` / `Purchase` types, and `com.android.billingclient:billing`
# ships the consumer rules that protect that parsing. Re-keeping
# `com.android.billingclient.**` here would only defeat them.
