# PayPal Module Happy Path Sequence Diagram

```mermaid
sequenceDiagram
    participant App as Merchant App
    participant PC as PayPalClient
    participant PIC as PayPalInternalClient
    participant BC as BraintreeClient
    participant DC as DataCollector
    participant API as Braintree API
    participant PL as PayPalLauncher
    participant Browser as Browser/PayPal App
    participant PPServer as PayPal Server

    Note over App, PPServer: PayPal Payment Flow - Happy Path

    %% Step 1: Initialize PayPalClient
    App->>PC: new PayPalClient(context, authorization, appLinkReturnUrl)
    PC->>BC: new BraintreeClient(context, authorization, ...)
    PC->>PIC: new PayPalInternalClient(braintreeClient)
    
    %% Step 2: Create Payment Auth Request
    App->>PC: createPaymentAuthRequest(context, payPalRequest, callback)
    Note over PC: isVaultRequest = payPalRequest is PayPalVaultRequest
    PC->>BC: getConfiguration(callback)
    BC->>API: GET /v1/configuration
    API-->>BC: configuration response
    BC-->>PC: configuration
    
    %% Step 3: Validate Configuration
    alt PayPal Enabled
        PC->>BC: sendAnalyticsEvent(TOKENIZATION_STARTED)
        PC->>PIC: sendRequest(context, payPalRequest, configuration, callback)
        
        %% Step 4: Prepare Request Body
        Note over PIC: Determine endpoint: "paypal_hermes/create_payment_resource" or "paypal_hermes/setup_billing_agreement"
        PIC->>PIC: createRequestBody(configuration, authorization, successUrl, cancelUrl, appLink)
        
        %% Step 5: Send Payment Request
        PIC->>BC: sendPOST(url, requestBody)
        BC->>API: POST /v1/paypal_hermes/create_payment_resource
        API->>PPServer: Create payment resource
        PPServer-->>API: Payment resource with redirect URL
        API-->>BC: Payment response
        BC-->>PIC: responseBody
        
        %% Step 6: Process Response and Collect Data
        PIC->>PIC: PayPalPaymentResource.fromJson(responseBody)
        PIC->>DC: getClientMetadataId(context, dataCollectorRequest, configuration)
        DC-->>PIC: clientMetadataId
        
        %% Step 7: Create Payment Auth Request Params
        PIC->>PIC: PayPalPaymentAuthRequestParams(payPalRequest, clientMetadataId, paypalContextId, successUrl)
        PIC->>PC: buildBrowserSwitchOptions(paymentAuthRequest)
        PC-->>PIC: browserSwitchOptions
        PIC-->>PC: PayPalPaymentAuthRequestParams
        PC-->>App: PayPalPaymentAuthRequest.ReadyToLaunch
        
        %% Step 8: Launch Browser Flow
        App->>PL: launch(activity, paymentAuthRequest)
        PL->>BC: sendAnalyticsEvent(BROWSER_PRESENTATION_STARTED or APP_SWITCH_STARTED)
        PL->>PL: browserSwitchClient.start(activity, options)
        PL->>Browser: Launch browser/PayPal app with approval URL
        Browser->>PPServer: User authentication flow
        
        %% Step 9: User Authentication & Authorization
        Note over Browser, PPServer: User logs in and authorizes payment
        PPServer-->>Browser: Redirect with authorization tokens
        Browser-->>App: Return to app via deep link/app link
        
        %% Step 10: Handle Return from Browser
        App->>PL: handleReturnToApp(pendingRequest, intent)
        PL->>BC: sendAnalyticsEvent(HANDLE_RETURN_STARTED)
        PL->>PL: browserSwitchClient.completeRequest(intent, pendingRequestString)
        PL-->>App: PayPalPaymentAuthResult.Success
        
        %% Step 11: Tokenize PayPal Account
        App->>PC: tokenize(paymentAuthResult, callback)
        PC->>PC: parseUrlResponseData(uri, successUrl, approvalUrl, tokenKey)
        PC->>PC: new PayPalAccount(clientMetadataId, urlResponseData, payPalIntent, merchantAccountId, paymentType)
        PC->>PIC: tokenize(payPalAccount, callback)
        PIC->>BC: tokenizeREST(payPalAccount, callback)
        BC->>API: POST /v1/payment_methods/paypal_accounts
        API-->>BC: PayPal account nonce
        BC-->>PIC: tokenization response
        PIC->>PIC: PayPalAccountNonce.fromJSON(tokenizationResponse)
        PIC-->>PC: PayPalAccountNonce
        PC->>BC: sendAnalyticsEvent(TOKENIZATION_SUCCEEDED)
        PC-->>App: PayPalResult.Success(payPalAccountNonce)
        
    else PayPal Not Enabled
        PC->>BC: sendAnalyticsEvent(TOKENIZATION_FAILED)
        PC-->>App: PayPalPaymentAuthRequest.Failure(BraintreeException)
    end

    Note over App: Success! PayPalAccountNonce can be sent to merchant server
```

## Flow Description

### 1. Initialization
- Merchant app creates a `PayPalClient` with context, authorization, and return URL
- `PayPalClient` internally creates `BraintreeClient` and `PayPalInternalClient`

### 2. Payment Request Creation
- App calls `createPaymentAuthRequest()` with a `PayPalRequest` (either `PayPalCheckoutRequest` or `PayPalVaultRequest`)
- Client fetches configuration from Braintree API to validate PayPal is enabled
- Analytics event `TOKENIZATION_STARTED` is sent

### 3. Payment Resource Creation
- `PayPalInternalClient` determines the appropriate endpoint based on request type:
  - Checkout: `/v1/paypal_hermes/create_payment_resource`
  - Vault: `/v1/paypal_hermes/setup_billing_agreement`
- Request body is created with payment details, return URLs, and configuration
- API call is made to create the PayPal payment resource

### 4. Data Collection & Request Preparation
- Payment resource response contains redirect URL for PayPal authentication
- `DataCollector` generates `clientMetadataId` for fraud prevention
- `PayPalPaymentAuthRequestParams` is created with all necessary data
- Browser switch options are configured for the authentication flow

### 5. Browser Authentication Flow
- `PayPalLauncher` launches browser or PayPal app with approval URL
- User authenticates with PayPal and authorizes the payment
- PayPal redirects back to the app with authorization tokens

### 6. Return Handling & Tokenization
- App handles the return intent in `handleReturnToApp()`
- URL response data is parsed and validated
- `PayPalAccount` object is created with authorization data
- Final tokenization call creates a `PayPalAccountNonce`
- Success analytics event is sent

### 7. Result
- App receives `PayPalResult.Success` with the `PayPalAccountNonce`
- The nonce can be sent to the merchant server for payment processing

## Key Components

- **PayPalClient**: Main entry point for PayPal integration
- **PayPalInternalClient**: Handles API communication and data collection
- **PayPalLauncher**: Manages browser switching and authentication flow
- **PayPalRequest**: Base class for checkout and vault requests
- **PayPalAccountNonce**: Final tokenized result containing payment method information

## Error Handling

The diagram shows the happy path, but the implementation includes comprehensive error handling for:
- Configuration errors (PayPal not enabled)
- Network failures
- User cancellation
- Invalid authentication responses
- Tokenization failures

Each error case triggers appropriate analytics events and returns failure results to the merchant app.