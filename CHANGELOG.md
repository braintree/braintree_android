# Braintree Android SDK Release Notes

## unreleased

* Breaking Changes
  * Rename `core` module to `shared-utils`
  * Rename `braintree` module to `braintree-core`
  * Create `american-express` module
  * Create `card` module
  * Create `local-payment` module
  * Create `pay-pal` module
  * Create `union-pay` module
  * Create `venmo` module
  * Delete PayPal `data-collector` module
  * Delete PayPal `paypal-one-touch` module

## 3.17.0

* Add `bic` (Bank Identification Code) to `LocalPaymentRequest`

## 3.16.1

* Bump Cardinal version to `2.2.5-1`

## 3.16.0

* Add `accountType` to `ThreeDSecureRequest`
* Add `offerPayLater` to `PayPalRequest`

## 3.15.0

* Add `paymentTypeCountryCode` to `LocalPaymentRequest`
* Upgrade PayPal Data Collector to 5.1.1 (fixes #325)

## 3.14.2

* Bump Cardinal version to `2.2.4-1` (fixes [#305](https://github.com/braintree/braintree_android/issues/305))
* Bump `browser-switch` to `1.1.3`

## 3.14.1

* Update `compileSdkVersion` and `targetSdkVersion` to 30

## 3.14.0

* Expose cardholder name on `CardNonce`.
* Expose expiration month and year on `CardNonce`.
* Update `browser-switch` module to `1.1.0`.
* Fix bug where `onError` callback was invoked instead of the `onCancel` callback in the Local Payment Method flow (fixes #299, thanks @vijayantil1)
* Fix bug where `getReturnUrlScheme` is called and an Activity is no longer attached to the fragment (fixes [#308](https://github.com/braintree/braintree_android/issues/308), thanks @hakanbagci)

## 3.13.0

* Update `browser-switch` module to `1.0.0`
* Make PayPalUAT::Environment enum public
* Add Card#tokenize overload to allow a custom `PaymentMethodNonceCallback` to be provided.

## 3.12.0

* Changed `Configuration#isPayPalEnabled` to no longer consider deprecated integration requirements.

## 3.11.1

* Add default `uiCustomization` to `ThreeDSecureRequest` to prevent null pointer exception when interacting with Cardinal SDK

## 3.11.0

* Bump Cardinal version to 2.2.3-2
* Check if Fragment is active before handling Pay with PayPal result (fixes #295, thanks @brudaswen)

## 3.10.0

* Allow new BraintreeFragment instances to be created using FragmentActivity
* Add support for authorizing the Braintree SDK with a `PayPalUAT` (universal access token)
* Fix bug that accepted raw JSON string as valid authorization to `BraintreeFragment.newInstance(...)`
* Add `threeDSecureAuthenticationId` field to `ThreeDSecureInfo`
* Update `braintree-android-google-payment` module to `3.3.1`

## 3.9.0

* Update Cardinal SDK to `2.2.2-1`
* Fix bug in 3DS1 browser switch around accented characters in the redirect button and description (fixes #288)

## 3.8.0

* Add support for basic UI customization of 3DS1 flows. See `ThreeDSecureV1UiCustomization`.

## 3.7.2

* Update Cardinal SDK to `2.2.1-2`
* Use `synchronized` when adding to callback queue in `BraintreeFragment` (thanks @skauss)
* Update paypal-data-collector to 4.1.2

## 3.7.1

* Update `braintree-android-google-payment` module to `3.1.0`
* Fix a bug so that `BraintreeFragment.newInstance` returns a new fragment whenever a new authorization string is passed in (Resolves issue #274. Thanks @krunk4ever and @bramley-stride.)

## 3.7.0

* Update 3DS `prepareLookup` method to function asynchronously to wait for Cardinal SDK
* Add ability to request `AuthenticationInsight` when tokenizing a credit card, which can be used to make a decision about whether to perform 3D Secure verification
* Set error message on `ThreeDSecureInfo` when 3D Secure 2.0 challenge fails
* Include reference to Cardinal's docs for `uiCustomization` property on `ThreeDSecureRequest`.
* Add `requiresUserAuthentication` method to `ThreeDSecureLookup`
* Add support for `PayPalLineItem`

## 3.6.0

* Add authentication and lookup transaction status information to ThreeDSecureInfo
* Add ability to customize UI for 3D Secure challenge views
* Fix race condition that caused inconsistent 3DS version flows

## 3.5.0

* Add 3DSecure authentication details to card nonce

## 3.4.2

* Add `acsTransactionId`, `threeDSecureServerTransactionId` and `paresStatus` fields to `ThreeDSecureInfo`

## 3.4.1

* Update Cardinal SDK to 2.1.4-1

## 3.4.0

* Send analytics timestamps in milliseconds
* Add additional fields to ThreeDSecureInfo
* Fix potential crash when 3DSecure 2.0 JWT is not available

## 3.3.0

* Correctly includes the 3DSecure 2.0 module

## 3.2.0

* Add 3DS 2 Support
* Update 3DS redirect to newest version

## 3.1.0

* BraintreeFragment can now attach to a Fragment (fixes [#252](https://github.com/braintree/braintree_android/issues/252))

## 3.0.1

* Update google-payment to 3.0.1
* Update endpoint for creating local payments

## 3.0.0

* Bump minSdkVersion to 21
* Convert to AndroidX
* BraintreeFragment moves to the support fragment
  * Requires AppCompatActivity to attach the BraintreeFragment to
* Removed Visa Checkout 1.0.0 as a dependency
  * Add Visa Checkout's dependency to your app to get the latest version
* Removed deprecated ThreeDSecureWebView flow
* Removed deprecated Venmo#isVenmoWhitelisted(ContentResolver)
* Removed deprecated method from PostalAddress
* Removed deprecated country setters
* Removed deprecated methods from DataCollector
* Removed deprecated PayPalOneTouchActivity
* Removed deprecated Ideal
* Rename AndroidPay classes to GooglePayment



## 2.21.0

* Deprecate PayPal Future Payments, use PayPal Billing Agreements
* Deprecate AndroidPayConfiguration, use the GooglePaymentConfiguration alias

## 2.20.1

* Fix null address properties on PayPalAccountNonce
  * Those addresses should always be at least an empty PostalAddress

## 2.20.0

* Google Pay
  * Add groundwork for v2 compatibility
* Split PostalAddress into PostalAddress and PostalAddressParser
  * Deprecates PostalAddress.fromJson - use PostalAddressParser.fromJson
  * Add fromUserAddressJsonn to PostalAddressParser
  * Add additional fields

## 2.19.0

* Move Google Payment to a separate module
* Downgrade browser-switch to 0.1.6
* Exclude customtabs from browser-switch dependency

## 2.18.1

* Upgrade browser-switch to 0.1.7 fixes Chrome Custom Tab integration when using Jetifier to use AndroidX

## 2.18.0

* Upgrade Android SDK to 28
* Fix PayPal JavaDoc

## 2.17.0

* Local Payments
* Upgrade PayPal Data Collector to 4.0.3

## 2.16.0

* Add optional merchant account Id to PayPalRequest
* Add openVenmoAppPageInGooglePlay method which opens Venmo on the Google Play

## 2.15.2

* Fix NoClassDefFoundError compile error for PayPalDataCollector

## 2.15.1

* Fix InvalidPathException error

## 2.15.0

* Add `PaymentMethod#deletePaymentMethod` which allows customers to remove their vaulted payment methods
* Fix DataCollector not being available for instant run builds

## 2.14.2

* Fix issue with TLS cipher in API < 21

## 2.14.1

* Removed unused PayPal analytics event

## 2.14.0

* Add shippingAddressEditable to PayPalRequest

## 2.13.2

* Fix issue where address override was not set for PayPal billing agreements

## 2.13.1

* Update 3D Secure redirect URL

## 2.13.0

* 3D Secure
  * Add support for American Express SafeKey params
* Update PayPalDataCollector library to 3.1.6
* Catch possible SQLite exceptions

## 2.12.0

* Move Visa Checkout to separate module
* Update Visa Checkout to 5.5.2
* Update SDK to 27

## 2.11.0

* Add support for Venmo profiles
* Update PayPalDataCollector library to 3.1.5

## 2.10.0

* Update GooglePaymentException to be parcelable
* Add browser switch support to 3D Secure integrations

## 2.9.0

* Internal performance optimizations
* Deprecate `countryName`, `countryCodeAlpha2`, `countryCodeAlpha3`, and `countryCodeNumeric` in favor of `countryCode` in `CardBuilder` and `UnionPayCardBuilder`.

## 2.8.1

* Support `lastFour` in `GooglePayCardNonce`
* Add Google Pay branding

## 2.8.0

* Add support for iDEAL payments

## 2.7.3

* Check package name is valid for PayPal Wallet switch

## 2.7.2

* Fix phoneNumberRequired in GooglePayment

## 2.7.1

* Add AmericanExpress support with getRewardsBalance method
* Use ExecutorService for async database operations

## 2.7.0

* Increase minimum version of Google Play Services Wallet to 11.4.0
* Add support for the Google Payments API
* Deprecate Android Pay
* Add additional billing address params to `CardBuilder`

## 2.6.2

* Fix potential crash due to optional Visa Checkout dependency

## 2.6.1

* Fix potential crash due to optional Google Play Services dependency

## 2.6.0

* Upgrade Kount DataCollector to 3.2
* Stop using dependency ranges (https://github.com/braintree/android-card-form/pull/29)
* Relax `PRNGFixes` check for `PRNGSecureRandomProvider` to prevent race condition with other providers ([#151](https://github.com/braintree/braintree_android/issues/151))
* Stop sending `Content-Type` header for GET requests ([#155](https://github.com/braintree/braintree_android/issues/155))
* Upgrade browser-switch to 0.1.4 to prevent losing Chrome Custom Tab when switching to a password manager or other app
* Add additional bin data to card based payment methods
* Add DOM and database storage to `ThreeDSecureWebView` to improve compatibility with some bank web pages ([#159](https://github.com/braintree/braintree_android/pull/159))
* Update compile and target SDK versions to 26
  * Any support library dependencies must now be 26.0.0 or newer

## 2.5.4

* Use custom task instead of overriding the clean task (fixes [#153](https://github.com/braintree/braintree_android/issues/153))
* Accept third party cookies in ThreeDSecureWebView for Lollipop and above

## 2.5.3

* Add PayPal Credit for Billing Agreements

## 2.5.2

* Include cause in `ConfigurationException` ([#143](https://github.com/braintree/braintree_android/pull/143))
* Ignore ProGuard warnings for Visa Checkout (fixes [#144](https://github.com/braintree/braintree_android/issues/144))
* Fix Android Pay behavior during configuration changes (fixes [#145](https://github.com/braintree/braintree_android/issues/145), [#146](https://github.com/braintree/braintree_android/issues/146), [#147](https://github.com/braintree/braintree_android/issues/147))
* Fix crash when run in an Android Instant App

## 2.5.1

* Fix non-optional `data-collector` dependency in Braintree
* Create `BraintreeRequestCodes` for use with `BraintreeCancelListener#onCancel`
* Move PayPal browser switches to use [browser-switch-android](https://github.com/braintree/browser-switch-android)

## 2.5.0

* Add option to set display name in `PayPalRequest`
* Add option to set landing page type in `PayPalRequest`
* Add option to enable PayPal Credit in `PayPalRequest`
* Add Visa Checkout as a payment method
* Prevent dependency resolution of alpha major versions of support libraries

## 2.4.3

* Improve `GoogleApiClientException` to include error type as well as reason code
* Changes to PayPalDataCollector to make it easier to use

## 2.4.2

* Fix NPE in `AndroidPay#changePaymentMethod` (fixes [#139](https://github.com/braintree/braintree_android/issues/139))
* `Venmo#authorizeAccount` will now correctly vault the payment method when the vault option is true
* Fix missing client metadata ids in `PayPalAccountNonce`s
* Update paypal-data-collector to 3.1.4

## 2.4.1

* Add workaround for [Kount/kount-android-sdk#2](https://github.com/Kount/kount-android-sdk/issues/2)
* Fix error returned by `AndroidPay#changePaymentMethod`

## 2.4.0

* Fix back button during PayPal browser switch on Samsung devices (fixes [#137](https://github.com/braintree/braintree_android/issues/137))
* Add new intent option to `PayPalRequest`
* Fix crash when excluding the PayPal dependency
* Increase `minSdkVersion` to 16
  * API 16 is the first version of Android that supports TLSv1.2. For more information on Braintree's upgrade to TLSv1.2 see [the blog post](https://www.braintreepayments.com/blog/updating-your-production-environment-to-support-tlsv1-2/).

## 2.3.12

* Improve PayPal address validations
* Work around `NullPointerException` in `BraintreeFragment#newInstance` (fixes [#125](https://github.com/braintree/braintree_android/issues/125))
* Document supported locales for PayPal
* Fix rare `NullPointerException` ([#128](https://github.com/braintree/braintree_android/pull/128))

## 2.3.11

* Fix ProGuard rules (fixes [#124](https://github.com/braintree/braintree_android/issues/124))
* Fix `NullPointerException` when using deprecated DataCollector methods
* Update compile and target SDK versions to 25

## 2.3.10

* Add `BraintreeFragment#getListeners` to get a list of all the registered listeners
* Upgrade paypal-data-collector to 3.1.3
* Upgrade Kount DataCollector to 3.1
* Add `AndroidPay#requestAndroidPay` and `AndroidPay#changePaymentMethod` methods to simplify requesting Android Pay from a user and changing the backing payment method.
* Include ProGuard directives in the SDK ([#120](https://github.com/braintree/braintree_android/pull/120))
* Work around bug in `JSONObject#optString`
* Use `FragmentTransaction#commitNow` and `FragmentManager#executePendingTransactions` in `BraintreeFragment#newInstance` to synchronously set up `BraintreeFragment` and avoid race conditions caused by asynchronous `Fragment` setup.

## 2.3.9

* Update `AndroidPayCardNonce` description to include card type and last 4

## 2.3.8

* Support changing user call to action in PayPal flows, see `PayPalRequest#userAction`
* Fix validate option not being sent when set to false in `PaymentMethodBuilder`
* Add merchant supported card types to `Configuration`
* Expose methods on `BraintreeFragment` for getting cached payment methods
* Update `paypal-data-collector` to 3.1.2
* Move Drop-In to [it's own repo](https://github.com/braintree/braintree-android-drop-in)

## 2.3.7

* Update exception message when Android Manifest setup is invalid
* Fix unclosed `InputStream` (fixes [#115](https://github.com/braintree/braintree_android/issues/115))
* Post exception to error listener instead of throwing `IllegalStateException` when `BraintreeFragment` is not attached to an `Activity`
* Restore url when `BraintreeFragment` is recreated (fixes [#117](https://github.com/braintree/braintree_android/issues/117))
* Upgrade gradle build tools to 2.1.3
* Parse and return errors when Android Pay tokenization fails
* Add support for changing the backing card for Android Pay in Drop-In
* Call configuration callback whenever a new Activity is attached to `BraintreeFragment`

## 2.3.6

* Allow vaulting of Venmo accounts. See `Venmo#authorizeAccount`.
* Remove Venmo whitelist check
* Fix `BraintreeCancelListener#onCancel` being invoked twice for PayPal cancellations (fixes [#112](https://github.com/braintree/braintree_android/issues/112))

## 2.3.5

* Change `UnionPayCallback` to include `smsCodeRequired`
* Change `UnionPayCapabilities#isUnionPayEnrollmentRequired` to `UnionPayCapabilities#isSupported`
* Upgrade Google Play Services to [9.0.0,10.0.0)
* Upgrade support annotations to [24.0.0,25.0.0)
* Upgrade build tools to 24.0.0
* Update compile and target API versions to 24
* Fix `NullPointerException` in `AnalyticsIntentService`

## 2.3.4

* Prevent invalid schemes from being used for browser switching (Packages containing underscores would generate invalid schemes)
* Fix `NoClassDefFoundError` in `DataCollector`
* Fix `NullPointerException` in `BraintreeFragment`

## 2.3.3

* Add PayPal Checkout intent option (authorize or sale). See `PayPalRequest#intent`
* Update UnionPay support in demo app custom integration
* Update `android-card-form` to 2.3.1
* Fix `NullPointerException` in `AddPaymentMethodViewController` (fixes [#100](https://github.com/braintree/braintree_android/issues/100))
* Fix `IllegalStateException` when creating a `BraintreeFragment` (fixes [#104](https://github.com/braintree/braintree_android/issues/104))
* Fix `NullPointerException` when `BraintreeFragment` is not attached to an `Activity` (fixes [#105](https://github.com/braintree/braintree_android/issues/105))

## 2.3.2

* Fix `NullPointerException` when handling a PayPal response (fixes [#101](https://github.com/braintree/braintree_android/issues/101))

## 2.3.1

* Fix `NullPointerException`s in `BraintreeFragment` when not attached to an `Activity`
* Fix Chrome Custom Tabs Intent flags interfering with browser switch
* Add new `DataCollector#collectDeviceData` methods that use a callback; deprecate synchronous methods
* Reduce size of assets in Drop-In

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
