# Braintree Android v4 (Beta) Migration Guide

See the [CHANGELOG](/CHANGELOG.md) for a complete list of changes. This migration guide outlines the basics for updating your Braintree integration from v3 to v4.

_Documentation for v4 will be published to https://developers.braintreepayments.com once it is available for general release._

## Table of Contents

1. [Gradle](#gradle)
1. [Browser Switch](#browser-switch)
1. [American Express](#american-express)
1. [Card](#card)
1. [Data Collector](#data-collector)
1. [Local Payment](#local-payment)
1. [Google Pay](#google-pay)
1. [PayPal](#paypal)
1. [Visa Checkout](#visa-checkout)
1. [Union Pay](#union-pay)
1. [Venmo](#venmo)
1. [3D Secure](#3d-secure)
1. [Integrating Multiple Payment Methods](#integrating-multiple-payment-methods)

## Gradle

The features of the Braintree SDK are now organized into modules and can each be imported as dependencies in your `build.gradle` file. You must remove the `com.braintreepayments.api:braintree:3.x.x` dependency when migrating to v4.

The examples below show the required dependencies for each feature. 

## Browser Switch

In v3, `com.braintreepayments.api.BraintreeBrowserSwitchActivity` was the designated deep link destination activity maintained by the Braintree SDK. In v4, we've removed `BraintreeBrowserSwitchActivity` to give apps more control over their deep link configuration.

In the `AndroidManifest.xml`, migrate the `intent-filter` from your v3 integration into an activity you own:

```xml
<activity android:name="com.company.app.MyPaymentsActivity">
    ...
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <data android:scheme="${applicationId}.braintree"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
    </intent-filter>
</activity>
``` 

## American Express

The American Express feature is now supported by implementing the following dependencies:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:american-express:4.0.0-beta2'
  implementation 'com.braintreepayments.api:card:4.0.0-beta2'
}
```

To use the feature, instantiate an `AmericanExpressClient`:

```java
package com.my.app;

public class AmericanExpressActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private AmericanExpressClient americanExpressClient;
  private CardClient cardClient;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      americanExpressClient = new AmericanExpressClient(braintreeClient);

      // you will also need a card client for tokenization in this example
      cardClient = new CardClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }

  private void tokenizeCard() {
    Card card = new Card();
    card.setNumber("378282246310005");
    card.setExpirationDate("12/2022");

    cardClient.tokenize(this, card, (cardNonce, error) -> {
      if (cardNonce != null) {
        getAmexRewardsBalance(cardNonce);
      } else {
        // handle error
      }
    });
  }

  private void getAmexRewardsBalance(CardNonce cardNonce) {
    String nonceString = cardNonce.getNonce();
    americanExpressClient.getRewardsBalance(nonceString, "USD", (rewardsBalance, error) -> {
      if (rewardsBalance != null) {
        // display rewards amount to user
        String rewardsAmount = rewardsBalance.getRewardsAmount();
      } else {
        // handle error
      }
    });
  }
}
```

## Card

The Card feature is now supported in a single dependency:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:card:4.0.0-beta2'
}
```

To use the feature, instantiate a `CardClient`:

```java
package com.my.app;

public class CardActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private CardClient cardClient;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      cardClient = new CardClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }

  private void tokenizeCard() {
    Card card = new Card();
    card.setNumber("4111111111111111");
    card.setExpirationDate("12/2022");

    cardClient.tokenize(this, card, (cardNonce, error) -> {
      if (cardNonce != null) {
        // send this nonce to your server
        String nonce = cardNonce.getNonce();
      } else {
        // handle error
      }
    });
  }
}
```

## Data Collector

The Data Collector feature is now supported in the following dependency:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:data-collector:4.0.0-beta2'
}
```

To use the feature, instantiate a `DataCollector`:

```java
package com.my.app;

public class PaymentsActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private DataCollector dataCollector;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      dataCollector = new DataCollector(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }
  
  private void collectDeviceData() {
    dataCollector.collectDeviceData(this, (deviceData, error) -> {
      // send deviceData to your server
    });
  }
}
```

## Local Payment

The Local Payment feature is now supported in a single dependency:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:local-payment:4.0.0-beta2'
}
```

To use the feature, instantiate a `LocalPaymentClient`:

```java
package com.my.app;

public class LocalPaymentActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private LocalPaymentClient localPaymentClient;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      localPaymentClient = new LocalPaymentClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    
    BrowserSwitchResult browserSwitchResult = braintreeClient.deliverBrowserSwitchResult(this);
    if (browserSwitchResult != null && browserSwitchResult.getRequestCode() == BraintreeRequestCodes.LOCAL_PAYMENT) {
      localPaymentClient.onBrowserSwitchResult(this, browserSwitchResult, (localPaymentNonce, error) -> {
        if (localPaymentNonce) {
          // send this nonce to your server
          String nonce = localPaymentNonce.getNonce();
        } else {
          // handle result
        }
      });
    }
  }

  @Override
  protected void onNewIntent(Intent newIntent) {
    super.onNewIntent(newIntent);
    // required if your activity's launch mode is "singleTop", "singleTask", or "singleInstance"
    setIntent(newIntent);
  }
  
  private void startLocalPayment() {
    PostalAddress address = new PostalAddress();
    address.setStreetAddress("836486 of 22321 Park Lake");
    address.setCountryCodeAlpha2("NL");
    address.setLocality("Den Haag");
    address.setPostalCode("2585 GJ");

    LocalPaymentRequest request = new LocalPaymentRequest();
    request.setPaymentType("ideal");
    request.setAmount("1.01");
    request.setAddress(address);
    request.setPhone("639847934");
    request.setEmail("joe@getbraintree.com");
    request.setGivenName("Jon");
    request.setSurname("Doe");
    request.setShippingAddressRequired(true);
    request.setCurrencyCode("EUR");
     
    localPaymentClient.startPayment(request, (localPaymentTransaction, error) -> {
      if (localPaymentTransaction != null) {
        // do any pre-processing transaction.getPaymentId()
        localPaymentClient.approvePayment(MyLocalPaymentActivity.this, transaction);
      } else {
        // handle error
      }
    });
  }
}
```

## Google Pay

The Google Pay feature is now supported in a single dependency:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:google-pay:4.0.0-beta2'
}
```

To use the feature, instantiate an `GooglePayClient`:

```java
package com.my.app;

public class GooglePayActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private GooglePayClient googlePayClient;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      googlePayClient = new GooglePayClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }

  private void checkIfGooglePayIsAvailable() {
    googlePayClient.isReadyToPay(this, (isReadyToPay, error) -> {
      if (isReadyToPay) {
        // Google Pay is available
      } else {
        // handle error
      }
    });
  }

  private void makeGooglePayRequest() {
    GooglePayRequest googlePayRequest = new GooglePayRequest();
    googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
      .setTotalPrice("1.00")
      .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
      .setCurrencyCode("USD")
      .build());
    googlePayRequest.setBillingAddressRequired(true);
    googlePayRequest.setGoogleMerchantId("merchant-id-from-google");

    googlePayClient.requestPayment(this, googlePayRequest, (success, error) -> {
      if (error != null) {
        // Handle error
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == BraintreeRequestCodes.GOOGLE_PAY) {
      googlePayClient.onActivityResult(resultCode, data, (paymentMethodNonce, error) -> {
        if (paymentMethodNonce != null) {
          // send this nonce to your server
          String nonce = paymentMethodNonce.getNonce();
        } else {
          // handle error
        }
      });
    }
  }
}
```

## PayPal

The PayPal feature is now supported in a single dependency:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:paypal:4.0.0-beta2'
}
```

To use the feature, instantiate a `PayPalClient`:

```java
package com.my.app;

public class PayPalActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private PayPalClient payPalClient;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      payPalClient = new PayPalClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    
    BrowserSwitchResult browserSwitchResult = braintreeClient.deliverBrowserSwitchResult(this);
    if (browserSwitchResult != null && browserSwitchResult.getRequestCode() == BraintreeRequestCodes.PAYPAL) {
      payPalClient.onBrowserSwitchResult(browserSwitchResult, (payPalAccountNonce, error) -> {
        if (payPalAccountNonce != null) {
          // Send nonce to server
          String nonce = payPalNonce.getNonce();
        } else {
          // handle error
        }
      });
    }
  }

  @Override
  protected void onNewIntent(Intent newIntent) {
    super.onNewIntent(newIntent);
    // required if your activity's launch mode is "singleTop", "singleTask", or "singleInstance"
    setIntent(newIntent);
  }

  private void myTokenizePayPalAccountWithCheckoutMethod() {
    PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");
    request.setCurrencyCode("USD");
    request.setIntent(PayPalPaymentIntent.AUTHORIZE);

    payPalClient.tokenizePayPalAccount(this, request, (error) -> {
      if (error != null) {
        // Handle error
      }
    });
  }

  private void myTokenizePayPalAccountWithVaultMethod() {
    PayPalVaultRequest request = new PayPalVaultRequest();
    request.setBillingAgreementDescription("Your agreement description");

    payPalClient.tokenizePayPalAccount(this, request, (error) -> {
      if (error != null) {
        // Handle error
      }
    });
  }
}
```

#### PayPal Request

v4 introduces two subclasses of `PayPalRequest`: 
- `PayPalCheckoutRequest`, for checkout flows
- `PayPalVaultRequest`, for vault flows

The setters on the request classes have been updated to remove method chaining.

The `requestOneTimePayment` and `requestBillingAgreement` methods on `PayPalClient` have been updated to expect instances of `PayPalCheckoutRequest` and `PayPalVaultRequest`, respectively.

However, `requestOneTimePayment` and `requestBillingAgreement` have been deprecated in favor of `tokenizePayPalAccount`.

## Visa Checkout

The Visa Checkout feature is now supported in a single dependency:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:visa-checkout:4.0.0-beta2'
}
```

To use the feature, instantiate a `VisaCheckoutClient`:

```java
package com.my.app;

public class VisaCheckoutActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private VisaCheckoutClient visaCheckoutClient;

  private CheckoutButton checkoutButton;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.visa_checkout_activity);
    checkoutButton = (CheckoutButton) findViewById(R.id.visa_checkout_button);

    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      visaCheckoutClient = new VisaCheckoutClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }
 
  private void getVisaCheckoutProfile() {
    visaCheckoutClient.createProfileBuilder((profileBuilder, error) -> {
      if (profileBuilder != null) {
        setupVisaCheckoutButton(profileBuilder.build());
      } else {
        // handle error
      }
    });
  }

  private void setupVisaCheckoutButton(Profile visaCheckoutProfile) {
    PurchaseInfo purchaseInfo = new PurchaseInfo.PurchaseInfoBuilder(new BigDecimal("1.00"), PurchaseInfo.Currency.USD)
        .setDescription("Description")
        .build();
    checkoutButton.init(this, visaCheckoutProfile, purchaseInfo, new VisaCheckoutSdk.VisaCheckoutResultListener() {
      @Override
      public void onButtonClick(LaunchReadyHandler launchReadyHandler) {
        launchReadyHandler.launch();
      }

      @Override
      public void onResult(VisaPaymentSummary visaPaymentSummary) {
        processVisaPaymentSummary(visaPaymentSummary);
      }
    });
  }

  private void processVisaPaymentSummary(VisaPaymentSummary visaPaymentSummary) {
    visaCheckoutClient.tokenize(visaPaymentSummary, (paymentMethodNonce, error) -> {
      if (paymentMethodNonce != null) {
        // send this nonce to your server
        String nonce = paymentMethodNonce.getNonce();
      } else {
        // handle error
      }
    });
  }
}
```

## Union Pay

The Union Pay feature is now supported by implementing the following dependencies:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:union-pay:4.0.0-beta2'
  implementation 'com.braintreepayments.api:card:4.0.0-beta2'
}
```

To use the feature, instantiate a `UnionPayClient`:

```java
package com.my.app;

public class UnionPayActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private UnionPayClient unionPayClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      unionPayClient = new UnionPayClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }
  
  private void fetchUnionPayCapabilities() {
    unionPayClient.fetchCapabilities("4111111111111111", (capabilities, error) -> {
      if (capabilities != null) {
        // inspect Union Pay capabilities
      } else {
        // handle error
      }
    });
  }

  private void enrollUnionPay() {
    UnionPayCard unionPayCard = new UnionPayCard();
    unionPayCard.setNumber("4111111111111111");
    unionPayCard.setExpirationMonth("12");
    unionPayCard.setExpirationYear("22");
    unionPayCard.setCvv("123");
    unionPayCard.setPostalCode("12345");
    unionPayCard.setMobileCountryCode("1");
    unionPayCard.setMobilePhoneNumber("1234567890");

    unionPayClient.enroll(unionPayCard, (enrollment, error) -> {
      unionPayCard.setSmsCode("1234");
      unionPayCard.setEnrollmentId(enrollment.getId());
      tokenizeUnionPay(unionPayCard);
    });
  }

  private tokenizeUnionPay(UnionPayCard unionPayCard) {
    unionPayClient.tokenize(unionPayCard, (cardNonce, error) -> {
      if (cardNonce != null) {
        // send this nonce to your server
        String nonce = cardNonce.getNonce();
      } else {
        // handle error
      }
    });
  }
}
```

## Venmo

The Venmo feature is now supported in a single dependency:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:venmo:4.0.0-beta2'
}
```

To use the feature, instantiate a `VenmoClient`:

```java
package com.my.app;

public class VenmoActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private VenmoClient venmoClient;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      venmoClient = new VenmoClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }

  // The authorizeAccount() method has been replaced with tokenizeVenmoAccount()
  private void tokenizeVenmoAccount() {
    VenmoRequest request = new VenmoRequest();
    request.setProfileId("your-profile-id");
    request.setShouldVault(false);
          
    venmoClient.tokenizeVenmoAccount(this, request, (error) -> {
      if (error != null) {
        // handle error
      }
    });
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == BraintreeRequestCodes.VENMO) {
      venmoClient.onActivityResult(this, resultCode, data, (venmoAccountNonce, error) -> {
        if (venmoAccountNonce != null) {
          // send nonce to server
          String nonce = venmoAccountNonce.getNonce();
        } else {
          // handle error
        }
      });
    }
  }
}
```

## 3D Secure

The 3D Secure feature is now supported in a single dependency:

```groovy
dependencies {
  implementation 'com.braintreepayments.api:three-d-secure:4.0.0-beta2'
}
```

Additionally, add the following Maven repository and (non-sensitive) credentials to your app-level gradle:

```groovy
repositories {
    maven {
        url "https://cardinalcommerce.bintray.com/android"
        credentials {
            username 'braintree-team-sdk@cardinalcommerce'
            password '220cc9476025679c4e5c843666c27d97cfb0f951'
        }
    }
}
```

To use the feature, instantiate a `ThreeDSecureClient`:

```java
package com.my.app;

public class ThreeDSecureActivity extends AppCompatActivity {

  private BraintreeClient braintreeClient;
  private ThreeDSecureClient threeDSecureClient;
  private CardClient cardClient;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      braintreeClient = new BraintreeClient(this, "TOKENIZATION_KEY_OR_CLIENT_TOKEN");
      threeDSecureClient = new ThreeDSecureClient(braintreeClient);

      // you will also need a card client for tokenization in this example
      cardClient = new CardClient(braintreeClient);
    } catch (InvalidArgumentException e) {
      // handle authorization error
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    
    BrowserSwitchResult browserSwitchResult = braintreeClient.deliverBrowserSwitchResult(this);
    if (browserSwitchResult != null && browserSwitchResult.getRequestCode() == BraintreeRequestCodes.THREE_D_SECURE) {
      threeDSecureClient.onBrowserSwitchResult(browserSwitchResult, this::handleThreeDSecureResult); 
    }
  }

  @Override
  protected void onNewIntent(Intent newIntent) {
    super.onNewIntent(newIntent);
    // required if your activity's launch mode is "singleTop", "singleTask", or "singleInstance"
    setIntent(newIntent);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == BraintreeRequestCodes.THREE_D_SECURE) {
      threeDSecureClient.onActivityResult(resultCode, data, this::handleThreeDSecureResult);
    }
  }

  private void tokenizeCard() {
    Card card = new Card();
    card.setNumber("378282246310005");
    card.setExpirationDate("12/2022"); 

    cardClient.tokenize(this, card, (cardNonce, error) -> {
      if (cardNonce != null) {
        performThreeDSecureVerification(cardNonce);
      } else {
        // handle error
      }
    });
  }
 
  private void performThreeDSecureVerification(CardNonce cardNonce) {
    ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
    billingAddress.setGivenName("Jill");
    billingAddress.setSurname("Doe");
    billingAddress.setPhoneNumber("5551234567");
    billingAddress.setStreetAddress("555 Smith St");
    billingAddress.setExtendedAddress("#2");
    billingAddress.setLocality("Chicago");
    billingAddress.setRegion("IL");
    billingAddress.setPostalCode("12345");
    billingAddress.setCountryCodeAlpha2("US");

    ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();
    additionalInformation.accountId("account-id");

    ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
    threeDSecureRequest.setAmount("10");
    threeDSecureRequest.setEmail("test@email.com");
    threeDSecureRequest.setBillingAddress(billingAddress);
    threeDSecureRequest.setNonce(cardNonce.getNonce());
    threeDSecureRequest.setShippingMethod(ThreeDSecureShippingMethod.GROUND);
    threeDSecureRequest.setAdditionalInformation(additionalInformation);

    threeDSecureClient.performVerification(this, threeDSecureRequest, (threeDSecureResult, error) -> {
      if (threeDSecureResult != null) {
        // examine lookup response (if necessary), then continue verification
        threeDSecureClient.continuePerformVerification(ThreeDSecureActivity.this, threeDSecureRequest, threeDSecureResult, this::handleThreeDSecureResult);
      } else {
        // handle error
      }
    });
  }

  private void handleThreeDSecureResult(ThreeDSecureResult threeDSecureResult, Exception error) {
    if (threeDSecureResult != null) {
      // send this nonce to your server
      String nonce = threeDSecureResult.getTokenizedCard().getNonce();
    } else {
      // handle error
    }
  }
}
```

#### 3DS1 UI Customization

The `ThreeDSecureV1UiCustomization` class setters have been updated to remove method chaining and follow standard Java getter/setter pattern.

#### 3DS2 UI Customization

On `ThreeDSecureRequest` the `uiCustomization` property was replaced with `v2UiCustomization` of type `ThreeDSecureV2UiCustomization`.
For 3DS2 UI customization, use the following new classes:
- `ThreeDSecureV2UiCustomization`
- `ThreeDSecureV2ButtonCustomization`
- `ThreeDSecureV2LabelCustomization`
- `ThreeDSecureV2TextBoxCustomization`
- `ThreeDSecureV2ToolbarCustomization`

#### Default 3DS Version

Previously, the `versionRequested` property on `ThreeDSecureRequest` defaulted to `VERSION_1`. It now defaults to `VERSION_2`.

#### Shipping Method

The `shippingMethod` property on `ThreeDSecureRequest` is now an enum rather than a string. Possible values:
- `SAME_DAY`
- `EXPEDITED`
- `PRIORITY`
- `GROUND`
- `ELECTRONIC_DELIVERY`
- `SHIP_TO_STORE`

## Integrating Multiple Payment Methods

Several features of the SDK require handling a browser switch result or an activity result. These can be handled together using the `BraintreeRequestCodes` associated with the result.

```java
package com.my.app;

public class PaymentsActivity extends AppCompatActivity {
  
  ...

  @Override
  protected void onResume() {
    super.onResume();
    myHandleBrowserSwitchResultMethod();    
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case BraintreeRequestCodes.GOOGLE_PAY:
        googlePayClient.onActivityResult(this, resultCode, data, this::handleGooglePayResult);
        break;
      case BraintreeRequestCodes.THREE_D_SECURE:
        threeDSecureClient.onActivityResult(resultCode, data, this::handleThreeDSecureResult);
        break;
      case BraintreeRequestCodes.VENMO:
        venmoClient.onActivityResult(this, resultCode, data, this::handleVenmoResult); 
        break;
    }
  }
  
  private void myHandleBrowserSwitchResultMethod() {
    BrowserSwitchResult browserSwitchResult = braintreeClient.deliverBrowserSwitchResult(this);
    if (browserSwitchResult != null) {
      int requestCode = browserSwitchResult.getRequestCode();
      
      switch (requestCode) {
        case BraintreeRequestCodes.LOCAL_PAYMENT:
          localPaymentClient.onBrowserSwitchResult(this, browserSwitchResult, this::handleLocalPaymentResult);
          break;
        case BraintreeRequestCodes.PAYPAL:
          payPalClient.onBrowserSwitchResult(browserSwitchResult, this::handlePayPalResult);
          break;
        case BraintreeRequestCodes.THREE_D_SECURE:
          threeDSecureClient.onBrowserSwitchResult(browserSwitchResult, this::handleThreeDSecureResult); 
          break;
      }
    }
  }    
}
```
