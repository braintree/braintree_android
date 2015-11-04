package com.paypal.android.networking.events;

/**
 * the root of the Error objects that can be produced during an API request
 *
 * this roughly corresponds to the PayPal standard of most errors having a code,
 * a short message and a long message
 */
abstract public class ErrorBase {
    protected String mErrorCode;
    protected String mErrorMsgShort;
    protected String mErrorMsgLong;
    protected final String mEventType;

    @Override
    public String toString() {
        return "ErrorBase[mErrorCode=" + mErrorCode + " mErrorMsgShort="+ mErrorMsgShort + "]";
    }

    public ErrorBase() {
        mEventType = "";
    }

    private ErrorBase(String type) {
        mEventType = type;
    }

    protected ErrorBase(String type, String code) {
        this(type);
        mErrorCode = code;
        mErrorMsgShort = null;
        mErrorMsgLong = null;
    }

    protected ErrorBase(String type, String code, String shortMsg, Exception e) {
        this(type);
        mErrorCode = code;
        mErrorMsgShort = shortMsg;
        mErrorMsgLong = e.getMessage();
    }

    protected ErrorBase(String type, String code, String shortMsg, String longMsg) {
        this(type);
        mErrorCode = code;
        mErrorMsgShort = shortMsg;
        mErrorMsgLong = longMsg;
    }

    protected ErrorBase(String type, String code, String msg) {
        this(type);
        mErrorCode = code;
        mErrorMsgShort = msg;
        mErrorMsgLong = msg;
    }

    public String getLongMessage() {
        return mErrorMsgLong;
    }

    public String getShortMessage() {
        return mErrorMsgShort;
    }

    public String getErrorCode() {
        return mErrorCode;
    }
}
