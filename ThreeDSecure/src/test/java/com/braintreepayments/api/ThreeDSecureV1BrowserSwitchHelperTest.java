package com.braintreepayments.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV1BrowserSwitchHelperTest {

    private ThreeDSecureLookup mThreeDSecureLookup;

    @Before
    public void setup() throws Exception {
        mThreeDSecureLookup = ThreeDSecureLookup.fromJson("{\n" +
                "\"acsUrl\": \"https://acs.com\",\n" +
                "\"md\": \"m d\",\n" +
                "\"termUrl\": \"https://terms.com\",\n" +
                "\"pareq\": \"pa.req\",\n" +
                "\"threeDSecureVersion\": \"1.0\",\n" +
                "\"transactionId\": \"some-transaction-id\"\n" +
            "}");
    }

    @Test
    public void getUrl_returnsUrlString() {
        String urlScheme = "com.braintreepayments.Demo.payments";
        String assetsUrl = "https://assets.com";

        ThreeDSecureV1BrowserSwitchHelper sut = new ThreeDSecureV1BrowserSwitchHelper();
        String actualUrl = sut.getUrl(urlScheme, assetsUrl, null, mThreeDSecureLookup);
        String expectedUrl = "https://assets.com/mobile/three-d-secure-redirect/0.2.0/index.html?" +
                "AcsUrl=https%3A%2F%2Facs.com&" +
                "PaReq=pa.req&" +
                "MD=m%20d&" +
                "TermUrl=https%3A%2F%2Fterms.com&" +
                "ReturnUrl=https%3A%2F%2Fassets.com%2Fmobile%2Fthree-d-secure-redirect%2F0.2.0%2Fredirect.html%3F" +
                "redirect_url%253Dcom.braintreepayments.Demo.payments%25253A%25252F%25252Fx-callback-url%25252Fbraintree%25252Fthreedsecure%25253F";

        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    public void getUrl_whenUiCustomizationIsPresent_returnsUrlStringWithUiCustomizationParameters() {
        String urlScheme = "com.braintreepayments.Demo.payments";
        String assetsUrl = "https://assets.com";

        ThreeDSecureV1UiCustomization v1UiCustomization = new ThreeDSecureV1UiCustomization();
        v1UiCustomization.setRedirectButtonText("button text");
        v1UiCustomization.setRedirectDescription("description text");

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.v1UiCustomization(v1UiCustomization);

        ThreeDSecureV1BrowserSwitchHelper sut = new ThreeDSecureV1BrowserSwitchHelper();
        String actualUrl = sut.getUrl(urlScheme, assetsUrl, request, mThreeDSecureLookup);
        String expectedUrl = "https://assets.com/mobile/three-d-secure-redirect/0.2.0/index.html?" +
                "AcsUrl=https%3A%2F%2Facs.com&" +
                "PaReq=pa.req&" +
                "MD=m%20d&" +
                "TermUrl=https%3A%2F%2Fterms.com&" +
                "ReturnUrl=https%3A%2F%2Fassets.com%2Fmobile%2Fthree-d-secure-redirect%2F0.2.0%2Fredirect.html%3F" +
                "b%253Dbutton%252520text%2526d%253Ddescription%252520text%2526" +
                "redirect_url%253Dcom.braintreepayments.Demo.payments%25253A%25252F%25252Fx-callback-url%25252Fbraintree%25252Fthreedsecure%25253F";

        assertEquals(expectedUrl, actualUrl);
    }
}
