package com.vp.whattowatch.data.local.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.min

class ImageStorageManager(private val context: Context) {

    val postersDir: File = File(context.filesDir, DIR_POSTERS).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    companion object {
        const val DIR_POSTERS = "posters"
        const val MAX_POSTER_WIDTH = 720
        const val MAX_POSTER_HEIGHT = 1080
        const val JPEG_QUALITY = 85

        fun getPosterFile(context: Context, relativeFilename: String): File {
            return File(File(context.filesDir, DIR_POSTERS), relativeFilename)
        }
    }

    fun getPosterFile(relativeFilename: String): File {
        return File(postersDir, relativeFilename)
    }

    fun posterExists(relativeFilename: String?): Boolean {
        if (relativeFilename.isNullOrBlank()) return false
        val file = getPosterFile(relativeFilename)
        return file.exists() && file.isFile && file.length() > 0
    }

    fun getAllStoredPosterFilenames(): List<String> {
        return postersDir.listFiles { file -> file.isFile && file.length() > 0 }
            ?.map { it.name }
            ?: emptyList()
    }

    suspend fun saveImageFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            saveImageFromStreamInternal(stream)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveImageFromStream(inputStream: InputStream): String? = withContext(Dispatchers.IO) {
        saveImageFromStreamInternal(inputStream)
    }

    suspend fun copyPosterFile(sourceFile: File): String? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() <= 0) return@withContext null
        try {
            sourceFile.inputStream().use { stream ->
                saveImageFromStreamInternal(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveImageFromStreamInternal(inputStream: InputStream): String? {
        val tempRawFile = File.createTempFile("raw_picker_", ".tmp", context.cacheDir)
        try {
            // Step 1: Copy stream to a temporary cache file to avoid multiple network/content provider queries
            inputStream.use { input ->
                tempRawFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (tempRawFile.length() <= 0 || !isValidImageHeader(tempRawFile)) return null

            // Step 2: Decode bounds to calculate sample size without loading full bitmap into RAM
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(tempRawFile.absolutePath, boundsOptions)

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                return null
            }

            // Step 3: Calculate inSampleSize to downscale efficiently
            var inSampleSize = 1
            while (rawWidth / inSampleSize > MAX_POSTER_WIDTH * 1.5 || rawHeight / inSampleSize > MAX_POSTER_HEIGHT * 1.5) {
                inSampleSize *= 2
            }

            // Step 4: Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val sampledBitmap = BitmapFactory.decodeFile(tempRawFile.absolutePath, decodeOptions) ?: return null

            // Step 5: Scale to exact target bounds if still larger than max dimensions
            val finalBitmap = scaleBitmapPreservingAspect(sampledBitmap, MAX_POSTER_WIDTH, MAX_POSTER_HEIGHT)

            // Step 6: Write to temporary file in posters dir, then atomically rename
            val relativeFilename = "poster_${UUID.randomUUID()}.jpg"
            val targetFile = File(postersDir, relativeFilename)
            val tempWriteFile = File(postersDir, "temp_${UUID.randomUUID()}.tmp")

            FileOutputStream(tempWriteFile).use { out ->
                val compressed = finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
                if (!compressed) {
                    tempWriteFile.delete()
                    return null
                }
            }

            if (finalBitmap != sampledBitmap) {
                finalBitmap.recycle()
            }
            sampledBitmap.recycle()

            if (!tempWriteFile.renameTo(targetFile)) {
                // Fallback copy if renameTo fails
                tempWriteFile.copyTo(targetFile, overwrite = true)
                tempWriteFile.delete()
            }

            return relativeFilename
        } catch (_: Exception) {
            return null
        } finally {
            if (tempRawFile.exists()) {
                tempRawFile.delete()
            }
        }
    }

    private fun scaleBitmapPreservingAspect(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxWidth && height <= maxHeight) {
            return source
        }

        val scale = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    fun deletePoster(relativeFilename: String): Boolean {
        if (relativeFilename.isBlank()) return false
        val file = getPosterFile(relativeFilename)
        return if (file.exists() && file.isFile) {
            file.delete()
        } else {
            false
        }
    }

    suspend fun deletePosterSafely(relativeFilename: String, contentDao: com.vp.whattowatch.data.local.dao.ContentDao): Boolean {
        if (relativeFilename.isBlank()) return false
        val remainingCount = contentDao.countByPosterPath(relativeFilename)
        return if (remainingCount <= 0) {
            deletePoster(relativeFilename)
        } else {
            false
        }
    }

    private fun isValidImageHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 12) return false
        val header = ByteArray(12)
        try {
            file.inputStream().use { it.read(header) }
        } catch (_: Exception) {
            return false
        }

        // JPEG: FF D8 FF
        val isJpeg = (header[0].toInt() and 0xFF == 0xFF) &&
                (header[1].toInt() and 0xFF == 0xD8) &&
                (header[2].toInt() and 0xFF == 0xFF)

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        val isPng = (header[0].toInt() and 0xFF == 0x89) &&
                (header[1] == 'P'.code.toByte()) &&
                (header[2] == 'N'.code.toByte()) &&
                (header[3] == 'G'.code.toByte())

        // WebP: "RIFF" .... "WEBP"
        val isWebP = (header[0] == 'R'.code.toByte()) &&
                (header[1] == 'I'.code.toByte()) &&
                (header[2] == 'F'.code.toByte()) &&
                (header[3] == 'F'.code.toByte()) &&
                (header[8] == 'W'.code.toByte()) &&
                (header[9] == 'E'.code.toByte()) &&
                (header[10] == 'B'.code.toByte()) &&
                (header[11] == 'P'.code.toByte())

        return isJpeg || isPng || isWebP
    }
}
