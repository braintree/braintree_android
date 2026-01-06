# Braintree-Android Development Notes

This document outlines development practices that we follow internally while developing this SDK.

## Setup

* Make sure Java 11 is installed and available in your `PATH`.
* If you do not have the Android SDK installed, install [Android Studio](https://developer.android.com/studio) which includes the Android SDK.
* If you do have the Android SDK installed, add a `local.properties` file to the top level directory with `sdk.dir=/path/to/your/sdk`
* Run `./gradlew :Demo:installDebug` to install the [Demo](Demo) app on a device.
* See [the testing section](#tests) for more about setting up and running tests.

## Development Merchant Server

The included demo app utilizes a test merchant server hosted on heroku ([https://braintree-sample-merchant.herokuapp.com](https://braintree-sample-merchant.herokuapp.com)).
It produces client tokens that point to Braintree's Sandbox Environment.

## Tests

### Unit Tests

Run all unit tests:
```bash
./gradlew test
```

### Integration Tests

Run all integration tests:
```bash
./gradlew connectedAndroidTest
```
**Note:** You need to start an emulator or connect a device before running integration tests.

## Architecture

There are several components that comprise this SDK:

* [AmericanExpress](AmericanExpress) provides American Express rewards balance functionality.
* [BraintreeCore](BraintreeCore) provides the networking, communication and modeling layer for Braintree.
* [Card](Card) provides credit and debit card tokenization functionality.
* [DataCollector](DataCollector) collects and provides data for PayPal fraud detection.
* [Demo](Demo) is a collection of Braintree reference integrations.
* [GooglePay](GooglePay) provides Google Pay integration.
* [LocalPayment](LocalPayment) provides LocalPayment integration.
* [PayPal](PayPal) provides PayPal integration.
* [PayPalMessaging](PayPalMessaging) provides PayPal messaging and promotional offers.
* [SEPADirectDebit](SEPADirectDebit) provides SEPA Direct Debit integration.
* [SharedUtils](SharedUtils) provides shared utilities across all modules in the SDK.
* [ShopperInsights](ShopperInsights) provides shopper insights and recommendations.
* [TestUtils](TestUtils) contains common test code used between modules.
* [ThreeDSecure](ThreeDSecure) provides 3D Secure authentication support.
* [UIComponents](UIComponents) provides PayPal/Venmo branded buttons that support complete PayPal/Venmo flows.
* [Venmo](Venmo) provides Venmo integration.
* [VisaCheckout](VisaCheckout) provides Visa Checkout integration.

The individual components may be of interest for advanced integrations and are each available as modules in maven.

## Environmental Assumptions

* Java 11
* Android Studio
* Gradle
* Android SDK >= 23
* Host app does not integrate with the Kount SDK
* Host app has a secure, authenticated server with a [Braintree server-side integration](https://developer.paypal.com/braintree/docs/start/hello-server)

## Committing

* Commits should be small but atomic. Tests should always be passing; the product should always function appropriately.
* Commit messages should be concise and descriptive.

## Deployment and Code Organization

* Code on main is assumed to be in a relatively good state at all times
  * Tests should be passing, all demo apps should run
  * Functionality and user experience should be cohesive
  * Dead code should be kept to a minimum
* Versioned deployments are tagged with their version numbers
  * Version numbers conform to [SEMVER](http://semver.org)
  * These versions are more heavily tested
  * We will provide support for these versions and commit to maintaining backwards compatibility on our servers
* Pull requests are welcome
  * Feel free to create an issue on Github before investing development time
* As needed, the Braintree team may develop features privately
  * If our internal and public branches get out of sync, we will reconcile this with merges (as opposed to rebasing)
  * In general, we will try to develop in the open as much as possible
