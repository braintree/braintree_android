# Deprecate Client Token Provider

**Status: Proposed**

## Context

The `ClientTokenProvider` integration pattern was created in response to the Braintree SDK's migration to the new Android [Activity Result API](https://developer.android.com/training/basics/intents/result). The new API has an unfortunate restriction–the entry method `Activity#registerForActivityResult()` must be called in (or before) [the host Activity's onCreate() method](https://stackoverflow.com/a/63883427). Braintree SDK feature clients (like `PayPalClient`, etc.) encapsulate this logic, and therefore also must be instantiatedby the time the host Activity or Fragment has reached a `CREATED` state. 

Feature clients support both [Tokenization Key](https://developer.paypal.com/braintree/docs/guides/authorization/tokenization-key) and [Client Token](https://developer.paypal.com/braintree/docs/guides/authorization/client-token) authorization methods. To create a client token, a merchant must integrate with one of the many Braintree Server SDKs. A merchant Android application will then require a network call to obtain a Client Token.

Assuming the networking call is asynchronous, a merchant would need to fetch a client token before presenting a payment activity that uses the Braintree SDK. Technically a merchant could forward a fetched client token and forward the token via Intent extras to the host Activity–however this method is cumbersome and error prone. A `ClientTokenProvider` helps by allowing a merchant to provide their own client token fetch strategy to the SDK. The SDK then will execute a client token fetch using the merchant implementation when an authorized request needs to be made. With a `ClientTokenProvider` instance, a merchant can now instantiate a Feature Client within `onCreate()` and satisfy Activity Result API requirements. 

## Decision



