package com.braintreepayments.api.paypal

/**
 * The upc type of PayPal line item.
 */
enum class PayPalLineItemUpcType(val stringValue: String) {
    UPC_TYPE_A("UPC-A"),
    UPC_TYPE_B("UPC-B"),
    UPC_TYPE_C("UPC-C"),
    UPC_TYPE_D("UPC-D"),
    UPC_TYPE_E("UPC-E"),
    UPC_TYPE_2("UPC-2"),
    UPC_TYPE_5("UPC-5"),
}
