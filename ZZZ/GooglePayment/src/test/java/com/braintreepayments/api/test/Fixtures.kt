package com.braintreepayments.api.test

object Fixtures {

    const val TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn"
    const val BASE64_CLIENT_TOKEN = "eyJ2ZXJzaW9uIjoyLCJhdXRob3JpemF0aW9uRmluZ2VycHJpbnQiOiJlbmNvZGVkX2F1dGhfZmluZ2VycHJpbnQiLCJjaGFsbGVuZ2VzIjpbImN2diIsInBvc3RhbF9jb2RlIl0sImNvbmZpZ1VybCI6ImVuY29kZWRfY2FwaV9jb25maWd1cmF0aW9uX3VybCIsImFzc2V0c1VybCI6Imh0dHA6Ly9sb2NhbGhvc3Q6OTAwMCIsInRocmVlRFNlY3VyZUVuYWJsZWQiOmZhbHNlLCJwYXlwYWxFbmFibGVkIjpmYWxzZX0="

    // language=JSON
    const val CONFIGURATION_WITH_ANDROID_PAY = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "androidPay": {
            "enabled": true,
            "displayName": "Android Pay Merchant",
            "environment": "sandbox",
            "googleAuthorizationFingerprint": "google-auth-fingerprint",
            "supportedNetworks": [
              "visa",
              "mastercard",
              "amex",
              "discover"
            ]
          }
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE = """
        {
          "apiVersion": 2,
          "apiVersionMinor": 0,
          "email": "android-user@example.com",
          "shippingAddress": {
            "name": "John Doe",
            "address1": "c/o Google LLC",
            "address2": "1600 Amphitheatre Pkwy",
            "address3": "Building 40",
            "locality": "Mountain View",
            "administrativeArea": "CA",
            "countryCode": "US",
            "postalCode": "94043",
            "sortingCode": ""
          },
          "paymentMethodData": {
            "type": "CARD",
            "description": "MasterCard 0276",
            "info": {
              "cardNetwork": "VISA",
              "cardDetails": "1234",
              "billingAddress": {
                "name": "John Doe",
                "address1": "c/o Google LLC",
                "address2": "1600 Amphitheatre Pkwy",
                "address3": "Building 40",
                "locality": "Mountain View",
                "administrativeArea": "CA",
                "countryCode": "US",
                "postalCode": "94043",
                "sortingCode": ""
              }
            },
            "tokenizationData": {
              "type": "PAYMENT_GATEWAY",
              "token": "{\"androidPayCards\":[{\"type\":\"AndroidPayCard\",\"nonce\":\"fake-android-pay-nonce\",\"description\":\"Android Pay\",\"details\":{\"cardType\":\"Visa\",\"lastTwo\":\"11\",\"lastFour\":\"1234\",\"isNetworkTokenized\":true},\"binData\":{\"prepaid\":\"Unknown\",\"healthcare\":\"Yes\",\"debit\":\"No\",\"durbinRegulated\":\"Unknown\",\"commercial\":\"Unknown\",\"payroll\":\"Unknown\",\"issuingBank\":\"Unknown\",\"countryOfIssuance\":\"Something\",\"productId\":\"123\"}}]}"
            }
          }
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_GOOGLE_PAYMENT_REQUEST = """
        {
          "apiVersion": 2,
          "apiVersionMinor": 0,
          "allowedPaymentMethods": [
            {
              "type": "CARD",
              "parameters": {
                "billingAddressRequired": true,
                "billingAddressParameters": {
                  "format": "MIN",
                  "phoneNumberRequired": true
                },
                "allowPrepaidCards": true,
                "allowedAuthMethods": [
                  "PAN_ONLY",
                  "CRYPTOGRAM_3DS"
                ],
                "allowedCardNetworks": [
                  "VISA",
                  "AMEX",
                  "JCB",
                  "DISCOVER",
                  "MASTERCARD"
                ]
              },
              "tokenizationSpecification": {
                "type": "PAYMENT_GATEWAY",
                "parameters": {
                  "gateway": "braintree",
                  "braintree:apiVersion": "v1",
                  "braintree:sdkVersion": "BETA",
                  "braintree:merchantId": "BRAINTREE_MERCHANT_ID",
                  "braintree:authorizationFingerprint": "BRAINTREE_AUTH_FINGERPRINT"
                }
              }
            },
            {
              "type": "PAYPAL",
              "parameters": {
                "purchase_context": "{\"purchase_context\":{\"purchase_units\":[{\"payee\":{\"client_id\":\"FAKE_PAYPAL_CLIENT_ID\"},\"recurring_payment\":false}]}}"
              },
              "tokenizationSpecification": {
                "type": "PAYMENT_GATEWAY",
                "parameters": {
                  "gateway": "braintree",
                  "braintree:apiVersion": "v1",
                  "braintree:sdkVersion": "BETA",
                  "braintree:merchantId": "BRAINTREE_MERCHANT_ID",
                  "braintree:authorizationFingerprint": "BRAINTREE_AUTH_FINGERPRINT"
                }
              }
            }
          ],
          "emailRequired": true,
          "shippingAddressRequired": true,
          "shippingAddressParameters": {
            "allowedCountryCodes": [
              "US",
              "CA",
              "MX",
              "GB"
            ],
            "phoneNumberRequired": true
          },
          "environment": "PRODUCTION",
          "merchantInfo": {
            "merchantId": "GOOGLE_MERCHANT_ID",
            "merchantName": "GOOGLE_MERCHANT_NAME"
          },
          "transactionInfo": {
            "totalPriceStatus": "FINAL",
            "totalPrice": "12.24",
            "currencyCode": "USD"
          }
        } 
    """

    // language=JSON
    const val PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE = """
        {
          "apiVersion": 2,
          "apiVersionMinor": 0,
          "email": "android-user@example.com",
          "shippingAddress": {
            "name": "John Doe",
            "address1": "c/o Google LLC",
            "address2": "1600 Amphitheatre Pkwy",
            "address3": "Building 40",
            "locality": "Mountain View",
            "administrativeArea": "CA",
            "countryCode": "US",
            "postalCode": "94043",
            "sortingCode": ""
          },
          "paymentMethodData": {
            "description": "PayPal: quinn@neumiiller.com",
            "type": "PAYPAL",
            "info": {
              "billingAddress": {
                "name": "John Doe",
                "address1": "c/o Google LLC",
                "address2": "1600 Amphitheatre Pkwy",
                "address3": "Building 40",
                "locality": "Mountain View",
                "administrativeArea": "CA",
                "countryCode": "US",
                "postalCode": "94043",
                "sortingCode": ""
              }
            },
            "tokenizationData": {
              "type": "PAYMENT_GATEWAY",
              "token": "{\n  \"paypalAccounts\": [\n    {\n      \"type\": \"PayPalAccount\",\n      \"nonce\": \"6f1bec22-ca87-0a24-6180-0a21c73235eb\",\n      \"description\": \"PayPal\",\n      \"consumed\": false,\n      \"details\": {\n        \"correlationId\": \"EC-HERMES-SANDBOX-EC-TOKEN\",\n        \"payerInfo\": {\n          \"email\": \"bt_buyer_us@paypal.com\",\n          \"payerId\": \"FAKE_PAYER_ID\",\n          \"firstName\": \"John\",\n          \"lastName\": \"Doe\",\n          \"phone\": \"312-123-4567\",\n          \"countryCode\": \"US\",\n          \"shippingAddress\": {\n            \"line1\": \"123 Division Street\",\n            \"line2\": \"Apt. #1\",\n            \"city\": \"Chicago\",\n            \"state\": \"IL\",\n            \"postalCode\": \"60618\",\n            \"countryCode\": \"US\",\n            \"recipientName\": \"John Doe\"\n          },\n          \"billingAddress\": {\n            \"line1\": \"123 Billing Street\",\n            \"line2\": \"Apt. #1\",\n            \"city\": \"Chicago\",\n            \"state\": \"IL\",\n            \"postalCode\": \"60618\",\n            \"countryCode\": \"US\"\n          }\n        }\n      }\n    }\n  ]\n}"
            }
          }
        }
    """

    // language=JSON
    const val RESPONSE_GOOGLE_PAYMENT_CARD = """
        {
          "apiVersionMinor": 0,
          "apiVersion": 2,
          "paymentMethodData": {
            "description": "Visa •••• 1234",
            "tokenizationData": {
              "type": "PAYMENT_GATEWAY",
              "token": "{\"androidPayCards\":[{\"type\":\"AndroidPayCard\",\"nonce\":\"d887f42c-bda5-091a-0798-af42d3ed173e\",\"description\":\"Android Pay\",\"consumed\":false,\"details\":{\"cardType\":\"Visa\",\"lastTwo\":\"34\",\"lastFour\":\"1234\"},\"binData\":{\"prepaid\":\"No\",\"healthcare\":\"No\",\"debit\":\"No\",\"durbinRegulated\":\"No\",\"commercial\":\"No\",\"payroll\":\"No\",\"issuingBank\":\"Issuing Bank USA\",\"countryOfIssuance\":\"USA\",\"productId\":\"A\"}}]}"
            },
            "type": "CARD",
            "info": {
              "cardNetwork": "VISA",
              "cardDetails": "1234"
            }
          }
        } 
    """

    // language=JSON
    const val REPSONSE_GOOGLE_PAYMENT_PAYPAL_ACCOUNT = """
        {
          "apiVersionMinor": 0,
          "apiVersion": 2,
          "paymentMethodData": {
            "description": "PayPal: quinn@neumiiller.com",
            "tokenizationData": {
              "type": "PAYMENT_GATEWAY",
              "token": "{\n  \"paypalAccounts\": [\n    {\n      \"type\": \"PayPalAccount\",\n      \"nonce\": \"6f1bec22-ca87-0a24-6180-0a21c73235eb\",\n      \"description\": \"PayPal\",\n      \"consumed\": false,\n      \"details\": {\n        \"correlationId\": \"EC-HERMES-SANDBOX-EC-TOKEN\",\n        \"payerInfo\": {\n          \"email\": \"bt_buyer_us@paypal.com\",\n          \"payerId\": \"FAKE_PAYER_ID\",\n          \"firstName\": \"John\",\n          \"lastName\": \"Doe\",\n          \"phone\": \"312-123-4567\",\n          \"countryCode\": \"US\",\n          \"shippingAddress\": {\n            \"line1\": \"123 Division Street\",\n            \"line2\": \"Apt. #1\",\n            \"city\": \"Chicago\",\n            \"state\": \"IL\",\n            \"postalCode\": \"60618\",\n            \"countryCode\": \"US\",\n            \"recipientName\": \"John Doe\"\n          },\n          \"billingAddress\": {\n            \"line1\": \"123 Billing Street\",\n            \"line2\": \"Apt. #1\",\n            \"city\": \"Chicago\",\n            \"state\": \"IL\",\n            \"postalCode\": \"60618\",\n            \"countryCode\": \"US\"\n          }\n        }\n      }\n    }\n  ]\n}"
            },
            "type": "PAYPAL"
          }
        } 
    """

    // language=JSON
    const val READY_TO_PAY_REQUEST_WITH_EXISTING_PAYMENT_METHOD = """
        {
          "apiVersion": 2,
          "apiVersionMinor": 0,
          "allowedPaymentMethods": [
            {
              "type": "CARD",
              "parameters": {
                "allowedAuthMethods": [
                  "PAN_ONLY",
                  "CRYPTOGRAM_3DS"
                ],
                "allowedCardNetworks": [
                  "AMEX",
                  "VISA"
                ]
              }
            }
          ],
          "existingPaymentMethodRequired": true
        }
    """

    // language=JSON
    const val READY_TO_PAY_REQUEST_WITHOUT_EXISTING_PAYMENT_METHOD = """
        {
          "apiVersion": 2,
          "apiVersionMinor": 0,
          "allowedPaymentMethods": [
            {
              "type": "CARD",
              "parameters": {
                "allowedAuthMethods": [
                  "PAN_ONLY",
                  "CRYPTOGRAM_3DS"
                ],
                "allowedCardNetworks": [
                  "AMEX",
                  "VISA"
                ]
              }
            }
          ]
        }
    """
}