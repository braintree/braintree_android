package com.braintreepayments.demo;

import android.text.Editable;

import com.braintreepayments.cardform.view.CardForm;

public class AutofillHelper {

    private final CardForm cardForm;

    AutofillHelper(CardForm cardForm) {
        this.cardForm = cardForm;
    }

    void fillCardNumber(String cardNumber) {
        cardForm.getCardEditText().requestFocus();
        Editable editable = cardForm.getCardEditText().getText();
        for (char c : cardNumber.toCharArray()) {
            if (c != ' ') {
                editable.append(c);
            }
        }
    }

    void fillExpirationDate(String expirationDate) {
        if (cardForm.getExpirationDateEditText() == null) {
            return;
        }
        cardForm.getExpirationDateEditText().requestFocus();
        Editable editable = cardForm.getExpirationDateEditText().getText();
        for (char c : expirationDate.toCharArray()) {
            if (c != ' ') {
                editable.append(c);
            }
        }
    }

    void fillCVV(String cvv) {
        if (cardForm.getCvvEditText() == null) {
            return;
        }
        cardForm.getCvvEditText().requestFocus();
        Editable editable = cardForm.getCvvEditText().getText();
        for (char c : cvv.toCharArray()) {
            if (c != ' ') {
                editable.append(c);
            }
        }
    }

    void fillPostalCode(String postalCode) {
        if (cardForm.getPostalCodeEditText() == null) {
            return;
        }
        cardForm.getPostalCodeEditText().requestFocus();
        Editable editable = cardForm.getPostalCodeEditText().getText();
        for (char c : postalCode.toCharArray()) {
            if (c != ' ') {
                editable.append(c);
            }
        }
    }
}
