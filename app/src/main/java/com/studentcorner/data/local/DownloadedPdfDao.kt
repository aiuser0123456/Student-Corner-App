package com.studentcorner.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "downloaded_pdfs")
data class DownloadedPdfEntity(
    @PrimaryKey val resourceId: String,
    val title: String,
    val subject: String,
    val filePath: String,          // internal files-dir path, not accessible outside app
    val downloadedAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
)

@Dao
interface DownloadedPdfDao {
    @Query("SELECT * FROM downloaded_pdfs ORDER BY downloadedAt DESC")
    fun getAllFlow(): Flow<List<DownloadedPdfEntity>>

    @Query("SELECT * FROM downloaded_pdfs ORDER BY downloadedAt DESC")
    suspend fun getAll(): List<DownloadedPdfEntity>

    @Query("SELECT * FROM downloaded_pdfs WHERE resourceId = :id")
    suspend fun getById(id: String): DownloadedPdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pdf: DownloadedPdfEntity)

    @Query("DELETE FROM downloaded_pdfs WHERE resourceId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM downloaded_pdfs WHERE resourceId = :id")
    suspend fun deleteById(id: String)
}
