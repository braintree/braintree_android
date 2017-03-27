## Only Required for Demo App

# Retrofit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Mailable_Log
-dontwarn ch.qos.logback.core.net.**

# Visa Checkout
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
# to keep all activity classes
-keep public class * extends android.app.Activity
-dontwarn com.visa.**
-dontwarn com.google.gson.**
-dontwarn com.threatmetrix.**
-dontwarn com.google.**
#threatmetrix support library
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.visa.** { *; }
-keep class com.threatmetrix.** { *; }
-keep class okio.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
# For Eventbus
-keep class de.greenrobot.** { *; }
# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keepattributes Signature
-keepattributes Exceptions
# For Gson
# For using GSON @Expose annotation
-keepattributes *Annotation*
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
  }
-keepclassmembers class * {
public void onEvent(...);
public void onEventMainThread(...);
public void onEventAsync(...);
}
