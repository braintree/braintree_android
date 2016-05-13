package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Builder used to construct a card tokenization request.
 */
public class CardBuilder extends BaseCardBuilder<CardBuilder> implements Parcelable {

    public CardBuilder() {}

    protected CardBuilder(Parcel in) {
        super(in);
    }

    public static final Creator<CardBuilder> CREATOR = new Creator<CardBuilder>() {
        @Override
        public CardBuilder createFromParcel(Parcel in) {
            return new CardBuilder(in);
        }

        @Override
        public CardBuilder[] newArray(int size) {
            return new CardBuilder[size];
        }
    };
}