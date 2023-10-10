# Braintree Android v5 Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the basics for updating your Braintree integration from v4 to v5.

## Table of Contents

1. [Android API](#android-api)
1. [Union Pay](#union-pay)
1. [Venmo](#venmo)

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

