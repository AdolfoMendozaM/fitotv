# Modelos que se guardan como JSON
-keep class com.fitotv.update.UpdateInfo { *; }
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

# Decodificador de sonido FFmpeg: su parte nativa llama a estas clases por nombre.
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep class androidx.media3.decoder.SimpleDecoderOutputBuffer { *; }
-keep class androidx.media3.decoder.DecoderInputBuffer { *; }
