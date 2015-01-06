# Card Form

Card Form is a ready made card form layout that can be included in your app making it easy to
accept credit and debit cards.

## Adding It To Your Project

In your `build.gradle`:

```groovy
dependencies {
    compile project('com.braintreepayments:card-form:1.+')
}
```

## Usage

Card Form is a LinearLayout that you can add to your layout:

```xml
<com.braintreepayments.cardform.view.CardForm
    android:id="@+id/bt_card_form"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

To change which fields are required for the user to enter, use
`CardForm#setRequiredFields(boolean cardNumberRequired, boolean expirationRequired, boolean cvvRequired, boolean postalCodeRequired, String imeActionLabel)`.
By default all fields are required.

```java
CardForm cardForm = (CardForm) findViewById(R.id.bt_card_form);
cardForm.setRequiredFields(true, true, false, false, "Purchase");
```

During rotation `CardForm#onSaveInstanceState(Bundle outState)` should be called with the `Bundle`
received in `Activity#onSaveInstanceState`. Likewise during creation `CardForm#onRestoreInstanceState(Bundle savedInstanceState)`
should be called with the `Bundle` received in `Activity#onCreate`.

To access the values in the form, there are getters for each field:

```java
cardForm.getCardNumber();
cardForm.getExpirationMonth();
cardForm.getExpirationYear();
cardForm.getCvv();
cardForm.getPostalCode();
```

To check if `CardForm` is valid simply call `CardForm#isValid()`. To validate each required field
and show the user which fields are incorrect, call `CardForm#validate()`. There are also setters
for each field that allow you to set if they are valid or not and display an error to the user.

Additionally `CardForm` has 3 available listeners that can be set on it.

* `CardForm#setOnCardFormValidListener` called when the form changes state from valid to invalid or invalid to valid.
* `CardForm#setOnCardFormSubmitListener` called when the form should be submitted.
* `CardForm#setOnFormFieldFocusedListener` called when a field in the form is focused.

## License

Card Form is open source and available under the MIT license. See the [LICENSE](LICENSE) file for more info.
