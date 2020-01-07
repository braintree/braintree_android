package com.braintreepayments.api.internal;

import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.models.ThreeDSecureV1UiCustomization;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV1BrowserSwitchHelperTest {

    private ThreeDSecureLookup mThreeDSecureLookup;

    @Before
    public void setup() throws Exception {
        String lookupResponse = stringFromFixture("three_d_secure/lookup_response_with_version_number1.json");
        mThreeDSecureLookup = ThreeDSecureLookup.fromJson(lookupResponse);
    }

    @Test
    public void getUrl_returnsUrlString() {
        String urlScheme = "com.braintreepayments.api.braintree";
        String assetsUrl = "https://www.some-assets.com";

        String actualUrl = ThreeDSecureV1BrowserSwitchHelper.getUrl(urlScheme, assetsUrl, null, mThreeDSecureLookup);
        String expectedUrl = "https://www.some-assets.com/mobile/three-d-secure-redirect/0.2.0/index.html?" +
                "AcsUrl=https%3A%2F%2Facs-url%2F&" +
                "PaReq=pareq&" +
                "MD=merchant-descriptor&" +
                "TermUrl=https%3A%2F%2Fterm-url%2F&" +
                "ReturnUrl=https%3A%2F%2Fwww.some-assets.com%2Fmobile%2Fthree-d-secure-redirect%2F0.2.0%2Fredirect.html%3F" +
                "redirect_url%253Dcom.braintreepayments.api.braintree%253A%252F%252Fx-callback-url%252Fbraintree%25252Fthreedsecure%25253F";

        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    public void getUrl_whenUiCustomizationIsPresent_returnsUrlStringWithUiCustomizationParameters() {
        ThreeDSecureV1UiCustomization v1UiCustomization = new ThreeDSecureV1UiCustomization()
                .redirectButtonText("button")
                .redirectDescription("description");

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .v1UiCustomization(v1UiCustomization);

        String urlScheme = "com.braintreepayments.api.braintree";
        String assetsUrl = "https://www.some-assets.com";

        String actualUrl = ThreeDSecureV1BrowserSwitchHelper.getUrl(urlScheme, assetsUrl, request, mThreeDSecureLookup);
        String expectedUrl = "https://www.some-assets.com/mobile/three-d-secure-redirect/0.2.0/index.html?" +
                "AcsUrl=https%3A%2F%2Facs-url%2F&" +
                "PaReq=pareq&" +
                "MD=merchant-descriptor&" +
                "TermUrl=https%3A%2F%2Fterm-url%2F&" +
                "ReturnUrl=https%3A%2F%2Fwww.some-assets.com%2Fmobile%2Fthree-d-secure-redirect%2F0.2.0%2Fredirect.html%3F" +
                "b%253Dbutton%2526d%253Ddescription%2526" +
                "redirect_url%253Dcom.braintreepayments.api.braintree%253A%252F%252Fx-callback-url%252Fbraintree%25252Fthreedsecure%25253F";

        assertEquals(expectedUrl, actualUrl);
    }
}
