package com.braintreepayments.demo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.uicomponents.PayPalButton
import com.braintreepayments.api.uicomponents.PayPalLaunchCallback

@Composable
fun ComposeButtons() {

}

@Composable
fun PayPalComposeButton(
    authString: String,
    payPalRequest: PayPalRequest,
    payPalLaunchCallback: PayPalLaunchCallback
) {
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            println("Result: $it")
        }
    AndroidView(
        modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
        factory = { context ->
            // Creates view
            PayPalButton(context).apply {
                initialize(
                    null, // ActivityResultCaller
                    authString,
                    "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri(),
                    "com.braintreepayments.demo.braintree"
                )
                setPayPalRequest(payPalRequest)
                this.payPalLaunchCallback = payPalLaunchCallback
                // Sets up listeners for View -> Compose communication
                setOnClickListener {
//                    onButtonClick()
                }
            }
        },
        update = { view ->
            // View's been inflated or state read in this block has been updated
            // Add logic here if necessary

            // As selectedItem is read here, AndroidView will recompose
            // whenever the state changes
            // Example of Compose -> View communication
//            view.selectedItem = selectedItem
        }

    )
}
