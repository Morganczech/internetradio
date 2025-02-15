# Spotify SDK
-keep class com.spotify.** { *; }
-keepclassmembers class com.spotify.** { *; }
-dontwarn com.spotify.**

# Last.fm
-keep class de.umass.** { *; }
-keepclassmembers class de.umass.** { *; }
-dontwarn de.umass.**

# JTransforms
-keep class edu.emory.mathcs.jtransforms.** { *; }
-keepclassmembers class edu.emory.mathcs.jtransforms.** { *; }
-dontwarn edu.emory.mathcs.jtransforms.**

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembers class retrofit2.** { *; }
-dontwarn retrofit2.**

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep your application classes
-keep class cz.internetradio.app.** { *; }
-keepclassmembers class cz.internetradio.app.** { *; } 