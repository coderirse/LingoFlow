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
