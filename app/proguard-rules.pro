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

-dontwarn kotlinx.coroutines.**
-dontwarn androidx.datastore.**
