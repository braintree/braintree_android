package com.paypal.android.sdk.onetouch.core.config;

import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The configuration loaded from default or stored in preferences.
 */
public class OtcConfiguration {

    private final ArrayList<OAuth2Recipe> mOauth2RecipesInDecreasingPriorityOrder =
            new ArrayList<>();
    private final ArrayList<CheckoutRecipe> mCheckoutRecipesInDecreasingPriorityOrder =
            new ArrayList<>();
    private final ArrayList<BillingAgreementRecipe>
            mBillingAgreementRecipesInDecreasingPriorityOrder = new ArrayList<>();
    private String mFileTimestamp;

    public OtcConfiguration withOauth2Recipe(OAuth2Recipe recipe) {
        mOauth2RecipesInDecreasingPriorityOrder.add(recipe);
        return this;
    }

    public OtcConfiguration fileTimestamp(String fileTimestamp) {
        mFileTimestamp = fileTimestamp;
        return this;
    }

    public String getFileTimestamp() {
        return mFileTimestamp;
    }

    /**
     * Returns the browser recipe that can handle these scopes, or null if there is none.
     *
     * @param scopes
     * @return
     */
    public OAuth2Recipe getBrowserOauth2Config(Set<String> scopes) {
        for (OAuth2Recipe recipe : mOauth2RecipesInDecreasingPriorityOrder) {
            if (recipe.getTarget() == RequestTarget.browser
                    && recipe.isValidForScopes(scopes)) {
                return recipe;
            }
        }

        return null;
    }

    /**
     * Returns the browser recipe that can handle checkout, or null if there is none.
     *
     * @return
     */
    public CheckoutRecipe getBrowserCheckoutConfig() {
        for (CheckoutRecipe recipe : mCheckoutRecipesInDecreasingPriorityOrder) {
            if (recipe.getTarget() == RequestTarget.browser) {
                return recipe;
            }
        }

        return null;
    }

    /**
     * Returns the browser recipe that can handle billing agreement, or null if there is none.
     *
     * @return
     */
    public BillingAgreementRecipe getBrowserBillingAgreementConfig() {
        for (BillingAgreementRecipe recipe : mBillingAgreementRecipesInDecreasingPriorityOrder) {
            if (recipe.getTarget() == RequestTarget.browser) {
                return recipe;
            }
        }

        return null;
    }

    public List<OAuth2Recipe> getOauth2Recipes() {
        return new ArrayList<>(mOauth2RecipesInDecreasingPriorityOrder);
    }

    public void withCheckoutRecipe(CheckoutRecipe recipe) {
        mCheckoutRecipesInDecreasingPriorityOrder.add(recipe);
    }

    public List<CheckoutRecipe> getCheckoutRecipes() {
        return new ArrayList<>(mCheckoutRecipesInDecreasingPriorityOrder);
    }

    public void withBillingAgreementRecipe(BillingAgreementRecipe recipe) {
        mBillingAgreementRecipesInDecreasingPriorityOrder.add(recipe);
    }

    public List<BillingAgreementRecipe> getBillingAgreementRecipes() {
        return new ArrayList<>(mBillingAgreementRecipesInDecreasingPriorityOrder);
    }
}
