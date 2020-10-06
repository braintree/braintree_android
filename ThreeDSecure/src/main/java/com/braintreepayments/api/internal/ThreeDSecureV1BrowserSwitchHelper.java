package com.braintreepayments.api.internal;

import android.net.Uri;

import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.models.ThreeDSecureV1UiCustomization;

public class ThreeDSecureV1BrowserSwitchHelper {

    private static final String MOBILE_HOSTED_ASSETS_PATH = "mobile/three-d-secure-redirect/0.2.0";

    // static utility class; no constructor needed
    private ThreeDSecureV1BrowserSwitchHelper() {}

    public static String getUrl(String appReturnUrlScheme,
                                String assetsUrl,
                                ThreeDSecureRequest threeDSecureRequest,
                                ThreeDSecureLookup threeDSecureLookup) {
        Uri redirectUrl = new Uri.Builder()
                .scheme(appReturnUrlScheme)
                .authority("x-callback-url")
                .appendEncodedPath("braintree/threedsecure?") // trailing question mark required
                .build();

        Uri returnUrl = Uri.parse(assetsUrl)
                .buildUpon()
                .appendEncodedPath(MOBILE_HOSTED_ASSETS_PATH)
                .appendEncodedPath("redirect.html")
                .build();

        if (threeDSecureRequest != null) {
            ThreeDSecureV1UiCustomization v1UiCustomization = threeDSecureRequest.getV1UiCustomization();

            if (v1UiCustomization != null && v1UiCustomization.getRedirectButtonText() != null) {
                returnUrl = returnUrl.buildUpon()
                        .appendQueryParameter("b", v1UiCustomization.getRedirectButtonText())
                        .build();
            }

            if (v1UiCustomization != null && v1UiCustomization.getRedirectDescription() != null) {
                returnUrl = returnUrl.buildUpon()
                        .appendQueryParameter("d", v1UiCustomization.getRedirectDescription())
                        .build();
            }
        }

        // redirect_url must be last query parameter in returnUrl
        returnUrl = returnUrl.buildUpon()
                .appendQueryParameter("redirect_url", redirectUrl.toString())
                .build();

        // The return url's query string needs to be encoded a second time
        returnUrl = returnUrl.buildUpon()
                .query(returnUrl.getEncodedQuery())
                .build();

        Uri browserSwitchUrl = Uri.parse(assetsUrl)
                .buildUpon()
                .appendEncodedPath(MOBILE_HOSTED_ASSETS_PATH)
                .appendEncodedPath("index.html")
                .appendQueryParameter("AcsUrl", threeDSecureLookup.getAcsUrl())
                .appendQueryParameter("PaReq", threeDSecureLookup.getPareq())
                .appendQueryParameter("MD", threeDSecureLookup.getMd())
                .appendQueryParameter("TermUrl", threeDSecureLookup.getTermUrl())
                .appendQueryParameter("ReturnUrl", returnUrl.toString())
                .build();

        return browserSwitchUrl.toString();
    }
}
