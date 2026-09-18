# Room rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# AndroidX Biometric
-keep class androidx.biometric.** { *; }

# Security Crypto
-keep class androidx.security.crypto.** { *; }
