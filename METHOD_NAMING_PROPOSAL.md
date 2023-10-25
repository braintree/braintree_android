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

### Minimal Change

<table>
<tr>
<th>v4 PayPal</th>
<th>v5 PayPal</th>
</tr>
<tr>

<td>

```kotlin 
payPalClient.tokenizePayPalAccount(activity, request)
```

</td>

<td>

```kotlin
payPalLauncher = PayPalLauncher() { payPalBrowserSwitchResult ->
  payPalClient.onBrowserSwitchResult(payPalBrowserSwitchResult) 
}
payPalClient.tokenizePayPalAccount(context, request) { payPalResponse, error ->
  payPalLauncher.launch(activity, payPalResponse) 
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
threeDSecureClient.performVerification(activity, request) { threeDSecureResult, error ->
  threeDSecureClient.continuePerformVerification(activity, request, threeDSecureResult)
}
```

</td>

<td>

```kotlin
threeDSecureLauncher = ThreeDSecureLauncher() { authenticationResult ->
  threeDSecureClient.continuePerformVerification(authenticationResult) 
}
threeDSecureClient.performVerification(context, request) { threeDSecureResult, error ->
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
venmoClientClient.tokenizeVenmoAccount(activity, request)
```

</td>

<td>

```kotlin
venmoLauncher = VenmoLauncher(activity) { authChallengeResult ->
  venmoClient.tokenizeVenmoAccount(authChallengeResult) 
}
venmoClient.requestAuthChallenge(context, request) { authChallenge, error ->
  venmoLauncher.launch(authChallenge) 
}
```

</td>

</tr>
</table>