package com.example.wallpaperapp.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FontStorageManager(private val context: Context) {

    fun saveCustomFont(uri: Uri): String? {
        return try {
            val fontsDir = File(context.filesDir, "fonts").apply { if (!exists()) mkdirs() }
            val fileName = "custom_font_${UUID.randomUUID()}.ttf"
            val destFile = File(fontsDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                destFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FontStorageManager", "Failed to save custom font file", e)
            null
        }
    }
}
