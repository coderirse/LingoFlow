# LingoFlow R8 rules. OkHttp, ML Kit, Hilt and Compose ship their own
# consumer rules; only kotlinx.serialization needs explicit configuration.

# --- kotlinx.serialization (official rules) ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.lingoflow.app.**$$serializer { *; }
-keepclassmembers class com.lingoflow.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.lingoflow.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- ML Kit / Google Play services internals ---
# R8 full mode strips the lazily-initialized holder classes ML Kit's
# language-id/translate clients resolve reflectively, which NPEs on first
# injection (observed: LanguageIdentifierImpl$Factory null in
# MlKitTranslator.<init>). Keep the two API packages and their gms
# plumbing whole; the heavy parts are the bundled .so models, not these.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.firebase.components.** { *; }

# --- DataStore (bundled protobuf-lite) ---
# The preferences artifact embeds a protobuf-lite runtime that resolves the
# generated proto fields BY NAME via reflection ("Field preferences_ not
# found" once renamed). Keep the proto runtime and generated messages whole.
-keep class androidx.datastore.preferences.protobuf.** { *; }
-keep class androidx.datastore.preferences.PreferencesProto* { *; }
-keepclassmembers class androidx.datastore.preferences.core.** { *; }
