package com.thecorporatedeveloper.moneymap.core.util

import android.content.Context
import android.database.sqlite.SQLiteFullException
import java.io.IOException
import java.nio.file.FileSystemException

object StorageSafety {
    fun isStorageOrDatabaseError(e: Throwable): Boolean {
        return e is SQLiteFullException ||
               e is IOException ||
               e is FileSystemException ||
               (e.message?.contains("disk full", ignoreCase = true) == true) ||
               (e.message?.contains("sqlite full", ignoreCase = true) == true) ||
               (e.message?.contains("no space left", ignoreCase = true) == true)
    }

    fun isLowStorage(context: Context): Boolean {
        return try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes < 50 * 1024 * 1024 // less than 50 MB
        } catch (e: Exception) {
            false
        }
    }
}
