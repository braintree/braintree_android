# Braintree Android v5 Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the basics for updating your Braintree integration from v4 to v5.

## Table of Contents

1. [Android API](#android-api)
1. [Data Collector](#data-collector)
1. [Union Pay](#union-pay)

## Android API

The minimum supported Android API level for v5 of this SDK has increased to 23.

## Data Collector

The `data-collector` module has been removed and replaced by the `paypal-data-collector` module. 
The `DataCollector` class within the `paypal-data-collector` module has the same 
`collectDeviceData` methods, so if you were previously using the `data-collector` library, no code 
changes are required aside from updating your dependency.

If you were using the `paypal-data-collector` library in v4, the `PayPal` prefix has been 
removed from all class names in v5.

```kotlin
val dataCollector = DataCollector(braintreeClient)
dataCollector.collectDeviceData(context) { deviceData, error -> 
    // send deviceData to your server
}
```

## Union Pay

The `union-pay` module, and all containing classes, was removed in v5. UnionPay cards can now be processed as regular cards, through the `card` module. You no longer need to manage card enrollment via SMS authorization.

Now, you can tokenize with just the card details:

// TODO: code snippet of card integration in v5



