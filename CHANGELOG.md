# Braintree Android SDK Release Notes

## 2.3.0

* UnionPay Beta *Please note*: this API is in beta and subject to change
* Add support for fetching a customer's payment methods
* Return a `RateLimitException` when a merchant account is being rate limited

## 2.2.5

* Fixes
  * Update BraintreeHttpClient to support UTF-8 encoding (fixes [#85](https://github.com/braintree/braintree_android/issues/85))

## 2.2.4

* Update PayPalDataCollector to 3.1.1
* Fixes
  * Update device collector to 2.6.1 (fixes [#87](https://github.com/braintree/braintree_android/issues/87))
  * Fix crash when `BraintreeFragment` has not been attached to an `Activity`
* Features
  * Add `PaymentRequest#defaultFirst` option
  * Add support for Chrome Custom tabs when browser switching

## 2.2.3

* Fixes
  * Fix incorrect `groupId` of dependencies in pom file for 2.2.2

## 2.2.2

:rotating_light: The `groupId`s in this version's pom files are incorrect and dependencies will not resolve. Do not use. :rotating_light:

* Update `PaymentButton` styling when PayPal is the only visible option
* Features
  * Add client side overrides for payment methods in Drop-in and `PaymentButton` to `PaymentRequest`
  * Add support for non-USD currencies and non-US shipping addresses in Android Pay
  * Return email, billing address and shipping address as part of an `AndroidPayCardNonce` from Drop-in
* Fixes
  * Fix back button not doing anything in Drop-in after an Android Pay error is returned
  * Deprecate `DataCollector#collectDeviceData` and add new signature to prevent a NullPointerException when using a fragment that is not attached to an `Activity`

## 2.2.1

* Fixes
  * Fix support annotations being bundled in PayPalDataCollector jar

## 2.2.0

* Open source PayPal SDK
* Deprecate `PayPalOneTouchActivity` and remove from Android manifest
* Add Travis CI build
* Improve errors and manifest validation
* Features
  * Add `CardBuilder#cardholderName`
  * Add `PayPalRequest#billingAgreementDescription`
* Fixes
  * Fix back button not working in Drop-in after adding a payment method
  * Fix failure to return a payment method nonce after browser switch when the fragment was recreated.

## 2.1.2

* Update Google Play Services Wallet to 8.4.0
* Use `ENVIRONMENT_TEST` for Android Pay requests in sandbox
* Add `AndroidPay#isReadyToPay` method

## 2.1.1

* Demo app upgrades
* Update PayPal SDK to 2.4.3 (fixes [#67](https://github.com/braintree/braintree_android/issues/67))
* Update android-card-form to 2.1.1
* Update gradle to 2.8
* Update build tools to 23.0.2
* Features
  * Add support for fraud data collection in Drop-in
* Fixes
  * Add rule to suppress ProGuard warnings
  * Fix Drop-in crash
  * Fix NPE when there is no active network (fixes [#77](https://github.com/braintree/braintree_android/issues/77))

## 2.1.0

* Pay with Venmo
* `PaymentButton#newInstance` now accepts a container id to add `PaymentButton` to that container
* Android Pay assets
* Fixes
  * Add `onInflate` method for Android versions < 23
  * PayPal cancel events (fixes [#63](https://github.com/braintree/braintree_android/issues/63))

## 2.0.1

* Make support annotations an optional dependency
* Cache configuration to prevent unnecessary network requests
* Fixes
  * Fix BraintreeDataCollector as an optional dependency
  * Fix `PaymentRequest` crash when Google Play Services is not present

## 2.0.0

* Increase `minSdkVersion` to 15 (see [Platform Versions](http://developer.android.com/about/dashboards/index.html#Platform) for the current distribution of Android versions)
* Remove Gson dependency
* Replace `Braintree` class with headless `BraintreeFragment`
  * Move methods for creating payment methods from central `Braintree` class to their own classes e.g. `PayPal#authorizeAccount`, `Card#tokenize`
* Add support for Tokenization Keys in addition to Client Tokens
* Rename PaymentMethod to PaymentMethodNonce
* Rename BraintreeData module to BraintreeDataCollector
* Update PayPal
  * Remove [PayPal Android SDK](https://github.com/paypal/PayPal-Android-SDK) dependency
  * Replace in-app log in with browser based log in
  * Add support for PayPal billing agreements and one-time payments
* Convert `PaymentButton` class from a view to a fragment
* Create `PaymentRequest` class for specifying options in Drop-in and the `PaymentButton`
* Remove Venmo One Touch. To join the beta for Pay with Venmo, contact [Braintree Support](mailto:support@braintreepayments.com)
* Remove Coinbase
* Many additional structural and name changes. For more details, see the [migration guide](https://developers.braintreepayments.com/reference/general/client-sdk-migration/android/v2) and the [source code](https://github.com/braintree/braintree_android)

## 1.7.4

* Fixes
  * Increase minimum version of Google Play Services Wallet to 8.0.0 to prevent `VerifyError`

## 1.7.3

* Fixes
  * Fix Android Pay bug caused by shared state between Activities

## 1.7.2

* Update PayPal SDK to 2.11.1 (fixes [#48](https://github.com/braintree/braintree_android/issues/48))

## 1.7.1

* Fixes
  * Fix tokenization failure in Coinbase

## 1.7.0

* Update gradle plugin to 1.3.1
* Update build tools to 23.0.1
* Update `compileSdkVersion` and `targetSdkVersion` to 23
* Update PayPal SDK to 2.10.0
* Increase maximum version of Google Play Services to 9.0.0 ([#50](https://github.com/braintree/braintree_android/pull/50))
* Set compile options to use Java 7
* Features
  * Add support for Coinbase. *Please note:* this API is in beta and subject to change.
* Fixes
  * Fix rare crash when Braintree was recreated
  * Fix 3D Secure bug that prevented a card from being returned
  * Remove use of Apache library ([#43](https://github.com/braintree/braintree_android/issues/43))
  * Remove single line description limitation ([#45](https://github.com/braintree/braintree_android/issues/45))

## 1.6.5

* Update PayPal SDK to 2.9.10
* Fixes
  * Fix incorrect custom integration in demo app
  * Fix incorrect selected payment method in Drop-in after creating a new payment method
  * Fix `NoClassDefFoundError` crash in Drop-in

## 1.6.4

* Update PayPal SDK to 2.9.8
* Improvements
  * Follow Android convention around button and text casing in Drop-in
  * Update android-card-form to [2.0.1](https://github.com/braintree/android-card-form/blob/master/CHANGELOG.md#201)

## 1.6.3

* Improvements
  * BraintreeData can now be optionally excluded
* Fixes
  * Remove optional dependency from full jar

## 1.6.2

* Update PayPal SDK to 2.9.7
* Add support for additional PayPal scopes to `PaymentButton`
* Fixes
  * Return error instead of silently failing setup with bad client tokens
  * Fix `NoClassDefFoundError` in Drop-in caused by optional dependency

## 1.6.1

* Fixes
  * Fix `NoClassDefFoundError` in Drop-in and `PaymentButton` caused by optional dependency ([#34](https://github.com/braintree/braintree_android/issues/34))

## 1.6.0

* Update PayPal SDK to 2.9.6
* Update gradle plugin to 1.2.3
* Update build tools to 22.0.1
* Features
  * Add Android Pay support. *Please note:* this API is in beta and subject to change.
  * Add `Braintree#onActivityResult` method
  * Add support for additional PayPal scopes
    * A `List` of additional scopes may be passed to `Braintree#startPayWithPayPal`
    * `PayPalAccount#getBillingAddress` can be used to retrieve the billing address when the address scope is requested.

## 1.5.1

* Update PayPal SDK to 2.9.5
* Switch to OkHttp for Demo app
* Improvements
  * Add methods to persist state across rotations
* Fixes
  * Fix Demo app crash when `MainActivity` was destroyed ([#26](https://github.com/braintree/braintree_android/pull/26))
  * Fix NPE in Drop-in ([#30](https://github.com/braintree/braintree_android/issues/30))
  * Fix ProGuard support and add ProGuard rules ([#29](https://github.com/braintree/braintree_android/issues/29))
  * Fix Drop-in error handling for non-card errors

## 1.5.0

* Update PayPal SDK to 2.9.4
* Move `CardForm` to [separate repo](https://github.com/braintree/android-card-form)
* Deprecate `Braintree#getInstance` in favor of `Braintree#setup`
* Fixes
  * Remove metadata from assets, fixes [#16](https://github.com/braintree/braintree_android/issues/16)

## 1.4.0

* Update gradle plugin to 1.1.2
* Update build tools to 22
* Update `compileSdkVersion` and `targetSdkVersion` to 22
* Update PayPal SDK to 2.9.0
* Features
  * Add support for 3D Secure. *Please note:* this API is in beta and subject to change.
* Fixes
  * Fix missing expiration date float label (#21)

## 1.3.0

* Remove Drop-In support for Eclipse
* Open source [card form](CardForm) separate from Drop-In
* Update PayPal SDK to 2.8.5
  * card.io is no longer included in the SDK
* Update Espresso to 2.0
* Remove unused PayPal `PROFILE` scope

## 1.2.7

* Update gradle plugin to 0.14.1
* Update build tools to 21.1.1
* Update PayPal SDK to 2.7.3
* Remove `android:allowBackup="false"` from library manifests, apps will now be able to choose if they allow backups
* Remove `ACCESS_WIFI_STATE` permission
* Improvements
  * Add localizations for more locales (da-rDK, en-rAU, es, fr-rCA, iw-rIL, nl, no, pl, pt, ru, sv-rSE, tr, zh-rCN)
  * Add initial right to left language support
  * Add type safety to `Braintree#addListener(Listener)`. Thanks @adstro!

## 1.2.6

* Increase `targetSdkVersion` to 21
* Increase `buildToolsVersion` to 21.0.2
* Fixes
  * Fix max length on `EditText`s
  * Fix crash caused by `PRNGFixes`
* Improvements
  * Update PayPal SDK
  * Add first and last name to `CardBuilder`

## 1.2.5

* Fixes
  * Fix incorrectly named language resource directories

## 1.2.4

* Fixes
  * Work around manifest merging issues on newer build plugins

## 1.2.3

* minSdk is now 10
* Fixes
  * Set max length on card field for unknown card types in Drop-In
  * Update PayPal SDK to fix rotation bug
  * Fix edge cases in expiration entry in Drop-In
* Improvements
  * Error messages are now returned from Drop-In
  * Drop-In auto advances to next field now

## 1.2.2

* Fixes
  * Fix crash caused by too large request code in `PaymentButton`
  * Resume the payment method form after rotation
* Improvements
  * Updated PayPal SDK
    * email scope is now requested in all PayPal requests
  * `correlationId` is now included in the device data string returned from `BraintreeData#collectDeviceData`

## 1.2.1

* Fixes
  * BraintreeApi release now includes the PayPal SDK again. Sorry!
* Improvements
  * All assets are now namespaced to avoid any conflicts on import.
  * Updated PayPal SDK

## 1.2.0

* Features
  * App switch based payments for PayPal and Venmo (One Touch)
    * No changes for existing Pay With PayPal integrations
    * See [the docs](https://developers.braintreepayments.com/android/guides/one-touch) for more information
  * Unified payment button (`PaymentButton`) for PayPal and/or Venmo payments
* Improvements
  * Minor bugfixes and internal tweaks
* Deprecations
  * `PayPalButton` is deprecated in favor of `PaymentButton`

## 1.1.0

* Breaking Change
  * BraintreeData returns `deviceData` instead of `deviceSessionId` on `collectDeviceData`
* Improvements
  * References `sdk-manager-plugin` from vendor to simplify build process

## 1.0.8

* Improvements
  * CardBuilder now accepts billing address fields other than postal code (credit: @chiuki)
* Packaging
  * Fixed an issue building Drop-In in Eclipse

## 1.0.7

* Improvements
  * BraintreeApi no longer depends on OkHttp
  * Added localizations for more locales (UK, FR, DE, IT)

## 1.0.6

* Fixes
  * Fixed disabled submit button in landscape
  * Fixed next field button in landscape
  * Add max length to expiration date and prevent user from typing illegal characters
* Move to sdk-manager-plugin for CI dependencies

## 1.0.5

* Packaging
  * Set Braintree package to default to AAR instead of ZIP

## 1.0.4 - Gradle and Maven will incorrectly download the ZIP instead of AAR, use 1.0.5+

* Improvements
  * Added assets for a wider range of resolutions
  * Enforce maximum length for card and postal code fields
  * Added README for fraud tools
* Packaging
  * Improvements for usage in environments other than Maven or Gradle
* Fixes
  * Fixed lint errors

## 1.0.3

* Fixes
  * Fix crash on Android SDK < 19
* Add PayPal `correlationId` to PayPal account creation

## 1.0.2 - crash on Android SDK < 19, do not use

* Fixes
  * Improved packaging for non-Gradle uses of SDK

## 1.0.1

* Fixes
  * Attach Javadocs and sources to Maven Central build.

## 1.0.0

* Public release of [v.zero](https://www.braintreepayments.com/v.zero) SDK

