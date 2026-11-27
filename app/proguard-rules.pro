# Modelos que se guardan como JSON
-keep class com.fitotv.data.** { *; }
-keepclassmembers class com.fitotv.data.** { *** Companion; }

# NanoHTTPD (servidor para agregar listas desde el celular)
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
