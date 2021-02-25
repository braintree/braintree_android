package com.braintreepayments.api;

import org.json.JSONObject;

class PostalAddressParser {

    static final String RECIPIENT_NAME_KEY = "recipientName";
    static final String STREET_ADDRESS_KEY = "street1";
    static final String EXTENDED_ADDRESS_KEY = "street2";
    static final String LOCALITY_KEY = "city";
    static final String COUNTRY_CODE_ALPHA_2_KEY = "country";
    static final String POSTAL_CODE_KEY = "postalCode";
    static final String REGION_KEY = "state";
    static final String LINE_1_KEY = "line1";
    static final String LINE_2_KEY = "line2";
    static final String COUNTRY_CODE_KEY = "countryCode";

    static final String USER_ADDRESS_NAME_KEY = "name";
    static final String USER_ADDRESS_PHONE_NUMBER_KEY = "phoneNumber";
    static final String USER_ADDRESS_ADDRESS_1_KEY = "address1";
    static final String USER_ADDRESS_ADDRESS_2_KEY = "address2";
    static final String USER_ADDRESS_ADDRESS_3_KEY = "address3";
    static final String USER_ADDRESS_ADDRESS_4_KEY = "address4";
    static final String USER_ADDRESS_ADDRESS_5_KEY = "address5";
    static final String USER_ADDRESS_POSTAL_CODE_KEY = "postalCode";
    static final String USER_ADDRESS_SORTING_CODE_KEY = "sortingCode";
    static final String USER_ADDRESS_COUNTRY_CODE_KEY = "countryCode";
    static final String USER_ADDRESS_LOCALITY_KEY = "locality";
    static final String USER_ADDRESS_ADMINISTRATIVE_AREA_KEY = "administrativeArea";

    static final String COUNTRY_CODE_UNDERSCORE_KEY = "country_code";
    static final String POSTAL_CODE_UNDERSCORE_KEY = "postal_code";
    static final String RECIPIENT_NAME_UNDERSCORE_KEY = "recipient_name";

    static PostalAddress fromJson(JSONObject accountAddress) {
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

    static PostalAddress fromUserAddressJson(JSONObject json) {
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
