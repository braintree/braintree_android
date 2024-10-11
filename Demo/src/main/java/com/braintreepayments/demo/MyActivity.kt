package com.braintreepayments.demo

// class MyActivity : AppCompatActivity() {
//
//    private lateinit var payPalLauncher: PayPalLauncher
//    private lateinit var payPalClient: PayPalClient
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // PayPalLauncher must be initialized in onCreate
//        payPalLauncher = PayPalLauncher()
//
//        // can initialize the PayPalClient outside of onCreate if desired
//        payPalClient = PayPalClient(
//            context = this,
//            authorization = "[Tokenization Key or Client Token]",
//            appLinkReturnUrl = Uri.parse("https://merchant-app.com") // Merchant App Link
//        )
//    }
//
//    // ONLY REQUIRED IF YOUR ACTIVITY LAUNCH MODE IS SINGLE_TOP
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        handleReturnToApp(intent)
//    }
//
//    // ALL OTHER ACTIVITY LAUNCH MODES
//    override fun onResume() {
//        super.onResume()
//        handleReturnToApp(intent)
//    }
//
//    private fun handleReturnToApp(intent: Intent) {
//        // fetch stored PayPalPendingRequest.Success
//        val pendingRequest: String = fetchPendingRequestFromPersistantStore()
//        when (val paymentAuthResult = payPalLauncher.handleReturnToApp(
//            pendingRequest = PayPalPendingRequest.Started(pendingRequest),
//            intent = intent
//        )) {
//            is PayPalPaymentAuthResult.Success -> {
//                completePayPalFlow(paymentAuthResult)
//                // clear stored PayPalPendingRequest.Success
//            }
//
//            is PayPalPaymentAuthResult.NoResult -> {
//                // user returned to app without completing PayPal flow, handle accordingly
//            }
//
//            is PayPalPaymentAuthResult.Failure -> {
//                // handle error case
//            }
//        }
//    }
//
//    private fun completePayPalFlow(paymentAuthResult: PayPalPaymentAuthResult.Success) {
//        payPalClient.tokenize(paymentAuthResult) { result ->
//            when (result) {
//                is PayPalResult.Success -> { /* handle result.nonce */ }
//                is PayPalResult.Failure -> { /* handle result.error */ }
//                is PayPalResult.Cancel -> { /* handle user canceled */ }
//            }
//        }
//    }
//
//    private fun onPayPalButtonClick() {
//        val payPalCheckoutRequest = PayPalCheckoutRequest(
//            ...
//            shouldOfferPayLater = true
//        )
//        payPalClient.createPaymentAuthRequest(this, payPalCheckoutRequest) { paymentAuthRequest ->
//            when (paymentAuthRequest) {
//                is PayPalPaymentAuthRequest.ReadyToLaunch -> {
//                    val pendingRequest = payPalLauncher.launch(this, paymentAuthRequest)
//                    when (pendingRequest) {
//                        is PayPalPendingRequest.Started -> {/* store pending request */ }
//                        is PayPalPendingRequest.Failure -> { /* handle error */ }
//                    }
//                }
//                is PayPalPaymentAuthRequest.Failure -> { /* handle paymentAuthRequest.error */ }
//            }
//        }
//    }
// }
