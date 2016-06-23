# Braintree-Android Development Notes

This document outlines development practices that we follow internally while developing this SDK.

## Setup

* Make sure Java 8 is installed and available in your `PATH`.
* If you do not have the Android SDK installed, run `./gradlew build` 3 times to download the Android SDK and install all required tools as well as set your `local.properties` file (we use [sdk-manager-plugin](https://github.com/JakeWharton/sdk-manager-plugin) to do this automatically).
* If you do have the Android SDK installed, add a `local.properties` file to the top level directory with `sdk.dir=/path/to/your/sdk/.android-sdk`
* Run `./gradlew :Demo:installDebug` to install the [Demo](Demo) app on a device.
* See [the testing section](#tests) for more about setting up and running tests.

## Development Merchant Server

The included demo app utilizes a test merchant server hosted on heroku ([https://braintree-sample-merchant.herokuapp.com](https://braintree-sample-merchant.herokuapp.com)).
It produces client tokens that point to Braintree's Sandbox Environment.

You can also host a locally running Gateway and/or merchant server.

## Tests

All tests can be run on the command line with `rake`. It's a good idea to run `rake`, before committing.

If you are running tests on an emulator or need to point at a specific url, the following can be set in the top level `gradle.properties` file:

```
// defaults to 10.0.2.2
systemProp.LOCALHOST_IP="URL_OR_IP_OF_YOUR_LOCALHOST"

// defaults to 10.0.2.2
systemProp.GATEWAY_IP="URL_OR_IP_OF_YOUR_GATEWAY"

// defaults to 3000
systemProp.GATEWAY_PORT="PORT_OF_YOUR_GATEWAY"

systemProp.DEVELOPMENT_URL="FULL_URL_AND_PORT_OF_A_DEVELOPMENT_GATEWAY"
```

Please note: It is not currently possible to run tests outside of Braintree.

## Architecture

There are several components that comprise this SDK:

* [Braintree](Braintree) provides the networking, communication and modeling layer for Braintree.
* [BraintreeDataCollector](BraintreeDataCollector) collects and provides data for fraud detection.
* [Core](Core) provides shared code across all the modules in the SDK.
* [Drop-In](Drop-In) uses `Braintree` to create a full checkout experience inside an `Activity`.
* [Demo](Demo) is the reference integration of [Drop-In](Drop-In).
* [PayPalOneTouch](PayPalOneTouch) provides support for PayPal app and browser switch.
* [PayPalDataCollector](PayPalDataCollector) collects and provides data for PayPal fraud detection.
* [TestUtils](TestUtils) contains common test code used between modules.

The individual components may be of interest for advanced integrations and are each available as modules in maven.

## Environmental Assumptions

* Java 8
* Android Studio
* Gradle
* Android SDK >= 15
* Host app does not integrate with the Kount SDK
* Host app has a secure, authenticated server with a [Braintree server-side integration](https://developers.braintreepayments.com/android/start/hello-server)

## Committing

* Commits should be small but atomic. Tests should always be passing; the product should always function appropriately.
* Commit messages should be concise and descriptive.
* Commit messages may reference the trello board by ID or URL. (Sorry, these are not externally viewable.)

## Deployment and Code Organization

* Code on master is assumed to be in a relatively good state at all times
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
