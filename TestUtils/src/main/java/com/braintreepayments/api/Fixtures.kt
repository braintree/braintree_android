package com.braintreepayments.api

object Fixtures {

    // region 3D Secure
    // language=JSON
    const val THREE_D_SECURE_AUTHENTICATION_RESPONSE = """
        {
            "paymentMethod": {
                "type": "CreditCard",
                "nonce": "12345678-1234-1234-1234-123456789012",
                "description": "ending in ••11",
                "isDefault": false,
                "isLocked": false,
                "securityQuestions": [],
                "details": {
                    "cardType": "Visa",
                    "lastTwo": "11",
                    "lastFour": "1111"
                },
                "threeDSecureInfo": {
                    "liabilityShifted": true,
                    "liabilityShiftPossible": true
                }
            },
            "threeDSecureInfo": {
                "liabilityShifted": true,
                "liabilityShiftPossible": true
            },
            "success": true
        }
    """

    // language=JSON
    const val THREE_D_SECURE_AUTHENTICATION_RESPONSE_WITH_ERROR = """
        {
            "error": {
                "message": "Failed to authenticate, please try a different form of payment."
            },
            "fieldErrors":[],
            "threeDSecureInfo": {
                "liabilityShifted": false,
                "liabilityShiftPossible": true
            },
            "success": false
        }
    """

    // language=JSON
    const val THREE_D_SECURE_LOOKUP_RESPONSE = """
        {
          "lookup": {
            "acsUrl": "https://acs-url/",
            "md": "merchant-descriptor",
            "termUrl": "https://term-url/",
            "pareq": "sample-pareq",
            "transactionId": "sample-transaction-id"
          },
          "paymentMethod": {
            "type": "CreditCard",
            "nonce": "123456-12345-12345-a-adfa",
            "description": "ending in ••11",
            "isDefault": false,
            "isLocked": false,
            "securityQuestions": [],
            "details": {
              "cardType": "Visa",
              "lastTwo": "11",
              "lastFour": "1111"
            },
            "threeDSecureInfo": {
              "liabilityShifted": true,
              "liabilityShiftPossible": true
            }
          },
          "threeDSecureInfo": {
            "liabilityShifted": true,
            "liabilityShiftPossible": true
          }
        }
    """

    // language=JSON
    const val THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL = """
        {
          "lookup": {
        "acsUrl": null,
        "md": "merchant-descriptor",
        "termUrl": "https://term-url/",
        "pareq": "pareq",
        "threeDSecureVersion": null,
        "transactionId": null
      },
      "paymentMethod": {
        "type": "CreditCard",
        "nonce": "123456-12345-12345-a-adfa",
        "description": "ending in ••11",
        "isDefault": false,
        "isLocked": false,
        "securityQuestions": [],
        "details": {
          "cardType": "Visa",
          "lastTwo": "11",
          "lastFour": "1111"
        },
        "threeDSecureInfo": {
          "liabilityShifted": true,
          "liabilityShiftPossible": true
        }
      },
      "threeDSecureInfo": {
        "liabilityShifted": true,
        "liabilityShiftPossible": true
      }
    }
    """

    // language=JSON
    const val THREE_D_SECURE_V1_LOOKUP_RESPONSE = """
        {
          "lookup": {
            "acsUrl": "https://acs-url/",
            "md": "merchant-descriptor",
            "termUrl": "https://term-url/",
            "pareq": "pareq",
            "threeDSecureVersion": "1.0.2",
            "transactionId": "some-transaction-id"
          },
          "paymentMethod": {
            "type": "CreditCard",
            "nonce": "123456-12345-12345-a-adfa",
            "description": "ending in ••11",
            "isDefault": false,
            "isLocked": false,
            "securityQuestions": [],
            "details": {
              "cardType": "Visa",
              "lastTwo": "11",
              "lastFour": "1111"
            },
            "threeDSecureInfo": {
              "liabilityShifted": true,
              "liabilityShiftPossible": true
            }
          },
          "threeDSecureInfo": {
            "liabilityShifted": true,
            "liabilityShiftPossible": true
          }
        }
    """

    // language=JSON
    const val THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE = """
        {
          "paymentMethod": {
            "type": "CreditCard",
            "nonce": "ceff1c43-0274-010d-6b0e-e15544dd8583",
            "description": "ending in 91",
            "consumed": false,
            "threeDSecureInfo": {
              "liabilityShifted": true,
              "liabilityShiftPossible": true,
              "status": "authenticate_successful",
              "enrolled": "Y",
              "cavv": "MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
              "xid": null,
              "eci_flag": "05",
              "three_d_secure_version": "2.1.0",
              "ds_transaction_id": "e1aaed8b-4c2e-4e50-b6a9-8d48779c90b3",
              "acs_transaction_id": "e2f621db-b23a-4b0a-983c-91f72b23d16b",
              "three_d_secure_server_transaction_id": "d6909f17-6a92-4ea1-9148-5499677dee60",
              "pares_status": "Y",
              "acsTransactionId": "e2f621db-b23a-4b0a-983c-91f72b23d16b",
              "dsTransactionId": "e1aaed8b-4c2e-4e50-b6a9-8d48779c90b3",
              "eciFlag": "05",
              "paresStatus": "Y",
              "threeDSecureAuthenticationId": "3fg8syh4nsmq3nzrmv",
              "threeDSecureServerTransactionId": "d6909f17-6a92-4ea1-9148-5499677dee60",
              "threeDSecureVersion": "2.1.0",
              "lookup": {
                "transStatus": "C",
                "transStatusReason": null
              },
              "authentication": {
                "transStatus": "Y",
                "transStatusReason": null
              }
            },
            "details": {
              "bin": "400000",
              "lastTwo": "91",
              "lastFour": "1091",
              "cardType": "Visa",
              "expirationYear": "2022",
              "expirationMonth": "01"
            },
            "bin_data": {
              "prepaid": "Unknown",
              "healthcare": "Unknown",
              "debit": "Unknown",
              "durbin_regulated": "Unknown",
              "commercial": "Unknown",
              "payroll": "Unknown",
              "issuing_bank": "Unknown",
              "country_of_issuance": "Unknown",
              "product_id": "Unknown"
            }
          },
          "threeDSecureInfo": {
            "liabilityShifted": true,
            "liabilityShiftPossible": true
          }
        }
    """

    // language=JSON
    const val THREE_D_SECURE_V2_LOOKUP_RESPONSE = """
        {
          "lookup": {
            "acsUrl": "https://acs-url/",
            "md": "merchant-descriptor",
            "termUrl": "https://term-url/",
            "pareq": "pareq",
            "threeDSecureVersion": "2.1.0",
            "transactionId": "some-transaction-id"
          },
          "paymentMethod": {
            "type": "CreditCard",
            "nonce": "123456-12345-12345-a-adfa",
            "description": "ending in ••11",
            "isDefault": false,
            "isLocked": false,
            "securityQuestions": [],
            "details": {
              "cardType": "Visa",
              "lastTwo": "11",
              "lastFour": "1111"
            },
            "threeDSecureInfo": {
              "liabilityShifted": true,
              "liabilityShiftPossible": true
            }
          },
          "threeDSecureInfo": {
            "liabilityShifted": true,
            "liabilityShiftPossible": true
          }
        }
    """

    // language=JSON
    const val THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR = """
        {
          "errors": [
            {
              "attribute": "three_d_secure_token",
              "message": "Failed to authenticate, please try a different form of payment.",
              "model": "transaction",
              "type": "user",
              "code": "81571"
            }
          ],
          "threeDSecureInfo": {
            "liabilityShifted": false,
            "liabilityShiftPossible": true
          }
        }
    """

    // language=JSON
    const val THREE_D_SECURE_V2_LOOKUP_RESPONSE_WITHOUT_LIABILITY_WITH_LIABILITY_SHIFT_POSSIBLE = """
        {
          "lookup": {
            "acsUrl": "https://acs-url/",
            "md": "merchant-descriptor",
            "termUrl": "https://term-url/",
            "pareq": "pareq",
            "threeDSecureVersion": "2.1.0",
            "transactionId": "some-transaction-id"
          },
          "paymentMethod": {
            "type": "CreditCard",
            "nonce": "123456-12345-12345-a-adfa",
            "description": "ending in ••11",
            "isDefault": false,
            "isLocked": false,
            "securityQuestions": [],
            "details": {
              "cardType": "Visa",
              "lastTwo": "11",
              "lastFour": "1111"
            },
            "threeDSecureInfo": {
              "liabilityShifted": false,
              "liabilityShiftPossible": true
            }
          },
          "threeDSecureInfo": {
            "liabilityShifted": false,
            "liabilityShiftPossible": true
          }
        }
    """
    // endregion

    // region American Express
    // language=JSON
    const val AMEX_REWARDS_BALANCE_INELIGIBLE_CARD = """
        {
          "error": {
            "code": "INQ2002",
            "message": "Card is ineligible"
          }
        }
    """

    // language=JSON
    const val AMEX_REWARDS_BALANCE_INSUFFICIENT_POINTS = """
        {
          "error": {
            "code": "INQ2003",
            "message": "Insufficient points on card"
          }
        }
    """

    // language=JSON
    const val AMEX_REWARDS_BALANCE_SUCCESS = """
        {
          "conversionRate": "0.0070",
          "currencyAmount": "316795.03",
          "currencyIsoCode": "USD",
          "requestId": "715f4712-8690-49ed-8cc5-d7fb1c2d",
          "rewardsAmount": 45256433,
          "rewardsUnit": "Points"
        }
    """
    // endregion

    // region Auth Tokens
    const val BASE64_CLIENT_TOKEN = "eyJ2ZXJzaW9uIjoyLCJhdXRob3JpemF0aW9uRmluZ2VycHJpbnQiOiJlbmNvZGVkX2F1dGhfZmluZ2VycHJpbnQiLCJjaGFsbGVuZ2VzIjpbImN2diIsInBvc3RhbF9jb2RlIl0sImNvbmZpZ1VybCI6ImVuY29kZWRfY2FwaV9jb25maWd1cmF0aW9uX3VybCIsImFzc2V0c1VybCI6Imh0dHA6Ly9sb2NhbGhvc3Q6OTAwMCIsInRocmVlRFNlY3VyZUVuYWJsZWQiOmZhbHNlLCJwYXlwYWxFbmFibGVkIjpmYWxzZX0="
    const val BASE64_PAYPAL_UAT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2FwaS5zYW5kYm94LnBheXBhbC5jb20iLCJzdWIiOiJQYXlQYWw6TUpGUDM5VjRNUVJBRSIsImFjciI6WyJjbGllbnQiXSwib3B0aW9ucyI6e30sImF6Ijoic2Iuc2xjIiwic2NvcGVzIjpbIkJyYWludHJlZTpWYXVsdCJdLCJleHAiOjE1ODY4MjU1NTMsImV4dGVybmFsX2lkIjpbIlBheVBhbDpNSkZQMzlWNE1RUkFFIiwiQnJhaW50cmVlOmNmeHMzZ2h6d2ZrMnJocW0iXSwianRpIjoiVTJBQUV5RTdnZXVYSmIweHZQNWV0UjBDTy1ld2tmcFVxZFJCX0thZnNhbkVEWjZJZDZUMVJUOUhhbUNNdGdSQWZ5bzBZbHdJT0xWYnZMbGRzNFg5cEwwTkNZaTlmUWlnY3Y5cUxnZjg1WHd3ZUJ0QW5OOHNxcUdaMEpEMlFlLXcifQ.iV5DXCg2E2ThH9Q8aYDBGaW19OOD8cAb3D6SnJAnM30"

    const val TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn"
    const val PROD_TOKENIZATION_KEY = "production_t2wns2y2_dfy45jdj3dxkmz5m"

    // language=JSON
    const val CLIENT_TOKEN = """
        {
            "configUrl": "client_api_configuration_url",
            "authorizationFingerprint": "authorization_fingerprint",
            "clientApiUrl": "client_api_url",
            "challenges": ["cvv"],
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "paypalEnabled": false,
            "threeDSecureEnabled": true
        }
    """

    // language=JSON
    const val CLIENT_TOKEN_WITH_AUTHORIZATION_FINGERPRINT_OPTIONS = """
        {
          "version": 2,
          "authorizationFingerprint": "37eae52b03b54963769b412b97ee6e4783622c3b9090621e1469da1ff20907c6|created_at=2016-09-13T14:20:37.116898032+0000&merchant_id=dcpspy2brwdjr3qn&public_key=9wwrzqk3vr3t4nc8",
          "configUrl": "https://api.sandbox.braintreegateway.com:443/merchants/dcpspy2brwdjr3qn/client_api/v1/configuration",
          "environment": "sandbox",
          "clientApiUrl": "https://api.sandbox.braintreegateway.com:443/merchants/dcpspy2brwdjr3qn/client_api"
        }
    """

    // language=JSON
    const val CLIENT_TOKEN_WITH_CUSTOMER_ID_IN_AUTHORIZATION_FINGERPRINT = """
        {
          "version": 2,
          "authorizationFingerprint": "37eae52b03b54963769b412b97ee6e4783622c3b9090621e1469da1ff20907c6|created_at=2016-09-13T14:20:37.116898032+0000&customer_id=fake-customer-123&merchant_id=dcpspy2brwdjr3qn&public_key=9wwrzqk3vr3t4nc8",
          "configUrl": "https://api.sandbox.braintreegateway.com:443/merchants/dcpspy2brwdjr3qn/client_api/v1/configuration",
          "environment": "sandbox",
          "clientApiUrl": "https://api.sandbox.braintreegateway.com:443/merchants/dcpspy2brwdjr3qn/client_api"
        }
    """
    // endregion

    // region Card
    // language=JSON
    const val BIN_DATA = """
        {
          "prepaid": "Unknown",
          "healthcare": "Yes",
          "debit": "No",
          "durbinRegulated": "Unknown",
          "commercial": "Unknown",
          "payroll": "Unknown",
          "issuingBank": "Unknown",
          "countryOfIssuance": "Something",
          "productId": "123"
        }
    """
    // endregion

    // region Configuration
    // language=JSON
    const val CONFIGURATION_WITH_ACCESS_TOKEN = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "braintreeApi": {
            "accessToken": "access-token-example",
            "url": "https://braintree-api.com"
          }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_ANALYTICS = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "analytics": {
                "url": "analytics_url"
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_GOOGLE_PAY = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "androidPay": {
            "enabled": true,
            "displayName": "Google Pay Merchant",
            "environment": "sandbox",
            "googleAuthorizationFingerprint": "google-auth-fingerprint",
            "paypalClientId": "pay-pal-client-id",
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
    const val CONFIGURATION_WITH_GOOGLE_PAY_PRODUCTION = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "production",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "androidPay": {
            "enabled": true,
            "displayName": "Google Pay Merchant",
            "environment": "production",
            "googleAuthorizationFingerprint": "google-auth-fingerprint",
            "paypalClientId": "pay-pal-client-id",
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
    const val CONFIGURATION_WITH_ASSETS_URL = """
        {
            "assetsUrl": "https://assets.braintreegateway.com",
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_CARD_COLLECT_DEVICE_DATA = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "creditCards": {
            "collectDeviceData": true,
            "supportedCardTypes": [
              "American Express",
              "Discover",
              "JCB",
              "MasterCard",
              "Visa"
            ]
          }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_CARDINAL_AUTHENTICATION_JWT = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "cardinalAuthenticationJWT": "cardinal_authentication_jwt"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_CLIENT_API_URL = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_CUSTOM_PAYPAL = """
        {
          "clientApiUrl": "client-api-url",
          "environment": "test",
          "paypalEnabled": true,
          "paypal": {
            "displayName": "paypal_merchant",
            "clientId": "paypal_client_id",
            "privacyUrl": "http://www.example.com/privacy",
            "userAgreementUrl": "http://www.example.com/user_agreement",
            "baseUrl": "http://localhost:9000",
            "directBaseUrl": "https://braintree.paypal.com",
            "environment": "custom"
          },
          "merchantId": "merchantId"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_CVV_CHALLENGE = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "challenges": ["cvv"]
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_DISABLED_PAYPAL = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "paypalEnabled": false
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_EMPTY_ANALYTICS_URL = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "analytics": {
                "url": ""
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_ENVIRONMENT = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_GRAPHQL = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "graphQL": {
            "url": "https://example-graphql.com/graphql",
            "features": ["tokenize_credit_cards"]
          }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_GRAPHQL_WITHOUT_FEATURES = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "graphQL": {
            "url": "https://example-graphql.com/graphql"
          }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_KOUNT = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "kount": {
                "kountMerchantId": "600000"
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_LIVE_PAYPAL = """
        {
          "clientApiUrl": "client-api-url",
          "environment": "test",
          "paypalEnabled": true,
          "paypal": {
            "displayName": "paypal_merchant",
            "clientId": "paypal_client_id",
            "privacyUrl": "http://www.example.com/privacy",
            "userAgreementUrl": "http://www.example.com/user_agreement",
            "baseUrl": "http://localhost:9000",
            "directBaseUrl": "https://www.paypal.com",
            "environment": "live",
            "touchDisabled": true,
            "currencyIsoCode": "USD"
          },
          "merchantId": "merchant-id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_LIVE_PAYPAL_INR = """
        {
          "clientApiUrl": "client-api-url",
          "environment": "test",
          "paypalEnabled": true,
          "paypal": {
            "displayName": "paypal_merchant",
            "clientId": "paypal_client_id",
            "privacyUrl": "http://www.example.com/privacy",
            "userAgreementUrl": "http://www.example.com/user_agreement",
            "baseUrl": "http://localhost:9000",
            "directBaseUrl": "https://www.paypal.com",
            "environment": "live",
            "currencyIsoCode": "INR"
          },
          "merchantId": "merchant-id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_LIVE_PAYPAL_NO_CLIENT_ID = """
        {
          "clientApiUrl": "client-api-url",
          "environment": "test",
          "paypalEnabled": true,
          "paypal": {
            "displayName": "paypal_merchant",
            "clientId": null,
            "privacyUrl": "http://www.example.com/privacy",
            "userAgreementUrl": "http://www.example.com/user_agreement",
            "baseUrl": "http://localhost:9000",
            "directBaseUrl": "https://www.paypal.com",
            "environment": "live"
          },
          "merchantId": "merchant-id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_MERCHANT_ACCOUNT_ID = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_MERCHANT_ID = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_MULTIPLE_CHALLENGES = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "challenges": ["cvv","postal_code"]
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_OFFLINE_PAYPAL = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "paypalEnabled": true,
            "paypal": {
                "displayName": "paypal_merchant",
                "clientId": "paypal_client_id",
                "privacyUrl": "http://www.example.com/privacy",
                "userAgreementUrl": "http://www.example.com/user_agreement",
                "baseUrl": "http://localhost:9000",
                "directBaseUrl": "http://localhost:9000",
                "environment": "offline",
                "touchDisabled": true
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_OFFLINE_PAYPAL_NO_CLIENT_ID = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "paypalEnabled": true,
            "paypal": {
                "displayName": "paypal_merchant",
                "clientId": null,
                "privacyUrl": "http://www.example.com/privacy",
                "userAgreementUrl": "http://www.example.com/user_agreement",
                "baseUrl": "http://localhost:9000",
                "directBaseUrl": "http://localhost:9000",
                "environment": "offline",
                "touchDisabled": true
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_PAY_WITH_VENMO = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "payWithVenmo": {
            "accessToken": "access-token",
            "environment": "environment",
            "merchantId": "merchant-id"
          }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_PAYPAL_TOUCH_DISABLED = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "paypalEnabled": true,
            "paypal": {
                "touchDisabled": true
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_PROD_ANALYTICS = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "production",
            "merchantId": "some-merchant-id",
            "merchantAccountId": "some-merchant-account-id",
            "analytics": {
                "url": "https://client-analytics.braintreegateway.com/some-merchant-id"
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_SAMSUNGPAY = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "samsungPay" : {
            "displayName": "some example merchant",
            "serviceId": "some-service-id",
            "supportedCardBrands": [
              "american_express",
              "diners",
              "discover",
              "jcb",
              "maestro",
              "mastercard",
              "visa"
            ],
            "samsungAuthorization": "example-samsung-authorization",
            "environment": "SANDBOX"
          }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_SANDBOX_ANALYTICS = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "some-merchant-id",
            "merchantAccountId": "some-merchant-account-id",
            "analytics": {
                "url": "https://origin-analytics-sand.sandbox.braintree-api.com/some-merchant-id"
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_SUPPORTED_CARD_TYPES = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "creditCards": {
            "supportedCardTypes": [
              "American Express",
              "Discover",
              "JCB",
              "MasterCard",
              "Visa"
            ]
          }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_THREE_D_SECURE = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "threeDSecureEnabled": true
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_UNIONPAY = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "unionPay": {
                "enabled": true,
                "merchantAccountId": "merchant_account_id"
            }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITH_VISA_CHECKOUT = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "creditCards": {
            "supportedCardTypes": [
              "American Express",
              "Discover",
              "JCB",
              "MasterCard",
              "Visa"
            ]
          },
          "visaCheckout": {
            "apikey": "gwApikey",
            "externalClientId": "gwExternalClientId",
            "supportedCardTypes": [
              "American Express",
              "Discover",
              "MasterCard",
              "Visa"
            ]
          }
        }
    """

    // language=JSON
    const val CONFIGURATION_WITHOUT_ACCESS_TOKEN = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITHOUT_ANALYTICS = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id",
            "challenges": ["cvv"],
            "paypalEnabled": false
        }
    """

    // language=JSON
    const val CONFIGURATION_WITHOUT_GOOGLE_PAY = """
        {
            "clientApiUrl": "client_api_url",
            "environment": "test",
            "merchantId": "integration_merchant_id",
            "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITHOUT_CHALLENGE = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITHOUT_CLIENT_API_URL = """
        {}
    """

    // language=JSON
    const val CONFIGURATION_WITHOUT_ENVIRONMENT = """
        {
          "clientApiUrl": "client_api_url",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITHOUT_MERCHANT_ID = """
        {
            "clientApiUrl": "client_api_url"
        }
    """

    // language=JSON
    const val CONFIGURATION_WITHOUT_THREE_D_SECURE = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id"
        }
    """
    // endregion

    // region Errors
    // language=JSON
    const val ERRORS_AUTH_FINGERPRINT_ERROR = """
        {
            "error": {
                "message": "Authorization fingerprint is invalid"
            },
            "fieldErrors": [
                {
                    "field": "authorizationFingerprint",
                    "message": "Authorization fingerprint signature did not match"
                }
            ]
        }
    """

    // language=JSON
    const val ERRORS_BRAINTREE_API_ERROR_RESPONSE = """
        {
          "meta": {
            "braintree_request_id": "fe62f36c-7616-4130-a83f-20dc341d5c79"
          },
          "error": {
            "user_message": "Invalid data detected. Please check your entries and try again.",
            "developer_message": "The provided parameters are invalid; see details for field-specific error messages.",
            "details": [
              {
                "code": "not_an_integer",
                "user_message": "must be a number",
                "developer_message": "The provided value must be a string encoding of a base-10 integer between 1 and 12.",
                "in": "body",
                "at": "/expiration_month"
              }
            ]
          }
        }
    """

    // language=JSON
    const val ERRORS_CREDIT_CARD_ERROR_RESPONSE = """
        {
            "error": {
                "message": "Credit card is invalid"
            },
            "fieldErrors": [
                {
                    "field": "creditCard",
                    "fieldErrors": [
                        {
                            "field": "expirationYear",
                            "message": "Expiration year is invalid"
                        },
                        {
                            "field": "number",
                            "message": "Credit card number is required"
                        },
                        {
                            "field": "base",
                            "message": "Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code"
                        }
                    ]
                }
            ]
        }
    """

    // language=JSON
    const val ERRORS_COMPLEX_ERROR_RESPONSE = """
        {
            "error": {
                "message": "Credit card is invalid"
            },
            "fieldErrors": [
                {
                    "field": "creditCard",
                    "fieldErrors": [
                        {
                            "field": "expirationYear",
                            "message": "Expiration year is invalid"
                        },
                        {
                            "field": "number",
                            "message": "Credit card number is required"
                        },
                        {
                            "field": "base",
                            "message": "Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code"
                        }
                    ]
                },
                {
                    "field": "customer",
                    "message": "is invalid"
                }
            ]
        }
    """

    // language=JSON
    const val ERRORS_GRAPHQL_CREDIT_CARD_ERROR = """
        {
          "data": {
            "tokenizeCreditCard": null
          },
          "errors": [
            {
              "message": "Expiration month is invalid",
              "path": [
                "tokenizeCreditCard"
              ],
              "locations": [
                {
                  "line": 1,
                  "column": 66
                }
              ],
              "extensions": {
                "errorType": "user_error",
                "legacyCode": "81712",
                "inputPath": [
                  "input",
                  "creditCard",
                  "expirationMonth"
                ]
              }
            },
            {
              "message": "Expiration year is invalid",
              "path": [
                "tokenizeCreditCard"
              ],
              "locations": [
                {
                  "line": 1,
                  "column": 66
                }
              ],
              "extensions": {
                "errorType": "user_error",
                "legacyCode": "81713",
                "inputPath": [
                  "input",
                  "creditCard",
                  "expirationYear"
                ]
              }
            },
            {
              "message": "CVV verification failed",
              "path": [
                "tokenizeCreditCard"
              ],
              "locations": [
                {
                  "line": 1,
                  "column": 66
                }
              ],
              "extensions": {
                "errorType": "user_error",
                "legacyCode": "81736",
                "inputPath": [
                  "input",
                  "creditCard",
                  "cvv"
                ]
              }
            }
          ],
          "extensions": {
            "requestId": "de1f7c67-4861-455f-89bb-1d208915f270"
          }
        }
    """

    // language=JSON
    const val ERRORS_GRAPHQL_COERCION_ERROR = """
        {
          "data" : null,
          "errors" : [{
            "message" : "Variable 'input' has coerced Null value for NonNull type 'String!'",
            "locations" : [{
              "line" : 1,
              "column" : 29
            }]
          }],
          "extensions" : {
            "requestId" : "42e50df8-c29d-4430-9d3f-00379b147543"
          }
        }
    """

    // language=JSON
    const val ERRORS_GRAPHQL_UNKNOWN_ERROR = """
        {
          "data" : null,
          "errors" : [{
            "locations" : [{
              "line" : 1,
              "column" : 1
            }]
          }],
          "extensions" : {
            "requestId" : "42e50df8-c29d-4430-9d3f-00379b147543"
          }
        }
    """

    // language=JSON
    const val ERRORS_GRAPHQL_VALIDATION_NOT_ALLOWED_ERROR = """
        {
          "data": {
            "tokenizeCreditCard": null
          },
          "errors": [
            {
              "message": "Validation is not supported for requests authorized with a tokenization key.",
              "locations": [
                {
                  "line": 2,
                  "column": 9
                }
              ],
              "path": [
                "tokenizeCreditCard"
              ],
              "extensions": {
                "errorType": "developer_error",
                "legacyCode": "50000",
                "inputPath": [
                  "input",
                  "options",
                  "validate"
                ]
              }
            }
          ],
          "extensions": {
            "requestId": "07d4e050-0d8b-4118-a2cb-1539ce0a1777"
          }
        }
    """

    // language=JSON
    const val ERROR_RESPONSE = """
        {
            "error": {
                "message": "There was an error"
            }
        }
    """

    // language=JSON
    const val RANDOM_JSON = """
        {
            "tweets": [
                {
                    "user": "braintree"
                },
                {
                    "user": "venmo"
                }
            ]
        }
    """
    // endregion

    // region GraphQL
    // language=JSON
    const val GRAPHQL_RESPONSE_CREDIT_CARD = """
        {
          "data": {
            "tokenizeCreditCard": {
              "token": "3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1",
              "creditCard": {
                "brand": "Visa",
                "expirationMonth": "01",
                "expirationYear": "2020",
                "cardholderName": "Joe Smith",
                "last4": "1111",
                "binData": {
                  "prepaid": "Yes",
                  "healthcare": "Yes",
                  "debit": "No",
                  "durbinRegulated": "Yes",
                  "commercial": "No",
                  "payroll": "Yes",
                  "issuingBank": "Bank of America",
                  "countryOfIssuance": "USA",
                  "productId": "123"
                }
              },
              "authenticationInsight" : {
                "customerAuthenticationRegulationEnvironment" : "UNREGULATED"
              }
            }
          },
          "extensions" : {
            "requestId" : "fef505c8-5930-4e30-b74a-c41fc7807fb8"
          }
        }
    """

    // language=JSON
    const val GRAPHQL_RESPONSE_CREDIT_CARD_MISSING_VALUES = """
        {
          "data": {
            "tokenizeCreditCard": {
              "token": "3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1",
              "creditCard": {
              }
            }
          },
          "extensions" : {
            "requestId" : "fef505c8-5930-4e30-b74a-c41fc7807fb8"
          }
        }
    """

    // language=JSON
    const val GRAPHQL_RESPONSE_UNKNOWN_CREDIT_CARD = """
        {
          "data" : {
            "tokenizeCreditCard" : {
              "token" : "tokencc_3bbd22_fpjshh_bqbvh5_mkf3nf_smz",
              "creditCard" : {
                "brand" : "Unknown",
                "last4" : "",
                "binData" : {
                  "prepaid" : "Unknown",
                  "healthcare" : "Unknown",
                  "debit" : "Unknown",
                  "durbinRegulated" : "Unknown",
                  "commercial" : "Unknown",
                  "payroll" : "Unknown",
                  "issuingBank" : null,
                  "countryOfIssuance" : null,
                  "productId" : null
                }
              }
            }
          },
          "extensions" : {
            "requestId" : "91cc8d47-d426-4571-9228-8414679d42c0"
          }
        }
    """
    // endregion

    // language=JSON
    const val PAYMENT_METHOD_CARD = """
       {
         "type": "CreditCard",
         "nonce": "123456-12345-12345-a-adfa",
         "description": "ending in ••11",
         "default": true,
         "isLocked": false,
         "securityQuestions": [],
         "details":
         {
           "cardType": "Visa",
           "lastTwo": "11",
           "lastFour": "1111"
         }
       }
    """

    // region Payment Methods
    // language=JSON
    const val PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE = """
        {
          "paymentMethods": [
            {
              "type": "CreditCard",
              "nonce": "123456-12345-12345-a-adfa",
              "description": "ending in ••11",
              "default": true,
              "isLocked": false,
              "securityQuestions": [],
              "details":
              {
                "cardType": "Visa",
                "lastTwo": "11",
                "lastFour": "1111"
              }
            },
            {
              "type": "PayPalAccount",
              "nonce": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
              "description": "with email paypalaccount@example.com",
              "default": false,
              "isLocked": false,
              "securityQuestions": [],
              "details": {
                "email": "paypalaccount@example.com",
                "payerInfo": {
                  "accountAddress": {
                    "street1": "123 Fake St.",
                    "street2": "Apt. 3",
                    "city": "Oakland",
                    "state": "CA",
                    "postalCode": "94602",
                    "country": "US"
                  }
                }
              }
            },
            {
              "type": "AndroidPayCard",
              "nonce": "fake-google-pay-nonce",
              "description": "Google Pay",
              "details": {
                "cardType": "Visa",
                "lastTwo": "11",
                "lastFour": "1111"
              }
            },
            {
              "type": "VenmoAccount",
              "nonce": "fake-venmo-nonce",
              "description": "VenmoAccount",
              "details": {
                "cardType": "Visa",
                "username": "happy-venmo-joe"
              }
            }
          ]
        }
    """

    // language=JSON
    const val TOKENIZE_CARD_SUCCESS_RESPONSE = """
        {
          "creditCards": [
            {
              "type": "CreditCard",
              "nonce": "3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1",
              "description": "ending in ••11",
              "consumed": false,
              "threeDSecureInfo": null,
              "details": {
                "bin": "411111",
                "lastTwo": "11",
                "lastFour": "1111",
                "cardType": "Visa",
                "cardholderName": null,
                "expirationYear": "2022",
                "expirationMonth": "02"
              },
              "binData": {
                "prepaid": "Unknown",
                "healthcare": "Unknown",
                "debit": "Unknown",
                "durbinRegulated": "Unknown",
                "commercial": "Unknown",
                "payroll": "Unknown",
                "issuingBank": "Unknown",
                "countryOfIssuance": "Unknown",
                "productId": "Unknown"
              }
            }
          ]
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_GET_PAYMENT_METHODS_GOOGLE_PAY_RESPONSE = """
        {
          "paymentMethods": [
            {
              "type": "AndroidPayCard",
              "nonce": "fake-google-pay-nonce",
              "description": "Google Pay",
              "details": {
                "cardType": "Visa",
                "lastTwo": "11"
              }
            }
          ]
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_GET_PAYMENT_METHODS_EMPTY_RESPONSE = """
        {
          "paymentMethods": []
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_HERMES_PAYMENT_RESOURCE_RESPONSE_WITH_AUTHENTICATE_URL = """
        {
          "paymentResource": {
            "paymentToken": "fake-token",
            "intent": "authorize",
            "redirectUrl": "fake-redirect-url",
            "authenticateUrl": "fake-authenticate-url"
          }
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_HERMES_PAYMENT_RESOURCE_RESPONSE_WITHOUT_AUTHENTICATE_URL = """
        {
          "paymentResource": {
            "paymentToken": "fake-token",
            "intent": "authorize",
            "redirectUrl": "fake-redirect-url",
            "authenticateUrl": null
          }
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE = """
        {
          "paypalAccounts": [
            {
              "type": "PayPalAccount",
              "nonce": "e11c9c39-d6a4-0305-791d-bfe680ef2d5d",
              "description": "PayPal",
              "consumed": false,
              "details": {
                "correlationId": "084afbf1db15445587d30bc120a23b09",
                "payerInfo": {
                  "email": "jon@getbraintree.com",
                  "firstName": "Jon",
                  "lastName": "Doe",
                  "payerId": "9KQSUZTL7YZQ4",
                  "shippingAddress": {
                    "recipientName": "Jon Doe",
                    "line1": "836486 of 22321 Park Lake",
                    "line2": "Apt B",
                    "city": "Den Haag",
                    "state": "CA",
                    "postalCode": "2585 GJ",
                    "countryCode": "NL"
                  },
                  "countryCode": "NL"
                }
              }
            }
          ]
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_LOCAL_PAYMENT_CREATE_RESPONSE = """
        {
          "paymentResource":{
            "paymentToken":"local-payment-id-123",
            "intent":"sale",
            "redirectUrl":"https://checkout.paypal.com/latinum?token=payment-token"
          }
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_PAYPAL_ACCOUNT = """
        {
          "type": "PayPalAccount",
          "nonce": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
          "description": "with email paypalaccount@example.com",
          "default": false,
          "isLocked": false,
          "securityQuestions": [],
          "details": {
            "email": "paypalaccount@example.com",
            "payerInfo": {
              "accountAddress": {
                "street1": "123 Fake St.",
                "street2": "Apt. 3",
                "city": "Oakland",
                "state": "CA",
                "postalCode": "94602",
                "country": "US"
              }
            }
          }
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE = """
        {
          "paypalAccounts": [
            {
              "authenticateUrl": "fake-authenticate-url",
              "type": "PayPalAccount",
              "nonce": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
              "description": "with email paypalaccount@example.com",
              "default": false,
              "isLocked": false,
              "securityQuestions": [],
              "details": {
                "email": "paypalaccount@example.com",
                "payerInfo": {
                  "accountAddress": {
                    "street1": "123 Fake St.",
                    "street2": "Apt. 3",
                    "city": "Oakland",
                    "state": "CA",
                    "postalCode": "94602",
                    "country": "US"
                  }
                },
                "creditFinancingOffered": {
                  "cardAmountImmutable": false,
                  "monthlyPayment": {
                    "currency": "USD",
                    "value": "13.88"
                  },
                  "payerAcceptance": true,
                  "term": 18,
                  "totalCost": {
                    "currency": "USD",
                    "value": "250.00"
                  },
                  "totalInterest": {
                    "currency": "USD",
                    "value": "0.00"
                  }
                }
              }
            }
          ]
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE_WITHOUT_ADDRESSES = """
        {
          "paypalAccounts": [
            {
              "type": "PayPalAccount",
              "nonce": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
              "description": "with email paypalaccount@example.com",
              "default": false,
              "isLocked": false,
              "securityQuestions": [],
              "details": {
                "email": "paypalaccount@example.com",
                "payerInfo": {
                }
              }
            }
          ]
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_PAYPAL_ADDRESS = """
        {
          "street1":"123 Fake St.",
          "street2": "Apt. 3",
          "city": "Oakland",
          "state": "CA",
          "postalCode": "94602",
          "country":"US",
          "recipientName": "John Fakerson"
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_PAYPAL_ADDRESS_ALTERNATE = """
        {
            "line1": "123 Fake St.",
            "line2": "Apt. 3",
            "city": "Oakland",
            "state": "CA",
            "postalCode": "94602",
            "countryCode": "US",
            "recipientName": "John Fakerson"
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD = """
        {
          "creditCards": [
            {
              "type": "CreditCard",
              "nonce": "123456-12345-12345-a-adfa",
              "description": "ending in ••11",
              "default": false,
              "isLocked": false,
              "securityQuestions": [],
              "details":
              {
                "cardType": "Visa",
                "lastTwo": "11",
                "lastFour": "1111",
                "expirationMonth": "01",
                "expirationYear": "2020",
                "cardholderName": "Joe Smith"
              },
              "binData": {
                "prepaid": "Unknown",
                "healthcare": "Yes",
                "debit": "No",
                "durbinRegulated": "Unknown",
                "commercial": "Unknown",
                "payroll": "Unknown",
                "issuingBank": "Unknown",
                "countryOfIssuance": "Something",
                "productId": "123"
              },
              "threeDSecureInfo": {
                "cavv": "fake-cavv",
                "dsTransactionId": "fake-txn-id",
                "eciFlag": "07",
                "enrolled": "Y",
                "liabilityShiftPossible": true,
                "liabilityShifted": false,
                "status": "lookup_enrolled",
                "threeDSecureVersion": "2.2.0",
                "xid": "fake-xid",
                "acsTransactionId": "fake-acs-transaction-id",
                "threeDSecureAuthenticationId": "fake-threedsecure-authentication-id",
                "threeDSecureServerTransactionId": "fake-threedsecure-server-transaction-id",
                "paresStatus": "fake-pares-status",
                "authentication":
                {
                  "transStatus": "Y",
                  "transStatusReason": "01"
                },
                "lookup":
                {
                  "transStatus": "N",
                  "transStatusReason": "02"
                }
              },
              "authenticationInsight":  {
                "regulationEnvironment": "UNREGULATED"
              }
            }
          ]
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE = """
        {
          "venmoAccounts": [{
            "type": "VenmoAccount",
            "nonce": "fake-venmo-nonce",
            "description": "VenmoAccount",
            "consumed": false,
            "default": true,
            "details": {
              "cardType": "Discover",
              "username": "venmojoe"
            }
          }]
        }
    """

    // language=JSON
    const val PAYMENT_METHOD_VENMO_PLAIN_OBJECT = """
        {
          "type": "VenmoAccount",
          "nonce": "fake-venmo-nonce",
          "description": "VenmoAccount",
          "details": {
            "cardType": "Visa",
            "username": "happy-venmo-joe"
          }
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE = """
        {
          "visaCheckoutCards":[{
            "type": "VisaCheckoutCard",
            "nonce": "123456-12345-12345-a-adfa",
            "description": "ending in ••11",
            "default": false,
            "details":
            {
              "cardType": "Visa",
              "lastTwo": "11"
            },
            "billingAddress": {
              "firstName": "billingFirstName",
              "lastName": "billingLastName",
              "streetAddress": "billingStreetAddress",
              "extendedAddress": "billingExtendedAddress",
              "locality": "billingLocality",
              "region": "billingRegion",
              "postalCode": "billingPostalCode",
              "countryCode": "billingCountryCode",
              "phoneNumber": "phoneNumber"
            },
            "shippingAddress": {
              "firstName": "shippingFirstName",
              "lastName": "shippingLastName",
              "streetAddress": "shippingStreetAddress",
              "extendedAddress": "shippingExtendedAddress",
              "locality": "shippingLocality",
              "region": "shippingRegion",
              "postalCode": "shippingPostalCode",
              "countryCode": "shippingCountryCode",
              "phoneNumber": "phoneNumber"
            },
            "userData": {
              "userFirstName": "userFirstName",
              "userLastName": "userLastName",
              "userFullName": "userFullName",
              "userName": "userUserName",
              "userEmail": "userEmail"
            },
            "callId": "callId",
            "binData": {
              "prepaid": "Unknown",
              "healthcare": "Yes",
              "debit": "No",
              "durbinRegulated": "Unknown",
              "commercial": "Unknown",
              "payroll": "Unknown",
              "issuingBank": "Unknown",
              "countryOfIssuance": "Something",
              "productId": "123"
            }
          }]
        }
    """
    // endregion

    // region PayPal
    // language=JSON
    const val PAYPAL_HERMES_BILLING_AGREEMENT_RESPONSE = """
        {
          "paymentResource":{
            "paymentToken":"token",
            "intent":"authorize",
            "redirectUrl":"https://checkout.paypal.com/one-touch-login-sandbox/index.html?action=create_payment_resource\u0026authorization_fingerprint=63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06%7Ccreated_at%3D2015-10-13T18%3A49%3A48.371382792%2B0000%26merchant_id%3Ddcpspy2brwdjr3qn%26public_key%3D9wwrzqk3vr3t4nc8\u0026cancel_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fcancel\u0026controller=client_api%2Fpaypal_hermes\u0026experience_profile%5Baddress_override%5D=false\u0026experience_profile%5Bno_shipping%5D=false\u0026merchant_id=dcpspy2brwdjr3qn\u0026return_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fsuccess\u0026ba_token=EC-HERMES-SANDBOX-EC-TOKEN\u0026offer_paypal_credit=true\u0026version=1"
          }
        }
    """

    // language=JSON
    const val PAYPAL_HERMES_BILLING_AGREEMENT_RESPONSE_SIMPLE = """
        {
          "paymentResource":{
            "paymentToken":"token",
            "intent":"authorize",
            "redirectUrl":"https://example.com/path"
          }
        }
    """

    // language=JSON
    const val PAYPAL_HERMES_RESPONSE = """
        {
          "paymentResource":{
            "paymentToken":"token",
            "intent":"authorize",
            "redirectUrl":"https://checkout.paypal.com/one-touch-login-sandbox/index.html?action=create_payment_resource\u0026amount=1.00\u0026authorization_fingerprint=63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06%7Ccreated_at%3D2015-10-13T18%3A49%3A48.371382792%2B0000%26merchant_id%3Ddcpspy2brwdjr3qn%26public_key%3D9wwrzqk3vr3t4nc8\u0026cancel_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fcancel\u0026controller=client_api%2Fpaypal_hermes\u0026currency_iso_code=USD\u0026experience_profile%5Baddress_override%5D=false\u0026experience_profile%5Bno_shipping%5D=false\u0026merchant_id=dcpspy2brwdjr3qn\u0026return_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fsuccess\u0026token=EC-HERMES-SANDBOX-EC-TOKEN\u0026offer_paypal_credit=true\u0026version=1"
          }
        }
    """

    // language=JSON
    const val PAYPAL_OTC_RESPONSE = """
        {
          "client": {
            "environment": "OneTouchCore-Android",
            "paypal_sdk_version": "1.0.4",
            "platform": "Android",
            "product_name": "OneTouchCore-Android"
          },
          "response": {
            "code": "test_auth_code"
          },
          "response_type": "authorization_code",
          "user": {
            "display_string": "some_email"
          }
        }
    """
    // endregion

    // region UnionPay
    // language=JSON
    const val UNIONPAY_CAPABILITIES_SUCCESS_RESPONSE = """
        {
          "isUnionPay": true,
          "isDebit": false,
          "unionPay": {
            "supportsTwoStepAuthAndCapture": true,
            "isSupported": true
          }
        }
    """
    // endregion

    // region PayPal
    // language=JSON
    const val SANDBOX_CONFIGURATION_WITH_GRAPHQL = """
        {
          "clientApiUrl": "https://api.sandbox.braintreegateway.com:443/merchants/dcpspy2brwdjr3qn/client_api",
          "environment": "sandbox",
          "merchantId": "dcpspy2brwdjr3qn",
          "graphQL": {
            "url": "https://payments.sandbox.braintree-api.com/graphql",
            "date": "2018-05-08",
            "features": ["tokenize_credit_cards"]
          }
        }
    """

    // language=JSON
    const val SANDBOX_CONFIGURATION_WITHOUT_GRAPHQL = """
        {
          "clientApiUrl": "https://api.sandbox.braintreegateway.com:443/merchants/dcpspy2brwdjr3qn/client_api",
          "environment": "sandbox",
          "merchantId": "dcpspy2brwdjr3qn"
        }
    """
    // endregion

    // region Google Pay
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
              "token": "{\"androidPayCards\":[{\"type\":\"AndroidPayCard\",\"nonce\":\"fake-google-pay-nonce\",\"description\":\"Google Pay\",\"details\":{\"cardType\":\"Visa\",\"lastTwo\":\"11\",\"lastFour\":\"1234\",\"isNetworkTokenized\":true},\"binData\":{\"prepaid\":\"Unknown\",\"healthcare\":\"Yes\",\"debit\":\"No\",\"durbinRegulated\":\"Unknown\",\"commercial\":\"Unknown\",\"payroll\":\"Unknown\",\"issuingBank\":\"Unknown\",\"countryOfIssuance\":\"Something\",\"productId\":\"123\"}}]}"
            }
          }
        }
    """

    // language=JSON
    const val PAYMENT_METHODS_GOOGLE_PAY_REQUEST = """
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
            "currencyCode": "USD",
            "countryCode": "US"
          }
        } 
    """


    // language=JSON
    const val RESPONSE_GOOGLE_PAY_CARD = """
        {
          "apiVersionMinor": 0,
          "apiVersion": 2,
          "paymentMethodData": {
            "description": "Visa •••• 1234",
            "tokenizationData": {
              "type": "PAYMENT_GATEWAY",
              "token": "{\"androidPayCards\":[{\"type\":\"AndroidPayCard\",\"nonce\":\"d887f42c-bda5-091a-0798-af42d3ed173e\",\"description\":\"Google Pay\",\"consumed\":false,\"details\":{\"cardType\":\"Visa\",\"lastTwo\":\"34\",\"lastFour\":\"1234\"},\"binData\":{\"prepaid\":\"No\",\"healthcare\":\"No\",\"debit\":\"No\",\"durbinRegulated\":\"No\",\"commercial\":\"No\",\"payroll\":\"No\",\"issuingBank\":\"Issuing Bank USA\",\"countryOfIssuance\":\"USA\",\"productId\":\"A\"}}]}"
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
    const val REPSONSE_GOOGLE_PAY_PAYPAL_ACCOUNT = """
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
    //endregion
}