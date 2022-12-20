package com.braintreepayments.api;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Provides helper functions to test the SEPA Direct Debit flow in sandbox.
 */
public class SEPADirectDebitTestHelper {
    /**
     * Generates a valid 27-digit IBAN (International Bank Account Number) for testing SEPA Direct Debit flows.
     * @return a valid IBAN
     */
    public static String generateSandboxIBAN() {
        String countryCode = "FR";
        long bankCode = 30006L;
        String branchCode = "00001";
        long accountNumber = ThreadLocalRandom.current().nextLong(10_000_000_000l, 100_000_000_000l);
        String accountNumberWithChecksum = accountNumberWithChecksum(bankCode, Long.parseLong(branchCode), accountNumber);
        String checksum = checksum(bankCode, branchCode, accountNumberWithChecksum);
        String result = countryCode + checksum + String.valueOf(bankCode) + branchCode + accountNumberWithChecksum;
        return result;
    }

    private static String accountNumberWithChecksum(long bankCode, long branchCode, long accountNumber) {
        long sum = 89 * bankCode + 15 * branchCode + 3 * accountNumber;
        long checksum = 97 - calculateMod97(sum);
        return String.valueOf(accountNumber) + String.valueOf(checksum);
    }

    private static long calculateMod97(long accountNumber) {
        long result = 0;
        List<Long> arr = new ArrayList<>();
        while(accountNumber > 0) {
            Long lastDigit = accountNumber % 10;
            arr.add(lastDigit);
            accountNumber /= 10;
        }
        for(int i = arr.size() - 1; i >= 0; i--) {
            result = (result * 10 + arr.get(i)) % 97;
        }
        return result;
    }

    private static String checksum(long bankCode, String branchCode, String accountNumber) {
        // 152700 is taken from the conversion table here: https://community.appway.com/screen/kb/article/generating-and-validating-an-iban-1683400256881#conversion-table
        // and is representative of the characters "FR" with 00 being added to the end for all bban's to calculate the checksum
        String bbanString = String.valueOf(bankCode) + branchCode + accountNumber + "152700";
        BigInteger bban = new BigInteger(bbanString);
        BigInteger modResult = bban.mod(BigInteger.valueOf(97));
        BigInteger subtractionResult = BigInteger.valueOf(98).subtract(modResult);
        return String.format("%02d", subtractionResult);
    }
}
