package com.braintreepayments.api;

import android.content.Intent;

import com.braintreepayments.api.internal.VisaCheckoutConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class VisaCheckoutFacade {
    static String sVisaCheckoutClassName = VisaCheckoutConstants.VISA_CHECKOUT_CLASSNAME;

    static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        try {
            Class<?> visaCheckoutClass = Class.forName(sVisaCheckoutClassName);
            Method onActivityResult = visaCheckoutClass.getDeclaredMethod("onActivityResult", BraintreeFragment.class,
                    int.class, Intent.class);
            onActivityResult.invoke(null, fragment, resultCode, data);
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        } catch (NoClassDefFoundError e) { }
    }
}
