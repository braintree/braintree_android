# Braintree Android SDK

![GitHub Actions Tests](https://github.com/braintree/braintree_android/workflows/Tests/badge.svg)

Welcome to Braintree's Android SDK. This library will help you accept card and alternative payments in your Android app.

:mega:&nbsp;&nbsp;A new major version of the SDK is now available. See the [v4.9.0+ migration guide](v4.9.0+_MIGRATION_GUIDE.md) for details.

**The Braintree SDK supports Android API 21 and above.**

> The PayPalNativeCheckout module supports Android API 23 and above.

The Braintree SDK requires Java 8 as of version 4.24.0. See the [CHANGELOG](https://github.com/braintree/braintree_android/blob/main/CHANGELOG.md#4240) for more details. The Braintree SDK uses Kotlin 1.7. 

## Adding It To Your Project

The features of the Braintree SDK are organized into modules that can be imported as dependencies in your `build.gradle` file.
See the [Migration Guide](v4.9.0+_MIGRATION_GUIDE.md) for specific dependencies required for each module.

For an integration offering card payments, add the following dependency in your `build.gradle`:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:card:4.41.0'
}
```

To preview the latest work in progress builds, add the following SNAPSHOT dependency in your `build.gradle`:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:card:4.41.1-SNAPSHOT'
}
```

You will also need to add the Sonatype snapshots repo to your top-level `build.gradle` to import SNAPSHOT builds:

```groovy
allprojects {
    repositories {
        maven {
            url 'https://oss.sonatype.org/content/repositories/snapshots/'
        }
    }
}
```

## Documentation

Start with [**'Hello, Client!'**](https://developer.paypal.com/braintree/docs/start/hello-client/android/v4) for instructions on basic setup and usage.

Next, read the [**full documentation**](https://developer.paypal.com/braintree/docs/guides/overview) for information about integration options, such as Drop-In UI, PayPal and credit card tokenization.

## Versions

This SDK abides by our Client SDK Deprecation Policy. For more information on the potential statuses of an SDK check our [developer docs](https://developer.paypal.com/braintree/docs/guides/client-sdk/deprecation-policy).

| Major version number | Status | Released | Deprecated | Unsupported |
| -------------------- | ------ | -------- | ---------- | ----------- |
| 4.x.x | Active | June 2021 | TBA | TBA |
| 3.x.x | Unsupported | February 2019 | June 2022 | June 2023 |
| 2.x.x | Unsupported | November 2015 | March 2020 | March 2021 |

Versions 2.7.3 and below use outdated SSL certificates and are unsupported.

## Help

* [Read the docs](https://developer.paypal.com/braintree/docs/guides/overview)
* [Check out the reference docs](https://braintree.github.io/braintree_android/index.html)
* Find a bug? [Open an issue](https://github.com/braintree/braintree_android/issues)
* Want to contribute? [Check out contributing guidelines](CONTRIBUTING.md) and [submit a pull request](https://help.github.com/articles/creating-a-pull-request).

## Feedback

The Braintree Android SDK is in active development. We welcome your feedback!

Here are a few ways to get in touch:

* [GitHub Issues](https://github.com/braintree/braintree_android/issues/new/choose) - For generally applicable issues and feedback
* [Braintree Support](https://developer.paypal.com/braintree/articles) / [support@braintreepayments.com](mailto:support@braintreepayments.com) -
for personal support at any phase of integration

## License

The Braintree Android SDK is open source and available under the MIT license. See the [LICENSE](LICENSE) file for more info.
