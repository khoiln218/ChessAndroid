# Rules added to the ones in proguard-android-optimize.txt, which build.gradle names alongside
# this file. Everything the app itself does is plain Java the shrinker can follow; the entries
# below are all about one library that arrives underneath the ads SDK.

# WorkManager keeps its state in a Room database, and Room reaches its generated implementation
# by name - Class.forName("...WorkDatabase_Impl") - which R8 has no way to see. Renamed or
# removed, the first launch of a release build dies on "Failed to create an instance of
# androidx.work.impl.WorkDatabase" before any activity starts.
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
}
