package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.WorkflowEntity
import com.example.data.model.WorkflowStepEntity

@Database(
    entities = [WorkflowEntity::class, WorkflowStepEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StepwiseDatabase : RoomDatabase() {
    abstract fun stepwiseDao(): StepwiseDao

    companion object {
        @Volatile
        private var INSTANCE: StepwiseDatabase? = null

        fun getDatabase(context: Context): StepwiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StepwiseDatabase::class.java,
                    "stepwise_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
