# Kotlinx Serialization — the plugin generates serializers referenced only reflectively via the
# @Serializable companion, so keep them for every serializable type in the app.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.github.worn.**$$serializer { *; }
-keepclassmembers class com.github.worn.** {
    *** Companion;
}

# Ktor + OkHttp engine
-dontwarn org.slf4j.**
-dontwarn kotlinx.coroutines.debug.**
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# SQLDelight / AndroidX SQLite
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# Koin resolves types by reflection through its DSL
-keep class com.github.worn.di.** { *; }

# ML Kit subject segmentation is loaded dynamically by Play services
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }
