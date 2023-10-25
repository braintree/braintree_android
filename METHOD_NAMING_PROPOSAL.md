# v5 Method Naming Proposal

## Overview

In v5 of the Braintree Android SDK, we are introducing a new integration pattern that includes a 
separate `Launcher` class for each payment method that requires launching a browser, external 
app, payment sheet, etc. to complete the payment flow. The addition of this pattern requires new 
method and result object names, and v5 provides an opportunity to revisit the existing naming 
patterns across payment methods.

## Integration Comparison

This document provides alternative naming options in comparison to the v4 integration to 
determine the best pattern for v5. 

In v4, most payment flows require one method call (ex: `tokenize`), and the browser/app switch is handled under the hood by the SDK, before returning a `PaymentMethodNonce` to the merchant's listener. Some payment methods require two method calls (ex: 3DS `performVerification` -> `continuePerformVerification`).

In v5, all app/broswer switching payment methods require starting the payment flow by invoking a method on the `Client` to retrieve data to be passed to the `Launcher`. With that data, the `Launcher` presents the payment sheet, launches the browser, or launches the exteral app for some user interaction/authentication/payment authorization. The `Launcher` will be instantiated with a callback that will be invoked upon return to the merchant app from the browser/external app/payment sheet. Within the callback, another method on the `Client` will be invoked to handle data from the app/browser switch result and return a `PaymentMethodNonce`. 

### Minimal Change

With this implementation, the method names from v4 are maintained where possible. The comments above each method show what the method actually does, as the naming doesn't entirely align with this approach.

<table>
<tr>
<th>v4 PayPal</th>
<th>v5 PayPal</th>
</tr>
<tr>

<td>

```kotlin 
// Retrieve data required to launch browser
// Launch browser flow, parse browser result
// Return nonce to listener
payPalClient.tokenizePayPalAccount(activity, request)
```

</td>

<td>

```kotlin
payPalLauncher = PayPalLauncher() { payPalBrowserSwitchResult ->
  // Parse the result from the PayPal browser flow and return a nonce
  payPalClient.onBrowserSwitchResult(payPalBrowserSwitchResult) 
}

// Retrieve data required to launch the PayPal flow in a browser
payPalClient.tokenizePayPalAccount(context, request) { payPalResponse, error ->
  // Launch the PayPal in a browser
  payPalLauncher.launch(activity, payPalResponse) 
}
```

</td>

</tr>

<tr>
<th>v4 Local Payment</th>
<th>v5 Local Payment</th>
</tr>
<tr>

<td>

```kotlin 
// Retrieve data required to start local payment authorization
// Or return result if no additional auth required
localPaymentClient.startPayment(activity, request) { localPaymentResult, error ->
  // Initiate browser flow and return nonce to listener
  localPaymentClient.approveLocalPayment(activity, localPaymentResult)
}
```

</td>

<td>

```kotlin
localPaymentLauncher = LocalPaymentLauncher() { localPaymentBrowserSwitchResult ->
  // Parse the result from the local payment browser flow and return a nonce
  localPaymentClient.approveLocalPayment(localPaymentBrowserSwitchResult) 
}

// Retrieve data required to launch the local payment flow in a browser
localPaymentClient.startPayment(context, request) { localPaymentResult, error ->
  // Launch the local payment in a browser
  localPaymentLauncher.launch(activity, localPaymentResult) 
}
```

</td>

</tr>
<tr>
<th>v4 3DS</th>
<th>v5 3DS</th>
</tr>
<tr>

<td>

```kotlin 
// Retrieve data required to start 3DS authentication
// Or return result if no additional auth required
threeDSecureClient.performVerification(activity, request) { threeDSecureResult, error ->
  // Launch 3DS flow in Cardinal SDK 
  // Return nonce to listener
  threeDSecureClient.continuePerformVerification(activity, request, threeDSecureResult)
}
```

</td>

<td>

```kotlin
threeDSecureLauncher = ThreeDSecureLauncher() { authenticationResult ->
  // Parse the result from the 3DS auth flow and return a nonce
  threeDSecureClient.continuePerformVerification(authenticationResult) 
}

// Retrieve data required to launch the 3DS flow
// Instantiate Cardinal SDK
threeDSecureClient.performVerification(context, request) { threeDSecureResult, error ->
  // Launch 3DS flow in Cardinal SDK 
  threeDSecureLauncher.launch(threeDSecureResult) 
}
```

</td>

</tr>

<tr>
<th>v4 Venmo</th>
<th>v5 Venmo</th>
</tr>
<tr>

<td>

```kotlin 
// Create Venmo payment context
// Launch Venmo app and create nonce from payment context app switch result
// Return nonce to listener
venmoClient.tokenizeVenmoAccount(activity, request)
```

</td>

<td>

```kotlin
venmoLauncher = VenmoLauncher(activity) { authChallengeResult ->
  // Create nonce from payment context app switch result and vault if required
  venmoClient.tokenizeVenmoAccount(authChallengeResult) 
}


  // Create Venmo payment context
venmoClient.requestAuthChallenge(context, request) { authChallenge, error ->
  // Launch Venmo app
  venmoLauncher.launch(authChallenge) 
}
```

</td>

</tr>

<tr>
<th>v4 Google Pay</th>
<th>v5 Google Pay</th>
</tr>
<tr>

<td>

```kotlin 
// Create PaymentDataRequest for Google Pay SDK
// Launch Google Pay payment sheet
// Parse Google Pay result an return a nonce
googlePayClient.requestPayment(activity, request)
```

</td>

<td>

```kotlin
googlePayLauncher = GooglePayLauncher(activity) { googlePayResult ->
  // Parse Google Pay result and return a nonce
  googlePayClient.tokenize(googlePayResult) 
}

// Create PaymentDataRequest for Google Pay SDK
googlePayClient.requestPayment(context, request) { googlePayIntentData, error ->
  // Launch Google Pay payment sheet
  googlePayLauncher.launch(googlePayIntentData) 
}
```

</td>

</tr>
</table>


### Consistent Naming Pattern

With this implementation, the method names do not align with v4, so there is greater merchant migration difficulty, but the naming pattern is consistent across payment methods and provides clarity to what is actually occurring.
For this pattern, I've chosen to use the `paymentAuth` naming pattern.

`client.createPaymentAuthRequest` returns `paymentAuthRequest`

`launcher.launch(paymentAuthRequest)` returns `paymentAuthResult`

`client.tokenize(paymentAuthResult)` returns `paymentMethodNonce`

This could also be called `paymentAuthorization`, `authChallenge`, `authenticationChallenge`, `paymentFlow`, `paymentSheet`, or any other suggested pattern. This naming should be able to be applied to launching an external browser, launching an external app, presenting a payment sheet. The user may or may not need to interact with these external flows to complete the payment flow (ex: Venmo app launches and returns without interaction, 3DS might require a code to be input).

<table>
<tr>
<th>v4 PayPal</th>
<th>v5 PayPal</th>
</tr>
<tr>

<td>

```kotlin 
// Retrieve data required to launch browser
// Launch browser flow, parse browser result
// Return nonce to listener
payPalClient.tokenizePayPalAccount(activity, request)
```

</td>

<td>

```kotlin
payPalLauncher = PayPalLauncher() { payPalPaymentAuthResult ->
  // Parse the result from the PayPal browser flow and return a nonce
  payPalClient.tokenize(payPalPaymentAuthResult) 
}

// Retrieve data required to launch the PayPal flow in a browser
payPalClient.createPaymentAuthRequest(context, request) { payPalPaymentAuthRequest, error ->
  // Launch the PayPal in a browser
  payPalLauncher.launch(activity, payPalPaymentAuthRequest) 
}
```

</td>

</tr>

<tr>
<th>v4 Local Payment</th>
<th>v5 Local Payment</th>
</tr>
<tr>

<td>

```kotlin 
// Retrieve data required to start local payment authorization
// Or return result if no additional auth required
localPaymentClient.startPayment(activity, request) { localPaymentResult, error ->
  // Initiate browser flow and return nonce to listener
  localPaymentClient.approveLocalPayment(activity, localPaymentResult)
}
```

</td>

<td>

```kotlin
localPaymentLauncher = LocalPaymentLauncher() { localPaymentAuthResult ->
  // Parse the result from the local payment browser flow and return a nonce
  localPaymentClient.tokenize(localPaymentAuthResult) 
}

// Retrieve data required to launch the local payment flow in a browser
localPaymentClient.createPaymentAuthRequest(context, request) { localPaymentAuthRequest, error ->
  // Launch the local payment in a browser
  localPaymentLauncher.launch(activity, localPaymentAuthRequest) 
}
```

</td>

</tr>
<tr>
<th>v4 3DS</th>
<th>v5 3DS</th>
</tr>
<tr>

<td>

```kotlin 
// Retrieve data required to start 3DS authentication
// Or return result if no additional auth required
threeDSecureClient.performVerification(activity, request) { threeDSecureResult, error ->
  // Launch 3DS flow in Cardinal SDK 
  // Return nonce to listener
  threeDSecureClient.continuePerformVerification(activity, request, threeDSecureResult)
}
```

</td>

<td>

```kotlin
threeDSecureLauncher = ThreeDSecureLauncher() { threeDSecurePaymentAuthResult ->
  // Parse the result from the 3DS auth flow and return a nonce
  threeDSecureClient.tokenize(threeDSecurePaymentAuthResult) 
}

// Retrieve data required to launch the 3DS flow
// Instantiate Cardinal SDK
threeDSecureClient.createPaymentAuthRequest(context, request) { threeDSecurePaymentAuthRequest, error ->
  // Launch 3DS flow in Cardinal SDK 
  threeDSecureLauncher.launch(threeDSecurePaymentAuthRequest) 
}
```

</td>

</tr>

<tr>
<th>v4 Venmo</th>
<th>v5 Venmo</th>
</tr>
<tr>

<td>

```kotlin 
// Create Venmo payment context
// Launch Venmo app and create nonce from payment context app switch result
// Return nonce to listener
venmoClient.tokenizeVenmoAccount(activity, request)
```

</td>

<td>

```kotlin
venmoLauncher = VenmoLauncher(activity) { venmoPaymentAuthResult ->
  // Create nonce from payment context app switch result and vault if required
  venmoClient.tokenize(venmoPaymentAuthResult) 
}


  // Create Venmo payment context
venmoClient.createPaymentAuthRequest(context, request) { venmoPaymentAuthRequest, error ->
  // Launch Venmo app
  venmoLauncher.launch(venmoPaymentAuthRequest) 
}
```

</td>

</tr>

<tr>
<th>v4 Google Pay</th>
<th>v5 Google Pay</th>
</tr>
<tr>

<td>

```kotlin 
// Create PaymentDataRequest for Google Pay SDK
// Launch Google Pay payment sheet
// Parse Google Pay result an return a nonce
googlePayClient.requestPayment(activity, request)
```

</td>

<td>

```kotlin
googlePayLauncher = GooglePayLauncher(activity) { googlePayPaymentAuthResult ->
  // Parse Google Pay result and return a nonce
  googlePayClient.tokenize(googlePayPaymentAuthResult) 
}

// Create PaymentDataRequest for Google Pay SDK
googlePayClient.createPaymentAuthRequest(context, request) { googlePayPaymentAuthRequest, error ->
  // Launch Google Pay payment sheet
  googlePayLauncher.launch(googlePayPaymentAuthRequest) 
}
```

</td>

</tr>
</table>