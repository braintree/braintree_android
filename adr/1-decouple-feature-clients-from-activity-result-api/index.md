# Decouple Feature Clients from Activity Result API

**Status: Proposed**

## Context

The `ClientTokenProvider` integration pattern was created in response to the Braintree SDK's migration to the new Android [Activity Result API](https://developer.android.com/training/basics/intents/result). The new API has an unfortunate restriction–the entry method `Activity#registerForActivityResult()` must be called in (or before) [the host Activity's onCreate() method](https://stackoverflow.com/a/63883427). Braintree SDK feature clients (like `PayPalClient`, etc.) encapsulate this logic, and therefore also must be instantiatedby the time the host Activity or Fragment has reached a `CREATED` state. 

Feature clients support both [Tokenization Key](https://developer.paypal.com/braintree/docs/guides/authorization/tokenization-key) and [Client Token](https://developer.paypal.com/braintree/docs/guides/authorization/client-token) authorization methods. To create a client token, a merchant must integrate with one of the many Braintree Server SDKs. A merchant Android application will then require a network call to obtain a Client Token.

Assuming the networking call is asynchronous, a merchant would need to fetch a client token before presenting a payment activity that uses the Braintree SDK. Technically a merchant could forward a fetched client token and forward the token via Intent extras to the host Activity–however this method is cumbersome and error prone. A `ClientTokenProvider` helps by allowing a merchant to provide their own client token fetch strategy to the SDK. The SDK then will execute a client token fetch using the merchant implementation when an authorized request needs to be made. With a `ClientTokenProvider` instance, a merchant can now instantiate a Feature Client within `onCreate()` and satisfy Activity Result API requirements. 

## Decision

We should decouple Feature Clients from the Activity Result API. Doing so will allow us to remove the `ClientTokenProvider` and reduce onboarding friction.

The Client Token Provider pattern has been known to be [too opinionated](https://github.com/braintree/braintree_android/discussions/496) when it comes to asynchronous fetching. `ClientTokenProvider` provides the merchant with a callback object to notify the SDK that a client token has either been successfully obtained or an error encountered was encountered. Some merchants may prefer to use corutines or some other asynchronous programming pattern in order to obtain a client token. We should enable all merchants to use whatever strategy they wish to obtain a token.

We can lessen the responsibility of Feature Clients to execute an app switch by creating Feature Launchers. We may still wrap the Activity Result API for convenience and to bring app switching functionality into the Braintree Domain. Braintree Client then will only need access to a Context, decoupling Feature Clients from Activities and Fragments. <- This is helpful for Jetpack Compose integrations.

We also have an opportunity to encapsulate the activity result with a "continuation" object that is opaque to the merchant, but allows them to forward the result to the SDK to complete a payment method's tokenization flow:

```kotlin
class MainActivity : AppCompatActivity() {
  private lateinit var threeDSecureClient: ThreeDSecureClient
  private val threeDSecureLauncher = ThreeDSecureLauncher(this, continuation -> {
    handleThreeDSecureContinuation(continuation)
  })

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    findViewById<Button>(R.id.submit).setOnClickListener {
      launchThreeDSecure(clientToken)
    }
  }

  private fun launchThreeDSecure() {
    lifecycleScope.launch {
      initThreeDSecureClientIfNecessary()
      try {
        val threeDSecureRequest = ThreeDSecureRequest()
        val threeDSecureResult = threeDSecureClient.performVerification(threeDSecureRequest)
        threeDSecureResult.authChallenge?.let { threeDSecureLauncher.launch(it) }
        // else send nonce to server
      } catch (error: ThreeDSecureError) {
        // handle error
      }
    }
  }

  private fun handleThreeDSecureContinuation(continuation: ThreeDSecureContinuation) {
    lifecycleScope.launch {
      initThreeDSecureClientIfNecessary()
      try {
        val threeDSecureResult = threeDSecureClient.continuePerformVerification(continuation)
        // send nonce to server
      } catch (error: ThreeDSecureError) {
        // handle error
      }
    }
  }

  private suspend fun initThreeDSecureClientIfNecessary() {
    // merchant may fetch a client asynchronously using coroutines (or any other method)
    val clientToken = merchantAPI.fetchClientToken()
    val braintreeClient = BraintreeClient(this, clientToken)
    threeDSecureClient = ThreeDSecureClient(braintreeClient)
  }
}
```

## Consequences

Removing abstraction will require more work for the merchant to onboard to the SDK. Existing merchants will also need to migrate to use the new version and integration patterns. On the other hand, less abstraction will provide merchants will more control. The SDK will become less opinionated and will have increased flexibility to fit into a wider range of merchant app architectures.
