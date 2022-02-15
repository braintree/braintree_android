# Migration Guide
This is an in-process guide that will be updated as we update each payment method to support this flow. For now, this includes a code snippet of what the basic flows will look like in a merchant Activity/Fragment.

## Venmo

```kotlin

// MerchantActivity.kt
class MerchantActivity : AppCompatActivity(), VenmoListener {
    
    lateinit var braintreeClient: BraintreeClient
    lateinit var venmoClient: VenmoClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        venmoClient = VenmoClient(this, braintreeClient)
        venmoClient.listener = this
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
        venmoClient.listener = this
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

## ThreeDSecure

## ThreeDSecure

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
        threeDSecureClient.performVerification(this, threeDSecureRequest) { threeDSecureLookupResult, lookupError ->
          threeDSecureClient.continuePerformVerification(this@MerchantActivity, threeDSecureRequest, threeDSecureLookupResult)
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
        threeDSecureClient.performVerification(requireActivity(), threeDSecureRequest) { threeDSecureLookupResult, lookupError ->
          threeDSecureClient.continuePerformVerification(requireActivity(), threeDSecureRequest, threeDSecureLookupResult)
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

## PayPal

```kotlin
// MerchantActivity.kt
class MerchantActivity : AppCompatActivity(), PayPalListener {

    lateinit var braintreeClient: BraintreeClient
    lateinit var payPalClient: PayPalClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        payPalClient = VenmoClient(this, braintreeClient)
        payPalClient.listener = this
    }
   
    override fun onPayPalSuccess(payPalAccountNonce: PayPalAccountNonce) {
        // send nonce to server and create a transaction
    }
    
    override fun onPayPalFailure(error: Exception) {
        // handle error
    }
}
// MerchantFragment.kt
class MerchantFragment: Fragment(), PayPalListener {

    lateinit var braintreeClient: BraintreeClient
    lateinit var payPalClient: PayPalClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        payPalClient = PayPalClient(this, braintreeClient)
        payPalClient.listener = this
    }
    
    override fun onPayPalSuccess(payPalAccountNonce: payPalAccountNonce) {
        // send nonce to server and create a transaction
    }
    
    override fun onPayPalFailure(error: Exception) {
        // handle error
    }
}
```
## Local Payment

```kotlin
// MerchantActivity.kt
class MerchantActivity : AppCompatActivity(), LocalPaymentListener {

    lateinit var braintreeClient: BraintreeClient
    lateinit var localPaymentClient: LocalPaymentClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        localPaymentClient = LocalPaymentClient(this, braintreeClient)
        localPaymentClient.setListener(getContext(), this)
    }

    private fun launchLocalPayment() {
        ...
        localPaymentClient.startPayment(localPaymentRequest) { localPaymentStartError ->
            localPaymentClient.approveLocalPayment(this@MerchantActivity, localPaymentResult)
        }
    }
   
    override fun onLocalPaymentSuccess(localPaymentNonce: LocalPaymentNonce) {
        // send nonce to server and create a transaction
    }
    
    override fun onLocalPaymentFailure(error: Exception) {
        // handle error
    }
}
// MerchantFragment.kt
class MerchantFragment: Fragment(), PayPalListener {

    lateinit var braintreeClient: BraintreeClient
    lateinit var localPaymentClient: LocalPaymentClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        braintreeClient =
          BraintreeClient(this, MerchantClientTokenProvider())
          
        localPaymentClient = LocalPaymentClient(this, braintreeClient)
        localPaymentClient.setListener(getContext(), this)
    }

    private fun launchLocalPayment() {
        ...
        localPaymentClient.startPayment(localPaymentRequest) { localPaymentStartError ->
            localPaymentClient.approveLocalPayment(requireActivity(), localPaymentResult)
        }
    }
    
    override fun onPayPalSuccess(payPalAccountNonce: payPalAccountNonce) {
        // send nonce to server and create a transaction
    }
    
    override fun onPayPalFailure(error: Exception) {
        // handle error
    }
}
```