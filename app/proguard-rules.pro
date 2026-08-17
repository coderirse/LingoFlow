# ============================================
# LingoFlow R8 ProGuard Rules — conservative strategy
# Better a larger APK than broken functionality.
# ============================================

# ---- Base attribute preservation ----
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions, Deprecated, SourceFile, LineNumberTable, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# ---- Hilt / Dagger (dependency injection core) ----
-keep class dagger.hilt.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# ---- Kotlinx Serialization (JSON parsing core; field names must survive) ----
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Required <fields>;
    @kotlinx.serialization.Transient <fields>;
}

# ---- Jetpack Compose (UI rendering core) ----
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**

# ---- ML Kit (on-device translation core) ----
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit.**

# ---- OkHttp / Okio (network layer core) ----
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- AndroidX family (Navigation, DataStore, Security, Lifecycle, Core) ----
-keep class androidx.datastore.** { *; }
-keep class androidx.security.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.core.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.fragment.** { *; }
-dontwarn androidx.**

# ---- Material 3 ----
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ---- App's own domain / data / ui layers (sealed classes, data classes,
#      enums, ViewModels used for JSON parsing and UI state) ----
-keep class **.domain.** { *; }
-keepclassmembers class **.domain.** { <init>(...); }
-keep class **.data.** { *; }
-keepclassmembers class **.data.** { <init>(...); }
-keep class **.ui.** { *; }

# ---- Sealed-class subclasses (TranslationResponse.Standard / Learning, ...) ----
-keep class **$* { *; }

# ---- Enums (TranslationMode, LlmProviderId, ...) ----
-keepclassmembers enum ** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Interfaces and their implementations (TranslationEngine, LlmProvider,
#      Repository, ...) ----
-keep interface **.domain.** { *; }
-keep class * implements **.domain.** { *; }
