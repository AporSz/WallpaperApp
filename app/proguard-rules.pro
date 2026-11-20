# Add project specific ProGuard rules here.

# Keep domain models and entities for Room and DataStore reflection/deserialization
-keep class com.example.wallpaperapp.domain.model.** { *; }
-keep class com.example.wallpaperapp.data.local.db.** { *; }

# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
