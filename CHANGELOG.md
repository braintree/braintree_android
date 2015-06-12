# Braintree Android SDK Release Notes

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

