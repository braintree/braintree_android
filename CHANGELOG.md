# Braintree Android SDK Release Notes

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

