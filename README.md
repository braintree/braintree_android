# Braintree Android SDK

[![Build Status](https://travis-ci.org/braintree/braintree_android.svg?branch=master)](https://travis-ci.org/braintree/braintree_android)

Welcome to Braintree's Android SDK. This library will help you accept card and alternative payments in your Android app.

:mega:&nbsp;&nbsp;A new major version of the SDK is available in beta. See the [v4 migration guide](v4_MIGRATION_GUIDE.md) for details.

Braintree's Android SDK is available for Android SDK >= 21.

## Adding It To Your Project

Add the dependency in your `build.gradle`:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:braintree:3.17.0'
}
```

To use the latest build from the `master` branch use:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:braintree:3.17.1-SNAPSHOT'
}
```

## Documentation

Start with [**'Hello, Client!'**](https://developers.braintreepayments.com/start/hello-client/android/v3) for instructions on basic setup and usage.

Next, read the [**full documentation**](https://developers.braintreepayments.com/guides/overview) for information about integration options, such as Drop-In UI, PayPal and credit card tokenization.

## Versions

This SDK abides by our Client SDK Deprecation Policy. For more information on the potential statuses of an SDK check our [developer docs](http://developers.braintreepayments.com/guides/client-sdk/deprecation-policy).

<!-- TODO: Update TBA dates when v4 GA releases. -->
| Major version number | Status | Released | Deprecated | Unsupported |
| -------------------- | ------ | -------- | ---------- | ----------- |
| 4.x.x | Beta | March 2021 | TBA | TBA |
| 3.x.x | Active | February 2019 | TBA | TBA |
| 2.x.x | Unsupported | November 2015 | March 2020 | March 2021 |

Versions 2.7.3 and below use outdated SSL certificates and are unsupported.

## Help

* [Read the docs](https://developers.braintreepayments.com/guides/overview)
* Find a bug? [Open an issue](https://github.com/braintree/braintree_android/issues)
* Want to contribute? [Check out contributing guidelines](CONTRIBUTING.md) and [submit a pull request](https://help.github.com/articles/creating-a-pull-request).

## Feedback

The Braintree Android SDK is in active development, we welcome your feedback!

Here are a few ways to get in touch:

* [GitHub Issues](https://github.com/braintree/braintree_android/issues) - For generally applicable issues and feedback
* [Braintree Support](https://articles.braintreepayments.com/) / [support@braintreepayments.com](mailto:support@braintreepayments.com) -
for personal support at any phase of integration

## License

The Braintree Android SDK is open source and available under the MIT license. See the [LICENSE](LICENSE) file for more info.
