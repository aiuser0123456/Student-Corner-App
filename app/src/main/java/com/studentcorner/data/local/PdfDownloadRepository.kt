package com.studentcorner.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadedPdfDao,
    private val okHttpClient: OkHttpClient,
) {
    private val pdfDir: File
        get() = File(context.filesDir, "pdfs").also { it.mkdirs() }

    val allDownloads: Flow<List<DownloadedPdfEntity>> = dao.getAllFlow()

    suspend fun isDownloaded(resourceId: String): Boolean = dao.getById(resourceId) != null

    /** Stream PDF bytes directly from URL into private storage — URL never leaves the app */
    suspend fun downloadPdf(
        resourceId: String,
        title: String,
        subject: String,
        url: String,
        onProgress: (Int) -> Unit = {},
    ): kotlin.Result<File> = withContext(Dispatchers.IO) {
        try {
            val dest = File(pdfDir, "$resourceId.pdf")
            val req = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "StudentCorner-Android/1.0")
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext kotlin.Result.failure(Exception("HTTP ${resp.code}"))
            val body = resp.body ?: return@withContext kotlin.Result.failure(Exception("Empty response body"))
            val total = body.contentLength()
            var downloaded = 0L
            dest.outputStream().use { out ->
                body.byteStream().use { inp ->
                    val buf = ByteArray(8192)
                    var n: Int
                    while (inp.read(buf).also { n = it } != -1) {
                        out.write(buf, 0, n)
                        downloaded += n
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                    }
                }
            }
            dao.insert(DownloadedPdfEntity(
                resourceId    = resourceId,
                title         = title,
                subject       = subject,
                filePath      = dest.absolutePath,
                fileSizeBytes = dest.length(),
            ))
            kotlin.Result.success(dest)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    fun getLocalFile(resourceId: String): File = File(pdfDir, "$resourceId.pdf")

    suspend fun deletePdfs(ids: List<String>) = withContext(Dispatchers.IO) {
        ids.forEach { File(pdfDir, "$it.pdf").delete() }
        dao.deleteByIds(ids)
    }

    suspend fun getAll() = dao.getAll()
}
