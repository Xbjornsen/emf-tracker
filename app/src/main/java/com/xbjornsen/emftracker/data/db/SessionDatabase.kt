package com.xbjornsen.emftracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: SessionDatabase? = null

        fun getInstance(context: Context): SessionDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    "emf_tracker.db"
                ).build().also { instance = it }
            }
    }
}
