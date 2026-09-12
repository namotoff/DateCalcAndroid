-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.datecalc.logic.** { *; }
-keep class com.datecalc.billing.** { *; }
-keep class com.datecalc.widget.** { *; }
-keep class com.datecalc.MainActivity { *; }
-keep class com.datecalc.AppScreenKt { *; }

-keepclassmembers class * extends android.app.Activity {
    *** onCreate(...);
}

-keepclassmembers class * {
    *** dataStore(...);
}

-keep class ru.rustore.sdk.** { *; }
-keep class ru.ok.tracer.** { *; }
-keep class androidx.glance.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn ru.rustore.sdk.**
-dontwarn ru.ok.tracer.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.datastore.**
-dontwarn androidx.glance.**
