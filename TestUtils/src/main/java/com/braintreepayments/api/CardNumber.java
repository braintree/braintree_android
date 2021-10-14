package com.braintreepayments.api;

public class CardNumber {

    public static final String VISA = "4111111111111111";
    public static final String VISA_2 = "4005519200000004";
    public static final String INVALID_VISA = "4111111111111112";
    public static final String AMEX = "378282246310005";
    public static final String INVALID_AMEX = "371111111111111";

    public static final String THREE_D_SECURE_VERIFICATON = "4000000000000002";
    public static final String THREE_D_SECURE_VERIFICATON_NOT_REQUIRED = "4000000000000051";
    public static final String THREE_D_SECURE_LOOKUP_ERROR = "4000000000000077";
    public static final String THREE_D_SECURE_LOOKUP_TIMEOUT = "4000000000000044";
    public static final String THREE_D_SECURE_AUTHENTICATION_FAILED = "4000000000000028";
    public static final String THREE_D_SECURE_AUTHENTICATION_UNAVAILABLE = "4000000000000069";
    public static final String THREE_D_SECURE_ISSUER_DOES_NOT_PARTICIPATE = "4000000000000101";
    public static final String THREE_D_SECURE_SIGNATURE_VERIFICATION_FAILURE = "340000000006022";
    public static final String THREE_D_SECURE_ISSUER_DOWN = "4000000000000036";
    public static final String THREE_D_SECURE_MPI_LOOKUP_ERROR = "4000000000000085";
    public static final String THREE_D_SECURE_MPI_SERVICE_ERROR = "4000000000000093";

    public static final String UNIONPAY_INTEGRATION_CREDIT = "6222821234560017";
    public static final String UNIONPAY_INTEGRATION_DEBIT = "6223164991230014";
    public static final String UNIONPAY_CREDIT = "6212345678901232";
    public static final String UNIONPAY_DEBIT = "6212345678901265";
    public static final String UNIONPAY_SINGLE_STEP_SALE = "6212345678900093";
    public static final String UNIONPAY_SMS_NOT_REQUIRED = "6212345678900085";
    public static final String UNIONPAY_NOT_ACCEPTED = "6212345678900028";
}
