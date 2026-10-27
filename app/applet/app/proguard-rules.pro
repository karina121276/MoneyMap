# Room database rules
-keep class androidx.room.** { * }
-dontwarn androidx.room.**
-keep class com.example.data.entity.** { * }
-keep class com.example.data.dao.** { * }
-keep class com.example.data.database.** { * }

# Moshi serialization rules
-keep class com.squareup.moshi.** { * }
-dontwarn com.squareup.moshi.**
-keepattributes Signature, InnerClasses, EnclosingMethod, Retention, Target

# Retrofit rules
-keepattributes *Annotation*
-keep class com.squareup.retrofit2.** { * }

# Jetpack Compose and Kotlin serialization
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# App models
-keep class com.example.core.model.** { * }
