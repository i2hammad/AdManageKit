# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of AdManageKit. Please upgrade to the newest release before reporting an issue.

| Version | Supported          |
| ------- | ------------------ |
| 4.4.x   | :white_check_mark: |
| < 4.4   | :x:                |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

Report them privately through GitHub's [private vulnerability reporting](https://github.com/i2hammad/AdManageKit/security/advisories/new) ("Report a vulnerability" on the repository's **Security** tab).

Please include as much of the following as you can:

- Affected module(s) (`AdManageKit`, `admanagekit-core`, `admanagekit-billing`, `admanagekit-compose`, `admanagekit-yandex`) and version
- The type of issue (e.g. purchase/entitlement bypass, exposed data, unsafe intent handling)
- Steps to reproduce, or a proof-of-concept
- The impact you believe the issue has

## What to Expect

- An acknowledgement of your report within **7 days**
- An assessment and, where confirmed, a fix plan within **30 days**
- Credit in the release notes once the fix ships, unless you prefer to remain anonymous

## Scope

In scope: code in this repository and the artifacts published from it via JitPack.

Out of scope: vulnerabilities in third-party SDKs (Google Mobile Ads, Google Play Billing, UMP, Yandex Mobile Ads, Firebase) — please report those to their respective vendors.
