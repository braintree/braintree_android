package com.braintreepayments.api;

import com.braintreepayments.api.dropin.utils.CardType;
import com.braintreepayments.api.dropin.utils.CardUtils;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class CardTypeTest extends TestCase {

    private static final int MIN_MIN_CARD_LENGTH = 12;
    private static final int MAX_MAX_CARD_LENGTH = 19;

    private static final int MIN_SECURITY_CODE_LENGTH = 3;
    private static final int MAX_SECURITY_CODE_LENGTH = 4;

    // ref: https://www.braintreepayments.com/docs/ruby/reference/sandbox
    private static final Map<String, CardType> SAMPLE_CARDS = new HashMap<String, CardType>();

    static {
        // Visa
        SAMPLE_CARDS.put("4111111111111111", CardType.VISA);
        SAMPLE_CARDS.put("4005519200000004", CardType.VISA);
        SAMPLE_CARDS.put("4009348888881881", CardType.VISA);
        SAMPLE_CARDS.put("4012000033330026", CardType.VISA);
        SAMPLE_CARDS.put("4012000077777777", CardType.VISA);
        SAMPLE_CARDS.put("4012888888881881", CardType.VISA);
        SAMPLE_CARDS.put("4217651111111119", CardType.VISA);
        SAMPLE_CARDS.put("4500600000000061", CardType.VISA);

        // Mastercard
        SAMPLE_CARDS.put("5555555555554444", CardType.MASTERCARD);
        SAMPLE_CARDS.put("5105105105105100", CardType.MASTERCARD);

        // Discover
        SAMPLE_CARDS.put("6011111111111117", CardType.DISCOVER);
        SAMPLE_CARDS.put("6011000990139424", CardType.DISCOVER);

        // Amex
        SAMPLE_CARDS.put("378282246310005", CardType.AMEX);
        SAMPLE_CARDS.put("371449635398431", CardType.AMEX);

        // Diner's club
        SAMPLE_CARDS.put("30000000000004", CardType.DINERS_CLUB);

        // JCB
        SAMPLE_CARDS.put("3530111333300000", CardType.JCB);
        SAMPLE_CARDS.put("3566002020360505", CardType.JCB);

        // Maestro
        SAMPLE_CARDS.put("5018000000000009", CardType.MAESTRO);

        // Unionpay
        SAMPLE_CARDS.put("6240888888888885", CardType.UNION_PAY);
    }

    public void testAllParametersSane() {
        for (final CardType cardType : CardType.values()) {
            final int minCardLength = cardType.getMinCardLength();
            assertTrue(String.format("%s: Min card length %s too small",
                    cardType, minCardLength), minCardLength >= MIN_MIN_CARD_LENGTH);

            final int maxCardLength = cardType.getMaxCardLength();
            assertTrue(String.format("%s: Max card length %s too large",
                    cardType, maxCardLength), maxCardLength <= MAX_MAX_CARD_LENGTH);

            assertTrue(String.format("%s: Min card length %s greater than its max %s",
                            cardType, minCardLength, maxCardLength),
                    minCardLength <= maxCardLength
            );

            final int securityCodeLength = cardType.getSecurityCodeLength();
            assertTrue(String.format("%s: Unusual security code length %s",
                            cardType, securityCodeLength),
                    securityCodeLength >= MIN_SECURITY_CODE_LENGTH &&
                            securityCodeLength <= MAX_SECURITY_CODE_LENGTH
            );

            assertTrue(String.format("%s: No front resource declared", cardType),
                    cardType.getFrontResource() != 0);
            assertTrue(String.format("%s: No cvv resource declared", cardType),
                    cardType.getSecurityCodeResource() != 0);

            if (cardType != CardType.UNKNOWN) {
                final Pattern pattern = cardType.getPattern();
                final String regex = pattern.toString();
                assertTrue(String.format("%s: Pattern must start with ^", cardType),
                        regex.startsWith("^"));
                assertTrue(String.format("%s: Pattern must end with \\d*", cardType),
                        regex.endsWith("\\d*"));
            }
        }
    }

    public void testSampleCards() {
        for (final Map.Entry<String, CardType> entry : SAMPLE_CARDS.entrySet()) {
            final String cardNumber = entry.getKey();
            final CardType cardType = entry.getValue();
            final CardType actualType = CardType.forCardNumber(cardNumber);

            assertEquals(String.format("CardType.forAccountNumber failed for %s", cardNumber),
                    cardType, actualType);
            assertTrue(String.format("%s: Luhn check failed for [%s]", cardType, cardNumber),
                    CardUtils.isLuhnValid(cardNumber));
        }
    }

    public void testAllCardsTested() {
        // It's just so meta.
        final Set<CardType> allCards = new HashSet<CardType>();
        Collections.addAll(allCards, CardType.values());
        allCards.remove(CardType.UNKNOWN);

        final Set<CardType> testCards = new HashSet<CardType>();
        testCards.addAll(SAMPLE_CARDS.values());

        if (!allCards.equals(testCards)) {
            allCards.removeAll(testCards);
            StringBuilder msgBuilder = new StringBuilder();
            msgBuilder.append("Found ")
                    .append(allCards.size())
                    .append(" CardType(s) not tested by SAMPLE_CARDS: ");
            for (final CardType missing : allCards) {
                msgBuilder.append(missing)
                        .append(' ');
            }
            fail(msgBuilder.toString());
        }
    }

}
