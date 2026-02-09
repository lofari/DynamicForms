# --- Debug info ---
-keepattributes SourceFile,LineNumberTable

# --- kotlinx.serialization ---
# Keep serializer classes and companion objects for polymorphic deserialization
-keepclassmembers class com.lfr.dynamicforms.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.lfr.dynamicforms.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lfr.dynamicforms.domain.model.**$$serializer { *; }
-keepnames class com.lfr.dynamicforms.domain.model.** { *; }

# Keep @SerialName annotations (needed for classDiscriminator)
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations

# --- Retrofit ---
-keep interface com.lfr.dynamicforms.data.remote.FormApi { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class * { *; }

# --- WorkManager ---
-keep class * extends androidx.work.ListenableWorker

# --- Hilt ---
# Hilt generates keep rules automatically via ksp, but keep entry points
-keep class com.lfr.dynamicforms.DynamicFormsApp
