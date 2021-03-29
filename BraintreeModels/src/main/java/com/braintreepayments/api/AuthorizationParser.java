package com.braintreepayments.api;

import android.text.TextUtils;

class AuthorizationParser {

    Authorization parse(String authorizationString) throws InvalidArgumentException {
        if (isTokenizationKey(authorizationString)) {
            return new TokenizationKey(authorizationString);
        } else if (isPayPalUAT(authorizationString)){
            return new PayPalUAT(authorizationString);
        } else if (isClientToken(authorizationString)) {
            return new ClientToken(authorizationString);
        } else {
            throw new InvalidArgumentException("Authorization provided is invalid: " + authorizationString);
        }
    }

    private static boolean isTokenizationKey(String tokenizationKey) {
        return !TextUtils.isEmpty(tokenizationKey) && tokenizationKey.matches(TokenizationKey.MATCHER);
    }

    private static boolean isPayPalUAT(String payPalUAT) {
        return !TextUtils.isEmpty(payPalUAT) && payPalUAT.matches(PayPalUAT.MATCHER);
    }

    private static boolean isClientToken(String clientToken) {
        return !TextUtils.isEmpty(clientToken) && clientToken.matches(ClientToken.BASE_64_MATCHER);
    }
}
