package com.braintreepayments.api.shopperinsights

/**
 * The page or view that a button is displayed on.
 */
enum class PageType(internal val stringValue: String) {
    /**
     * The homepage
     */
    HOMEPAGE("homepage"),

    /**
     * The about page
     */
    ABOUT("about"),

    /**
     * The contact page
     */
    CONTACT("contact"),

    /**
     * A product category page
     */
    PRODUCT_CATEGORY("product_category"),

    /**
     * The product details page
     */
    PRODUCT_DETAILS("product_details"),

    /**
     * The search page
     */
    SEARCH("search"),

    /**
     * The checkout page
     */
    CHECKOUT("checkout"),

    /**
     * The order review page
     */
    ORDER_REVIEW("order_review"),

    /**
     * The order confirmation page
     */
    ORDER_CONFIRMATION("order_confirmation"),

    /**
     * The mini cart
     */
    MINI_CART("mini_cart"),

    /**
     * The payment method selection page
     */
    PAYMENT_METHOD_SELECTION("payment_method_selection"),

    /**
     * The payment method addition page
     */
    PAYMENT_METHOD_ADDITION("payment_method_addition"),

    /**
     * Some other page
     */
    OTHER("other")
}
