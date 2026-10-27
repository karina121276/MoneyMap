package com.thecorporatedeveloper.moneymap.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class OcrResult {
    data class Success(
        val rawText: String,
        val extracted: ExtractedTransaction,
        val cachedImageUri: Uri?
    ) : OcrResult()

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val cachedImageUri: Uri? = null
    ) : OcrResult()
}

object OcrProcessor {

    private const val TAG = "OcrProcessor"

    /**
     * Copies a shared Content URI to app cache so it is safely retained across lifecycle events
     * and permission changes, then decodes and runs local ML Kit OCR.
     */
    suspend fun processImageUri(
        context: Context,
        uri: Uri,
        accounts: List<AccountEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList()
    ): OcrResult = withContext(Dispatchers.IO) {
        try {
            // 1. Verify content type
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            if (mimeType.isNotEmpty() && !mimeType.startsWith("image/")) {
                return@withContext OcrResult.Error(
                    message = "The shared content is not a supported image file (type: $mimeType). Expected a PNG, JPEG, or WEBP screenshot.",
                    canRetry = false
                )
            }

            // 2. Safely copy the shared stream into a private cache file
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val cacheFile = File(context.cacheDir, "shared_screenshot_${System.currentTimeMillis()}.$extension")

            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext OcrResult.Error(
                    message = "Could not open the shared image. The sharing application may not have granted temporary read permissions."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error copying shared URI", e)
                return@withContext OcrResult.Error(
                    message = "Failed to read the shared image: ${e.localizedMessage ?: "Unknown I/O error"}"
                )
            }

            val cachedUri = Uri.fromFile(cacheFile)

            // 3. Decode bitmap with bounds check
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(cacheFile.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext OcrResult.Error(
                    message = "The shared image contains no valid picture data or is corrupted.",
                    cachedImageUri = cachedUri
                )
            }

            // Calculate sample size if very large (e.g., > 2048px)
            var sampleSize = 1
            while (options.outWidth / sampleSize > 2048 || options.outHeight / sampleSize > 2048) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath, decodeOptions)
                ?: return@withContext OcrResult.Error(
                    message = "Unable to decode bitmap from the shared screenshot.",
                    cachedImageUri = cachedUri
                )

            // 4. Run ML Kit Text Recognition
            val rawText = try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = Tasks.await(recognizer.process(inputImage), 15, TimeUnit.SECONDS)
                visionText.text
            } catch (t: Throwable) {
                Log.w(TAG, "ML Kit text recognition failed or unavailable: ${t.message}", t)
                ""
            }

            // 5. Validate text content
            if (rawText.isBlank() || rawText.none { it.isLetterOrDigit() }) {
                return@withContext OcrResult.Error(
                    message = "No readable transaction text was found in this screenshot. Please check that the receipt is clear, uncropped, and not blurry.",
                    canRetry = true,
                    cachedImageUri = cachedUri
                )
            }

            // 6. Extract fields using enhanced SmartEntryParser
            val extracted = SmartEntryParser.parseText(rawText, accounts, categories)

            // Check if amount is missing
            if (!extracted.isAmountDetected || extracted.amountPaise.value <= 0) {
                return@withContext OcrResult.Success(
                    rawText = rawText,
                    extracted = extracted,
                    cachedImageUri = cachedUri
                )
            }

            OcrResult.Success(
                rawText = rawText,
                extracted = extracted,
                cachedImageUri = cachedUri
            )

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error processing shared screenshot", e)
            OcrResult.Error(
                message = "An error occurred while processing the screenshot: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
