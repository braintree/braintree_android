# Deprecate Client Token Provider

**Status: Proposed**

## Context

The `ClientTokenProvider` integration pattern was created in response to the Braintree SDK's migration to the new Android [Activity Result API](https://developer.android.com/training/basics/intents/result). The new API has an unfortunate restriction–the entry method `Activity#registerForActivityResult()` must be called in (or before) [the host Activity's onCreate() method](https://stackoverflow.com/a/63883427).

