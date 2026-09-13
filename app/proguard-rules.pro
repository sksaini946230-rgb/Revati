# AstroVeda ProGuard / R8 Rules

# General Rules
# LineNumberTable keeps crash traces readable once de-obfuscated with the
# mapping; renaming SourceFile removes the original filenames from the APK
# without losing anything Crashlytics needs.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,*Annotation*

# Data Models and Entities (Preserve reflection/serialization for Firestore, Room, Moshi)
-keep @androidx.room.Entity class *
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** {
    <fields>;
    public <init>();
}

# Firebase and Play Services
#
# There used to be two rules here:
#
#     -keep class com.google.firebase.** { *; }
#     -keep class com.google.android.gms.** { *; }
#
# Between them they held the two largest dependency trees in the app
# completely immune to R8. Play measured the result and said so on the
# release dashboard for versionCode 7: optimization 27%, obfuscation 28%,
# shrinking 28%. Roughly three quarters of the app was being shipped
# unshrunk and unobfuscated, and the mapping file came out at 84 MB, which
# is also why uploading it to Crashlytics kept failing.
#
# Neither rule was ever needed. Firebase and Play Services ship their own
# consumer ProGuard rules inside their AARs, and R8 applies those
# automatically — that is the designed mechanism, and a blanket -keep on
# top of it only defeats the optimiser. What genuinely needs keeping is
# this app's OWN classes that Firestore reaches by reflection, and those
# are kept below by name.
#
# Removed with a device in hand: sign-in, Firestore sync, Firebase AI and
# AdMob were all exercised on a real phone against the minified build.
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
    public <init>();
}
-dontwarn com.google.firebase.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.local.** { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}
-dontwarn androidx.room.**

# WorkManager Workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.worker.** { *; }

# Google Play Billing.
#
# This keep stays, alone among the blanket rules, for one reason: it is
# the only path here that cannot be tested. PRO is not purchasable until
# a Google Payments merchant account exists, so there is no way to walk a
# real purchase through a minified build and see it work. The billing
# library is small next to Play Services, so keeping it costs little, and
# a purchase that silently fails in production costs a great deal.
# Revisit when the merchant account is live and a purchase can be tested.
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# Google Play In-App Review
-keep class com.google.android.play.review.** { *; }
-dontwarn com.google.android.play.review.**

# Google Identity / Credentials (Google Sign-In). Both libraries carry
# their own rules; Google Sign-In was verified on a device after removing
# the blanket keeps.
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.**

# PDF report generation. android.graphics.pdf is framework, not bundled
# code — R8 never touches it, so the -keep that used to be here did
# nothing at all.
-dontwarn android.graphics.pdf.**

# Google Mobile Ads (AdMob)
# play-services-ads ships its own consumer rules; the blanket keeps that
# used to be here (including com.google.ads.**, a package that has not
# existed since the pre-Firebase SDK) only stopped R8 working. Banner and
# interstitial were both confirmed serving real ads on a device after this.
-dontwarn com.google.android.gms.ads.**

# Kotlin Serialization / Coroutines
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-dontwarn kotlinx.coroutines.**

# --- Security & Privacy Hardening ---
# Strip Android debug and verbose logging in release builds (prevents sensitive PII leaks)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Preserve SecurityUtils and Billing integrity
-keep class com.example.util.SecurityUtils { *; }
-keepclassmembers class com.example.util.SecurityUtils {
    public static <methods>;
}

# JVM & Third-Party Library Warnings Suppression
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
-dontwarn com.google.android.gms.internal.**


# User Messaging Platform (ad consent). play-services-ads' rules cover
# com.google.android.gms.ads.** but UMP lives in its own package.
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

# Firebase AI Logic serialises its request/response models with
# kotlinx.serialization; keep the generated serializers.
-keepclassmembers class com.google.firebase.ai.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.google.firebase.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SQLCipher's classes are reached from its native library through JNI, so R8
# cannot see them used. The project's own documentation asks for this keep.
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
