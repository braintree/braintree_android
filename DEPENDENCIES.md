# Braintree Android SDK Dependencies

This document lists the third-party runtime dependencies for each module in the Braintree Android SDK. For exact versions, see [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

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

## Per-Module Dependencies

### BraintreeCore

| Dependency | Scope |
|---|---|
| `:SharedUtils` | api |
| `com.braintreepayments.api:browser-switch` | api |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | api |
| `androidx.appcompat:appcompat` | implementation |
| `androidx.work:work-runtime` | implementation |
| `androidx.core:core-ktx` | implementation |
| `androidx.room:room-runtime` | implementation |
| `org.jetbrains.kotlin:kotlin-stdlib` | implementation |

### Card

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `androidx.annotation:annotation` | implementation |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | implementation |

### PayPal

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `:DataCollector` | implementation |
| `androidx.appcompat:appcompat` | implementation |

### Venmo

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `androidx.appcompat:appcompat` | implementation |

### GooglePay

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `:PayPal` | api |
| `:Card` | api |
| `com.google.android.gms:play-services-wallet` | api |
| `androidx.appcompat:appcompat` | implementation |

### ThreeDSecure

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `:Card` | api |
| `org.jfrog.cardinalcommerce.gradle:cardinalmobilesdk` | implementation |
| `androidx.appcompat:appcompat` | implementation |
| `androidx.lifecycle:lifecycle-runtime` | implementation |

### AmericanExpress

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `androidx.annotation:annotation` | implementation |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | implementation |

### DataCollector

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| Magnes SDK (local JAR) | implementation |
| `androidx.annotation:annotation` | implementation |

### LocalPayment

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `:DataCollector` | implementation |
| `androidx.appcompat:appcompat` | implementation |

### SEPADirectDebit

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `androidx.appcompat:appcompat` | implementation |

### ShopperInsights

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `androidx.core:core-ktx` | implementation |

### PayPalMessaging

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `com.paypal.messages:paypal-messages` | implementation |
| `androidx.appcompat:appcompat` | implementation |
| `androidx.core:core-ktx` | implementation |
| `org.jetbrains.kotlin:kotlin-stdlib` | implementation |

### UIComponents

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `:PayPal` | api |
| `:Venmo` | api |
| `com.google.accompanist:accompanist-drawablepainter` | implementation |
| `androidx.activity:activity-compose` | implementation |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | implementation |
| `androidx.datastore:datastore-preferences` | implementation |
| `androidx.compose:compose-bom` | implementation (BOM) |
| `androidx.compose.material3:material3` | implementation |
| `androidx.compose.ui:ui-tooling-preview` | implementation |
| `androidx.compose.animation:animation-graphics` | implementation |
| `androidx.appcompat:appcompat` | implementation |
| `androidx.core:core-ktx` | implementation |

### VisaCheckout (Deprecated)

| Dependency | Scope |
|---|---|
| `:BraintreeCore` | api |
| `:Card` | api |
| `com.visa.checkout:visacheckout-android-sdk` | api |
| `androidx.appcompat:appcompat` | implementation |

### SharedUtils

| Dependency | Scope |
|---|---|
| `com.squareup.okhttp3:okhttp` | implementation |
| `androidx.annotation:annotation` | implementation |
| `org.jetbrains.kotlin:kotlin-stdlib` | implementation |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | implementation |