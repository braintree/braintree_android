# Braintree Android SDK Dependencies

This document lists the runtime dependencies (both internal module dependencies and third-party libraries) for each module in the Braintree Android SDK. For most dependency versions, see [`gradle/libs.versions.toml`](gradle/libs.versions.toml). Exception: the Magnes SDK is a bundled JAR in `DataCollector/libs/`.

## Build Requirements (Merchant-Facing)

This table lists the minimum requirements for building the Braintree Android SDK, those will impose requirements on the merchant's app as well.

| Setting | Value |
|---|---|
| `compileSdk` | 37 |
| `minSdk` | 23 |
| Java bytecode level (`targetCompatibility`) | 11 | 
| Kotlin version | 1.9.10 | 
| Android Gradle Plugin | 8.13.2 |

## Key Third-Party Libraries

| Library                                                                                                                           | Module(s) |
|-----------------------------------------------------------------------------------------------------------------------------------|---|
| [browser-switch](https://github.com/braintree/browser-switch-android)                                                             | BraintreeCore |
| [Cardinal Mobile SDK](https://developer.cardinaltrusted.com/docs/welcome)                                                         | ThreeDSecure |
| [Google Play Services Wallet](https://developers.google.com/pay/api)                                                              | GooglePay |
| [PayPal Messaging](https://developer.paypal.com/braintree/docs/guides/paypal/messaging/android/v5/)                               | PayPalMessaging |
| [OkHttp](https://square.github.io/okhttp/)                                                                                        | SharedUtils |
| [Magnes SDK](https://developer.paypal.com/braintree/docs/guides/premium-fraud-management-tools/device-data-collection/android/v5) | DataCollector |
| [Accompanist](https://google.github.io/accompanist/)                                                                              | UIComponents |

## browser-switch Dependencies

`com.braintreepayments.api:browser-switch:3.5.1` is declared as a dependency of `BraintreeCore`. Its own runtime dependencies are:

| Dependency | Version |
|---|---|
| `androidx.annotation:annotation` | 1.7.0 |
| `androidx.appcompat:appcompat` | 1.6.0 |
| `androidx.browser:browser` | 1.10.0-alpha02 |

## Per-Module Dependencies

### BraintreeCore

| Dependency | Version |
|---|---|
| `:SharedUtils` | — |
| `com.braintreepayments.api:browser-switch` | 3.5.1 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.7.1 |
| `androidx.appcompat:appcompat` | 1.6.0 |
| `androidx.work:work-runtime` | 2.8.1 |
| `androidx.core:core-ktx` | 1.12.0 |
| `androidx.room:room-runtime` | 2.6.1 |
| `org.jetbrains.kotlin:kotlin-stdlib` | 1.9.10 |

### Card

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `androidx.annotation:annotation` | 1.7.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.7.1 |

### PayPal

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `:DataCollector` | — |
| `androidx.appcompat:appcompat` | 1.6.0 |

### Venmo

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `androidx.appcompat:appcompat` | 1.6.0 |

### GooglePay

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `:PayPal` | — |
| `:Card` | — |
| `com.google.android.gms:play-services-wallet` | 19.5.0 |
| `androidx.appcompat:appcompat` | 1.6.0 |

### ThreeDSecure

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `:Card` | — |
| `org.jfrog.cardinalcommerce.gradle:cardinalmobilesdk` | 2.2.7-7 |
| `androidx.appcompat:appcompat` | 1.6.0 |
| `androidx.lifecycle:lifecycle-runtime` | 2.6.2 |

### AmericanExpress

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `androidx.annotation:annotation` | 1.7.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.7.1 |

### DataCollector

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `android-magnessdk` (bundled JAR) | 5.6.0 |
| `androidx.annotation:annotation` | 1.7.0 |

### LocalPayment

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `:DataCollector` | — |
| `androidx.appcompat:appcompat` | 1.6.0 |

### SEPADirectDebit

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `androidx.appcompat:appcompat` | 1.6.0 |

### ShopperInsights

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `androidx.core:core-ktx` | 1.12.0 |

### PayPalMessaging

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `com.paypal.messages:paypal-messages` | 1.1.13 |
| `androidx.appcompat:appcompat` | 1.6.0 |
| `androidx.core:core-ktx` | 1.12.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | 1.9.10 |

### UIComponents

| Dependency | Version |
|---|---|
| `:BraintreeCore` | — |
| `:Card` | — |
| `:PayPal` | — |
| `:Venmo` | — |
| `com.google.accompanist:accompanist-drawablepainter` | 0.37.3 |
| `androidx.activity:activity-compose` | 1.12.0 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.5.1 |
| `androidx.datastore:datastore-preferences` | 1.2.0 |
| `androidx.compose:compose-bom` | 2025.06.01 |
| `androidx.compose.material3:material3` | 1.3.2 (BOM-managed) |
| `androidx.compose.ui:ui-tooling-preview` | 1.9.2 (BOM-managed) |
| `androidx.compose.animation:animation-graphics` | 1.8.3 (BOM-managed) |
| `androidx.autofill:autofill` | 1.3.0 |
| `androidx.appcompat:appcompat` | 1.6.0 |
| `androidx.core:core-ktx` | 1.12.0 |

### SharedUtils

| Dependency | Version |
|---|---|
| `com.squareup.okhttp3:okhttp` | 4.12.0 |
| `androidx.annotation:annotation` | 1.7.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | 1.9.10 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.7.1 |