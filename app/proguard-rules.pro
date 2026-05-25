-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.datecalc.logic.** { *; }

-keep class com.my.target.** { *; }

-keepclassmembers class * extends android.app.Activity {
    *** onCreate(...);
}
