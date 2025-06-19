# MANTIENI CLASSI ESSENZIALI
-keep class androidx.** { *; }

# Biometric API
-keep class androidx.biometric.** { *; }

# Room Entities
-keep class com.example.securenotes.model.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Jetpack Security Crypto
-keep class androidx.security.crypto.** { *; }

# Mantieni entry point (Application)
-keep class com.example.securenotes.SecureNotesApplication { *; }

# OFFUSCA TUTTO IL RESTO (default R8)


