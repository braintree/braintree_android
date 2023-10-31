

```kotlin
payPalLauncher = PayPalLauncher { paymentAuthResult ->
    payPalClient.tokenize(paymentAuthResult) { paymentMethodNonce, error ->
        error?.let {
            if (it is UserCanceledException) {
                // handle user canceled payment flow
            } else {
                // handle error
            }
            return
        }
        paymentMethodNonce?.let {
            // send nonce to server
            return
        }
        // unexpected error - one of nonce or error should not be null
    }
}

payPalClient.createPaymentAuthRequest(activity, request) { paymentAuthRequest, error ->
    error?.let {
        // handle error
        return
    }
    paymentAuthRequest?.let {
        payPalLauncher.launch(activity, paymentAuthRequest)
        return
    }
    // unexpected error - one of paymentAuthRequest or error should not be null
}
```

```kotlin
payPalLauncher = PayPalLauncher { paymentAuthResult ->
    payPalClient.tokenize(paymentAuthResult) { paymentResult ->
        when(paymentResult) {
            is PaymentResult.Success -> {
                // send nonce to server
                val payPalAccountNonce = paymentResult.nonce as PayPalAccountNonce
            }
            is PaymentResult.Cancel -> {
                // handle user canceled payment flow
            }
            is PaymentResult.Failure -> {
                val error = paymentResult.error
            }
        }
    }
}

payPalClient.createPaymentAuthRequest(activity, request) { paymentAuthRequest ->
    when(paymentAuthRequest) {
        is PaymentAuthRequest.Ready -> {
            payPalLauncher.launch(activity, paymentAuthRequest.launchRequest)
        }
        is PaymentAuthRequest.Failure -> {
            val error = paymentAuthRequest.error
        }
    }
}
```

```kotlin
payPalLauncher = PayPalLauncher { paymentAuthResult ->
    payPalClient.tokenize(paymentAuthResult, object : PayPalResultCallback {
        override fun onResult(nonce: PayPalAccountNonce) {
            // send nonce to server
        }

        override fun onError(error: Exception) {
            // handle error
        }

        override fun onCancel() {
            // handle cancel
        }
    })
}

payPalClient.createPaymentAuthRequest(activity, request, object : PaymentAuthRequestCallback {
    override fun onRequest(request: PaymentAuthRequest) {
        payPalLauncher.launch(activity, request)
    }
    override fun onError(error: Exception) {
        // handle error
    }
})
```

```java
PayPalLauncher payPalLauncher = new PayPalLauncher(paymentAuthResult ->
        payPalClient.tokenize(paymentAuthResult, (paymentMethodNonce, error) -> {
            if (error != null) {
                if (error instanceof UserCanceledException) {
                    // handle user canceled payment flow
                } else {
                    // handle error 
                }
                return;
            }
            if (paymentMethodNonce != null) {
                // send nonce to server
                return;
            }
            // unexpected error - one of nonce or error should not be null
        }));

payPalClient.createPaymentAuthRequest(activity, request, (paymentAuthRequest, error) -> {
    if (error != null) {
        // handle error
        return;
    }
    if (paymentAuthRequest != null) {
        payPalLauncher.launch(activity, paymentAuthRequest);
        return;
    }
    // unexpected error - one of paymentAuthRequest or error should not be null
});
```

```java
PayPalLauncher payPalLauncher = new PayPalLauncher(paymentAuthResult -> 
    payPalClient.tokenize(paymentAuthResult, (paymentResult) -> {
        if (paymentResult instanceof PaymentReslt.Success) {
            // send nonce to server
            PaymentMethodNonce nonce = ((PaymentResult.Success) paymentResult).getNonce();
        } else if (paymentResult instanceof PaymentResult.Cancel) {
            // handle user canceled payment flow 
        } else if (paymentResult instanceof PaymentResult.Failure) {
            Exception error = ((PaymentResult.Failure) paymentResult).getError();
        }
}));

payPalClient.createPaymentAuthRequest(activity, request, (paymentAuthRequest) -> {
    if (paymentAuthRequest instanceof PaymentAuthRequest.Ready) {
        payPalLauncher.launch(activity, ((PaymentAuthRequest.Ready) paymentAuthRequest).
                getLaunchRequest());
    } else if (paymentAuthRequest instanceof PaymentAuthRequest.Failure) {
        Exception error = ((PaymentAuthRequest.Failure) paymentAuthRequest).getError();
    }
});
```

```java
PayPalLauncher payPalLauncher = new PayPalLauncher(paymentAuthResult ->
        payPalClient.tokenize(paymentAuthResult, new PayPalResultCallback() {
    @Override
    public void onResult(PayPalAccountNonce nonce) {
        // handle result
    }

    @Override
    public void onError(Exception error) {
        // handle error
    }

    @Override
    public void onCancel() {
        // handle cancel
    }
});
    
payPalClient.createPaymentAuthRequest(activity, request, new PaymentAuthRequestCallback() {
    @Override
    public void onRequest(PaymentAuthRequest request) {
        payPalLauncher.launch(activity, paymentAuthRequest);
    }

    @Override
    public void onError(Exception error) {
        // handle error
    }
});
```

