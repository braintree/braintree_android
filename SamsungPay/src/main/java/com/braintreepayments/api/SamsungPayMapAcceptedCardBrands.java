package com.braintreepayments.api;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SamsungPayMapAcceptedCardBrands {

    private SamsungPayMapAcceptedCardBrands() {}

    static Set<SpaySdk.Brand> mapToSamsungPayCardBrands(List<String> braintreeAcceptedCardBrands) {
        List<SpaySdk.Brand> result = new ArrayList<>();

        for (String brand : braintreeAcceptedCardBrands) {
            switch (brand.toLowerCase()) {
                case "visa":
                    result.add(SpaySdk.Brand.VISA);
                    break;
                case "mastercard":
                    result.add(SpaySdk.Brand.MASTERCARD);
                    break;
                case "discover":
                    result.add(SpaySdk.Brand.DISCOVER);
                    break;
                case "american_express":
                    result.add(SpaySdk.Brand.AMERICANEXPRESS);
                    break;
            }
        }
        return new HashSet<>(result);
    }
}
