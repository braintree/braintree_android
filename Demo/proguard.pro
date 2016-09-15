## Only Required for Demo App

# Retrofit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Mailable_Log
-dontwarn ch.qos.logback.core.net.**

# Visa Checkout
-keepattributes EnclosingMethod
-dontwarn android.support.**
-dontwarn javax.**
-dontwarn io.card.payment.**
-dontwarn com.squareup.**
-dontwarn retrofit.**
-dontwarn okio.**
-dontwarn com.google.gson.**
-dontwarn com.samsung.**
-dontwarn com.threatmetrix.**
-dontwarn com.google.appengine.**
-dontwarn rx.*
-dontwarn java.nio.**
-dontwarn org.apache.**
-dontwarn org.codehaus.**
-dontwarn com.visa.**
# Visa checkout
-keep class com.visa.** { *; }
# APIGuard.
#-keep class com.apiguard.** { *; }
# TrustDefender
-keep class com.threatmetrix.** { *; }
-keep class android.support.** { *; }
#GreenRobot Event Bus
-keep class de.greenrobot.** { *; }
#Card IO
-keep class io.card.**
-keepclassmembers class io.card.** { *;}
#Retrofit
-keep class retrofit.** { *; }
-keep class com.squareup.** { *; }
-keep class okio.** { *; }
-keep class com.google.gson.** { *; }
#Samsung Pass SDK
-keep class com.samsung.** {*;}