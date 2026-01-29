package org.haokee.recorder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.haokee.recorder.data.model.Thought

@Database(entities = [Thought::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ThoughtDatabase : RoomDatabase() {
    abstract fun thoughtDao(): ThoughtDao

    companion object {
        @Volatile
        private var INSTANCE: ThoughtDatabase? = null

        fun getDatabase(context: Context): ThoughtDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ThoughtDatabase::class.java,
                    "thought_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
