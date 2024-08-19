# Braintree Android SDK Release Notes

## unreleased
* All Modules
  * Android 15 Support
    * Upgrade `compileSdkVersion` and `targetSdkVersion` to API 35
* BraintreeCore
  * Update `endpoint` syntax sent to FPTI for 3D Secure and Venmo flows
* ThreeDSecure
  * Update `ThreeDSecureActivity` theme attributes to prevent the Action Bar title from displaying and enforce transparency properly with AppCompat theme attributes
* Breaking Changes
    * PayPal
        * Remove `appLinkEnabled` from `PayPalRequest` as Android app links are now required
        * Update `PayPalCreditFinancing.hasPayerAcceptance()` to `getHasPayerAcceptance()` (Java)
        * Change `PayPalPaymentIntent` to an enum
        * Change `PayPalLandingPageType` to an enum
        * Change `PayPalPaymentUserAction` to an enum
        * Update `PayPalRequest.hasUserLocationConsent()` to `getHasUserLocationConsent()` (Java)
        * Change `PayPalLineItem.desc()` to `setDescription()`
        * Change `PayPalLineItemKind` to an enum
        * Rename `PayPalLineItemKind.KIND_CREDIT` to `CREDIT`
        * Rename `PayPalLineItemKind.KIND_DEBIT` to `DEBIT`

## 5.0.0-beta1 (2024-07-23)

* Breaking Changes
    * All Modules
        * Bump `minSdkVersion` to API 23
        * Bump target Java version to Java 11
        * Upgrade Kotlin version to 1.9.10
        * Upgrade to Android Gradle Plugin 8
    * BraintreeCore
        * Update package name to `com.braintreepayments.api.core`
        * Remove `BraintreeClient` public constructors
        * Remove `deliverBrowserSwitchResult` and `deliverBrowserSwitchResultFromNewTask` from `BraintreeClient`
        * Remove `ClientTokenProvider`
        * Update payment method constructor parameters from `braintreeClient` to `context` and
          `authorization`
        * Remove `BraintreeSharedPreferencesException`
        * Convert `PostalAddress` to data class
        * Remove `open` modifier on `Configuration`
        * Remove `UserCanceledException.isExplicitCancelation`
    * UnionPay
        * Remove `union-pay` module
            * UnionPay cards can now be processed as regular cards (through the `card` module) due to their partnership with Discover
    * BraintreeDataCollector
        * Update package name to `com.braintreepayments.api.datacollector`
        * Replace `DataCollector#collectDeviceData(context, merchantId, callback)` with
          `DataCollector#collectDeviceData(context, riskCorrelationId, callback)`
        * Add `DataCollectorResult` and update `DataCollectorCallback` parameters
    * PayPalDataCollector
        * Remove `paypal-data-collector` module (use `data-collector`)
    * Venmo
        * Update package name to `com.braintreepayments.api.venmo`
        * Remove `setFallbackToWeb()` from `VenmoRequest` - fallback to web is default behavior now
        * Remove `VenmoListener`, `VenmoTokenizeAccountCallback`, and `VenmoResultCallback`
        * Add `VenmoLauncher`, `VenmoPaymentAuthRequest`, `VenmoPaymentAuthRequestCallback`,
          `VenmoPaymentAuthResult`, `VenmoResult`, `VenmoTokenizeCallback`, and
          `VenmoLauncherCallback`
        * Rename `VenmoOnActivityResultCallback` to `VenmoResultCallback`
        * Remove overload constructors, `setListener`, and `onActivityResult` from `VenmoClient`
        * Change `VenmoClient#tokenizeVenmoAccount` parameters and rename to
          `VenmoClient#tokenize`
        * Remove `VenmoIsReadyToPayCallback`
        * Add `VenmoClient#createPaymentAuthRequest`
        * Move `showVenmoInGooglePlayStore` to `VenmoLauncher`
        * Remove `isVenmoAppSwitchAvailable` and `isReadyToPay` (no longer required as Venmo flow will fallback to web if app is not installed)
    * GooglePay
        * Update package name to `com.braintreepayments.api.googlepay`
        * Remove `GooglePayListener` and `GooglePayRequestPaymentCallback`
        * Add `GooglePayLauncher`, `GooglePayPaymentAuthRequest`,
          `GooglePayPaymentAuthRequestCallback`, `GooglePayPaymentAuthResult`,
          `GooglePayTokenizeCallback`, `GooglePayTokenizationParameters` and `GooglePayLauncherCallback`
        * Remove overload constructors, `setListener, and `onActivityResult` from `GooglePayClient`
        * Change `GooglePayClient#requestPayment` parameters and rename to
          `GooglePayClient#createPaymentAuthRequest`
        * Change `GooglePayClient#isReadyToPay` and `GooglePayIsReadyToPayCallback` parameters
        * Add `GooglePayClient#tokenize`
        * Remove `merchantId` from `GooglePayRequest`
        * Change `GooglePayGetTokenizationParametersCallback` parameters
        * Rename `GooglePayLauncherCallback#onResult` to
          `GooglePayLauncherCallback#onGooglePayLauncherResult`
    * ThreeDSecure
        * Remove `ThreeDSecureListener`
        * Add `ThreeDSecureLauncher`, `ThreeDSecurePaymentAuthResult`,
          `ThreeDSecureTokenizeCallback`, `ThreeDSecurePaymentAuthRequest`,
          `ThreeDSecurePaymentAuthRequestCallback`, `ThreeDSecurePrepareLookupResult`,
          `ThreeDSecurePrepareLookupCallback`, and `ThreeDSecureLancherCallback`
        * Remove overload constructors, `setListener`, `continuePerformVerification`, `onBrowserSwitchResult` and `onActivityResult` from `ThreeDSecureClient`
        * Change `ThreeDSecureClient#initializeChallengeWithLookupResponse` parameters
        * Convert `ThreeDSecureResult` into sealed class
        * Add `ThreeDSecureClient#tokenize`
        * Rename `ThreeDSecureClient#performVerification` to
          `ThreeDSecureClient#createPaymentAuthRequest` and change parameters
        * Remove `versionRequested` from `ThreeDSecureRequest`
        * Add `ThreeDSecureNonce` class
        * Rename `ThreeDSecureResult#tokenizedCard` to `ThreeDSecureResult#threeDSecureNonce`
        * Remove `ThreeDSecureV1UICustomization`
        * Remove `versionRequesed` from `ThreeDSecureRequest` as version 1 is no longer supported
        * Remove `ThreeDSecureV2BaseCustomization`
        * Remove `CardinalValidateReceiver` from `ThreeDSecureActivity`
        * Make empty `ThreeDSecureLookup` constructor package-private
    * PayPal
        * Update package name to `com.braintreepayments.api.paypal`
        * Require Android App Links to return to merchant app from PayPal flows
        * Remove `PayPalListener`
        * Add `PayPalLauncher`, `PayPalPaymentAuthRequest`, `PayPalPendingRequest`, `PayPalPaymentAuthResult`,
          `PayPalPaymentAuthCallback`, `PayPalTokenizeCallback`, and `PayPalResult`
        * Remove`PayPalFlowStartedCallback`
        * Remove overload constructors, `setListener`, `parseBrowserSwitchResult`,
          `clearActiveBrowserSwitchResult`, `requestOneTimePayment`, and `requestBillingAgreement` from
          `PayPalClient`
        * Rename `PayPalClient#tokenizePayPalAccount` to `PayPalClient#createPaymentAuthRequest` and
          change parameters
        * Rename `PayPalClient#onBrowserSwitchResult` to `PayPalCient#tokenize` and change parameters
    * LocalPayment
        * Remove `LocalPaymentListener`
        * Add `LocalPaymentLauncher`, `LocalPaymentPendingRequest`, `LocalPaymentTokenizeCallback`,
          `LocalPaymentAuthRequest`, `LocalPaymentAuthRequestCallback` and `LocalPaymentAuthResult`
        * Change `LocalPaymentResult` type
        * Remove overload constructors, `setListener`, `parseBrowserSwitchResult`,
          `clearActiveBrowserSwitchResult`, `approveLocalPayment`, and `approvePayment` from
          `LocalPaymentClient`
        * Rename `LocalPaymentClient#startPayment` to `LocalPaymentClient#creatPaymentAuthRequest`
          and change parameters
        * Rename `LocalPaymentClient#onBrowserSwithResult` to `LocalPaymentClient#tokenize` and
          change parameters
        * Update package name to `com.braintreepayments.api.localpayment`
    * Card
        * Update package name to `com.braintreepayments.api.card`
        * Remove `threeDSecureInfo` from `CardNonce`
        * Move `ThreeDSecureInfo` to `three-d-secure` module
        * Add `CardResult` object
        * Change `CardTokenizeCallback` parameters
    * SEPA Direct Debit
        * Update package name to `com.braintreepayments.api.sepadirectdebit`
        * Remove `SEPADirectDebitLifecycleObserver` and `SEPADirectDebitListener`
        * Add `SEPADirectDebitLauncher`, `SEPADirectDebitPendingRequest`,
          `SEPADirectDebitPaymentAuthRequestCallback`, `SEPADirectDebitPaymentAuthRequest`,
          `SEPADirectDebitResult`, `SEPADirectDebitPaymentAuthRequestParams` and
          `SEPADirectDebitTokenizeCallback`
        * Remove Fragment or Activity requirement from `SEPADirectDebitClient` constructor
        * Replace `SEPADirectDebitClient#onBrowserSwitchResult` with `SEPADirectDebitClient#tokenize` and
          modify parameters
        * Replace `SEPADirectDebitClient#tokenize` with`SEPADirectDebitClient#createPaymentAuthRequest`
          and modify parameters
        * Rename `SEPADirectDebitPaymentAuthRequestCallback#onResult` to
          `SEPADirectDebitPaymentAuthRequestCallback#onSEPADirectDebitPaymentAuthResult`
    * Visa Checkout
        * Visa checkout is not yet available for v5
    * American Express
        * Update package name to `com.braintreepayments.api.americanexpress`
        * Change parameters of `AmericanExpressGetRewardsBalanceCallback`
        * Add `AmericanExpressResult`
    * Samsung Pay
        * Remove entire Samsung Pay module
    * PayPal Native Checkout
        * Remove entire PayPal Native Checkout module
    * SharedUtils
        * Update package name to `com.braintreepayments.api.sharedutils`
    * PayPal Messaging (BETA) 
        * Remove `BraintreeClient` from constructor
        * Update package name to `com.braintreepayments.api.paypalmessaging`
    * Shopper Insights (BETA)
        * Remove `BraintreeClient` from constructor
        * Update package name to `com.braintreepayments.api.shopperinsights`

## 4.49.1 (2024-07-15)

* PayPal
    * Fix issue that causes a JSON parsing error when Pay Later is selected during checkout.
* ShopperInsights (BETA)
    * Add error when using an invalid authorization type

## 4.49.0 (2024-07-08)

* PayPalNativeCheckout (DEPRECATED)
    * **NOTE:** This module is being deprecated and will be removed in the future version of the SDK
    * Add deprecated warning message to all public classes and methods
* ThreeDSecure
    * Add customFields param to ThreeDSecureRequest

## 4.48.0 (2024-07-02)

* PayPal
    * Fix `PayPalAccountNonce` Null Pointer Exception by ensuring that all `@NonNull` values are initialized with a non-null value.
* PayPalNativeCheckout
    * Fix `PayPalNativeCheckoutAccountNonce` Null Pointer Exception by ensuring that all `@NonNull` values are initialized with a non-null value.
* BraintreeCore
    * Use TLS 1.3 for all HTTP requests, when available
    * Refactor TLSCertificatePinning `certInputStream` property to initialize a `ByteArrayInputStream` once instead of every time the property is accessed.
* ThreeDSecure
    * Move Cardinal cleanup from SDK internals into `ThreeDSecureActivity`.
  
## 4.47.0 (2024-06-06)

* BraintreeCore
  * Add `appLinkReturnUri` to `BraintreeClient` constructors for Android App Link support (for PayPal web flows only)
  * Bump `browser-switch` version to `2.7.0`
* PayPal
  * Add `appLinkEnabled` property to `PayPalRequest` for Android App Link support
  * Add optional property `PayPalCheckoutRequest.setUserAuthenticationEmail()`
* ShopperInsights (BETA)
  * Requires opt in - `@OptIn(ExperimentalBetaApi::class)`
  * Add `ShopperInsightsClient.getRecommendedPaymentMethods()` for returning recommendations based on the buyer
* ThreeDSecure
  * Fix issue that causes a black screen to display after successful 3DS validation.
* Venmo
  * Send `link_type` in `event_params` to PayPal's analytics service (FPTI)

## 4.46.0 (2024-05-30)

* PayPalMessaging (BETA)
    * Add `PayPalMessagingRequest`, `PayPalMessagingColor`, `PayPalMessagingLogoType`, `PayPalMessagingOfferType`, `PayPalMessagingPageType`, `PayPalMessagingTextAlignment`, and `PayPalMessagingListener`
    * Add `PayPalMessagingView(BraintreeClient, Context)` to display PayPal messages to promote offers such as Pay Later and PayPal Credit to customers.
        * To get started call `PayPalMessagingView#start()` with an optional `PayPalMessagingRequest`

## 4.45.1 (2024-05-28)

* PayPal
    * Update `PayPalInternalClient` to use pairing ID as client metadata ID by default.
    * Send `is_vault` in `event_params` analytics
* Venmo
    * Send `link_type` and `is_vault` in `event_params` analytics
  
## 4.45.0 (2024-04-16)

* BraintreeCore
  * Updated expiring pinned vendor SSL certificates
* GooglePay
  * Add `GooglePayClient#tokenize(PaymentData, GooglePayOnActivityResultCallback)` to be invoked after direct Google Play Services integration
* PayPalNativeCheckout
  * Bump native-checkout version to `1.3.2`
  * Fixes Google Play Store Rejection
    * Add `hasUserLocationConsent` property to `PayPalNativeCheckoutRequest`, `PayPalNativeCheckoutVaultRequest` and `PayPalNativeRequest`
    * Deprecate existing constructors that do not pass in `hasUserLocationConsent`
* PayPalDataCollector
  * Bump Magnes version to `5.5.1`

## 4.44.0 (2024-04-05)

* Local Payment
  * Fixes Google Play Store Rejection
    * Add `hasUserLocationConsent` property to `LocalPaymentRequest`
    * Deprecate existing constructor that does not pass in `hasUserLocationConsent`
* PayPal
  * Fixes Google Play Store Rejection
    * Add `hasUserLocationConsent` property to `PayPalCheckoutRequest`, `PayPalVaultRequest` and `PayPalRequest`
    * Deprecate existing constructors that do not pass in `hasUserLocationConsent`
* BraintreeDataCollector
  * Bump Magnes SDK to version 5.5.0
  * Fixes Google Play Store Rejection
    * Add `DataCollectorRequest` to pass in `hasUserLocationConsent`
    * Update `DataCollector.collectDeviceData()` to take in `DataCollectorRequest`
    * Deprecate existing `DataCollector.collectDeviceData()`
* PayPalDataCollector
  * Fixes Google Play Store Rejection
    * Add `PayPalDataCollectorRequest` to pass in `hasUserLocationConsent`
    * Update `PayPalDataCollector.collectDeviceData()` to take in `PayPalDataCollectorRequest`
    * Deprecate existing `PayPalDataCollector.collectDeviceData()`
* GooglePay
  * Add `GooglePayClient#isReadyToPay(Context, ReadyForGooglePayRequest, GooglePayIsReadyToPayCallback)` method
  * Deprecate  `GooglePayClient#isReadyToPay(FragmentActivity, ReadyForGooglePayRequest, GooglePayIsReadyToPayCallback)` method

## 4.43.0 (2024-03-19)

* Move from Braintree to PayPal analytics service (FPTI)
* Venmo
  * Fix bug where SDK is not sending metadata as expected when creating payment context or constructing App Link URL

## 4.42.0 (2024-03-12)

* PayPal
  * Add optional property `PayPalVaultRequest.setUserAuthenticationEmail()`
* BraintreeCore
  * Send `paypal_context_id` in `analytics_event` to PayPal's analytics service (FPTI) when available
* Venmo
  * Add `setIsFinalAmount()` to `VenmoRequest`
  * Add `setFallbackToWeb()` to `VenmoRequest`
    * If set to `true` customers will fallback to a web based Venmo flow if the Venmo app is not installed
    * This method uses App Links instead of Deep Links
  * Add `VenmoClient#parseBrowserSwitchResult(Context, Intent)` method
  * Add `VenmoClient#clearActiveBrowserSwitchRequests(Context)` method
  * Add `VenmoClient#onBrowserSwitchResult(BrowserSwitchResult, VenmoOnActivityResultCallback)` method
* ThreeDSecure
  * Call cleanup method to resolve `Cardinal.getInstance` memory leak

## 4.41.0 (2024-01-18)

* PayPal
  * Add imageUrl, upcCode, and upcType to PayPalLineItem
* PayPalNativeCheckout
  * Bump native-checkout version to release `1.2.1`
  * Upgraded the data-collector SDK to version 3.21.0 which made updates to Device Data collection related to Google Play's User Data Policy. For more info read the [release notes](https://github.com/paypal/android-checkout-sdk/releases/tag/v1.2.1)

## 4.40.1 (2023-12-13)

* BraintreeCore
  * Bump `browser-switch` version to `2.6.1` (fixes #799)
* PayPal
  * Fix issue where inaccurate error message was being returned on authorization or configuration error (fixes #821)
* Venmo
  * Fix NPE when `VenmoListener` is null (fixes #832)

## 4.40.0 (2023-11-16)

* PayPalNativeCheckout
  * Bump native-checkout version to release `1.2.0`
  * Add `setUserAuthenticationEmail()` to `PayPalNativeRequest`
* GooglePay
  * Bump `play-services-wallet` version to `19.2.1`
  * Add `totalPriceLabel` to `GooglePayRequest`

## 4.39.0 (2023-10-16)

* BraintreeCore
  * Remove beta features `PreferredPaymentMethodsClient`, `PreferredPaymentMethodsResult`, and `PreferredPaymentmethodsCallback`
* GooglePay
  * Fix bug where credit cards were allowed when `GooglePayRequest#setAllowedCreditCards(false)`

## 4.38.2 (2023-09-18)

* BraintreeCore
  * Internal Fixes

## 4.38.1 (2023-09-14)

* ThreeDSecure
  * Bump Cardinal version to `2.2.7-5`

## 4.38.0 (2023-09-06)

* All Modules
  * Android 14 Support
    * Upgrade `compileSdkVersion` and `targetSdkVersion` to API 34
* ThreeDSecure
  * Bump Cardinal version to `2.2.7-4`
* BraintreeCore
  * Bump `browser-switch` version to `2.6.0`
* PayPalNativeCheckout
  * Bump min SDK version to 23 to prevent crashes on lower Android versions

## 4.37.0 (2023-08-22)

* PayPalNativeCheckout
  * Bump native-checkout version to release `1.1.0`
  * Fix bug where `PayPalNativeCheckoutVaultRequest` flow in the EU results in failed requests when using the nonce in a server side request

## 4.36.0 (2023-07-18)

* BraintreeCore
  * Bump `browser-switch` version to `2.5.0`
* All Modules
  * Revert Kotlin version to `1.7.10`

## 4.35.0 (2023-07-12)

* GooglePay
  * Add `GooglePayCardNonce.getCardNetwork()`

## 4.34.0 (2023-07-10)

* GooglePay
  * Add `GooglePayRequest.setAllowCreditCards()`
* PayPalNativeCheckout (General Availability release)
  * Bump native-checkout version to release `1.0.0`
  * Fix an issue where the return from web fallback was not returning the correct information

## 4.33.0 (2023-06-27)

* PayPalNativeCheckout (BETA)
  * Fix bug where setting `setUserAction()` does not update button as expected
* SEPADirectDebit
  * Add `SEPADirectDebitRequest.setLocale()`
* Venmo
  * Add missing space to Venmo `PaymentContext` GraphQL query (fixes #749)

## 4.32.0 (2023-06-20)

* Bump target Kotlin version to `1.8.0`
* PayPal
  * Undeprecate `PayPalClient(BraintreeClient)` constructor
  * Undeprecate `PayPalClient#onBrowserSwitchResult(BrowserSwitchResult, PayPalBrowserSwitchResultCallback)`
  * Add `PayPalClient#parseBrowserSwitchResult(Context, Intent)` method
  * Add `PayPalClient#clearActiveBrowserSwitchRequests(Context)` method
* LocalPayment
  * Undeprecate `LocalPaymentClient(BraintreeClient)` constructor
  * Undeprecate `LocalPaymentClient#onBrowserSwitchResult(Context, BrowserSwitchResult, LocalPaymentBrowserSwitchResultCallback)`
  * Add `LocalPaymentClient#parseBrowserSwitchResult(Context, Intent)` method
  * Add `LocalPaymentClient#clearActiveBrowserSwitchRequests(Context)` method
* Venmo
  * Fix issue caused by `VenmoActivityResultContract` where a user cancelation is being misinterpreted as an unknown exception because the intent data is `null` (fixes #734)
  * Add the following properties to `VenmoRequest`:
    * `collectCustomerBillingAddress`
    * `collectCustomerShippingAddress`
    * `totalAmount`
    * `subTotalAmount`
    * `discountAmount`
    * `taxAmount`
    * `shippingAmount`
    * `lineItems`

## 4.31.0 (2023-06-08)

* BraintreeDataCollector
  * Remove Kount dependency
  * Kount is no longer supported via Braintree, instead please use our [Fraud Protection Advanced product](https://developer.paypal.com/braintree/articles/guides/fraud-tools/premium/fraud-protection-advanced)

## 4.30.0 (2023-06-07)

* GooglePay
  * Add `GooglePayCardNonce.getBin()`

## 4.29.0 (2023-05-18)

* PayPalNativeCheckout (BETA)
  * Reverting native version upgrade
* ThreeDSecure
  * Bump Cardinal version to `2.2.7-3`
  * Add `setUiType` and `setRenderTypes` to `ThreeDSecureRequest`

## 4.28.0 (2023-04-24)

* PayPalNativeCheckout (BETA)
  * Bump native-checkout version to release `0.112.0`

## 4.27.2 (2023-04-19)

* ThreeDSecure
  * Guard `ThreeDSecureClient` against potential Cardinal SDK runtime exceptions

## 4.27.1 (2023-04-12)

* ThreeDSecure
  * Catch Null Pointer Exception in `ThreeDSecureActivity` to prevent crash (fixes #715)

## 4.27.0 (2023-04-03)

* DataCollector
  * Use `applicationContext` in `DataCollector#collectDeviceData()` callback to prevent potential `Activity` leaks
* GooglePay
  * Fix issue that causes `GooglePayNonce#isNetworkTokenized` to always return `false` after being parceled
* ThreeDSecure
  * Catch `TransactionTooLargeException` to prevent crash on large data (fixes #642)
  * Deprecate 3DS v1. Any attempt to use 3DS v1 will now throw an error. See [Migrating to 3D Secure 2](https://developer.paypal.com/braintree/docs/guides/3d-secure/migration) for more information.

## 4.26.1 (2023-02-28)

* BraintreeDataCollector
  * Bump Magnes dependency to version 5.4.0 (fixes #657)
* PayPal
  * Fix issue that causes a null pointer exception when `PayPalClient` attempts to notify success or failure when the listener is `null`

## 4.26.0 (2023-02-13)

* PayPalNativeCheckout (BETA)

  * Fixes a bug where an error was not thrown inside `PayPalNativeCheckoutClient` when no PayPal response was received from the API

* BraintreeCore
  * Add `BraintreeClient#deliverBrowserSwitchResultFromNewTask()` method to allow browser switch results to be captured manually when `BraintreeClient#launchesBrowserSwitchAsNewTask()` is set to true.
* SharedUtils
  * Replace EncryptedSharedPreferences with SharedPreferences for internal persistent data storage for all payment flows
  * Deprecate `BraintreeSharedPreferencesException`

## 4.25.2 (2023-02-08)

* BraintreeCore
  * Provide more detailed information for Browser Switch errors for PayPal, PayPalNativeCheckout, and ThreeDSecure payment flows
* SamsungPay
  * Support legacy `sourceCardLast4` property when parsing Samsung Pay response

## 4.25.1 (2023-02-07)

* SharedUtils
  * Revert androidx `security-crypto` dependency to `1.1.0-alpha03` (`1.1.0-alpha04` requires a compile target of 33)

## 4.25.0 (2023-02-06)

* SharedUtils
  * Bump androidx `security-crypto` dependency to `1.1.0-alpha04`
* PayPalNativeCheckout (BETA)
  * Bump native-checkout version to `0.8.8`
  * Fix an issue where address override was not being honored in `PayPalNativeCheckoutRequest`
  * Fixes bug in `PayPalNativeCheckoutAccountNonce` where the `intent` was not being set correctly from the `PayPalNativeCheckoutRequest`
  * Breaking changes
    * `PayPalNativeRequest` requires a `returnUrl` to redirect correctly after authentication
* ThreeDSecure
  * Apply `Theme.AppCompat` to `ThreeDSecureActivity`
* SamsungPay
  * Support legacy `singleUseToken` property when parsing Samsung Pay response (fixes #668)

## 4.24.0 (2023-01-30)

* BraintreeCore
  * Allow uppercase characters in default return url scheme
* ThreeDSecure
  * Add `setRequestedExemptionType` to `ThreeDSecureRequest`
* *Please note:* This version is dependent on the Java 8 programming language. Please read [Use Java 8 language features](https://developer.android.com/studio/write/java8-support) in the Android developer guide to learn how to use it in your project. If this causes an issue with your integration, please contact our [support](https://developer.paypal.com/braintree/help) team for further assistance.
  
## 4.23.1 (2023-01-09)

* ThreeDSecure
  * Defensively guard against `ThreeDSecureActivity` launch without extras (fixes #641)

## 4.23.0 (2022-12-22)

* BraintreeCore
  * Bump `browser-switch` version to `2.3.2`

## 4.22.0 (2022-12-19)

* PayPalNativeCheckout (BETA)
  * Bump native-checkout version to `0.8.7`

## 4.21.1 (2022-12-14)

* PayPal
  * Update exception documentation links to point to valid PayPal Braintree documentation URL
* ThreeDSecure
  * Update exception documentation links to point to valid PayPal Braintree documentation URL
* BraintreeCore
  * Update pinned certificates used by `BraintreeGraphQLClient` and `BraintreeHttpClient`

## 4.21.0 (2022-12-07)

* PayPalNativeCheckout (BETA)
  * Pass the risk correlation ID from the Native Checkout SDK if it is not provided in the initial PayPal request
  
## 4.20.0 (2022-11-07)

* SharedUtils
  * Allow `BraintreeSharedPreferences` to gracefully degrade when `EncryptedSharedPreferences` fails (fix for #619)
  * Add new `BraintreeSharedPreferencesException` to notify when an error occurs while interacting with shared preferences

## 4.19.0 (2022-10-26)

* GooglePay
  * Bump `play-services-wallet` version to `19.1.0`
* SharedUtils
  * Add explicit key alias for encrypted shared prefs (potential fix for #604)

## 4.18.0 (2022-10-19)

* Android 13 Support
  * Upgrade `targetSdkVersion` and `compileSdkVersion` to API 33
* ThreeDSecure
  * Bump Cardinal version to `2.2.7-2`
* BraintreeCore
  * Bump `browser-switch` version to `2.3.1`

## 4.17.0 (2022-10-05)

* PayPalNativeCheckout (BETA)
  * Bumping native-checkout version to 0.8.2
  * Fixes an issue where merchants with multiple client IDs would fallback to web on subsequent checkout sessions
  * Remove exit survey when canceling Native Checkout flow

## 4.16.0 (2022-09-16)

* PayPalNativeCheckout (BETA)
  * Bumping native-checkout version to 0.8.1 
  * Adding in Native checkout support for one time password
* BraintreeCore
  * Add `BraintreeClient#launchesBrowserSwitchAsNewTask()` boolean flag to allow the SDK to capture deep link results on behalf of the host application
  * Create `BraintreeDeepLinkActivity` to capture deep link results on behalf of the host application

## 4.15.0 (2022-08-17)

* BraintreeCore
  * Add BraintreeError `code` read-only property.
* PayPalNativeCheckoutClient
  * Add new `PayPalNativeCheckoutClient` that requires only a `BraintreeClient` instance.
  * Add new `PayPalNativeCheckoutClient#launchNativeCheckout` method that launches native checkout without throwing.
  * Deprecate `PayPalNativeCheckoutClient` constructor that requires both `Fragment` and `BraintreeClient` instances.
  * Deprecate `PayPalNativeCheckoutClient#tokenizePayPalAccount` method that throws an exception.

## 4.14.0 (2022-08-09)

* PayPalDataCollector
  * Create new module to allow for device data collection without Kount.
* BraintreeSEPADirectDebit
  * Update nonce to pull in ibanLastFour as expected

## 4.13.0 (2022-07-20)

* DataCollector
  * Reference Kount library only when needed to prevent JVM from loading it when it isn't being used by a merchant.
* SEPADirectDebit
  * Add support for SEPA Direct Debit for approved merchants through the Braintree SDK
  * SEPA Direct Debit is only available to select merchants, please contact your Customer Support Manager or Sales to start processing SEPA bank payments
  * Merchants should use the `BTSepaDirectDebitClient.tokenize` method while passing in the activity and `BTSEPADirectDebitRequest`
* PayPalNativeCheckout (BETA)
  * Requirement - Needs to be built on Java 11
  * Adding in [PayPalNativeCheckout] module to use the native checkout for PayPal
  * Adds `PayPalNativeCheckoutClient` that handles launching the native checkout session, the session
    start parameters are similar to that of `PaypalClient` with the main difference being it doesn't
    use the browserSwitch to checkout on web but instead consumes the native checkout sdk. This provides
    a much more native feel to checking out with PayPal.
  * Adds `PayPalNativeCheckoutAccount` to represent tokenizing a PayPal request
  * Adds `PayPalNativeCheckoutAccountNonce` that represents the value returned from the web
  * Adds `PayPalNativeCheckoutFragment` that shows how to launch the native checkout sdk
  * Adds `PayPalNativeCheckoutCreditFinancing` to represent the PayPal credit financing response
  * Adds `PayPalNativeCheckoutCreditFinancingAmount` to represent the PayPal finance amount
  * Adds `PayPalNativeCheckoutLineItem` to represent a line item for checkout flows
  * Adds `PayPalNativeCheckoutListener` to receive result notifications
  * Adds `PayPalNativeCheckoutPaymentIntent` to represent the payment intent for an order
  * Adds `PayPalNativeCheckoutPaymentResource` to represent the data returned from the internal checkout client
    to fetch the return url
  * Adds `PayPalNativeCheckoutRequest` to represent all items needed to begin the native checkout flow
  * Adds `PayPalNativeCheckoutVaultRequest` to represent all items needed to begin the native vault flow
  * Adds `PayPalNativeRequest` to represent the base items needed for checkout and vault requests
  * Adds `PayPalNativeCheckoutResultCallback` to listen to the result returned from the checkout response

## 4.12.0 (2022-06-10)

* SharedUtils
  * Update `BraintreeSharedPreferences` to no-op when a reference to Android `EncryptedSharedPreferences` cannot be obtained (fixes #561)
* ThreeDSecure
  * Bump Cardinal version to `2.2.6-2`

## 4.11.0 (2022-05-18)

* Add `invalidateClientToken` method to `BraintreeClient` (thanks @josephyanks)
* Add `isExplicitCancelation` parameter to `UserCanceledException`
* Trim tokenization key and client token before parsing

## 4.10.1 (2022-04-14)

* DataCollector
  * Use configuration environment to set Magnes environment correctly

## 4.10.0 (2022-04-01)

* ThreeDSecure  
  * Support AndroidX and remove Jetifier requirement (fixes #315)
  * Bump Cardinal version to `2.2.6-1`
  * Fix null pointer error in V2 UI customization
  * Deprecate `ThreeDSecureV2BaseCustomization`
  * Deliver browser switch result asynchronously on main thread
* SamsungPay
  * Support AndroidX and remove Jetifier requirement
* Local Payment
  * Deliver browser switch result asynchronously on main thread
* PayPal
  * Deliver browser switch result asynchronously on main thread (fixes #500)

## 4.9.0 (2022-03-18)

* Braintree Core
  * Add `ClientTokenProvider` interface for asynchronously fetching client token authorization
  * Add new `BraintreeClient` constructors that accept `ClientTokenProvider`
  * Update pinned certificates used by `BraintreeGraphQLClient`
* Google Pay
  * Add `GooglePayListener` to receive results from the Google Pay flow
  * Deprecate methods requiring a callback in favor of listener pattern 
* ThreeDSecure
  * Add `ThreeDSecureListener` to receive results from the 3DS flow
  * Deprecate methods requiring a callback in favor of listener pattern
* Venmo
  * Add `VenmoListener` to receive results from the Venmo flow
  * Deprecate methods requiring a callback in favor of listener pattern
* PayPal
  * Add `PayPalListener` to receive results from the PayPal flow
  * Deprecate methods requiring a callback in favor of listener pattern
* Local Payment 
  * Add `LocalPaymentListener` to receive results from the Local Payment flow
  * Deprecate methods requiring a callback in favor of listener pattern

## 4.8.3 (2022-03-01)

* PayPal
  * Fix issue where billing agreement description was not showing (fixes #509)

## 4.8.2 (2022-02-01)

* Venmo
  * Fix issue where null value causes VenmoAccountNonce#fromJSON() to throw.

## 4.8.1 (2022-01-10)

* GooglePay
  * Deprecate `googleMerchantId`
  * Bump `play-services-wallet` version to `18.1.3`
* SharedUtils
  * Use byte array to hold `HttpRequest` data. Dispose data immediately after making http request.

## 4.8.0 (2021-11-18)

* BraintreeCore
  * Bump `browser-switch` version to `2.1.1`

## 4.7.0 (2021-10-15)

* SharedPreferences
  * Encrypt shared preferences data stored by SDK (fixes #440)
* Local Payments
  * Add `displayName` to `LocalPaymentRequest`
* DataCollector
  * Fix memory leak from `PayPalDataCollector` (fixes #419)
* Local Payments
  * Add `displayName` to `LocalPaymentRequest`
* PayPal
  * Fix issue that caused user cancelations from PayPal browser flow to be incorrectly reported as failures
* Venmo
  * Make `VenmoRequest` parcelable
* ThreeDSecure
  * Make `pareq` optional on `ThreeDSecureLookup`

## 4.6.0 (2021-09-07)

* Android 12 Support
  * Upgrade `targetSdkVersion` and `compileSdkVersion` to API 31
  * Bump `browser-switch` version to `2.1.0`
  * Fix issue where Venmo app is not detected on Android 12 devices

## 4.5.0 (2021-08-31)

* BraintreeCore
  * Add `BraintreeClient` constructor to allow a custom return url scheme to be used for browser and app switching
* BraintreeDataCollector
  * Bump Magnes dependency to version 5.3.0
  * Add `BraintreeCore` as an `api` dependency (Fixes #437)
* SamsungPay
  * Add additional alias for Amex in `SamsungPay` (fixes #430)

## 4.4.1 (2021-08-12)

* ThreeDSecure
  * Fix issue that causes `ThreeDSecureRequest` to throw a `NullPointerException` when parceling.

## 4.4.0 (2021-08-11)

* Core
  * Bump `browser-switch` version to `2.0.2`
* SamsungPay
  * Add `SamsungPayClient`
  * Add `SamsungPayClient#goToUpdatePage()`
  * Add `SamsungPayClient#activateSamsungPay()`
  * Add `SamsungPayClient#isReadyToPay()`
  * Add `SamsungPayClient#startSamsungPay()`
  * Add `SamsungPayClient#buildCustomSheetPaymentInfo()`
  * Add `SamsungPayClient#updateCustomSheet()`

## 4.3.0 (2021-07-21)

* Core
  * Make `Configuration#getSupportedCardTypes()` public
* ThreeDSecure
  * Make `ThreeDSecureResult#getLookup()` public
  * Bump Cardinal version to `2.2.5-4`
  * Add `cardAddChallengeRequested` to `ThreeDSecureRequest`

## 4.2.0 (2021-06-23)

* Add `VenmoClient#isReadyToPay()` method
* Bump `browser-switch` to `2.0.1` (fixes #409)

## 4.1.0 (2021-06-08)

* Bump Cardinal version to `2.2.5-3`

**Note:** The credentials for integrating with 3DS have changed. If you are using 3DS please update the credentials in your app-level `build.gradle` see [v4 Migration Guide](/v4_MIGRATION_GUIDE.md#3d-secure) 

## 4.0.0 (2021-06-07)

* Make `PayPalRequest` and subclasses `Parcelable`
* Add getters to data classes to improve support for Kotlin synthesized properties
* Add `displayName` property to `VenmoRequest`
* Bump `browser-switch` to `2.0.0`
* Breaking Changes
  * Rename `LocalPaymentTransaction` to `LocalPaymentResult`
  * Rename `LocalPaymentClient#approveTransaction()` to `LocalPaymentClient#approvePayment()` 
  * Make `PayPalCreditFinancing#fromJson()` package-private
  * Make `PayPalCreditFinancingAmount#fromJson()` package-private
  * Make `UnionPayCapabilities#fromJson()` package-private
  * Make `PaymentMethodClient#parsePaymentMethodNonces()` package-private
  * Return `UserCanceledException` on user cancellation
  * Remove `DataCollector#collectPayPalDeviceData()`
  * Remove `DataCollector#collectRiskData()`
  * Make `DataCollector#getPayPalClientMetadataId()` private
  * Remove `PaymentMethodClient`
  * Remove `PaymentMethodType`
  * Remove `PaymentMethodDeleteException`
  * Remove `GetPaymentMethodNoncesCallback`
  * Remove `DeletePaymentMethodNonceCallback`
  * Use primitives instead of boxed types where possible
  * Add nullability annotations to public methods
  * Remove `Context` parameter from `CardClient#tokenize()` method
  * Fix typo in `ThreeDSecureAdditionalInformation#getPaymentAccountIndicator()` method name

**Note:** Includes all changes in [4.0.0-beta1](#400-beta1), [4.0.0-beta2](#400-beta2), and [4.0.0-beta3](#400-beta3)

## 4.0.0-beta3 (2021-05-13)

* Add `PaymentMethodType` enum
* Add `PaymentMethodNonce#getType()` method
* Add wallet enabled metadata tag to `AndroidManifest.xml` in `google-pay` module 
* Bump `browser-switch` to `2.0.0-beta3`
* Callback `BraintreeException` on user cancellation of payment flows
* Add `paymentMethodUsage` to `VenmoRequest`
* Breaking Changes
  * Rename `DownForMaintenanceException` to `ServiceUnavailableException`
  * Remove `GoogleApiClientException`
  * Make `BraintreeWalletConstants` package-private
  * Make `PaymentMethodNonceFactory` package-private
  * Make `GooglePayException` constructor package-private
  * Make `VisaCheckoutAccount` package-private
  * Make `VenmoAccount` package-private
  * Return an `IllegalArgumentException` instead of `GoogleApiClientException` to `GooglePayIsReadyToPayCallback#onResult()` when activity is null
  * Refactor `GetPaymentMethodNoncesCallback` to have a single `onResult()` method instead of `success()` and `failure()`
  * Remove `Context` parameter from `PaymentMethodClient#getPaymentMethodNonces`
  * Rename `PaymentMethodNonce#getNonce()` to `getString()`
  * Move `VenmoAccountNonce` to `Venmo` module
  * Move `AuthenticationInsight` to `Card` module
  * Move `BinData` to `Card` module
  * Move `Card` to `Card` module
  * Move `CardNonce` to `Card` module
  * Move `ThreeDSecureInfo` to `Card` module
  * Move `PayPalAccountNonce` to `PayPal` module
  * Move `PayPalCreditFinancing` to `PayPal` module
  * Move `PayPalCreditFinancingAmount` to `PayPal` module
  * Move `UnionPayCapabilities` to `UnionPay` module
  * Move `UnionPayCard` to `UnionPay` module
  * Move `VisaCheckoutAddress` to `VisaCheckout` module
  * Move `VisaCheckoutNonce` to `VisaCheckout` module
  * Move `VisaCheckoutUserData` to `VisaCheckout` module
  * Remove `PaymentMethodNonce#getTypeLabel()` method
  * Remove `PaymentMethodNoncesCallback`
  * Remove `PaymentMethodNonce#getDescription()` method
  * `BraintreeClient` constructor no longer throws `InvalidArgumentException`
  * Make protected static member variables `OPTIONS_KEY`, `OPERATION_NAME_KEY` on `PaymentMethod` package-private
  * Make `PaymentMethod` constructor package-private
  * Rename `setValidate` to `setShouldValidate` and move it from `PaymentMethod` base class to `Card` subclass 
  * Make `buildJSON()` package-private for `PaymentMethod` base class and all subclasses
  * Remove `buildGraphQL()` method from `PaymentMethod` base class and all subclasses
  * Make `PaymentMethod` `Parcelable` constructor package-private
  * Make `PaymentMethod#writeToParcel()` method package-private
  * Make `PaymentMethod#getDefaultSource()` method package-private
  * Make `PaymentMethod#getDefaultIntegration()` method package-private
  * Make `getApiPath()` method package-private in `PaymentMethod` base class and subclasses
  * Remove `getResponsePaymentMethodType()` method from `PaymentMethod` base class and subclasses
  * Make `BaseCard` class package-private

## 4.0.0-beta2 (2021-03-31)

* Add `setCountryCode` to `GooglePayRequest`
* Add Google Pay support for Elo cards. 
* Add `VenmoRequest` 
* Add new classes for 3DS2 UI Customization:
  * `ThreeDSecureV2UiCustomization`
  * `ThreeDSecureV2ButtonCustomization`
  * `ThreeDSecureV2LabelCustomization`
  * `ThreeDSecureV2TextBoxCustomization`
  * `ThreeDSecureV2ToolbarCustomization`
* Add `PayPalCheckoutRequest`
* Add `PayPalVaultRequest`
* Add `tokenizePayPalAccount` method to `PayPalClient`
* Add `requestBillingAgreement` to `PayPalCheckoutRequest`
* Fix issue where `onBrowserSwitchResult` crashes if `browserSwitchResult` is null
* Add `ThreeDSecureResult`
* Bump `browser-switch` to `2.0.0-beta2`
* Breaking Changes
  * Make `AmericanExpressRewardsBalance#fromJson()` package-private
  * Make `TYPE` and `API_RESOURCE_KEY` in `CardNonce` package-private
  * Make `CardNonce#fromJson()` methods package-private
  * Make `CardNonce` constructor package-private
  * Make `TYPE`, `API_RESOURCE_KEY`, `PAYMENT_METHOD_DATA_KEY`, `TOKENIZATION_DATA_KEY` and `TOKEN_KEY` in `PayPalAccountNonce` package-private
  * Make `PayPalAccountNonce#fromJson()` methods package-private 
  * Make `PayPalAccountNonce` constructor package-private
  * Make `DATA_KEY` and `TOKEN_KEY` in `PaymentMethodNonce` package-private
  * Make `PaymentMethodNonce#fromJson()` package-private
  * Make `PaymentMethodNonce#parsePayentMethodNonces()` methods package-private
  * Make `PaymentMethodNonces` constructor package-private
  * Make `ThreeDSecureAuthenticationResponse#fromJson()` package-private
  * Make `ThreeDSecureAuthenticationResponse` constructor package-private
  * Make `ThreeDSecureInfo#fromJson()` package-private
  * Make `ThreeDSecureInfo#setThreeDSecureAuthenticationResponse()` package-private
  * Make `ThreeDSecureLookup#fromJson()` package-private
  * Make `TYPE` and `API_RESOURCE_KEY` in `VenmoAccountNonce` package-private
  * Make `VenmoAccountNonce#fromJson()` methods package-private
  * Make `VenmoAccountNonce` constructor package-private
  * Make `VenmoAccountNonce` parcelable constructor private
  * Make `TYPE` and `API_RESOURCE_KEY` in `VisaCheckoutNonce` package-private
  * Make `VisaCheckoutNonce#fromJson()` methods package-private
  * Make `VisaCheckoutNonce` constructor package-private
  * Make `API_RESOURCE_KEY` in `GooglePayCardNonce` package-private
  * Make `GooglePayCardNonce#fromJson()` methods package-private
  * Make `GooglePayCardNonce#postalAddressFromJson()` package-private
  * Make `GooglePayCardNonce` constructor package-private
  * Make `API_RESOURCE_KEY` in `LocalPaymentNonce` package-private
  * Make `LocalPaymentNonce#fromJson()` methods package-private
  * Make `LocalPaymentNonce` constructor package-private
  * Make `GooglePayClient#tokenize()` package-private
  * The `shippingMethod` property on `ThreeDSecureRequest` is now an enum rather than a string. Possible values:
    * `SAME_DAY`
    * `EXPEDITED`
    * `PRIORITY`
    * `GROUND`
    * `ELECTRONIC_DELIVERY`
    * `SHIP_TO_STORE`
  * Change default `versionRequested` on `ThreeDSecureRequest` to `ThreeDSecureVersion.VERSION_2`
  * Rename `uiCustomization` on `ThreeDSecureRequest` to `v2UiCustomization` and change parameter to `ThreeDSecureV2UiCustomization`
  * Update setters on `V1UiCustomization` to remove method chaining
  * Change Cardinal dependency from `api` to `implementation`
  * Replace `VenmoClient#authorizeAccount()` with `VenmoClient#tokenizeVenmoAccount()`
  * Rename `VenmoAuthorizeAccountCallback` to `VenmoTokenizeAccountCallback`
  * Remove `activity` parameter from `GooglePayClient#onActivityResult()`
  * Remove `activity` parameter from `GooglePayClient#getTokenizationParameters()`
  * Update `PayPalClient#requestOneTimePayment()` to expect a `PayPalCheckoutRequest` and deprecate method
  * Update `PayPalClient#requestBillingAgreement()` to expect a `PayPalVaultRequest` and deprecate method
  * Make `PayPalRequest` abstract
  * Update `PayPalRequest` setter method names and remove method chaining
  * Make `PayPalAccountBuilder` package-private
  * Remove `ThreeDSecureClient#performVerification()` convenience overload
  * Remove `ThreeDSecureAuthenticationResponse`
  * Remove `errorMessage` and `threeDSecureAuthenticationResponse` properties from `ThreeDSecureInfo`
  * Remove `cardNonce` property from `ThreeDSecureLookup`
  * Remove `ThreeDSecureLookupCallback`
  * Remove `firstName` and `lastName` properties from `ThreeDSecurePostalAddress`
  * Update `ThreeDSecureResultCallback` to expect a `ThreeDSecureResult` parameter
  * Update `ThreeDSecureClient#continuePerformVerification()` to expect a `ThreeDSecureResult` parameter
  * Update callback type in `ThreeDSecureClient#initializeChallengeWithLookupResponse` methods
  * Replace `CardBuilder` with `Card`
  * Replace `BaseCardBuilder` with `BaseCard`
  * Replace `PaymentMethodBuilder` with `PaymentMethod`
  * Replace `UnionPayCardBuilder` with `UnionPayCard`
  * Replace `PayPalAccountBuilder` with `PayPalAccount`
  * Replace `VenmoAccountBuilder` with `VenmoAccount`
  * Replace `VisaCheckoutBuilder` with `VisaCheckoutAccount`
  * Remove builder pattern from the following classes:
    * `PostalAddress`
    * `GooglePayRequest`
    * `ReadyForGooglePayRequest`
    * `LocalPaymentRequest`
    * `ThreeDSecureAdditionalInformation`
    * `ThreeDSecurePostalAddress`
    * `ThreeDSecureRequest`
  * Rename `PayPalPaymentIntent` enums:
    * `INTENT_ORDER` to `ORDER`
    * `INTENT_SALE` to `SALE`
    * `INTENT_AUTHORIZE` to `AUTHORIZE`
  * Remove `paymentRequested` param from `GooglePayRequestPaymentCallback`
  * Refactor `BraintreeClient` constructor to take a `String` instead of `Authorization` and change parameter ordering 
  * Make `Authorization` package-private
  * Make `TokenizationKey` package-private
  * Make `ClientToken` package-private
  * Make `PayPalUAT` package-private

## 3.21.1 (2023-08-18)

* LocalPayment
  * Fixed bug where the configuration was not returned the expected result for Local Payment Methods being enabled

## 3.21.0 (2023-06-12)

* BraintreeDataCollector
  * Remove Kount dependency
  * Deprecate `DataCollector#collectDeviceData(BraintreeFragment, String, BraintreeResponseListener<String>)`
  * Kount is no longer supported via Braintree, instead please use our [Fraud Protection Advanced product](https://developer.paypal.com/braintree/articles/guides/fraud-tools/premium/fraud-protection-advanced)

## 3.20.1 (2023-03-28)

* Update paypal-data-collector to 5.4.0

## 3.20.0 (2023-01-12)

* Bump Cardinal version to `2.2.7-2`
* Update pinned certificates used by `BraintreeGraphQLHttpClient` and `BraintreeHttpClient`

## 3.19.0 (2022-01-31)

* Add `requestBillingAgreement` to `PayPalRequest`

## 3.18.1 (2021-11-10)

* Bump Cardinal version to `2.2.5-4`

## 3.18.0 (2021-09-16)

* Upgrade `targetSdkVersion` and `compileSdkVersion` to API 31
* Bump `browser-switch` version to `1.2.0`
* Fix issue where Venmo and PayPal apps are not detected on Android 12 devices

## 3.17.4 (2021-05-03)

* Revert release 3.17.3 (local repository does not propagate MPI aar file)

## ~3.17.3~

* ~Remove Bintray dependency for Cardinal SDK (fixes #373 - [Cardinal bintray credentials can now be removed](https://developers.braintreepayments.com/guides/3d-secure/client-side/android/v3#generate-a-client-token))~

## 3.17.2 (2021-03-25)

* Bump Cardinal version to `2.2.5-2`
* Add PayPal to `queries` element in `AndroidManifest.xml`

## 3.17.1 (2021-03-24)

* Add Venmo to `queries` element in `AndroidManifest.xml` (fixes issue in Android 11 not properly detecting if Venmo app is installed)

## 4.0.0-beta1 (2021-03-08)

* Add a `client` for each feature:
  * `AmericanExpressClient`
  * `BraintreeClient`
  * `CardClient`
  * `DataCollector`
  * `GooglePayClient`
  * `LocalPaymentClient`
  * `PayPalClient`
  * `PaymentMethodClient`
  * `PreferredPaymentMethodsClient`
  * `ThreeDSecureClient`
  * `UnionPayClient`
  * `VenmoClient`
  * `VisaCheckoutClient`
* Create callback interfaces to enforce callback pattern:
  * `AmericanExpressGetRewardsBalanceCallback`
  * `CardTokenizeCallback`
  * `ConfigurationCallback`
  * `DataCollectorCallback`
  * `DeletePaymentMethodNonceCallback`
  * `GetPaymentMethodNoncesCallback`
  * `LocalPaymentBrowserSwitchResultCallback`
  * `LocalPaymentStartCallback`
  * `PayPalBrowserSwitchResultCallback`
  * `PayPalFlowStartedCallback`
  * `PreferredPaymentMethodsCallback`
  * `ThreeDSecureLookupCallback`
  * `ThreeDSecurePrepareLookupCallback`
  * `ThreeDSecureResultCallback`
  * `UnionPayEnrollCallback`
  * `UnionPayFetchCapabilitiesCallback`
  * `UnionPayTokenizeCallback`
  * `VenmoAuthorizeAccountCallback`
  * `VenmoOnActivityResultCallback`
* Migrate `braintree-android-google-payment` into `braintree_android`
* Migrate `braintree-android-visa-checkout` into `braintree_android`
* Add `Configuration#getEnvironment()`
* Add `Configuration#getPayPalPrivacyUrl()` 
* Add `Configuration#getPayPalUserAgreementUrl()` 
* Add `Configuration#isGooglePayEnabled()`
* Add `Configuration#isLocalPaymentEnabled()`
* Add `Configuration#isSamsungPayEnabled()`
* Add `Configuration#isUnionPayEnabled()`
* Add `Configuration#isVenmoEnabled()`
* Add `Configuration#isVisaCheckoutEnabled()`
* Update Visa Checkout aar dependency to version `6.6.1`
* Add `LocalPaymentTransaction` to represent Local Payment transactions
* Add `amount` setter to `PayPalRequest`
* Breaking Changes
  * Bump `browser-switch` to `2.0.0-beta1`
  * Change `GooglePayCapabilities#isGooglePayEnabled()` parameters
  * Create `american-express` module
  * Create `card` module
  * Create `local-payment` module
  * Create `pay-pal` module
  * Create `union-pay` module
  * Create `venmo` module
  * Remove PayPal `data-collector` module
  * Remove PayPal `paypal-one-touch` module
  * Remove `AmericanExpressListener`
  * Remove `AnalyticsIntentService`
  * Remove `AnalyticsSender`
  * Remove `AmericanExpressRewardsBalance` default constructor
  * Remove `amount` parameter from `PayPalRequest` constructor
  * Remove `approvalUrl` and `paymentId` properties from `LocalPaymentRequest`
  * Remove `Beta` interface
  * Remove `BraintreeApiError`
  * Remove `BraintreeApiErrorResponse`
  * Remove `BraintreeApiHttpClient`
  * Remove `BraintreeBrowserSwitchActivity`
  * Remove `BraintreeCancelListener`
  * Remove `BraintreeErrorListener` 
  * Remove `BraintreeFragment`
  * Remove `BraintreeListener`
  * Remove `BraintreePaymentResult`
  * Remove `BraintreePaymentResultListener`
  * Remove `BrowserSwitchException` constructor
  * Remove `ConfigurationListener`
  * Remove `ConfigurationManager`
  * Remove `Configuration#getCardConfiguration()`
  * Remove `Configuration#getGraphQL()`
  * Remove `Configuration#getGooglePayment()`
  * Remove `Configuration#getKount()`
  * Remove deprecated 3DS `performVerification` methods
  * Remove `InstallationIdentifier`
  * Remove `LocalPaymentResult`
  * Remove `PaymentMethodNonceCreatedListener`
  * Remove `PaymentMethodNonceDeletedListener`
  * Remove `PaymentMethodNoncesUpdatedListener`
  * Remove `PaymentMethodNotAvailableException`
  * Remove `PayPalApprovalCallback`
  * Remove `PayPalApprovalHandler`
  * Remove `PayPalProductAttributes`
  * Remove `PayPalTwoFactorAuth`
  * Remove `PayPalTwoFactorAuthCallback`
  * Remove `PayPalTwoFactorAuthRequest`
  * Remove `PayPalTwoFactorAuthResponse`
  * Remove `PreferredPaymentMethodsListener`
  * Remove `QueuedCallback`
  * Remove `ThreeDSecureLookupListener`
  * Remove `ThreeDSecurePrepareLookupListener`
  * Remove `TokenizationParametersListener`
  * Remove `UnionPayListener`
  * Remove `VisaCheckoutConstants`
  * Remove `VisaCheckoutNotAvailableException`
  * Rename `AmericanExpress` to `AmericanExpressClient`
  * Rename `Card` to `CardClient`
  * Rename `GooglePayment` to `GooglePayClient`
  * Rename `LocalPayment` to `LocalPaymentClient`
  * Rename `PayPal` to `PayPalClient`
  * Rename `PaymentMethod` to `PaymentMethodClient`
  * Rename `PreferredPaymentMethods` to `PreferredPaymentMethodsClient`
  * Rename `ThreeDSecure` to `ThreeDSecureClient`
  * Rename `UnionPay` to `UnionPayClient`
  * Rename `Venmo` to `VenmoClient`
  * Rename `VisaCheckout` to `VisaCheckoutClient`
  * Rename `core` module to `shared-utils`
  * Rename `LocalPaymentResult` to `LocalPaymentNonce`
  * Rename `braintree` module to `braintree-core`
  * Rename `GooglePayment` classes to `GooglePay`
  * Rename `BraintreeRequestCodes.GOOGLE_PAYMENT` to `BraintreeRequestCodes.GOOGLE_PAY`
  * Make `AnalyticsConfiguration` package-private
  * Make `AnalyticsDatabase` package-private
  * Make `AnalyticsEvent` package-private
  * Make `AppHelper` package-private
  * Make `AppSwitchNotAvailableException` constructor package-private
  * Make `AuthenticationException` constructor package-private
  * Make `AuthorizationException` package-private
  * Make `BraintreeApiConfiguration` package-private
  * Make `BraintreeError` constructors package-private
  * Make `BraintreeException` package-private
  * Make `BraintreeGraphQLHttpClient` package-private
  * Make `BraintreeHttpClient` package-private
  * Make `BraintreeSharedPreferences` package-private
  * Make `BraintreeResponseListener` package-private
  * Make `CardConfiguration` package-private
  * Make `ClassHelper` package-private
  * Make `ConfigurationException` constructor package-private
  * Make `DeviceInspector` package-private
  * Make `DownForMaintenanceException` constructor package-private
  * Make `ErrorWithResponse` constructors package-private
  * Make `GraphQLConfiguration` package-private
  * Make `GraphQLConstants` package-private
  * Make `GraphQLQueryHelper` package-private
  * Make `GooglePaymentConfiguration` package-private
  * Make `HttpClient` package-private
  * Make `HttpResponseCallback` package-private
  * Make `IntegrationType` package-private
  * Make `InvalidArgumentException` package-private
  * Make `Json` package-private
  * Make `KountConfiguration` package-private
  * Make `ManifestValidator` package-private
  * Make `MetadataBuilder` package-private
  * Make `PaymentMethodDeleteException` constructor package-private
  * Make `PayPalConfiguration` package-private
  * Make `PayPalDataCollector` package-private
  * Make `PayPalDataCollectorRequest` package-private
  * Make `PayPalPaymentResource` package-private
  * Make `PostalAddressParser` package-private
  * Make `PreferredPaymentMethodsResult` constructor package-private
  * Make `RateLimitException` constructor package-private
  * Make `SamsungPayConfiguration` package-private
  * Make `ServerException` constructor package-private
  * Make `SignatureVerification` package-private
  * Make `StreamHelper` package-private
  * Make `ThreeDSecureV1BrowserSwitchHelper` package-private
  * Make `TLSSocketFactory` package-private
  * Make `UnexpectedException` constructor package-private
  * Make `UnionPayConfiguration` package-private
  * Make `UnprocessableEntityException` constructor package-private
  * Make `UpgradeRequiredException` constructor package-private
  * Make `UUIDHelper` package-private
  * Make `VenmoConfiguration` package-private
  * Make `VisaCheckoutConfiguration` package-private
  * Move all classes to `com.braintreepayments.api` package

## 3.17.0 (2021-03-05)

* Add `bic` (Bank Identification Code) to `LocalPaymentRequest`

## 3.16.1 (2021-02-11)

* Bump Cardinal version to `2.2.5-1`

## 3.16.0 (2021-02-04)

* Add `accountType` to `ThreeDSecureRequest`
* Add `offerPayLater` to `PayPalRequest`

## 3.15.0 (2021-01-08)

* Add `paymentTypeCountryCode` to `LocalPaymentRequest`
* Upgrade PayPal Data Collector to 5.1.1 (fixes #325)

## 3.14.2 (2020-10-19)

* Bump Cardinal version to `2.2.4-1` (fixes [#305](https://github.com/braintree/braintree_android/issues/305))
* Bump `browser-switch` to `1.1.3`

## 3.14.1 (2020-09-25)

* Update `compileSdkVersion` and `targetSdkVersion` to 30

## 3.14.0 (2020-08-25)

* Expose cardholder name on `CardNonce`.
* Expose expiration month and year on `CardNonce`.
* Update `browser-switch` module to `1.1.0`.
* Fix bug where `onError` callback was invoked instead of the `onCancel` callback in the Local Payment Method flow (fixes #299, thanks @vijayantil1)
* Fix bug where `getReturnUrlScheme` is called and an Activity is no longer attached to the fragment (fixes [#308](https://github.com/braintree/braintree_android/issues/308), thanks @hakanbagci)

## 3.13.0 (2020-07-21)

* Update `browser-switch` module to `1.0.0`
* Make PayPalUAT::Environment enum public
* Add Card#tokenize overload to allow a custom `PaymentMethodNonceCallback` to be provided.

## 3.12.0 (2020-06-30)

* Changed `Configuration#isPayPalEnabled` to no longer consider deprecated integration requirements.

## 3.11.1 (2020-06-17)

* Add default `uiCustomization` to `ThreeDSecureRequest` to prevent null pointer exception when interacting with Cardinal SDK

## 3.11.0 (2020-06-16)

* Bump Cardinal version to 2.2.3-2
* Check if Fragment is active before handling Pay with PayPal result (fixes #295, thanks @brudaswen)

## 3.10.0 (2020-06-08)

* Allow new BraintreeFragment instances to be created using FragmentActivity
* Add support for authorizing the Braintree SDK with a `PayPalUAT` (universal access token)
* Fix bug that accepted raw JSON string as valid authorization to `BraintreeFragment.newInstance(...)`
* Add `threeDSecureAuthenticationId` field to `ThreeDSecureInfo`
* Update `braintree-android-google-payment` module to `3.3.1`

## 3.9.0 (2020-02-20)

* Update Cardinal SDK to `2.2.2-1`
* Fix bug in 3DS1 browser switch around accented characters in the redirect button and description (fixes #288)

## 3.8.0 (2020-01-15)

* Add support for basic UI customization of 3DS1 flows. See `ThreeDSecureV1UiCustomization`.

## 3.7.2 (2019-12-11)

* Update Cardinal SDK to `2.2.1-2`
* Use `synchronized` when adding to callback queue in `BraintreeFragment` (thanks @skauss)
* Update paypal-data-collector to 4.1.2

## 3.7.1 (2019-11-21)

* Update `braintree-android-google-payment` module to `3.1.0`
* Fix a bug so that `BraintreeFragment.newInstance` returns a new fragment whenever a new authorization string is passed in (Resolves issue #274. Thanks @krunk4ever and @bramley-stride.)

## 3.7.0 (2019-10-02)

* Update 3DS `prepareLookup` method to function asynchronously to wait for Cardinal SDK
* Add ability to request `AuthenticationInsight` when tokenizing a credit card, which can be used to make a decision about whether to perform 3D Secure verification
* Set error message on `ThreeDSecureInfo` when 3D Secure 2.0 challenge fails
* Include reference to Cardinal's docs for `uiCustomization` property on `ThreeDSecureRequest`.
* Add `requiresUserAuthentication` method to `ThreeDSecureLookup`
* Add support for `PayPalLineItem`

## 3.6.0 (2019-09-06)

* Add authentication and lookup transaction status information to ThreeDSecureInfo
* Add ability to customize UI for 3D Secure challenge views
* Fix race condition that caused inconsistent 3DS version flows

## 3.5.0 (2019-08-30)

* Add 3DSecure authentication details to card nonce

## 3.4.2 (2019-08-15)

* Add `acsTransactionId`, `threeDSecureServerTransactionId` and `paresStatus` fields to `ThreeDSecureInfo`

## 3.4.1 (2019-08-09)

* Update Cardinal SDK to 2.1.4-1

## 3.4.0 (2019-07-26)

* Send analytics timestamps in milliseconds
* Add additional fields to ThreeDSecureInfo
* Fix potential crash when 3DSecure 2.0 JWT is not available

## 3.3.0 (2019-07-15)

* Correctly includes the 3DSecure 2.0 module

## 3.2.0 (2019-07-10)

* Add 3DS 2 Support
* Update 3DS redirect to newest version

## 3.1.0 (2019-06-05)

* BraintreeFragment can now attach to a Fragment (fixes [#252](https://github.com/braintree/braintree_android/issues/252))

## 3.0.1 (2019-05-14)

* Update google-payment to 3.0.1
* Update endpoint for creating local payments

## 3.0.0 (2019-02-02)

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



## 2.21.0 (2019-01-30)

* Deprecate PayPal Future Payments, use PayPal Billing Agreements
* Deprecate AndroidPayConfiguration, use the GooglePaymentConfiguration alias

## 2.20.1 (2019-01-16)

* Fix null address properties on PayPalAccountNonce
  * Those addresses should always be at least an empty PostalAddress

## 2.20.0 (2018-12-17)

* Google Pay
  * Add groundwork for v2 compatibility
* Split PostalAddress into PostalAddress and PostalAddressParser
  * Deprecates PostalAddress.fromJson - use PostalAddressParser.fromJson
  * Add fromUserAddressJsonn to PostalAddressParser
  * Add additional fields

## 2.19.0 (2018-12-10)

* Move Google Payment to a separate module
* Downgrade browser-switch to 0.1.6
* Exclude customtabs from browser-switch dependency

## 2.18.1 (2018-10-31)

* Upgrade browser-switch to 0.1.7 fixes Chrome Custom Tab integration when using Jetifier to use AndroidX

## 2.18.0 (2018-10-29)

* Upgrade Android SDK to 28
* Fix PayPal JavaDoc

## 2.17.0 (2018-10-05)

* Local Payments
* Upgrade PayPal Data Collector to 4.0.3

## 2.16.0 (2018-09-04)

* Add optional merchant account Id to PayPalRequest
* Add openVenmoAppPageInGooglePlay method which opens Venmo on the Google Play

## 2.15.2 (2018-08-29)

* Fix NoClassDefFoundError compile error for PayPalDataCollector

## 2.15.1 (2018-08-17)

* Fix InvalidPathException error

## 2.15.0 (2018-08-16)

* Add `PaymentMethod#deletePaymentMethod` which allows customers to remove their vaulted payment methods
* Fix DataCollector not being available for instant run builds

## 2.14.2 (2018-07-19)

* Fix issue with TLS cipher in API < 21

## 2.14.1 (2018-07-12)

* Removed unused PayPal analytics event

## 2.14.0 (2018-06-15)

* Add shippingAddressEditable to PayPalRequest

## 2.13.2 (2018-06-13)

* Fix issue where address override was not set for PayPal billing agreements

## 2.13.1 (2018-06-04)

* Update 3D Secure redirect URL

## 2.13.0 (2018-05-02)

* 3D Secure
  * Add support for American Express SafeKey params
* Update PayPalDataCollector library to 3.1.6
* Catch possible SQLite exceptions

## 2.12.0 (2018-04-03)

* Move Visa Checkout to separate module
* Update Visa Checkout to 5.5.2
* Update SDK to 27

## 2.11.0 (2018-03-20)

* Add support for Venmo profiles
* Update PayPalDataCollector library to 3.1.5

## 2.10.0 (2018-02-06)

* Update GooglePaymentException to be parcelable
* Add browser switch support to 3D Secure integrations

## 2.9.0 (2018-01-24)

* Internal performance optimizations
* Deprecate `countryName`, `countryCodeAlpha2`, `countryCodeAlpha3`, and `countryCodeNumeric` in favor of `countryCode` in `CardBuilder` and `UnionPayCardBuilder`.

## 2.8.1 (2018-01-04)

* Support `lastFour` in `GooglePayCardNonce`
* Add Google Pay branding

## 2.8.0 (2017-12-21)

* Add support for iDEAL payments

## 2.7.3 (2017-11-14)

* Check package name is valid for PayPal Wallet switch

## 2.7.2 (2017-11-07)

* Fix phoneNumberRequired in GooglePayment

## 2.7.1 (2017-11-07)

* Add AmericanExpress support with getRewardsBalance method
* Use ExecutorService for async database operations

## 2.7.0 (2017-09-29)

* Increase minimum version of Google Play Services Wallet to 11.4.0
* Add support for the Google Payments API
* Deprecate Android Pay
* Add additional billing address params to `CardBuilder`

## 2.6.2 (2017-08-24)

* Fix potential crash due to optional Visa Checkout dependency

## 2.6.1 (2017-08-24)

* Fix potential crash due to optional Google Play Services dependency

## 2.6.0 (2017-08-15)

* Upgrade Kount DataCollector to 3.2
* Stop using dependency ranges (https://github.com/braintree/android-card-form/pull/29)
* Relax `PRNGFixes` check for `PRNGSecureRandomProvider` to prevent race condition with other providers ([#151](https://github.com/braintree/braintree_android/issues/151))
* Stop sending `Content-Type` header for GET requests ([#155](https://github.com/braintree/braintree_android/issues/155))
* Upgrade browser-switch to 0.1.4 to prevent losing Chrome Custom Tab when switching to a password manager or other app
* Add additional bin data to card based payment methods
* Add DOM and database storage to `ThreeDSecureWebView` to improve compatibility with some bank web pages ([#159](https://github.com/braintree/braintree_android/pull/159))
* Update compile and target SDK versions to 26
  * Any support library dependencies must now be 26.0.0 or newer

## 2.5.4 (2017-06-07)

* Use custom task instead of overriding the clean task (fixes [#153](https://github.com/braintree/braintree_android/issues/153))
* Accept third party cookies in ThreeDSecureWebView for Lollipop and above

## 2.5.3 (2017-05-11)

* Add PayPal Credit for Billing Agreements

## 2.5.2 (2017-04-28)

* Include cause in `ConfigurationException` ([#143](https://github.com/braintree/braintree_android/pull/143))
* Ignore ProGuard warnings for Visa Checkout (fixes [#144](https://github.com/braintree/braintree_android/issues/144))
* Fix Android Pay behavior during configuration changes (fixes [#145](https://github.com/braintree/braintree_android/issues/145), [#146](https://github.com/braintree/braintree_android/issues/146), [#147](https://github.com/braintree/braintree_android/issues/147))
* Fix crash when run in an Android Instant App

## 2.5.1 (2017-03-31)

* Fix non-optional `data-collector` dependency in Braintree
* Create `BraintreeRequestCodes` for use with `BraintreeCancelListener#onCancel`
* Move PayPal browser switches to use [browser-switch-android](https://github.com/braintree/browser-switch-android)

## 2.5.0 (2017-03-30)

* Add option to set display name in `PayPalRequest`
* Add option to set landing page type in `PayPalRequest`
* Add option to enable PayPal Credit in `PayPalRequest`
* Add Visa Checkout as a payment method
* Prevent dependency resolution of alpha major versions of support libraries

## 2.4.3 (2017-03-23)

* Improve `GoogleApiClientException` to include error type as well as reason code
* Changes to PayPalDataCollector to make it easier to use

## 2.4.2 (2017-02-10)

* Fix NPE in `AndroidPay#changePaymentMethod` (fixes [#139](https://github.com/braintree/braintree_android/issues/139))
* `Venmo#authorizeAccount` will now correctly vault the payment method when the vault option is true
* Fix missing client metadata ids in `PayPalAccountNonce`s
* Update paypal-data-collector to 3.1.4

## 2.4.1 (2017-01-25)

* Add workaround for [Kount/kount-android-sdk#2](https://github.com/Kount/kount-android-sdk/issues/2)
* Fix error returned by `AndroidPay#changePaymentMethod`

## 2.4.0 (2017-01-14)

* Fix back button during PayPal browser switch on Samsung devices (fixes [#137](https://github.com/braintree/braintree_android/issues/137))
* Add new intent option to `PayPalRequest`
* Fix crash when excluding the PayPal dependency
* Increase `minSdkVersion` to 16
  * API 16 is the first version of Android that supports TLSv1.2. For more information on Braintree's upgrade to TLSv1.2 see [the blog post](https://www.braintreepayments.com/blog/updating-your-production-environment-to-support-tlsv1-2/).

## 2.3.12 (2016-11-15)

* Improve PayPal address validations
* Work around `NullPointerException` in `BraintreeFragment#newInstance` (fixes [#125](https://github.com/braintree/braintree_android/issues/125))
* Document supported locales for PayPal
* Fix rare `NullPointerException` ([#128](https://github.com/braintree/braintree_android/pull/128))

## 2.3.11 (2016-10-24)

* Fix ProGuard rules (fixes [#124](https://github.com/braintree/braintree_android/issues/124))
* Fix `NullPointerException` when using deprecated DataCollector methods
* Update compile and target SDK versions to 25

## 2.3.10 (2016-10-07)

* Add `BraintreeFragment#getListeners` to get a list of all the registered listeners
* Upgrade paypal-data-collector to 3.1.3
* Upgrade Kount DataCollector to 3.1
* Add `AndroidPay#requestAndroidPay` and `AndroidPay#changePaymentMethod` methods to simplify requesting Android Pay from a user and changing the backing payment method.
* Include ProGuard directives in the SDK ([#120](https://github.com/braintree/braintree_android/pull/120))
* Work around bug in `JSONObject#optString`
* Use `FragmentTransaction#commitNow` and `FragmentManager#executePendingTransactions` in `BraintreeFragment#newInstance` to synchronously set up `BraintreeFragment` and avoid race conditions caused by asynchronous `Fragment` setup.

## 2.3.9 (2016-09-09)

* Update `AndroidPayCardNonce` description to include card type and last 4

## 2.3.8 (2016-09-09)

* Support changing user call to action in PayPal flows, see `PayPalRequest#userAction`
* Fix validate option not being sent when set to false in `PaymentMethodBuilder`
* Add merchant supported card types to `Configuration`
* Expose methods on `BraintreeFragment` for getting cached payment methods
* Update `paypal-data-collector` to 3.1.2
* Move Drop-In to [it's own repo](https://github.com/braintree/braintree-android-drop-in)

## 2.3.7 (2016-08-26)

* Update exception message when Android Manifest setup is invalid
* Fix unclosed `InputStream` (fixes [#115](https://github.com/braintree/braintree_android/issues/115))
* Post exception to error listener instead of throwing `IllegalStateException` when `BraintreeFragment` is not attached to an `Activity`
* Restore url when `BraintreeFragment` is recreated (fixes [#117](https://github.com/braintree/braintree_android/issues/117))
* Upgrade gradle build tools to 2.1.3
* Parse and return errors when Android Pay tokenization fails
* Add support for changing the backing card for Android Pay in Drop-In
* Call configuration callback whenever a new Activity is attached to `BraintreeFragment`

## 2.3.6 (2016-07-29)

* Allow vaulting of Venmo accounts. See `Venmo#authorizeAccount`.
* Remove Venmo whitelist check
* Fix `BraintreeCancelListener#onCancel` being invoked twice for PayPal cancellations (fixes [#112](https://github.com/braintree/braintree_android/issues/112))

## 2.3.5 (2016-07-20)

* Change `UnionPayCallback` to include `smsCodeRequired`
* Change `UnionPayCapabilities#isUnionPayEnrollmentRequired` to `UnionPayCapabilities#isSupported`
* Upgrade Google Play Services to [9.0.0,10.0.0)
* Upgrade support annotations to [24.0.0,25.0.0)
* Upgrade build tools to 24.0.0
* Update compile and target API versions to 24
* Fix `NullPointerException` in `AnalyticsIntentService`

## 2.3.4 (2016-07-07)

* Prevent invalid schemes from being used for browser switching (Packages containing underscores would generate invalid schemes)
* Fix `NoClassDefFoundError` in `DataCollector`
* Fix `NullPointerException` in `BraintreeFragment`

## 2.3.3 (2016-06-16)

* Add PayPal Checkout intent option (authorize or sale). See `PayPalRequest#intent`
* Update UnionPay support in demo app custom integration
* Update `android-card-form` to 2.3.1
* Fix `NullPointerException` in `AddPaymentMethodViewController` (fixes [#100](https://github.com/braintree/braintree_android/issues/100))
* Fix `IllegalStateException` when creating a `BraintreeFragment` (fixes [#104](https://github.com/braintree/braintree_android/issues/104))
* Fix `NullPointerException` when `BraintreeFragment` is not attached to an `Activity` (fixes [#105](https://github.com/braintree/braintree_android/issues/105))

## 2.3.2 (2016-06-06)

* Fix `NullPointerException` when handling a PayPal response (fixes [#101](https://github.com/braintree/braintree_android/issues/101))

## 2.3.1 (2016-05-24)

* Fix `NullPointerException`s in `BraintreeFragment` when not attached to an `Activity`
* Fix Chrome Custom Tabs Intent flags interfering with browser switch
* Add new `DataCollector#collectDeviceData` methods that use a callback; deprecate synchronous methods
* Reduce size of assets in Drop-In

## 2.3.0 (2016-05-03)

* UnionPay Beta *Please note*: this API is in beta and subject to change
* Add support for fetching a customer's payment methods
* Return a `RateLimitException` when a merchant account is being rate limited

## 2.2.5 (2016-04-13)

* Fixes
  * Update BraintreeHttpClient to support UTF-8 encoding (fixes [#85](https://github.com/braintree/braintree_android/issues/85))

## 2.2.4 (2016-04-11)

* Update PayPalDataCollector to 3.1.1
* Fixes
  * Update device collector to 2.6.1 (fixes [#87](https://github.com/braintree/braintree_android/issues/87))
  * Fix crash when `BraintreeFragment` has not been attached to an `Activity`
* Features
  * Add `PaymentRequest#defaultFirst` option
  * Add support for Chrome Custom tabs when browser switching

## 2.2.3 (2016-03-11)

* Fixes
  * Fix incorrect `groupId` of dependencies in pom file for 2.2.2

## 2.2.2 (2016-03-11)

:rotating_light: The `groupId`s in this version's pom files are incorrect and dependencies will not resolve. Do not use. :rotating_light:

* Update `PaymentButton` styling when PayPal is the only visible option
* Features
  * Add client side overrides for payment methods in Drop-in and `PaymentButton` to `PaymentRequest`
  * Add support for non-USD currencies and non-US shipping addresses in Android Pay
  * Return email, billing address and shipping address as part of an `AndroidPayCardNonce` from Drop-in
* Fixes
  * Fix back button not doing anything in Drop-in after an Android Pay error is returned
  * Deprecate `DataCollector#collectDeviceData` and add new signature to prevent a NullPointerException when using a fragment that is not attached to an `Activity`

## 2.2.1 (2016-01-30)

* Fixes
  * Fix support annotations being bundled in PayPalDataCollector jar

## 2.2.0 (2016-01-29)

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

## 2.1.2 (2016-01-11)

* Update Google Play Services Wallet to 8.4.0
* Use `ENVIRONMENT_TEST` for Android Pay requests in sandbox
* Add `AndroidPay#isReadyToPay` method

## 2.1.1 (2016-01-08)

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

## 2.1.0 (2015-12-07)

* Pay with Venmo
* `PaymentButton#newInstance` now accepts a container id to add `PaymentButton` to that container
* Android Pay assets
* Fixes
  * Add `onInflate` method for Android versions < 23
  * PayPal cancel events (fixes [#63](https://github.com/braintree/braintree_android/issues/63))

## 2.0.1 (2015-11-16)

* Make support annotations an optional dependency
* Cache configuration to prevent unnecessary network requests
* Fixes
  * Fix BraintreeDataCollector as an optional dependency
  * Fix `PaymentRequest` crash when Google Play Services is not present

## 2.0.0 (2015-11-11)

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

## 1.7.4 (2015-10-29)

* Fixes
  * Increase minimum version of Google Play Services Wallet to 8.0.0 to prevent `VerifyError`

## 1.7.3 (2015-10-23)

* Fixes
  * Fix Android Pay bug caused by shared state between Activities

## 1.7.2 (2015-10-21)

* Update PayPal SDK to 2.11.1 (fixes [#48](https://github.com/braintree/braintree_android/issues/48))

## 1.7.1 (2015-10-06)

* Fixes
  * Fix tokenization failure in Coinbase

## 1.7.0 (2015-10-05)

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

## 1.6.5 (2015-08-06)

* Update PayPal SDK to 2.9.10
* Fixes
  * Fix incorrect custom integration in demo app
  * Fix incorrect selected payment method in Drop-in after creating a new payment method
  * Fix `NoClassDefFoundError` crash in Drop-in

## 1.6.4 (2015-07-08)

* Update PayPal SDK to 2.9.8
* Improvements
  * Follow Android convention around button and text casing in Drop-in
  * Update android-card-form to [2.0.1](https://github.com/braintree/android-card-form/blob/master/CHANGELOG.md#201)

## 1.6.3 (2015-06-24)

* Improvements
  * BraintreeData can now be optionally excluded
* Fixes
  * Remove optional dependency from full jar

## 1.6.2 (2015-06-23)

* Update PayPal SDK to 2.9.7
* Add support for additional PayPal scopes to `PaymentButton`
* Fixes
  * Return error instead of silently failing setup with bad client tokens
  * Fix `NoClassDefFoundError` in Drop-in caused by optional dependency

## 1.6.1 (2015-06-15)

* Fixes
  * Fix `NoClassDefFoundError` in Drop-in and `PaymentButton` caused by optional dependency ([#34](https://github.com/braintree/braintree_android/issues/34))

## 1.6.0 (2015-06-12)

* Update PayPal SDK to 2.9.6
* Update gradle plugin to 1.2.3
* Update build tools to 22.0.1
* Features
  * Add Android Pay support. *Please note:* this API is in beta and subject to change.
  * Add `Braintree#onActivityResult` method
  * Add support for additional PayPal scopes
    * A `List` of additional scopes may be passed to `Braintree#startPayWithPayPal`
    * `PayPalAccount#getBillingAddress` can be used to retrieve the billing address when the address scope is requested.

## 1.5.1 (2015-05-23)

* Update PayPal SDK to 2.9.5
* Switch to OkHttp for Demo app
* Improvements
  * Add methods to persist state across rotations
* Fixes
  * Fix Demo app crash when `MainActivity` was destroyed ([#26](https://github.com/braintree/braintree_android/pull/26))
  * Fix NPE in Drop-in ([#30](https://github.com/braintree/braintree_android/issues/30))
  * Fix ProGuard support and add ProGuard rules ([#29](https://github.com/braintree/braintree_android/issues/29))
  * Fix Drop-in error handling for non-card errors

## 1.5.0 (2015-05-08)

* Update PayPal SDK to 2.9.4
* Move `CardForm` to [separate repo](https://github.com/braintree/android-card-form)
* Deprecate `Braintree#getInstance` in favor of `Braintree#setup`
* Fixes
  * Remove metadata from assets, fixes [#16](https://github.com/braintree/braintree_android/issues/16)

## 1.4.0 (2015-03-26)

* Update gradle plugin to 1.1.2
* Update build tools to 22
* Update `compileSdkVersion` and `targetSdkVersion` to 22
* Update PayPal SDK to 2.9.0
* Features
  * Add support for 3D Secure. *Please note:* this API is in beta and subject to change.
* Fixes
  * Fix missing expiration date float label (#21)

## 1.3.0 (2015-02-05)

* Remove Drop-In support for Eclipse
* Open source [card form](CardForm) separate from Drop-In
* Update PayPal SDK to 2.8.5
  * card.io is no longer included in the SDK
* Update Espresso to 2.0
* Remove unused PayPal `PROFILE` scope

## 1.2.7 (2014-12-05)

* Update gradle plugin to 0.14.1
* Update build tools to 21.1.1
* Update PayPal SDK to 2.7.3
* Remove `android:allowBackup="false"` from library manifests, apps will now be able to choose if they allow backups
* Remove `ACCESS_WIFI_STATE` permission
* Improvements
  * Add localizations for more locales (da-rDK, en-rAU, es, fr-rCA, iw-rIL, nl, no, pl, pt, ru, sv-rSE, tr, zh-rCN)
  * Add initial right to left language support
  * Add type safety to `Braintree#addListener(Listener)`. Thanks @adstro!

## 1.2.6 (2014-10-31)

* Increase `targetSdkVersion` to 21
* Increase `buildToolsVersion` to 21.0.2
* Fixes
  * Fix max length on `EditText`s
  * Fix crash caused by `PRNGFixes`
* Improvements
  * Update PayPal SDK
  * Add first and last name to `CardBuilder`

## 1.2.5 (2014-10-16)

* Fixes
  * Fix incorrectly named language resource directories

## 1.2.4 (2014-10-15)

* Fixes
  * Work around manifest merging issues on newer build plugins

## 1.2.3 (2014-10-10)

* minSdk is now 10
* Fixes
  * Set max length on card field for unknown card types in Drop-In
  * Update PayPal SDK to fix rotation bug
  * Fix edge cases in expiration entry in Drop-In
* Improvements
  * Error messages are now returned from Drop-In
  * Drop-In auto advances to next field now

## 1.2.2 (2014-10-01)

* Fixes
  * Fix crash caused by too large request code in `PaymentButton`
  * Resume the payment method form after rotation
* Improvements
  * Updated PayPal SDK
    * email scope is now requested in all PayPal requests
  * `correlationId` is now included in the device data string returned from `BraintreeData#collectDeviceData`

## 1.2.1 (2014-09-12)

* Fixes
  * BraintreeApi release now includes the PayPal SDK again. Sorry!
* Improvements
  * All assets are now namespaced to avoid any conflicts on import.
  * Updated PayPal SDK

## 1.2.0 (2014-09-08)

* Features
  * App switch based payments for PayPal and Venmo (One Touch)
    * No changes for existing Pay With PayPal integrations
    * See [the docs](https://developers.braintreepayments.com/android/guides/one-touch) for more information
  * Unified payment button (`PaymentButton`) for PayPal and/or Venmo payments
* Improvements
  * Minor bugfixes and internal tweaks
* Deprecations
  * `PayPalButton` is deprecated in favor of `PaymentButton`

## 1.1.0 (2014-08-25)

* Breaking Change
  * BraintreeData returns `deviceData` instead of `deviceSessionId` on `collectDeviceData`
* Improvements
  * References `sdk-manager-plugin` from vendor to simplify build process

## 1.0.8 (2014-08-14)

* Improvements
  * CardBuilder now accepts billing address fields other than postal code (credit: @chiuki)
* Packaging
  * Fixed an issue building Drop-In in Eclipse

## 1.0.7 (2014-08-12)

* Improvements
  * BraintreeApi no longer depends on OkHttp
  * Added localizations for more locales (UK, FR, DE, IT)

## 1.0.6 (2014-08-08)

* Fixes
  * Fixed disabled submit button in landscape
  * Fixed next field button in landscape
  * Add max length to expiration date and prevent user from typing illegal characters
* Move to sdk-manager-plugin for CI dependencies

## 1.0.5 (2014-08-05)

* Packaging
  * Set Braintree package to default to AAR instead of ZIP

## 1.0.4 (2014-08-01) - Gradle and Maven will incorrectly download the ZIP instead of AAR, use 1.0.5+

* Improvements
  * Added assets for a wider range of resolutions
  * Enforce maximum length for card and postal code fields
  * Added README for fraud tools
* Packaging
  * Improvements for usage in environments other than Maven or Gradle
* Fixes
  * Fixed lint errors

## 1.0.3 (2014-07-18)

* Fixes
  * Fix crash on Android SDK < 19
* Add PayPal `correlationId` to PayPal account creation

## 1.0.2 (2014-07-16) - crash on Android SDK < 19, do not use

* Fixes
  * Improved packaging for non-Gradle uses of SDK

## 1.0.1 (2014-07-11)

* Fixes
  * Attach Javadocs and sources to Maven Central build.

## 4.23.0
* Breaking Changes
  * Bump `minSdkVersion` to 21.

## 3.3.1
* Fix `allowedCardNetworks` in `isReadyToPayRequest` to be uppercased. Thanks @fcastagnozzi.

## 3.3.0
* Add support for Google Pay's `existingPaymentMethodRequired` option

## 3.2.0
* Add support for `isNetworkTokenized`

## 3.1.1
Fix setting the correct version in metadata

## 3.1.0

* Add check in `requestPayment` to avoid Null Pointer Exception

## 3.0.1

* Resolve issue where optional shipping parameters were treated as if they were required
* Use GooglePayment PayPal client ID

## 3.0.0

* Convert to AndroidX
* Replace AndroidPayConfiguration with GooglePaymentConfiguration

## 2.0.1

* Disable PayPal payment method in Google Payment when the merchant is not able to process PayPal

## 2.0.0

* Add support for Google Pay v2
* Remove support for Google Pay v1
  * To continue using v1, add google-payment:1.0.0 to your build.gradle
  * v1 will remain the defaul for braintree android until the next major version bump
* Replace all UserAddress objects with PostalAddress objects

## 1.0.0

* Public release of [v.zero](https://www.braintreepayments.com/v.zero) SDK
