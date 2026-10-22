package xyz.teccheck.alphacompanion.db

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [SonyCamera::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sonyCameraDao(): SonyCameraDao
}
