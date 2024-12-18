package com.braintreepayments.api.shopperinsights

/**
 * The page or view that a button is displayed on.
 */
enum class PageType {
    /**
     * The homepage
     */
    homepage,

    /**
     * The about page
     */
    about,

    /**
     * The contact page
     */
    contact,

    /**
     * A product category page
     */
    product_category,

    /**
     * The product details page
     */
    product_details,

    /**
     * The search page
     */
    search,

    /**
     * The checkout page
     */
    checkout,

    /**
     * The order review page
     */
    order_review,

    /**
     * The order confirmation page
     */
    order_confirmation,

    /**
     * The mini cart
     */
    mini_cart,

    /**
     * Some other page
     */
    other
}
