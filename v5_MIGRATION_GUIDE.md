# Braintree Android v5 Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the
basics for updating your Braintree integration from v4 to v5.

## Table of Contents

1. [Android API](#android-api)
1. [Braintree Client](#braintree-client)
1. [Data Collector](#data-collector)
1. [Card](#card)
1. [Union Pay](#union-pay)
1. [Venmo](#venmo)
1. [Google Pay](#google-pay)
1. [3DS](#3ds)
1. [PayPal](#paypal)
1. [Local Payment](#local-payment)
1. [SEPA Direct Debit](#sepa-direct-debit)

## Android API

The minimum supported Android API level for v5 of this SDK has increased to 23.

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
`collectDeviceData` methods, so if you were previously using the `paypal-data-collector` library,
no code changes are required aside from updating your dependency.

If you were using the `data-collector` library in v4, `DataCollector#collectDeviceData(context,
merchantId, callback)` is now `DataCollector#collectDeviceData(context, riskCorrelationId,
callback)`, where `riskCorrelationId` is an optional client metadata ID.

```kotlin
val dataCollector = DataCollector(context, authorization)
dataCollector.collectDeviceData(context) { deviceData, error ->
    // send deviceData to your server
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

## Union Pay

The `union-pay` module, and all containing classes, was removed in v5. UnionPay cards can now be
processed as regular cards, through the `card` module. You no longer need to manage card enrollment
via SMS authorization.

Now, you can tokenize with just the card details:

// TODO: code snippet of card integration in v5

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
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       venmoLauncher = VenmoLauncher(this) { paymentAuthResult ->
+            venmoClient.tokenize(paymentAuthResult) { result ->
+               when(result) {
+                   is VenmoResult.Success -> { /* handle result.nonce */ }
+                   is VenmoResult.Failure -> { /* handle result.error */ }
+                   is VenmoResult.Cancel -> { /* handle user canceled */ }
+               }
+            }
+       }
    }
    
    fun initializeClients() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       venmoClient = VenmoClient(this, braintreeClient)
+       venmoClient = VenmoClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       venmoClient.setListener(this)
    }
    
    fun onVenmoButtonClick() {
-       venmoClient.tokenizeVenmoAccount(activity, request)
+       venmoClient.createPaymentAuthRequest(this, venmoRequest) { paymentAuthRequest ->
+           when(paymentAuthRequest) {
+               is VenmoPaymentAuthRequest.ReadyToLaunch -> {
+                   venmoLauncher.launch(paymentAuthRequet)
+               }
+               is VenmoPaymentAuthRequest.Failure -> { /* handle paymentAuthRequest.error +/ }
+           }
+       }
    }
    
-   override fun onVenmoSuccess(venmoAccountNonce: VenmoAccountNonce) {
-        // handle Venmo account nonce
-   }
      
-   override fun onVenmoFailure(error: java.lang.Exception) {
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

```diff
class MyActivity : FragmentActivity() {
    
+   private lateinit var googlePayLauncher: GooglePayLauncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var googlePayClient: GooglePayClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       googlePayLauncher = GooglePayLauncher(this) { paymentAuthResult ->
+            googlePayClient.tokenize(paymentAuthResult) { paymentMethodNonce, error ->
+                error?.let { /* handle error */ }
+                paymentMethodNonce?.let { /* handle nonce */ }
+            }
+       }
    }
    
    fun initializeClients() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       goolePayClient = GooglePayClient(this, braintreeClient)
+       googlePayClient = GooglePayClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       googlePayClient.setListener(this)
    }
    
    fun onGooglePayButtonClick() {
-       googlePayClient.requestPayment(activity, request)
+       googlePayClient.createPaymentAuthRequest(this, request) { paymentAuthRequest, error ->
+            error?.let { /* handle error */ }
+            paymentAuthRequest?.let { googlePayLauncher.launch(it) }
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
    private lateinit var threeDSecureClient: VenmoClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       threeDSecureLauncher = ThreeDSecureLauncher(this) { paymentAuthResult ->
+            threeDSecureClient.tokenize(paymentAuthResult) { threeDSecureResult, error ->
+                error?.let { /* handle error */ }
+                threeDSecureResult?.let { /* handle threeDSecureResult.tokenizedCard */ }
+            }
+       }
    }
    
    fun initializeClients() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       threeDSecureClient = ThreeDSecureClient(this, braintreeClient)
+       threeDSecureClient = ThreeDSecureClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       threeDSecureClient.setListener(this)
    }
    
    fun onCardTokenization() {
-       threeDSecureClient.performVerification(activity, threeDSecureRequest) { 
+       threeDSecureClient.createPaymentAuthRequest(requireContext(), threeDSecureRequest) { 
          threeDSecureResult, error ->
             error?.let { /* handle error */ }
             threeDSecureResult?.let {
                if (it.lookup.requiresAuthentication) {
-                   threeDSecureClient.continuePerformVerification(MyActivity@this, request, it)
+                   threeDSecureLauncher.launch(it) 
                else { /* no additional user authentication needed, handle threeDSecureResult */ }
             }
        }
    }
    
-   override fun onThreeDSecureSuccess(threeDSecureResult: ThreeDSecureResult) {
-        // handle threeDSecureResult.tokenizedCard
-   }
      
-   override fun onThreeDSecureFailure(error: java.lang.Exception) {
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

```diff
class MyActivity : FragmentActivity() {

+   private lateinit var payPalLauncher: payPalLauncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var payPalClient: PayPalClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       payPalLauncher = PayPalLauncher() { paymentAuthResult ->
+           payPalClient.tokenize(paymentAuthResult) { result ->
+               when(result) {
+                   is PayPalResult.Success -> { /* handle result.nonce */ }
+                   is PayPalResult.Failure -> { /* handle result.error */ }
+                   is PayPalResult.Cancel -> { /* handle user canceled */ }
+               }          
+           }
+       }
    }
    
    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
+       payPalLauncher.handleReturnToAppFromBrowser(requireContext(), intent)
    }
    
    // ALL OTHER ACTIVITY LAUNCH MODES 
    override fun onResume() {
+       payPalLauncher.handleReturnToAppFromBrowser(requireContext(), requireActivity().intent)
    }
    
    
    fun initializeClients() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       payPalClient = PayPalClient(this, braintreeClient)
+       payPalClient = PayPalClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       payPalClient.setListener(this)
    }
    
    fun onPayPalButtonClick() {
-       payPalClient.tokenizePayPalAccount(activity, request)
+       payPalClient.createPaymentAuthRequest(this, request) { paymentAuthRequest ->
+           when(paymentAuthRequest) {
+               is PayPalPaymentAuthRequest.ReadyToLaunch -> {
+                   payPalLauncher.launch(this@MyActivity, paymentAuthRequet)
+               }
+               is PayPalPaymentAuthRequest.Failure -> { /* handle paymentAuthRequest.error +/ }
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

+   private lateinit var localPaymentLauncher: localPaymentLauncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var localPaymentClient: LocalPaymentClient

    @override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       localPaymentLauncher = LocalPaymentLauncher() { paymentAuthResult ->
+           localPaymentClient.tokenize(paymentAuthResult) { result -> 
+                when(result) {
+                   is LocalPaymentResult.Success -> { /* handle result.nonce */ }
+                   is LocalPaymentResult.Failure -> { /* handle result.error */ }
+                   is LocalPaymentResult.Cancel -> { /* handle user canceled */ }
+               }        
+           }
+       }
    }

    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
+       localPaymentLauncher.handleReturnToAppFromBrowser(requireContext(), intent)
    }

    // ALL OTHER ACTIVITY LAUNCH MODES 
    override fun onResume() {
+       localPaymentLauncher.handleReturnToAppFromBrowser(requireContext(), requireActivity().
+           intent)
    }


    fun initializeClients() {
-       braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       localPaymentClient = LocalPaymentClient(this, braintreeClient)
+       localPaymentClient = LocalPaymentClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       localPaymentClient.setListener(this)
    }

    fun onPaymentButtonClick() {
-       localPaymentClient.startPayment(activity, request)
+       localPaymentClient.createPaymentAuthRequest(this, request) { paymentAuthRequest ->
+           when(paymentAuthRequest) {
+               is LocalPaymentAuthRequest.ReadyToLaunch -> {
+                   lacalPaymentLauncher.launch(this@MyActivity, paymentAuthRequet)
+               }
+               is LocalPaymentAuthRequest.Failure -> { /* handle paymentAuthRequest.error +/ }
+           }
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

+   private lateinit var sepaDirectDebitLauncher: SEPADirectDebitLAuncher
-   private lateinit var braintreeClient: BraintreeClient
    private lateinit var sepaDirectDebitClient: SEPADirectDebitClient

    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       sepaDirectDebitLauncher = SEPADirectDebitLauncher() { paymentAuthResult ->
+           sepaDirectDebitClient.tokenize(paymentAuthResult) { 
+               sepaDirectDebitNonce, error ->
+                   // handle nonce or error
+           }
+       }
    }

    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
+       sepaDirectDebitLauncher.handleReturnToAppFromBrowser(requireContext(), intent)
    }

    // ALL OTHER ACTIVITY LAUNCH MODES 
    override fun onResume() {
+       sepaDirectDebitLauncher.handleReturnToAppFromBrowser(requireContext(), requireActivity().
+           intent)
    }

    fun initializeClients() {
-        braintreeClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       sepaDirectDebitClient = SEPADirectDebitClient(this, braintreeClient)
+       sepaDirectDebitClient = sepaDirectDebitClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       sepaDirectDebitClient.setListener(this)
    }

    fun onPaymentButtonClick() {
-       sepaDirectDebitClient.tokenize(activity, request)
+       sepaDirectDebitClient.tokenize(activity, request) { paymentAuthRequest, error ->
+           if (error != null) {
+               // handle error
+           } else if (paymentAuthRequest.nonce != null) {      // web-flow mandate not required
+               // handle nonce
+           } else {                                                 // web-flow mandate required
+               sepaDirectDebitLauncher.launch(activity, paymentAuthRequest)
+           }
+       }
    }

-   override fun onSEPADirectDebitSuccess(sepaDirectDebitNonce: SEPADirectDebitNonce) {
-        // handle SEPA nonce
-   }

-   override fun onSEPADirectDebitFailure(error: java.lang.Exception) {
-        // handle error
-   }
}