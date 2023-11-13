# Braintree Android v5 Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the
basics for updating your Braintree integration from v4 to v5.

## Table of Contents

1. [Android API](#android-api)
1. [Data Collector](#data-collector)
1. [Union Pay](#union-pay)
1. [Venmo](#venmo)
1. [Google Pay](#google-pay)
1. [3DS](#3ds)
1. [PayPal](#paypal)
1. [Local Payment](#local-payment)
1. [SEPA Direct Debit](#sepa-direct-debit)

## Android API

The minimum supported Android API level for v5 of this SDK has increased to 23.

## Data Collector

The `paypal-data-collector` module has been removed and replaced by the `data-collector` module.
The `DataCollector` class within the `data-collector` module has the same
`collectDeviceData` methods, so if you were previously using the `paypal-data-collector` library,
no code changes are required aside from updating your dependency.

If you were using the `data-collector` library in v4, `DataCollector#collectDeviceData(context,
merchantId, callback)` is now `DataCollector#collectDeviceData(context, riskCorrelationId,
callback)`, where `riskCorrelationId` is an optional client metadata ID.

```kotlin
val dataCollector = DataCollector(braintreeClient)
dataCollector.collectDeviceData(context) { deviceData, error ->
    // send deviceData to your server
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
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var venmoClient: VenmoClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       venmoLauncher = VenmoLauncher(this) { paymentAuthResult ->
+            venmoClient.tokenize(paymentAuthResult) { venmoAccountNonce, error ->
+                error?.let { /* handle error */ }
+                venmoAccountNonce?.let { /* handle Venmo account nonce */ }
+            }
+       }
    }
    
    fun initializeClients() {
        braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       venmoClient = VenmoClient(this, braintreeClient)
+       venmoClient = VenmoClient(braintreeClient)
-       venmoClient.setListener(this)
    }
    
    fun onVenmoButtonClick() {
-       venmoClient.tokenizeVenmoAccount(activity, request)
+       venmoClient.requestAuthChallenge(this, venmoRequest) { paymentAuthRequest, error ->
+            error?.let { /* handle error */ }
+            paymentAuthRequest?.let { venmoLauncher.launch(it) }
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
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var googlePayClient: GooglePayClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       googlePayLauncher = GooglePayLauncher(this) { googlePayResult ->
+            googlePayClient.tokenize(googlePayResult) { paymentMethodNonce, error ->
+                error?.let { /* handle error */ }
+                paymentMethodNonce?.let { /* handle nonce */ }
+            }
+       }
    }
    
    fun initializeClients() {
        braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       goolePayClient = GooglePayClient(this, braintreeClient)
+       googlePayClient = GooglePayClient(braintreeClient)
-       googlePayClient.setListener(this)
    }
    
    fun onGooglePayButtonClick() {
-       googlePayClient.requestPayment(activity, request)
+       googlePayClient.requestPayment(this, request) { googlePayIntentData, error ->
+            error?.let { /* handle error */ }
+            googlePayIntentData?.let { googlePayLauncher.launch(it) }
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
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var threeDSecureClient: VenmoClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       threeDSecureLauncher = ThreeDSecureLauncher(this) { cardinalResult ->
+            threeDSecureClient.onCardinalResult(cardinalResult) { threeDSecureResult, error ->
+                error?.let { /* handle error */ }
+                threeDSecureResult?.let { /* handle threeDSecureResult.tokenizedCard */ }
+            }
+       }
    }
    
    fun initializeClients() {
        braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       threeDSecureClient = ThreeDSecureClient(this, braintreeClient)
+       threeDSecureClient = ThreeDSeucreClient(braintreeClient)
-       threeDSecureClient.setListener(this)
    }
    
    fun onCardTokenization() {
-       threeDSecureClient.performVerification(activity, threeDSecureRequest) { 
+       threeDSecureClient.performVerification(requireContext(), threeDSecureRequest) { 
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
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var payPalClient: PayPalClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       payPalLauncher = PayPalLauncher() { paymentAuthResult ->
+           payPalClient.tokenize(paymentAuthResult) { payPalAccountNonce, error ->
+               // handle paypal account nonce or error
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
        braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       payPalClient = PayPalClient(this, braintreeClient)
+       payPalClient = PayPalClient(braintreeClient)
-       payPalClient.setListener(this)
    }
    
    fun onPayPalButtonClick() {
-       payPalClient.tokenizePayPalAccount(activity, request)
+       payPalClient.createPaymentAuthRequest(this, request) { paymentAuthRequest, error ->
+            error?.let { /* handle error */ }
+            paymentAuthRequest?.let { 
+                payPalLauncher.launch(requireActivity(), paymentAuthRequest) 
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
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var localPaymentClient: LocalPaymentClient

    @override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       localPaymentLauncher = LocalPaymentLauncher() { localPaymentBrowserSwitchResult ->
+           localPaymentClient.onBrowserSwitchResult(localPaymentBrowserSwitchResult) { 
+               localPaymentNonce, error ->
+                   // handle local payment nonce or error
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
        braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       localPaymentClient = LocalPaymentClient(this, braintreeClient)
+       localPaymentClient = LocalPaymentClient(braintreeClient)
-       localPaymentClient.setListener(this)
    }

    fun onPaymentButtonClick() {
-       localPaymentClient.startPayment(activity, request)
+       localPaymentClient.startPayment(this, request) { localPaymentResult, error ->
+            error?.let { /* handle error */ }
+            localPaymentResult?.let { 
+                localPaymentLauncher.launch(requireActivity(), it) 
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
    private lateinit var braintreeClient: BraintreeClient
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
        braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       sepaDirectDebitClient = SEPADirectDebitClient(this, braintreeClient)
+       sepaDirectDebitClient = SEPADirectDebitClient(braintreeClient)
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