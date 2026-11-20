# Add project specific ProGuard rules here.

# Preserve line numbers and source file attributes for crash reporting stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep domain models and Room database entities
-keep class com.example.wallpaperapp.domain.model.** { *; }
-keep class com.example.wallpaperapp.data.local.db.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep WorkManager Worker constructor reflection
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# Keep graphics & image transformation classes
-keep class com.example.wallpaperapp.graphics.** { *; }
