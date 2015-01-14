package com.braintreepayments.api.models;

import android.content.Intent;
import android.test.AndroidTestCase;

import com.braintreepayments.testutils.FixturesHelper;

public class ThreeDSecureLookupTest extends AndroidTestCase {

    private ThreeDSecureLookup mLookup;

    @Override
    protected void setUp() throws Exception {
        mLookup = ThreeDSecureLookup.fromJson(FixturesHelper.stringFromFixture(getContext(),
                "three_d_secure/lookup_response.json"));
    }

    public void testCanInstantiateFromJsonString() {
        assertEquals("https://acs-url/", mLookup.getAcsUrl());
        assertEquals("merchant-descriptor", mLookup.getMd());
        assertEquals("https://term-url/", mLookup.getTermUrl());
        assertEquals("pareq", mLookup.getPareq());
    }

    public void testCanBeSerialized() {
        Intent intent = new Intent().putExtra("lookup", mLookup);
        ThreeDSecureLookup parsedLookup = intent.getParcelableExtra("lookup");

        assertEquals(mLookup.getAcsUrl(), parsedLookup.getAcsUrl());
        assertEquals(mLookup.getMd(), parsedLookup.getMd());
        assertEquals(mLookup.getTermUrl(), parsedLookup.getTermUrl());
        assertEquals(mLookup.getPareq(), parsedLookup.getPareq());
    }

}
