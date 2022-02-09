# Migration Guide

## Venmo

```kotlin

// MerchantActivity.kt
class MerchantActivity : AppCompatActivity(), VenmoListener {
    
    lateinit var braintreeClient: BraintreeClient
    lateinit var venmoClient: venmoClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        venmoClient = VenmoClient(this, braintreeClient)
        venmoClient.setListener(this)
    }
   
    override fun onVenmoSuccess(venmoAccountNonce: VenmoAccountNonce) {
        // send nonce to server and create a transaction
    }
    
    override fun onVenmoFailure(error: Exception) {
        // handle error
    }
}


// MerchantFragment.kt
class MerchantFragment: Fragment(), VenmoListener {
    
    lateinit var braintreeClient: BraintreeClient
    lateinit var venmoClient: VenmoClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        venmoClient = VenmoClient(this, braintreeClient)
        venmoClient.setListener(this)
    }
    
    override fun onVenmoSuccess(venmoAccountNonce: venmoAccountNonce) {
        // send nonce to server and create a transaction
    }
    
    override fun onVenmoFailure(error: Exception) {
        // handle error
    }
}
```



## 3DS

This is an in-process guide that will be updated as we update each payment method to support this flow. For now, this includes a code snippet of what the basic 3DS flow will look like in a merchant Activity/Fragment.

```kotlin
// MerchantActivity.kt
class MerchantActivity : AppCompatActivity(), ThreeDSecureListener {
    
    lateinit var braintreeClient: BraintreeClient
    lateinit var threeDSecureClient: ThreeDSecureClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        threeDSecureClient = ThreeDSecureClient(this, braintreeClient)
        threeDSecureClient.listener = this
    }
    
    private fun launch3DS() {
        ...
        threeDSecureClient.performVerification(activity, threeDSecureRequest) { threeDSecureLookupResult, lookupError ->
          threeDSecureClient.continuePerformVerification(activity, threeDSecureRequest, threeDSecureLookupResult)
        }
    }
    
    override fun onThreeDSecureSuccess(threeDSecureResult: ThreeDSecureResult) {
        // send nonce to server and create a transaction
    }
    
    override fun onThreeDSecureFailure(error: Exception) {
        // handle error
    }
}

// MerchantFragment.kt
class MerchantFragment: Fragment(), ThreeDSecureListener {
    
    lateinit var braintreeClient: BraintreeClient
    lateinit var threeDSecureClient: ThreeDSecureClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        threeDSecureClient = ThreeDSecureClient(this, braintreeClient)
        threeDSecureClient.listener = this
    }
    
    private fun launch3DS() {
        ...
        threeDSecureClient.performVerification(activity, threeDSecureRequest) { threeDSecureLookupResult, lookupError ->
          threeDSecureClient.continuePerformVerification(activity, threeDSecureRequest, threeDSecureLookupResult)
        }
    }
    
    override fun onThreeDSecureSuccess(threeDSecureResult: ThreeDSecureResult) {
        // send nonce to server and create a transaction
    }
    
    override fun onThreeDSecureFailure(error: Exception) {
        // handle error
    }
}
```
