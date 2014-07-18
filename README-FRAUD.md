# BraintreeData Fraud Tools

`BraintreeData` is an Android library for fraud detection while using the [Braintree Gateway](http://braintreepayments.com/).
It collects and provides data about the mobile device for fraud detection.

`BraintreeData` does not function alone - it requires data to be passed to the Braintree Gateway via a [server integration](https://developers.braintreepayments.com/android/start/hello-server).

## Getting Started

### Gradle

```groovy
dependencies {
  compile 'com.braintreepayments.api:data:1.+'
}
```

### Maven

```xml
<dependency>
    <groupId>com.braintreepayments.api</groupId>
    <artifactId>data</artifactId>
    <version>[1.0,)</version>
</dependency>
```

### JAR

[Download the latest JAR from Maven Central](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.braintreepayments.api&a=data&v=LATEST) and include it in your project.

## Android Setup

If you are using gradle, your manifest will automatically include the required permissions. There is no
other setup required.

If you are using maven or a JAR, ensure your `minSdk` is set to`8` or higher:

```xml
<uses-sdk android:minSdkVersion="8" />
```

and specify `INTERNET` and `ACCESS_NETWORK_STATE` permissions in `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" /> 
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Quick example

Get an instance of `BraintreeData`. The second parameter should be either `BraintreeEnvironment.SANDBOX` for testing
or `BraintreeEnvironment.PRODUCTION` for production.

```java
BraintreeData mBraintreeData = new BraintreeData(context, BraintreeEnvironment.PRODUCTION);
```

When processing a user's purchase, call `collectDeviceData()`:

```java
String deviceSessionId = mBraintreeData.collectDeviceData();
```

Send `deviceSessionId` to your server along with transaction data to be included with the request to the Braintree Gateway.

In Ruby, submitting a transaction will include:

```ruby
result = Braintree::Transaction.sale(
  :amount => "100.00",
  :credit_card => {
    :number => params["credit_card_number"],
    :expiration_date => params["credit_card_expiration_date"],
    :cvv => params["credit_card_cvv"]
  },
  :device_session_id => << contents of deviceSessionId from BraintreeData.collectDeviceData() >>
)
```

### Caveats

Keep a reference to the `BraintreeData` instance after calling `collectDeviceData()` until after
a transaction has finished. The reference should persist through device rotations. Doing so allows
us to ensure the best possible fraud detection measures.