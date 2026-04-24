# Keep Room and WorkManager generated implementation classes
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class * extends androidx.room.RoomDatabase

# General WorkManager rules (often bundled, but good for safety)
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**