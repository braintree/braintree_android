# Result Handling Android v5

This document proposes alternative patterns for result handling in Braintree Android v5. In 
Braintree Android v4, there are two different patterns for delivering results: a listener with 
two methods (`onSuccess`/`onFailure`) or a callback with one method and two nullable parameters 
(`onResult(@Nullable nonce, @Nullable error)`). A merchant brought to our attention [some issues](https://github.com/braintree/braintree_android/issues/491) 
with the nullable callback pattern. This pattern assumes that one of the nullable parameters 
will be non-null, but there is no code guarantee of that from the merchant perspective, so 
merchants have to handle each of the null/non-null scenarios.

This proposal documents three result handling scenarios in both Kotlin and Java. The first shows 
the v5 integration if we keep the two parameter callback pattern from v4. The second shows the 
v5 integration if we keep the listener pattern from v4. The third shows a single method and result 
object callback. The fourth shows a callback with multiple non-null result methods.

## Two Object Callback

This integration aligns most closely with the v4 integration callback pattern, but requires 
merchants to explicitly handle each null/non-null scenario, and results in an "unexpected error" 
scenario if both result and error are null. This should never happen based on our SDK code, but 
merchant code can't assume that, so their code would need to handle that unexpected case. 

### Kotlin

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

### Java

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

## Listener 

This integration aligns most closely with the v4 integration listener pattern, but either 
requires two separate listeners (for launcher and client), or couples the launcher and the 
client because both need to be invoked in different listener methods. The benefit of a single 
listener is that errors throughout the entire flow can be handled in one method (rather than two 
separate error handling points with the callback patterns). 

### Kotlin

```kotlin
payPalLauncher = PayPalLauncher(listener) 
payPalClient = PayPalClient(listener)
payPalClient.createPaymentAuthRequest(activity, request)

override fun onPaymentAuthRequest(paymentAuthRequest: PaymentAuthRequest) {
    payPalLauncher.launch(paymentAuthRequest)
}

override fun onPaymentAuthResult(paymentAuthResult: PaymentAuthResult) {
    payPalClient.tokenize(paymentAuthResult)
}

override fun onSuccess(payPalAccountNonce: PayPalAccountNonce) {
    // send nonce to server
}

override fun onCancel() {
    // handle cancel
}

override fun onError(error: Exception) {
    // handle error
}
```

### Java

```java
PayPalLauncher payPalLauncher = new PayPalLauncher(listener);
PayPalClient payPalClient = new PayPalClient(listener);
payPalClient.createPaymentAuthRequest(activity, request);

@Override
public void onPaymentAuthRequest(PaymentAuthRequest paymentAuthRequest) {
    payPalLauncher.launch(paymentAuthRequest)
}

@Override
public void onPaymentAuthResult(PaymentAuthResult paymentAuthResult) {
    payPalClient.tokenize(paymentAuthResult)
}

@Override
public void onSuccess(PayPalAccountNonce payPalAccountNonce) {
    // send nonce to server
}

@Override
public void onCancel() {
    // handle cancel
}

@Ovrride
public void onError(Exception error) {
    // handle error
}
```
## Single Result Object With Types

This approach is the most Kotlin-first pattern since the return type handling and casting can be 
done in the least lines of code. It also resolves the nullability issues. However, the Java 
integration becomes somewhat complex with casting. This approach aligns with how other payment 
SDKs handle results. 

This approach relies on the Kotlin sealed class, so requires the `PaymentAuthResult` and 
`PaymentResult` objects to live in `BraintreeCore`. This reduces code duplication in our SDK, but 
requires casting by the merchants (ex: PaymentMethodNonce is returned instead of 
module-specific PayPalAccountNonce). Once the SDK is fully converted to Kotlin, these could be 
moved into payment module specific sealed classes (ex: `PayPalPaymentAuthResult`).

### Kotlin

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

### Java

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

## Overloaded Callback

This approach solves the nullability issue for merchants, since each callback method would only be 
invoked with a non-null parameter. It also does not require casting of returned objects. However,
it is not a very Kotlin-forward approach and requires merchant Kotlin code to be overly verbose.

### Kotlin

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

### Java

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

