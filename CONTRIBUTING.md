# Contributing

Thanks for considering contributing to this project. Ways you can help:

* [Create a pull request](https://help.github.com/articles/creating-a-pull-request)
* [Add an issue](https://github.com/braintree/braintree_android/issues)
* [Contact us](README.md#feedback) with feedback

__Note on Translations:__ We cannot accept language translation requests. We support the same [languages that are supported by PayPal](https://developer.paypal.com/docs/api/reference/locale-codes/) and have a dedicated localization team to provide the translations.

## Development

Clone this repo and open it with Android Studio.

Read our [development guidelines](DEVELOPMENT.md) to get a sense of how we think about working on this codebase.

## Environments

The architecture of the Client API means that you'll need to develop against a merchant server when developing braintree-android.
The merchant server uses a server side client library such as [`braintree_ruby`](https://github.com/braintree/braintree_ruby) to
coordinate with a particular Braintree Gateway environment. The various Gateway environments, such as `development`, `sandbox` and `production`,
in turn determine the specific behaviors around merchant accounts, credit cards, PayPal, etc.

## Tests

It is not currently possible to run the tests outside of Braintree development, we are working on this for a future release.
