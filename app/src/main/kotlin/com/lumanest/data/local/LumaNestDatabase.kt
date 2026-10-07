package com.lumanest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class, FriendEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LumaNestDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun friendDao(): FriendDao

    companion object {
        @Volatile
        private var INSTANCE: LumaNestDatabase? = null

        fun getInstance(context: Context): LumaNestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LumaNestDatabase::class.java,
                    "lumanest.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
