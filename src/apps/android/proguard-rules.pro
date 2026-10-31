# Room's generated database is found by name.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class com.hubidubicash.data.CashDatabase_Impl { *; }
# Line numbers in a crash report, under the shortened names.
-keepattributes SourceFile,LineNumberTable
