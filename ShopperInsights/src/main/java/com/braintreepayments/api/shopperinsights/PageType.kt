package com.braintreepayments.api.shopperinsights

/**
 * The page or view that a button is displayed on.
 */
enum class PageType {
    /**
     * The homepage
     */
    HOMEPAGE,

    /**
     * The about page
     */
    ABOUT,

    /**
     * The contact page
     */
    CONTACT,

    /**
     * A product category page
     */
    PRODUCT_CATEGORY,

    /**
     * The product details page
     */
    PRODUCT_DETAILS,

    /**
     * The search page
     */
    SEARCH,

    /**
     * The checkout page
     */
    CHECKOUT,

    /**
     * The order review page
     */
    ORDER_REVIEW,

    /**
     * The order confirmation page
     */
    ORDER_CONFIRMATION,

    /**
     * The mini cart
     */
    MINI_CART,

    /**
     * Some other page
     */
    OTHER
}
