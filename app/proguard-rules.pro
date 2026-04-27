# Keep kotlinx.serialization
-keep,includedescriptorclasses class com.wardcompanion.**$$serializer { *; }
-keepclassmembers class com.wardcompanion.** {
    *** Companion;
}
-keepclasseswithmembers class com.wardcompanion.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Socket.IO uses reflection
-keep class io.socket.** { *; }
