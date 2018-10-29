package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.Json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Use {@link com.braintreepayments.api.LocalPayment}
 *
 * Class that contains details about an iDEAL issuing bank.
 */
@Deprecated
public class IdealBank implements Parcelable {

    private static final String DATA_KEY = "data";
    private static final String COUNTRY_CODE_KEY = "country_code";
    private static final String ISSUERS_KEY = "issuers";
    private static final String ID_KEY = "id";
    private static final String NAME_KEY = "name";
    private static final String IMAGE_FILE_NAME_KEY = "image_file_name";
    private static final String ASSETS_URL_PATH = "web/static/images/ideal_issuer-logo_";

    private String mCountryCode;
    private String mId;
    private String mName;
    private String mImageUri;

    /**
     * Convert an API response to a list of {@link IdealBank}.
     *
     * @param configuration {@link Configuration} corresponding to this merchant.
     * @param json Raw JSON response from Braintree of a {@link IdealBank} list.
     * @return {@link List<IdealBank>}.
     * @throws JSONException when parsing the response fails.
     */
    public static List<IdealBank> fromJson(Configuration configuration, String json) throws JSONException {
        List<IdealBank> result = new ArrayList<>();

        if (json == null) {
            return result;
        }

        JSONObject jsonObj = new JSONObject(json);

        JSONArray data = jsonObj.getJSONArray(DATA_KEY);
        for (int i = 0; i < data.length(); i++) {
            JSONObject banksJson = data.getJSONObject(i);
            String countryCode = banksJson.optString(COUNTRY_CODE_KEY);
            JSONArray issuers = banksJson.getJSONArray(ISSUERS_KEY);
            for (int j = 0; j < issuers.length(); j++) {
                JSONObject issuer = issuers.getJSONObject(j);

                String id = Json.optString(issuer, ID_KEY, "");
                String name = Json.optString(issuer, NAME_KEY, "");
                String imageName = Json.optString(issuer, IMAGE_FILE_NAME_KEY, "");
                String imageUri = configuration.getAssetsUrl() + "/" + ASSETS_URL_PATH + imageName;

                result.add(new IdealBank(countryCode, id, name, imageUri));
            }
        }

        return result;
    }

    private IdealBank(String countryCode, String id, String name, String imageUri) {
        mCountryCode = countryCode;
        mId = id;
        mName = name;
        mImageUri = imageUri;
    }

    /**
     * @return the ID of this issuing bank.
     */
    public String getId() {
        return mId;
    }

    /**
     * @return the name of this issuing bank.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the image uri of an icon for this issuing bank.
     */
    public String getImageUri() {
        return mImageUri;
    }

    /**
     * @return the country code of the country in which this issuing bank resides.
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCountryCode);
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeString(mImageUri);
    }

    protected IdealBank(Parcel in) {
        mCountryCode = in.readString();
        mId = in.readString();
        mName = in.readString();
        mImageUri = in.readString();
    }

    public static final Creator<IdealBank> CREATOR = new Creator<IdealBank>() {
        @Override
        public IdealBank createFromParcel(Parcel in) {
            return new IdealBank(in);
        }

        @Override
        public IdealBank[] newArray(int size) {
            return new IdealBank[size];
        }
    };
}