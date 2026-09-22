# Consumer rules for ad-manage-kit-core.
#
# Deliberately empty of -keep rules.
#
# The module is a zero-dependency set of interfaces and small value types
# (`AppPurchaseProvider`, the `core.ad` provider interfaces, `AdProviderConfig`,
# `AdUnitMapping`, `AdKitAdError`, `AdKitAdValue`) plus the `BillingConfig`
# service locator. Every one of them is resolved at compile time from the
# consumer's call sites — the "service locator" is a plain object holding a
# field, not a reflective lookup — so R8 needs no help to keep what is live.
