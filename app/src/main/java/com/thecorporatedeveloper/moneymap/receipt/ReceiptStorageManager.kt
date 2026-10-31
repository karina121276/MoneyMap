package com.thecorporatedeveloper.moneymap.receipt

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.thecorporatedeveloper.moneymap.data.entity.ReceiptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ReceiptStorageManager {

    private const val RECEIPTS_DIR = "receipts"
    private const val TEMP_DIR = "receipt_temp"

    fun getReceiptsDirectory(context: Context, transactionId: Long): File {
        val dir = File(context.filesDir, "$RECEIPTS_DIR/$transactionId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getTempDirectory(context: Context): File {
        val dir = File(context.cacheDir, TEMP_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createTempReceiptFile(context: Context): File {
        val dir = getTempDirectory(context)
        return File(dir, "temp_receipt_${UUID.randomUUID()}.jpg")
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    suspend fun copyUriToTempFile(context: Context, sourceUri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val tempFile = createTempReceiptFile(context)
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ReceiptStorage", "Error copying URI to temp file", e)
            null
        }
    }

    suspend fun rotateImageFile(file: File, degrees: Float): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || degrees % 360f == 0f) return@withContext true
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            FileOutputStream(file).use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated.recycle()
            true
        } catch (e: Exception) {
            android.util.Log.e("ReceiptStorage", "Error rotating image", e)
            false
        }
    }

    suspend fun cropImageFile(
        sourceFile: File,
        cropRectNormalized: RectF
    ): File? = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return@withContext null
            val imgWidth = bitmap.width
            val imgHeight = bitmap.height

            val left = (cropRectNormalized.left.coerceIn(0f, 1f) * imgWidth).toInt().coerceIn(0, imgWidth - 1)
            val top = (cropRectNormalized.top.coerceIn(0f, 1f) * imgHeight).toInt().coerceIn(0, imgHeight - 1)
            val right = (cropRectNormalized.right.coerceIn(0f, 1f) * imgWidth).toInt().coerceIn(left + 1, imgWidth)
            val bottom = (cropRectNormalized.bottom.coerceIn(0f, 1f) * imgHeight).toInt().coerceIn(top + 1, imgHeight)

            val cropWidth = (right - left).coerceAtLeast(1)
            val cropHeight = (bottom - top).coerceAtLeast(1)

            val cropped = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
            val outputFile = File(sourceFile.parentFile, "cropped_${UUID.randomUUID()}.jpg")

            FileOutputStream(outputFile).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            if (cropped != bitmap) {
                bitmap.recycle()
            }
            cropped.recycle()
            outputFile
        } catch (e: Exception) {
            android.util.Log.e("ReceiptStorage", "Error cropping image", e)
            null
        }
    }

    suspend fun enhanceDocumentImage(sourceFile: File): File? = enhanceReceiptImage(sourceFile)

    suspend fun enhanceReceiptImage(sourceFile: File): File? = withContext(Dispatchers.IO) {
        try {
            val original = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return@withContext null
            val enhanced = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(enhanced)
            val paint = Paint()

            // Increase contrast (1.3x) and slight brightness boost (+10) for crisp text legibility
            val contrast = 1.35f
            val brightness = 10f
            val cm = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(original, 0f, 0f, paint)

            val outputFile = File(sourceFile.parentFile, "enhanced_${UUID.randomUUID()}.jpg")
            FileOutputStream(outputFile).use { out ->
                enhanced.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            original.recycle()
            enhanced.recycle()
            outputFile
        } catch (e: Exception) {
            android.util.Log.e("ReceiptStorage", "Error enhancing image", e)
            null
        }
    }

    suspend fun saveConfirmedReceipt(
        context: Context,
        transactionId: Long,
        tempFile: File,
        originalFileName: String = "",
        displayOrder: Int = 0,
        isPrimary: Boolean = false
    ): ReceiptEntity = withContext(Dispatchers.IO) {
        val targetDir = getReceiptsDirectory(context, transactionId)
        val receiptUid = UUID.randomUUID().toString().replace("-", "")
        val permanentFile = File(targetDir, "receipt_$receiptUid.jpg")
        val thumbFile = File(targetDir, "thumb_$receiptUid.jpg")

        // Copy / compress temp file to permanent file
        tempFile.copyTo(permanentFile, overwrite = true)

        // Generate thumbnail (~360px)
        try {
            createThumbnail(permanentFile, thumbFile, maxDimension = 360)
        } catch (e: Exception) {
            android.util.Log.w("ReceiptStorage", "Thumbnail generation failed, fallback to main file", e)
        }

        val relativeFilePath = "$RECEIPTS_DIR/$transactionId/${permanentFile.name}"
        val relativeThumbPath = if (thumbFile.exists()) "$RECEIPTS_DIR/$transactionId/${thumbFile.name}" else relativeFilePath

        ReceiptEntity(
            transactionId = transactionId,
            localFilePath = relativeFilePath,
            thumbnailPath = relativeThumbPath,
            originalFileName = originalFileName.ifBlank { permanentFile.name },
            mimeType = "image/jpeg",
            fileSize = permanentFile.length(),
            displayOrder = displayOrder,
            isPrimary = isPrimary,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createThumbnail(sourceFile: File, thumbFile: File, maxDimension: Int) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, options)

        val srcWidth = options.outWidth
        val srcHeight = options.outHeight
        var sampleSize = 1
        while (srcWidth / sampleSize > maxDimension * 2 || srcHeight / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val sampledBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return

        val scale = minOf(
            maxDimension.toFloat() / sampledBitmap.width,
            maxDimension.toFloat() / sampledBitmap.height
        ).coerceAtMost(1f)

        val scaledWidth = (sampledBitmap.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (sampledBitmap.height * scale).toInt().coerceAtLeast(1)

        val thumbnailBitmap = Bitmap.createScaledBitmap(sampledBitmap, scaledWidth, scaledHeight, true)
        FileOutputStream(thumbFile).use { out ->
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        if (thumbnailBitmap != sampledBitmap) {
            sampledBitmap.recycle()
        }
        thumbnailBitmap.recycle()
    }

    fun getReceiptFile(context: Context, receipt: ReceiptEntity): File {
        return if (receipt.localFilePath.startsWith("/")) {
            File(receipt.localFilePath)
        } else {
            File(context.filesDir, receipt.localFilePath)
        }
    }

    fun getReceiptThumbnailFile(context: Context, receipt: ReceiptEntity): File {
        val path = receipt.thumbnailPath ?: receipt.localFilePath
        val file = if (path.startsWith("/")) File(path) else File(context.filesDir, path)
        return if (file.exists()) file else getReceiptFile(context, receipt)
    }

    fun getShareableUri(context: Context, receipt: ReceiptEntity): Uri {
        val file = getReceiptFile(context, receipt)
        return getFileProviderUri(context, file)
    }

    suspend fun deleteReceiptFiles(context: Context, receipt: ReceiptEntity): Boolean = withContext(Dispatchers.IO) {
        var success = true
        try {
            val mainFile = getReceiptFile(context, receipt)
            if (mainFile.exists()) {
                success = success && mainFile.delete()
            }
            receipt.thumbnailPath?.let { thumbPath ->
                val thumbFile = if (thumbPath.startsWith("/")) File(thumbPath) else File(context.filesDir, thumbPath)
                if (thumbFile.exists() && thumbFile != mainFile) {
                    thumbFile.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ReceiptStorage", "Error deleting receipt files", e)
            success = false
        }
        success
    }

    suspend fun deleteTransactionReceiptsFolder(context: Context, transactionId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "$RECEIPTS_DIR/$transactionId")
            if (dir.exists()) {
                dir.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("ReceiptStorage", "Error deleting transaction receipts folder", e)
            false
        }
    }

    suspend fun downloadReceiptToPublicStorage(
        context: Context,
        receipt: ReceiptEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val srcFile = getReceiptFile(context, receipt)
            if (!srcFile.exists()) {
                return@withContext Result.failure(java.io.FileNotFoundException("Receipt file does not exist"))
            }

            val fileName = "MoneyMap_Receipt_${receipt.transactionId}_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MoneyMap Receipts")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collection, values)
                    ?: return@withContext Result.failure(java.io.IOException("Failed to create MediaStore entry"))

                context.contentResolver.openOutputStream(itemUri)?.use { out ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)

                Result.success("Saved to Pictures/MoneyMap Receipts/$fileName")
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val targetDir = File(picturesDir, "MoneyMap Receipts").apply { if (!exists()) mkdirs() }
                val targetFile = File(targetDir, fileName)

                srcFile.copyTo(targetFile, overwrite = true)
                Result.success("Saved to ${targetFile.absolutePath}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ReceiptStorage", "Download failed", e)
            Result.failure(e)
        }
    }

    fun deleteTempFile(file: File?) {
        try {
            if (file != null && file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun deleteTempFiles(files: List<File>) {
        for (f in files) {
            deleteTempFile(f)
        }
    }

    suspend fun cleanupOrphanedTempFiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            val tempDir = getTempDirectory(context)
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            tempDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Non-critical cleanup
        }
    }
}
