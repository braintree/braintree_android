package com.braintreepayments.api;

import org.json.JSONObject;

public class PostalAddressParser {

    public static final String RECIPIENT_NAME_KEY = "recipientName";
    public static final String STREET_ADDRESS_KEY = "street1";
    public static final String EXTENDED_ADDRESS_KEY = "street2";
    public static final String LOCALITY_KEY = "city";
    public static final String COUNTRY_CODE_ALPHA_2_KEY = "country";
    public static final String POSTAL_CODE_KEY = "postalCode";
    public static final String REGION_KEY = "state";
    public static final String LINE_1_KEY = "line1";
    public static final String LINE_2_KEY = "line2";
    public static final String COUNTRY_CODE_KEY = "countryCode";

    public static final String USER_ADDRESS_NAME_KEY = "name";
    public static final String USER_ADDRESS_PHONE_NUMBER_KEY = "phoneNumber";
    public static final String USER_ADDRESS_ADDRESS_1_KEY = "address1";
    public static final String USER_ADDRESS_ADDRESS_2_KEY = "address2";
    public static final String USER_ADDRESS_ADDRESS_3_KEY = "address3";
    public static final String USER_ADDRESS_ADDRESS_4_KEY = "address4";
    public static final String USER_ADDRESS_ADDRESS_5_KEY = "address5";
    public static final String USER_ADDRESS_POSTAL_CODE_KEY = "postalCode";
    public static final String USER_ADDRESS_SORTING_CODE_KEY = "sortingCode";
    public static final String USER_ADDRESS_COUNTRY_CODE_KEY = "countryCode";
    public static final String USER_ADDRESS_LOCALITY_KEY = "locality";
    public static final String USER_ADDRESS_ADMINISTRATIVE_AREA_KEY = "administrativeArea";

    public static final String COUNTRY_CODE_UNDERSCORE_KEY = "country_code";
    public static final String POSTAL_CODE_UNDERSCORE_KEY = "postal_code";
    public static final String RECIPIENT_NAME_UNDERSCORE_KEY = "recipient_name";

    public static PostalAddress fromJson(JSONObject accountAddress) {
        // If we don't have an account address, return an empty PostalAddress.
        if (accountAddress == null) {
            return new PostalAddress();
        }

        String streetAddress = Json.optString(accountAddress, STREET_ADDRESS_KEY, null);
        String extendedAddress = Json.optString(accountAddress, EXTENDED_ADDRESS_KEY, null);
        String countryCodeAlpha2 = Json.optString(accountAddress, COUNTRY_CODE_ALPHA_2_KEY, null);

        //Check alternate keys
        if (streetAddress == null) {
            streetAddress = Json.optString(accountAddress, LINE_1_KEY, null);
        }
        if (extendedAddress == null) {
            extendedAddress = Json.optString(accountAddress, LINE_2_KEY, null);
        }
        if (countryCodeAlpha2 == null) {
            countryCodeAlpha2 = Json.optString(accountAddress, COUNTRY_CODE_KEY, null);
        }

        // If this is a UserAddress-like JSON, parse it as such
        if (streetAddress == null && Json.optString(accountAddress, USER_ADDRESS_NAME_KEY, null) != null) {
            return fromUserAddressJson(accountAddress);
        }

        return new PostalAddress().recipientName(Json.optString(accountAddress, RECIPIENT_NAME_KEY, null))
                .streetAddress(streetAddress)
                .extendedAddress(extendedAddress)
                .locality(Json.optString(accountAddress, LOCALITY_KEY, null))
                .region(Json.optString(accountAddress, REGION_KEY, null))
                .postalCode(Json.optString(accountAddress, POSTAL_CODE_KEY, null))
                .countryCodeAlpha2(countryCodeAlpha2);
    }

    public static PostalAddress fromUserAddressJson(JSONObject json) {
        PostalAddress address = new PostalAddress();

        address
                .recipientName(Json.optString(json, USER_ADDRESS_NAME_KEY, ""))
                .phoneNumber(Json.optString(json, USER_ADDRESS_PHONE_NUMBER_KEY, ""))
                .streetAddress(Json.optString(json, USER_ADDRESS_ADDRESS_1_KEY, ""))
                .extendedAddress(formatExtendedUserAddress(json))
                .locality(Json.optString(json, USER_ADDRESS_LOCALITY_KEY, ""))
                .region(Json.optString(json, USER_ADDRESS_ADMINISTRATIVE_AREA_KEY, ""))
                .countryCodeAlpha2(Json.optString(json, USER_ADDRESS_COUNTRY_CODE_KEY, ""))
                .postalCode(Json.optString(json, USER_ADDRESS_POSTAL_CODE_KEY, ""))
                .sortingCode(Json.optString(json, USER_ADDRESS_SORTING_CODE_KEY, ""));

        return address;
    }

    private static String formatExtendedUserAddress(JSONObject address) {
        String extendedAddress = "" +
                Json.optString(address, USER_ADDRESS_ADDRESS_2_KEY, "") + "\n" +
                Json.optString(address, USER_ADDRESS_ADDRESS_3_KEY, "") + "\n" +
                Json.optString(address, USER_ADDRESS_ADDRESS_4_KEY, "") + "\n" +
                Json.optString(address, USER_ADDRESS_ADDRESS_5_KEY, "");

        return extendedAddress.trim();
    }
}
