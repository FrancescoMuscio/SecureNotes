# MANTIENI CLASSI ESSENZIALI

# AndroidX (necessario per Jetpack)
-keep class androidx.** { *; }

# Biometric API
-keep class androidx.biometric.** { *; }

# Room Entities (se usi Room)
-keep class com.example.securenotes.model.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Jetpack Security Crypto
-keep class androidx.security.crypto.** { *; }

# OFFUSCA TUTTO IL RESTO (default R8)

