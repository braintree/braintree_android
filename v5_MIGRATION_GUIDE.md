# Braintree Android v5 Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the
basics for updating your Braintree integration from v4 to v5.

## Table of Contents

1. [Supported Versions](#supported-versions)
1. [Gradle Dependencies](#gradle-dependencies)
1. [Braintree Client](#braintree-client)
1. [American Express](#american-express)
1. [Data Collector](#data-collector)
1. [Card](#card)
1. [Union Pay](#union-pay)
1. [Venmo](#venmo)
1. [Google Pay](#google-pay)
1. [3DS](#3ds)
1. [PayPal](#paypal)
1. [Local Payment](#local-payment)
1. [SEPA Direct Debit](#sepa-direct-debit)
1. [Visa Checkout](#visa-checkout)
1. [Samsung Pay](#samsung-pay)
1. [PayPal Native Checkout](#paypal-native-checkout)
1. [Chrome Custom Tab Picture-in-Picture](#chrome-custom-tab-picture-in-picture)

## Supported Versions

V5 of the Braintree Android SDK bumps the following supported versions:

* Minimum supported Android API 23
* Requires Gradle JDK 17+
* Requires Kotlin 1.9.10+
* Requires Android Gradle Plugin 8.1.4+ (Giraffe or higher)

## Gradle Dependencies

Version 4 of the SDK is not compatible with version 5 of the SDK, so all Braintree Android SDK 
dependencies must be updated to version 5.x in order to upgrade. No other changes in dependencies 
are required.

```diff
- implementation 'com.braintreepayments.api:card:4.x.x'
+ implementation 'com.braintreepayments.api:card:5.3.0'
```

## Braintree Client

You no longer need to instantiate a `BraintreeClient` in order to instantiate the payment method 
clients. Instead, construct the payment method clients with `context` and `authorization` 
parameters directly. 

```kotlin
val cardClient = CardClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
```

## Data Collector

The `paypal-data-collector` module has been removed and replaced by the `data-collector` module.
The `DataCollector` class within the `data-collector` module has the same
`collectDeviceData` methods.

If you were using the `data-collector` library in v4, `DataCollector#collectDeviceData(context,
merchantId, callback)` is now `DataCollector#collectDeviceData(context, riskCorrelationId,
callback)`, where `riskCorrelationId` is an optional client metadata ID.

```kotlin
val dataCollector = DataCollector(context, authorization)
val dataCollectorRequest = DataCollectorRequest(hasUserLocationConsent)

dataCollector.collectDeviceData(context, dataCollectorRequest) { result ->
    if (result is DataCollectorResult.Success) {
        // send result.deviceData to your server
    }
}
```

## Card

The card tokenization integration has been updated to simplify instantiation and result handling.

```kotlin
val cardClient = CardClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")

cardClient.tokenize(card) { cardResult ->
    when (cardResult) {
        is CardResult.Success -> { /* handle cardResult.nonce */ }
        is CardResult.Failure -> { /* handle cardResult.error */ }
    }
}
```

## American Express

The result handling of fetching American Express rewards balance has been updated so that the
`AmericanExpressGetRewardsBalanceCallback` returns a single `AmericanExpressResult` object

```kotlin
val americanExpressClient = AmericanExpressClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")

americanExpressClient.getRewardsBalance(nonce, currencyCode) { result ->
    when(result) {
        is AmericanExpressResult.Success -> { /* handle result.rewardsBalance */ }
        is AmericanExpressResult.Failure -> { /* handle result.error */ }
    }
}
```

## Union Pay

The `union-pay` module, and all containing classes, was removed in v5. UnionPay cards can now be
processed as regular cards, through the `card` module. You no longer need to manage card enrollment
via SMS authorization.

Now, you can tokenize with just the card details:

```kotlin
val cardClient = CardClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")

cardClient.tokenize(unionPayCard) { cardResult ->
    when (cardResult) {
        is CardResult.Success -> { /* handle cardResult.nonce */ }
        is CardResult.Failure -> { /* handle cardResult.error */ }
    }
}
```

## Venmo

The Venmo integration has been updated to allow for more flexibility with app switching and
activity result handling.

`VenmoLauncher` has been added to handle the app switching portion of the Venmo flow for user 
authentication via the Venmo app. This class uses the Android Activity Result API and therefore 
must be instantiated in the `OnCreate` method of your Activity or `OnCreateView` of your Fragment.

`BraintreeClient` and `VenmoClient` no longer require references to Fragment or Activity and do not
need to be instantiated in `OnCreate`.

```diff
class MyActivity : FragmentActivity() {
    
+   private lateinit var venmoLauncher: VenmoLauncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var venmoClient: VenmoClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize Venmo classes outside of onCreate if desired
        initializeVenmo()

+       // VenmoLauncher must be initialized in onCreate 
+       venmoLauncher = VenmoLauncher()
    }
    
    private fun initializeVenmo() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       venmoClient = VenmoClient(this, braintreeClient)
-       venmoClient.setListener(this)
+       venmoClient = VenmoClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
    }
    
    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
+       handleReturnToApp(intent)
    }
    
    // ALL OTHER ACTIVITY LAUNCH MODES 
    override fun onResume() {
+       handleReturnToApp(intent)
    }
    
    private fun handleReturnToApp(intent: Intent) {
       // fetch stored VenmoPendingRequest.Success 
+       fetchPendingRequestFromPersistantStore()?.let {
+          when (val paymentAuthResult = venmoLauncher.handleReturnToApp(VenmoPendingRequest.Started(it), intent)) {
+               is VenmoPaymentAuthResult.Success -> {
+                   completeVenmoFlow(paymentAuthResult)
+                   // clear stored VenmoPendingRequest.Success
+               }
+               is VenmoPaymentAuthResult.NoResult -> {
+                   // user returned to app without completing Venmo flow, handle accordingly
+               }
+               is VenmoPaymentAuthResult.Failure -> {
+                   // handle error case
+               }
+          }
+       }   
    }
    
    private fun onVenmoButtonClick() {
-       venmoClient.tokenizeVenmoAccount(activity, request)
+       venmoClient.createPaymentAuthRequest(this, venmoRequest) { paymentAuthRequest ->
+           when (paymentAuthRequest) {
+               is VenmoPaymentAuthRequest.ReadyToLaunch -> {
+                   val pendingRequest = venmoLauncher.launch(this, paymentAuthRequest)
+                   when (pendingRequest) {
+                       is VenmoPendingRequest.Started -> { /* store pending request */ }
+                       is VenmoPendingRequest.Failure -> { /* handle error */ }
+                   }
+               }
+               is VenmoPaymentAuthRequest.Failure -> { /* handle paymentAuthRequest.error */ }
+           }
+       }
    }
    
    private fun completeVenmoFlow(paymentAuthResult: VenmoPaymentAuthResult.Success) {
+       venmoClient.tokenize(paymentAuthResult) { result ->
+           when (result) {
+               is VenmoResult.Success -> { /* handle result.nonce */ }
+               is VenmoResult.Failure -> { /* handle result.error */ }
+               is VenmoResult.Cancel -> { /* handle user canceled */ }
+           }          
+       }
    }
    
-   override fun onVenmoSuccess(venmoAccountNonce: VenmoAccountNonce) {
-        // handle Venmo account nonce
-   }
      
-   override fun onVenmoFailure(error: Exception) {
-        // handle error
-   }
}
```

## Google Pay

The Google Pay integration has been updated to allow for more flexibility with app switching and
activity result handling.

`GooglePayLauncher` has been added to handle the app switching portion of the Google Pay flow for 
payment authorization via the Google Pay payment sheet. This class uses the Android Activity Result 
API and therefore must be instantiated in the `OnCreate` method of your Activity or `OnCreateView` 
of your Fragment.

`BraintreeClient` and `GooglePayClient` no longer require references to Fragment or Activity and
do not need to be instantiated in `OnCreate`.

The `TransactionInfo` object has been replaced with individual parameters on the `GooglePayRequest` 
for transaction info: `currencyCode`, `totalPrice`, and `totalPriceStatus`.

```diff
class MyActivity : FragmentActivity() {
    
+   private lateinit var googlePayLauncher: GooglePayLauncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var googlePayClient: GooglePayClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize the GooglePayClient outside of onCreate if desired
-       initializeGooglePayClient()
        
+       // GooglePayLauncher must be initialized in onCreate 
+       googlePayLauncher = GooglePayLauncher(this) { paymentAuthResult ->
+            googlePayClient.tokenize(paymentAuthResult) { googlePayResult ->
+               when (googlePayResult) {
+                   is GooglePayResult.Failure -> { /* handle error */ }
+                   is GooglePayResult.Cancel -> { /* handle cancel */ }
+                   is GooglePayResult.Success -> { /* handle nonce */ }
+               }
+            }
+       }
    }
    
    private fun initializeGooglePayClient() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       goolePayClient = GooglePayClient(this, braintreeClient)
+       googlePayClient = GooglePayClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       googlePayClient.setListener(this)

-       googlePayClient.isReadyToPay(this) { isReadyToPay, error ->
+       googlePayClient.isReadyToPay(this) { readinessResult ->
+           if (readinessResult is GooglePayReadinessResult.ReadyToPay) {
+                // show Google Pay button 
+           }
+        }
    }
    
    private fun onGooglePayButtonClick() {
-       googlePayClient.requestPayment(activity, request)
+       googlePayClient.createPaymentAuthRequest(request) { paymentAuthRequest ->
+           when (paymentAuthRequest) {
+            is GooglePayPaymentAuthRequest.Failure -> { /* handle error */ }
+            is GooglePayPaymentAuthRequest.ReadyToLaunch -> { 
+               googlePayLauncher.launch(paymentAuthRequest) 
+            }
+       }
    }
    
-   override fun onGooglePaySuccess(paymentMethodNonce: PaymentMethodNonce) {
-        // handle payment method nonce
-   }
      
-   override fun onGooglePayFailure(error: java.lang.Exception) {
-        // handle error
-   }
}
```

## 3DS

The ThreeDSecure integration has been updated to allow for more flexibility with app switching and
activity result handling.

`ThreeDSecureLauncher` has been added to handle the app switching portion of the 3DS flow for user
authentication. This class uses the Android Activity Result API and therefore must be instantiated
in the `OnCreate` method of your Activity or `onCreateView` method of your Fragment.

`BraintreeClient` and `ThreeDSecureClient` no longer require references to Fragment or Activity and
do not need to be instantiated in `OnCreate`.

3DS V1 is no longer supported, so `versionRequested` has been removed from `ThreeDSecureRequest` 
and `ThreeDSecureV1UICustomization` has been removed.

```diff
class MyActivity : FragmentActivity() {
    
+   private lateinit var threeDSecureLauncher: ThreeDSecureLauncher
-    private lateinit var braintreeClient: BraintreeClient
    private lateinit var threeDSecureClient: ThreeDSecureClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeThreeDSecure()

+       // ThreeDSecureLauncher must be initialized in onCreate
+       threeDSecureLauncher = ThreeDSecureLauncher(this) { paymentAuthResult ->
+            threeDSecureClient.tokenize(paymentAuthResult) { result ->
+               when (result) {
+                   is ThreeDSecureResult.Success -> { /* send result.nonce to server */}
+                   is ThreeDSecureResult.Failure -> { /* handle result.error */}
+                   is ThreeDSecureResult.Cancel -> { /* user canceled authentication */}
+            }
+       }
    }
    
    private fun initializeThreeDSecure() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       threeDSecureClient = ThreeDSecureClient(this, braintreeClient)
+       threeDSecureClient = ThreeDSecureClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       threeDSecureClient.setListener(this)
    }
    
    fun onCardTokenization() {
-       threeDSecureClient.performVerification(activity, threeDSecureRequest) { 
-          threeDSecureResult, error ->
-             error?.let { /* handle error */ }
-             threeDSecureInternalResult?.let {
-                if (it.lookup.requiresAuthentication) {
-                   threeDSecureClient.continuePerformVerification(MyActivity@this, request, it)
-                else { /* no additional user authentication needed, handle threeDSecureResult */ }
-              }
+       threeDSecureClient.createPaymentAuthRequest(this, threeDSecureRequest) { paymentAuthRequest -> 
+             when (paymentAuthRequest) {
+                is ThreeDSecurePaymentAuthRequest.ReadyToLaunch -> {
+                    threeDSecureLauncher.launch(paymentAuthRequest) 
+                }
+                is ThreeDSecurePaymentAuthRequest.LaunchNotRequired -> {
+                    // no additional authentication challenge needed
+                    // send paymentAuthRequest.nonce to server
+                }
+                is ThreeDSecurePaymentAuthRequest.Failure -> { /* handle error */ }
+            }
+        }
    }
    
-   override fun onThreeDSecureSuccess(threeDSecureParams: ThreeDSecureResult) {
-        // handle threeDSecureParams.tokenizedCard
-   }
      
-   override fun onThreeDSecureFailure(error: Exception) {
-        // handle error
-   }
}
```

## PayPal

The PayPal integration has been updated to allow for more flexibility with browser switching and
result handling.

`PayPalLauncher` has been added to handle the browser switching portion of the PayPal flow for
payment authorization via PayPal login. This class is used to handle browser switch 
results upon returning to your app from the web flow, therefore must be instantiated in or 
before the `OnResume` method of your Activity or Fragment.

`BraintreeClient` and `PayPalClient` no longer require references to Fragment or Activity and
do not need to be instantiated in `OnCreate`.

The PayPal integration now requires an Android App link be configured to return to your app from the 
PayPal flow. See the [App Link Setup Guide](APP_LINK_SETUP.md) for more information.

```diff
class MyActivity : FragmentActivity() {

+   private lateinit var payPalLauncher: PayPalLauncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var payPalClient: PayPalClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize client outside of onCreate if desired
-       initializePayPal()

+       // PayPalLauncher must be initialized in onCreate
+       payPalLauncher = PayPalLauncher()
    }
    
    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
+       handleReturnToApp(intent)
    }
    
    // ALL OTHER ACTIVITY LAUNCH MODES 
    override fun onResume() {
+       handleReturnToApp(intent)
    }
    
    private fun handleReturnToApp(intent: Intent) {
       // fetch stored PayPalPendingRequest.Success 
+       fetchPendingRequestFromPersistantStore()?.let {
+          when (val paymentAuthResult = payPalLauncher.handleReturnToApp(PayPalPendingRequest.Started(it), intent)) {
+               is PayPalPaymentAuthResult.Success -> {
+                   completePayPalFlow(paymentAuthResult)
+                   // clear stored PayPalPendingRequest.Success
+               }
+               is PayPalPaymentAuthResult.NoResult -> {
+                   // user returned to app without completing PayPal flow, handle accordingly
+               }
+               is PayPalPaymentAuthResult.Failure ->  {
+                   // handle error case
+               }
+          }
+       }   
    }
    
    private fun initializePayPal() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       payPalClient = PayPalClient(this, braintreeClient)
+       payPalClient = PayPalClient(
+            this, 
+            "TOKENIZATION_KEY_OR_CLIENT_TOKEN", 
+            Uri.parse("https://merchant-app.com") // Merchant App Link
+       )
-       payPalClient.setListener(this)
    }
    
    fun onPayPalButtonClick() {
-       payPalClient.tokenizePayPalAccount(activity, request)
+       payPalClient.createPaymentAuthRequest(this, request) { paymentAuthRequest ->
+           when (paymentAuthRequest) {
+               is PayPalPaymentAuthRequest.ReadyToLaunch -> {
+                   val pendingRequest = payPalLauncher.launch(this, paymentAuthRequest)
+                   when (pendingRequest) {
+                       is PayPalPendingRequest.Started -> { /* store pending request */ }
+                       is PayPalPendingRequest.Failure -> { /* handle error */ }
+                   }
+               }
+               is PayPalPaymentAuthRequest.Failure -> { /* handle paymentAuthRequest.error */ }
+           }
+       }
    }
    
    private fun completePayPalFlow(paymentAuthResult: PayPalPaymentAuthResult.Success) {
+       payPalClient.tokenize(paymentAuthResult) { result ->
+           when (result) {
+               is PayPalResult.Success -> { /* handle result.nonce */ }
+               is PayPalResult.Failure -> { /* handle result.error */ }
+               is PayPalResult.Cancel -> { /* handle user canceled */ }
+           }          
+       }
    }
    
-   override fun onPayPalSuccess(payPalAccountNonce: PayPalAccountNonce) {
-        // handle paypal account nonce
-   }
      
-   override fun onPayPalFailure(error: java.lang.Exception) {
-        // handle error
-   }
}
```

## Local Payment

The Local Payment integration has been updated to allow for more flexibility with browser
switching and result handling.

`LocalPaymentLauncher` has been added to handle the browser switching portion of the local payment
flow for payment authorization via bank login. This class is used to handle browser switch results
upon returning to your app from the web flow, therefore must be instantiated in or before
the `OnResume` method of your Activity or Fragment.

`BraintreeClient` and `LocalPaymentClient` no longer require references to Fragment or Activity and
do not need to be instantiated in `OnCreate`.

```diff
class MyActivity : FragmentActivity() {

+   private lateinit var localPaymentLauncher: LocalPaymentLauncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var localPaymentClient: LocalPaymentClient

    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeLocalPayment()

+       // LocalPaymentLauncher must be initialized in onCreate
+       localPaymentLauncher = LocalPaymentLauncher()
    }

    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
+       handleReturnToApp(intent)
    }
    
    // ALL OTHER ACTIVITY LAUNCH MODES 
    override fun onResume() {
+       handleReturnToApp(intent)
    }
    
    private fun handleReturnToApp(intent: Intent) {
       // fetch stored LocalPaymentPendingRequest.Success 
+       fetchPendingRequestFromPersistantStore()?.let {
+          when (val paymentAuthResult = localPaymentLauncher.handleReturnToApp(LocalPaymentPendingRequest.Started(it), intent)) {
+               is LocalPaymentAuthResult.Success -> {
+                   completeLocalPaymentFlow(paymentAuthResult)
+                   // clear stored LocalPaymentPendingRequest.Success
+               }
+               is LocalPaymentAuthResult.NoResult -> {
+                   // user returned to app without completing Local Payment flow, handle accordingly
+               }
+               is LocalPaymentAuthResult.Failure -> {
+                   // handle error case
+               }
+          }
+       }   
    }

    private fun initializeLocalPayment() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       localPaymentClient = LocalPaymentClient(this, braintreeClient)
+       localPaymentClient = LocalPaymentClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       localPaymentClient.setListener(this)
    }

    private fun onPaymentButtonClick() {
-       localPaymentClient.startPayment(activity, request)
+       localPaymentClient.createPaymentAuthRequest(request) { paymentAuthRequest ->
+           when (paymentAuthRequest) {
+               is LocalPaymentAuthRequest.ReadyToLaunch -> {
+                   localPaymentLauncher.launch(this, paymentAuthRequest)
+               }
+               is LocalPaymentAuthRequest.Failure -> { /* handle paymentAuthRequest.error */ }
+           }
+       }
    }
    
    private fun completeLocalPaymentFlow(paymentAuthResult: LocalPaymentAuthResult.Success) {
+       localPaymentClient.tokenize(this, paymentAuthResult) { result ->
+            when (result) {
+                is LocalPaymentResult.Success -> { /* handle result.nonce */ }
+                is LocalPaymentResult.Failure -> { /* handle result.error */ }
+                is LocalPaymentResult.Cancel -> { /* handle user canceled */ }
+            }        
+       }
    }

-   override fun onLocalPaymentSuccess(localPaymentNonce: LocalPaymentNonce) {
-        // handle local payment nonce
-   }

-   override fun onLocalPaymentFailure(error: java.lang.Exception) {
-        // handle error
-   }
}
```

## SEPA Direct Debit

The SEPA Direct Debit integration has been updated to allow for more flexibility with browser
switching and result handling.

`SEPADirectDebitLauncher` has been added to handle the browser switching portion of the SEPA
flow for payment authorization via bank mandate. This class is used to handle browser switch results
upon returning to your app from the web flow, therefore must be instantiated in or before
the `OnResume` method of your Activity or Fragment.

`BraintreeClient` and `SEPADirectDebitClient` no longer require references to Fragment or Activity and
do not need to be instantiated in `OnCreate`.

```diff
class MyActivity : FragmentActivity() {

+   private lateinit var sepaDirectDebitLauncher: SEPADirectDebitLauncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var sepaDirectDebitClient: SEPADirectDebitClient

    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeSEPA()

+       // SEPADirectDebitLauncher must be initialized in onCreate
+       sepaDirectDebitLauncher = SEPADirectDebitLauncher() 
    }

    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
+       handleReturnToApp(intent)
    }

    // ALL OTHER ACTIVITY LAUNCH MODES 
    override fun onResume() {
+       handleReturnToApp(intent)
    }
    
    private fun handleReturnToApp(intent: Intent) {
       // fetch stored SEPADirectDebitPendingRequest.Success 
+       fetchPendingRequestFromPersistantStore()?.let {
+          when (val paymentAuthResult = sepaDirectDebitLauncher.handleReturnToApp(SEPADirectDebitPendingRequest.Started(it), intent)) {
+               is SEPADirectDebitPaymentAuthResult.Success -> {
+                   completSEPAFlow(paymentAuthResult)
+                   // clear stored SEPADirectDebitPendingRequest.Success
+               }
+               is SEPADirectDebitPaymentAuthResult.NoResult -> {
+                   // user returned to app without completing flow, handle accordingly
+               }
+               is SEPADirectDebitPaymentAuthResult.Failure -> {
+                   // handle error case
+               }
+          }
+       }   
    }

    private fun initializeSEPA() {
-        braintreeClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       sepaDirectDebitClient = SEPADirectDebitClient(this, braintreeClient)
+       sepaDirectDebitClient = SEPADirectDebitClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       sepaDirectDebitClient.setListener(this)
    }

    private fun onPaymentButtonClick() {
-       sepaDirectDebitClient.createPaymentAuthRequest(activity, request)
+       sepaDirectDebitClient.createPaymentAuthRequest(request) { paymentAuthRequest ->
+           when (paymentAuthRequest) {
+               is SEPADirectDebitPaymentAuthRequest.Failure -> { 
+                   // handle paymentAuthRequest.error
+               }
+               is SEPADirectDebitPaymentAuthRequest.LaunchNotRequired -> {      
+                   // web-flow mandate not required, handle paymentAuthRequest.nonce
+               }
+               is SEPADirectDebitPaymentAuthRequest.ReadyToLaunch -> {
+                    // web-flow mandate required
+                   val pendingRequest = sepaDirectDebitLauncher.launch(this, paymentAuthRequest)
+                   when (pendingRequest) {
+                       is SEPADirectDebitPendingRequest.Started -> { /* store pending request */ }
+                       is SEPADirectDebitPendingRequest.Failure -> { /* handle error */ }
+                   }              
+               }
+           }
+       }
    }
    
    private fun completeSEPAFlow(paymentAuthResult: SEPADirectDebitPaymentAuthResult.Success) {
+       sepaDirectDebitClient.tokenize(paymentAuthResult) { result -> 
+            when (result) {
+               is SEPADirectDebitResult.Success -> { /* handle result.nonce */ }
+               is SEPADirectDebitResult.Failure -> { /* handle result.error */ }
+               is SEPADirectDebitResult.Cancel -> { /* handle user canceled */ }
+            }  
+       }
    }

-   override fun onSEPADirectDebitSuccess(sepaDirectDebitNonce: SEPADirectDebitNonce) {
-        // handle SEPA nonce
-   }

-   override fun onSEPADirectDebitFailure(error: java.lang.Exception) {
-        // handle error
-   }
}
```

## Visa Checkout

Visa checkout is not yet available for v5.

## Samsung Pay

The Samsung Pay integration is no longer supported. Please remove it from your app.

## PayPal Native Checkout

The PayPal Native Checkout integration is no longer supported. Please remove it from your app and 
use the PayPal (web) integration.

## Chrome Custom Tab Picture-in-Picture
Google has added a Picture-in-Picture feature to Chrome Custom Tabs. Users are now able to minimize the checkout flow at
any point while the Chrome Custom Tab is active.

When the Chrome Custom Tab is minimized and your app is resumed, calling `handleReturnToApp()` on the launcher class 
will return `NoResult` instead of `Success` or `Failure`.  At this point you can prompt the user to return to the Chrome
Custom Tab and complete the checkout flow.

PayPal Example:
```kotlin
override fun onResume() {
    super.onResume()

    getPendingRequest()?.let { pendingRequest ->
        when (val paymentAuthResult = payPalLauncher.handleReturnToApp(pendingRequest, intent)) {
            is PayPalPaymentAuthResult.NoResult -> {
                // Prompt user to return to the Chrome Custom Tab to complete the checkout flow
            }
            ...
        }
    }
}
```
