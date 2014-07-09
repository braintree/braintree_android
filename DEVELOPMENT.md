# Braintree-Android Development Notes

This document outlines development practices that we follow internally while developing this SDK.

## Development Merchant Server

The included demo app utilizes a test merchant server hosted on heroku ([https://braintree-sample-merchant.herokuapp.com](https://braintree-sample-merchant.herokuapp.com)). It produces client tokens that point to Braintree's Sandbox Environment.

This merchant server is also provided as a gem called [taproot](https://github.com/benmills/taproot/). If you'd like, you can run taproot locally and hit a development Gateway running on `localhost`:

```
git clone https://github.com/benmills/taproot.git
cd taproot
bundle
taprootd

# In a new shell
curl localhost:3132
curl localhost:3132/client_token
```

You can now change the merchant server base URL specified in `Drop-InSample/src/main/java/com/braintreepayments/sample/BaseActivity.java`.

## Tests

You can run all tests on the command line with `bundle && rake`.

It's a good idea to run `rake`, which runs all tests, before committing.

Please note: It is not currently possible to run tests outside of Braintree.

## Architecture

There are several components that comprise this SDK:

* [BraintreeApi](BraintreeApi) provides the networking and communication layer. Includes the PayPal Android mobile SDK.
* [BraintreeData](BraintreeData) collects and provides data for fraud detection.
* [Drop-In](Drop-In) uses `BraintreeApi` to create a full checkout experience inside an `Activity`.
* [Drop-InSample](Drop-InSample) the reference integration of [Drop-In](Drop-In)

The individual components may be of interest for advanced integrations and are each available as modules.

## Environmental Assumptions

* Android Studio 0.8.x and Android 19
* Android 19 target sdk
* Gradle
* Host app does not integrate the [PayPal Android SDK](https://github.com/paypal/PayPal-Android-SDK)
* Host app does not integrate with the Kount SDK
* Host app does not integrate with [card.io](https://www.card.io/)
* Host app has a secure, authenticated server with a [Braintree server-side integration](https://developers.braintreepayments.com/ios/start/hello-server)

## Committing

* Commits should be small but atomic. Tests should always be passing; the product should always function appropriately.
* Commit messages should be concise and descriptive.
* Commit messages reference the trello board by ID or URL. (Sorry, these are not externally viewable.)

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
