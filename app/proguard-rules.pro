-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.datecalc.logic.** { *; }
-keep class com.datecalc.billing.** { *; }

-keepclassmembers class * extends android.app.Activity {
    *** onCreate(...);
}

-keepclassmembers class * {
    *** dataStore(...);
}

-keep class ru.rustore.sdk.** { *; }
-keep class ru.ok.tracer.** { *; }
-dontwarn ru.rustore.sdk.**
-dontwarn ru.ok.tracer.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.datastore.**
