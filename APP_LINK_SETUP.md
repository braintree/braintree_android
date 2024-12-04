## Set Up App Links

In order to use the PayPal flow your application must be set up for [App Links](https://developer.android.com/training/app-links).

### Register App Link in Control Panel

Before using this feature, you must register your App Link domain in the Braintree Control Panel:

1. Log into your Control Panel (e.g. [Sandbox](https://sandbox.braintreegateway.com/login), or [Production](https://www.braintreegateway.com/login)).
2. Click on the **gear icon** in the top right corner. A drop-down menu will open.
3. Select **Account Settings** from the drop-down menu.
4. In the **Processing Options** tab, go to **Payment Methods** section.
5. Next to **PayPal**, click the **Link Sandbox** link. This will give you option to link your Braintree and PayPal accounts.
   - If your accounts are already linked, you'd see an **Options** button instead.
6. Click the **View Domain Names** button. This will take you to the **PayPal Domain Names** page. 
   - Note: If you have a single PayPal account, it will be at the bottom of the page. If you have multiple PayPal accounts, it will be at the top right of the page.
7. Click the **+ Add** link on the top right of the page or scroll to the **Specify Your Domain Names** section.
8. In the text box enter your list of domain names separated by commas. 
   - Note: The value you enter must match your fully qualified domain name exactly – including the "www." if applicable.
9. Click the **Add Domain Names** button.
10. If the domain registration was successful for all the domain names listed in the text box, a banner will display the text "Successfully added domains". The registered domain names will be displayed in alphabetical order under the **+ Add** button.
11. If the registration was not successful for any of the domain names listed in the text box, a banner will display a list of domain names that failed qualified domain name validation along with their reasons for rejection. Any domain names that were successfully registered will be displayed in alphabetical order under the **+ Add** button. 
    - Note: You can re-enter the rejected domain names in the text area with the corrections applied.

### Set App Link in SDK

Pass your App Link to the `PayPalClient` constructor:

```kotlin
val payPalClient =  PayPalClient(
  this, 
  "<#CLIENT_AUTHORIZATION#>",
  Uri.parse("https://demo-app.com/braintree-payments") // Merchant App Link
)
```