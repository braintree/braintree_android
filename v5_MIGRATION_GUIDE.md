# Braintree Android v5 Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the basics for updating your Braintree integration from v4 to v5.

## Table of Contents

1. [Android API](#android-api)
1. [Union Pay](#union-pay)
1. [Venmo](#venmo)
1. [Google Pay](#google-pay)
1. [PayPal](#paypal)

## Android API

The minimum supported Android API level for v5 of this SDK has increased to 23.

## Union Pay

The `union-pay` module, and all containing classes, was removed in v5. UnionPay cards can now be processed as regular cards, through the `card` module. You no longer need to manage card enrollment via SMS authorization.

Now, you can tokenize with just the card details:

// TODO: code snippet of card integration in v5

## Venmo

The Venmo integration has been updated to allow for more flexibility with app switching and 
activity result handling. 

`VenmoLauncher` has been added to handle the app switching portion of the Venmo flow for user 
authentication via the Venmo app. This class uses the Android Activity Result API and therefore 
must be instantiated in the `OnCreate` method of your Activity or Fragment.

`BraintreeClient` and `VenmoClient` no longer require references to Fragment or Activity and do not 
need to be instantiated in `OnCreate`.

```diff
class MyActivity : FragmentActivity() {
    
+   private lateinit var venmoLauncher: VenmoLauncher
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var venmoClient: VenmoClient
    
    @override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       venmoLauncher = VenmoLauncher(this) { authChallengeResult ->
+            venmoClient.tokenizeVenmoAccount(authChallengeResult) { venmoAccountNonce, error ->
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
+       venmoClient.requestAuthChallenge(this, venmoRequest) { authChallenge, error ->
+            error?.let { /* handle error */ }
+            authChallenge?.let { venmoLauncher.launch(it) }
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
API and therefore must be instantiated in the `OnCreate` method of your Activity or Fragment.

`BraintreeClient` and `GooglePayClient` no longer require references to Fragment or Activity and 
do not need to be instantiated in `OnCreate`.

```diff
class MyActivity : FragmentActivity() {
    
+   private lateinit var googlePayLauncher: GooglePayLauncher
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var googlePayClient: GooglePayClient
    
    @override fun onCreate(savedInstanceState: Bundle?) {
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
    
    @override fun onCreate(savedInstanceState: Bundle?) {
+       // can initialize clients outside of onCreate if desired
-       initializeClients()
+       payPalLauncher = PayPalLauncher() 
    }
    
    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
    @override fun onNewIntent(intent: Intent) {
        setIntent(intent)
+       val result = payPalLauncher.deliverResult(requireContext(), intent)
+       result?.let {
+           payPalClient.onBrowserSwitchResult(it) { payPalAccountNonce, error ->
+               // handle paypal account nonce or error
+           }
+       }
    }
    
    // ALL OTHER ACTIVITY LAUNCH MODES 
    @override fun onResume() {
+       val result = payPalLauncher.deliverResult(requireContext(), requireActivity().intent)
+       result?.let {
+           payPalClient.onBrowserSwitchResult(it) { payPalAccountNonce, error ->
+               // handle paypal account nonce or error
+           }
+       }
    }
    
    
    fun initializeClients() {
        braintreClient = BraintreeClient(context, "TOKENIZATION_KEY_OR_CLIENT_TOKEN")
-       payPalClient = PayPalClient(this, braintreeClient)
+       payPalClient = PayPalClient(braintreeClient)
-       payPalClient.setListener(this)
    }
    
    fun onPayPalButtonClick() {
-       payPalClient.tokenizePayPalAccount(activity, request)
+       payPalClient.tokenizePayPalAccount(this, request) { payPalResponse, error ->
+            error?.let { /* handle error */ }
+            payPalResponse?.let { 
+               try {
+                   payPalLauncher.launch(requireActivity(), it) 
+               } catch (error: BrowserSwitchException) {
+                   // handle error
+               }
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
