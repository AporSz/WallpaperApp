package com.example.wallpaperapp.data.local

import android.content.Context
import android.graphics.Typeface
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

            if (destFile.exists() && destFile.length() > 0 && isValidFontHeader(destFile)) {
                val typeface = try {
                    Typeface.createFromFile(destFile)
                } catch (e: Exception) {
                    null
                }

                if (typeface != null) {
                    destFile.absolutePath
                } else {
                    destFile.delete()
                    null
                }
            } else {
                if (destFile.exists()) destFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e("FontStorageManager", "Failed to save custom font file", e)
            null
        }
    }

    private fun isValidFontHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(4)
                if (stream.read(header) < 4) return false
                val b0 = header[0].toInt() and 0xFF
                val b1 = header[1].toInt() and 0xFF
                val b2 = header[2].toInt() and 0xFF
                val b3 = header[3].toInt() and 0xFF

                // Check TrueType 1.0 (0x00010000), OpenType ("OTTO"), TTC ("ttcf"), WOFF ("wOFF"), WOFF2 ("wOF2")
                (b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00) ||
                (b0 == 'O'.code && b1 == 'T'.code && b2 == 'T'.code && b3 == 'O'.code) ||
                (b0 == 't'.code && b1 == 't'.code && b2 == 'c'.code && b3 == 'f'.code) ||
                (b0 == 'w'.code && b1 == 'O'.code && b2 == 'F'.code && b3 == 'F'.code) ||
                (b0 == 'w'.code && b1 == 'O'.code && b2 == 'F'.code && b3 == '2'.code)
            }
        } catch (e: Exception) {
            false
        }
    }
}
