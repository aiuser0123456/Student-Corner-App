package com.studentcorner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadedPdfEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedPdfDao(): DownloadedPdfDao
}
