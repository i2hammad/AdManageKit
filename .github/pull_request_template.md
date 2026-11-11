## Summary

<!-- What does this PR change, and why? Link related issues (e.g. "Fixes #123"). -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking API change
- [ ] Documentation / samples
- [ ] Build / CI

## Affected module(s)

- [ ] AdManageKit
- [ ] admanagekit-core
- [ ] admanagekit-billing
- [ ] admanagekit-compose
- [ ] admanagekit-yandex
- [ ] app (sample)

## Checklist

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] Callback contracts preserved (`onNextAction()` fires exactly once; waterfall shows deliver one terminal event)
- [ ] Purchased users still get no ads (error 1001, no retry)
- [ ] New `AdManageKitConfig` fields are covered by `resetToDefaults()`
- [ ] Tests added/updated for the change
- [ ] Docs / README / CHANGELOG updated if the public API changed

## How was this tested?

<!-- Unit tests, sample app on a device, ad formats exercised, etc. -->
