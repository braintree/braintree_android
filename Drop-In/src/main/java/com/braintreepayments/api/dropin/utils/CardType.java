package com.braintreepayments.api.dropin.utils;

import android.text.TextUtils;

import com.braintreepayments.api.dropin.R;

import java.util.regex.Pattern;

/**
 * Card types and related formatting and validation rules.
 */
public enum CardType {

    VISA("^4\\d*",
            R.drawable.bt_visa,
            16, 16,
            3),
    MASTERCARD("^5[1-5]\\d*",
            R.drawable.bt_mastercard,
            16, 16,
            3),
    DISCOVER("^(6011|65|64[4-9]|622)\\d*",
            R.drawable.bt_discover,
            16, 16,
            3),
    AMEX("^3[47]\\d*",
            R.drawable.bt_amex,
            15, 15,
            4),
    DINERS_CLUB("^(36|38|30[0-5])\\d*",
            R.drawable.bt_diners,
            14, 14,
            3),
    JCB("^35\\d*",
            R.drawable.bt_jcb,
            16, 16,
            3),
    MAESTRO("^(5018|5020|5038|6304|6759|676[1-3])\\d*",
            R.drawable.bt_maestro,
            12, 19,
            3),
    UNION_PAY("^62\\d*",
            R.drawable.bt_card_highlighted,
            16, 19,
            3),
    UNKNOWN("",
            R.drawable.bt_card_highlighted,
            12, 19,
            3);

    private static final int[] AMEX_SPACE_INDICES = {4, 10};
    private static final int[] DEFAULT_SPACE_INDICES = {4, 8, 12};

    private final Pattern mPattern;
    private final int mFrontResource;
    private final int mMinCardLength;
    private final int mMaxCardLength;
    private final int mSecurityCodeLength;

    private CardType(String regex, int frontResource, int minCardLength, int maxCardLength,
            int securityCodeLength) {
        mPattern = Pattern.compile(regex);
        mFrontResource = frontResource;
        mMinCardLength = minCardLength;
        mMaxCardLength = maxCardLength;
        mSecurityCodeLength = securityCodeLength;
    }

    /**
     * @return The regex matching this card type.
     */
    public Pattern getPattern() {
        return mPattern;
    }

    /**
     * @return The android resource id for the front card image, highlighting card number format.
     */
    public int getFrontResource() {
        return mFrontResource;
    }

    /**
     * @return The android resource id for the security code card image.
     */
    public int getSecurityCodeResource() {
        if (this == AMEX) {
            return R.drawable.bt_cid_highlighted;
        } else {
            return R.drawable.bt_cvv_highlighted;
        }
    }

    /**
     * @return The length of the current card's security code.
     */
    public int getSecurityCodeLength() {
        return mSecurityCodeLength;
    }

    public int getMinCardLength() {
        return mMinCardLength;
    }

    public int getMaxCardLength() {
        return mMaxCardLength;
    }

    public int[] getSpaceIndices() {
        return this == AMEX ? AMEX_SPACE_INDICES : DEFAULT_SPACE_INDICES;
    }

    public boolean isLegalCardLength(String cardNumber) {
        final int len = cardNumber.length();
        return len >= mMinCardLength && len <= mMaxCardLength;
    }

    /**
     * @param cardNumber The card number to validate.
     * @return {@code true} if this card number is locally valid.
     */
    public boolean validate(String cardNumber) {
        if (TextUtils.isEmpty(cardNumber)) {
            return false;
        }

        final int numberLength = cardNumber.length();
        if (numberLength < mMinCardLength || numberLength > mMaxCardLength) {
            return false;
        } else if (!mPattern.matcher(cardNumber).matches()) {
            return false;
        }
        return CardUtils.isLuhnValid(cardNumber);
    }

    /**
     * Returns the card type matching this account, or {@link com.braintreepayments.api.dropin.utils.CardType#UNKNOWN}
     * for no match.
     * <p/>
     * A partial account type may be given, with the caveat that it may not have enough digits to
     * match.
     */
    public static CardType forCardNumber(String cardNumber) {
        for (final CardType cardType : values()) {
            final Pattern pattern = cardType.getPattern();
            if (pattern.matcher(cardNumber).matches()) {
                return cardType;
            }
        }
        return UNKNOWN;
    }

}