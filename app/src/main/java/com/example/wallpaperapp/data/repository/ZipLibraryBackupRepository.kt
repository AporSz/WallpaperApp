package com.example.wallpaperapp.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.wallpaperapp.domain.model.BackupScope
import com.example.wallpaperapp.domain.model.ExportSummary
import com.example.wallpaperapp.domain.model.ImportSummary
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.WallpaperImage
import com.example.wallpaperapp.domain.repository.ImageRepository
import com.example.wallpaperapp.domain.repository.LibraryBackupRepository
import com.example.wallpaperapp.domain.repository.QuoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipLibraryBackupRepository(
    private val context: Context,
    private val quoteRepository: QuoteRepository,
    private val imageRepository: ImageRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LibraryBackupRepository {

    override suspend fun exportLibrary(
        destinationUriString: String,
        scope: BackupScope
    ): Result<ExportSummary> = withContext(ioDispatcher) {
        try {
            val destUri = Uri.parse(destinationUriString)
            val outputStream = context.contentResolver.openOutputStream(destUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open output stream for URI: $destinationUriString"))

            var exportedQuotes = 0
            var exportedImages = 0
            var skippedImages = 0

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->

                // 1. Export Quotes if scope is QUOTES_ONLY or BOTH
                if (scope == BackupScope.QUOTES_ONLY || scope == BackupScope.BOTH) {
                    val quotes = quoteRepository.getAllQuotes()
                    val quotesJson = JsonBackupHelper.serializeQuotes(quotes)
                    val quotesBytes = quotesJson.toByteArray(Charsets.UTF_8)

                    val quoteEntry = ZipEntry("quotes.json")
                    zipOut.putNextEntry(quoteEntry)
                    zipOut.write(quotesBytes)
                    zipOut.closeEntry()
                    exportedQuotes = quotes.size
                }

                // 2. Export Images if scope is IMAGES_ONLY or BOTH
                if (scope == BackupScope.IMAGES_ONLY || scope == BackupScope.BOTH) {
                    val images = imageRepository.getAllImages()
                    for ((index, image) in images.withIndex()) {
                        val imageUri = try {
                            Uri.parse(image.uri)
                        } catch (e: Exception) {
                            skippedImages++
                            continue
                        }

                        val inputStream: InputStream? = try {
                            context.contentResolver.openInputStream(imageUri)
                        } catch (e: Exception) {
                            Log.w("ZipLibraryBackup", "Failed to open input stream for image: ${image.uri}", e)
                            null
                        }

                        if (inputStream == null) {
                            skippedImages++
                            continue
                        }

                        val ext = when {
                            image.uri.contains(".png", ignoreCase = true) -> "png"
                            image.uri.contains(".webp", ignoreCase = true) -> "webp"
                            else -> "jpg"
                        }

                        val entryName = "images/image_${index + 1}_${image.id}.$ext"
                        val imageEntry = ZipEntry(entryName)
                        zipOut.putNextEntry(imageEntry)

                        inputStream.use { input ->
                            input.copyTo(zipOut, bufferSize = 8192)
                        }

                        zipOut.closeEntry()
                        exportedImages++
                    }
                }
            }

            Result.success(
                ExportSummary(
                    exportedQuotesCount = exportedQuotes,
                    exportedImagesCount = exportedImages,
                    skippedImagesCount = skippedImages
                )
            )
        } catch (e: Exception) {
            Log.e("ZipLibraryBackup", "Export failed for URI: $destinationUriString", e)
            Result.failure(e)
        }
    }

    override suspend fun importLibrary(
        sourceUriString: String,
        scope: BackupScope
    ): Result<ImportSummary> = withContext(ioDispatcher) {
        try {
            val sourceUri = Uri.parse(sourceUriString)
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream for URI: $sourceUriString"))

            var importedQuotesCount = 0
            var skippedDuplicateQuotesCount = 0
            var importedImagesCount = 0
            var skippedImagesCount = 0

            val importedImagesDir = File(context.filesDir, "imported_images").apply {
                if (!exists()) mkdirs()
            }

            val newImages = mutableListOf<WallpaperImage>()

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                while (true) {
                    val entry = zipIn.nextEntry ?: break
                    val name = entry.name

                    // Zip Slip Path Traversal Protection
                    if (name.contains("..") || name.startsWith("/")) {
                        zipIn.closeEntry()
                        continue
                    }

                    if (name == "quotes.json" && (scope == BackupScope.QUOTES_ONLY || scope == BackupScope.BOTH)) {
                        val baos = ByteArrayOutputStream()
                        zipIn.copyTo(baos, bufferSize = 8192)
                        val jsonString = baos.toString(Charsets.UTF_8.name())

                        val parsedQuotes = JsonBackupHelper.parseQuotes(jsonString)
                        val existingQuotes = quoteRepository.getAllQuotes()
                        val existingKeys = existingQuotes.map {
                            it.text.trim().lowercase() to (it.author?.trim()?.lowercase() ?: "")
                        }.toSet()

                        val newQuotes = mutableListOf<Quote>()
                        for (q in parsedQuotes) {
                            val key = q.text.trim().lowercase() to (q.author?.trim()?.lowercase() ?: "")
                            if (existingKeys.contains(key)) {
                                skippedDuplicateQuotesCount++
                            } else {
                                newQuotes.add(q)
                            }
                        }

                        if (newQuotes.isNotEmpty()) {
                            quoteRepository.addQuotes(newQuotes)
                            importedQuotesCount = newQuotes.size
                        }
                    } else if (name.startsWith("images/") && !entry.isDirectory && (scope == BackupScope.IMAGES_ONLY || scope == BackupScope.BOTH)) {
                        val ext = when {
                            name.endsWith(".png", ignoreCase = true) -> "png"
                            name.endsWith(".webp", ignoreCase = true) -> "webp"
                            else -> "jpg"
                        }

                        val destFile = File(importedImagesDir, "imported_${UUID.randomUUID()}.$ext")
                        try {
                            FileOutputStream(destFile).use { out ->
                                zipIn.copyTo(out, bufferSize = 8192)
                            }

                            val newImage = WallpaperImage(
                                id = UUID.randomUUID().toString(),
                                uri = Uri.fromFile(destFile).toString()
                            )

                            newImages.add(newImage)
                            importedImagesCount++
                        } catch (e: Exception) {
                            Log.e("ZipLibraryBackup", "Failed to extract image entry: $name", e)
                            if (destFile.exists()) destFile.delete()
                            skippedImagesCount++
                        }
                    }

                    zipIn.closeEntry()
                }
            }

            if (newImages.isNotEmpty()) {
                imageRepository.addImages(newImages)
            }

            Result.success(
                ImportSummary(
                    importedQuotesCount = importedQuotesCount,
                    skippedDuplicateQuotesCount = skippedDuplicateQuotesCount,
                    importedImagesCount = importedImagesCount,
                    skippedImagesCount = skippedImagesCount
                )
            )
        } catch (e: Exception) {
            Log.e("ZipLibraryBackup", "Import failed for URI: $sourceUriString", e)
            Result.failure(e)
        }
    }
}

object JsonBackupHelper {

    fun serializeQuotes(quotes: List<Quote>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        for ((i, q) in quotes.withIndex()) {
            sb.append("  {\n")
            sb.append("    \"id\": \"${escapeJson(q.id)}\",\n")
            sb.append("    \"text\": \"${escapeJson(q.text)}\",\n")
            if (q.author != null) {
                sb.append("    \"author\": \"${escapeJson(q.author)}\",\n")
            } else {
                sb.append("    \"author\": null,\n")
            }
            sb.append("    \"showQuotationMarks\": ${q.showQuotationMarks}\n")
            sb.append("  }")
            if (i < quotes.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    fun parseQuotes(jsonString: String): List<Quote> {
        val list = mutableListOf<Quote>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val text = obj.optString("text", "")
                if (text.isBlank()) continue
                val id = obj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                val author = if (obj.isNull("author")) null else obj.optString("author").ifBlank { null }
                val showQuotationMarks = if (obj.has("showQuotationMarks")) obj.optBoolean("showQuotationMarks", true) else true
                list.add(Quote(id = id, text = text, author = author, showQuotationMarks = showQuotationMarks))
            }
        } catch (e: Exception) {
            list.addAll(parseQuotesFallback(jsonString))
        }
        if (list.isEmpty() && jsonString.contains("\"text\"")) {
            list.addAll(parseQuotesFallback(jsonString))
        }
        return list
    }

    private fun parseQuotesFallback(jsonString: String): List<Quote> {
        val list = mutableListOf<Quote>()
        val blocks = jsonString.split("}")
        for (block in blocks) {
            if (!block.contains("\"text\"")) continue
            val textMatch = Regex("\"text\"\\s*:\\s*\"(.*?)\"").find(block)
            val text = textMatch?.groupValues?.get(1)?.let { unescapeJson(it) } ?: continue
            if (text.isBlank()) continue

            val idMatch = Regex("\"id\"\\s*:\\s*\"(.*?)\"").find(block)
            val id = idMatch?.groupValues?.get(1) ?: UUID.randomUUID().toString()

            val authorMatch = Regex("\"author\"\\s*:\\s*\"(.*?)\"").find(block)
            val author = authorMatch?.groupValues?.get(1)?.let { unescapeJson(it) }

            val showQuotesMatch = Regex("\"showQuotationMarks\"\\s*:\\s*(true|false)").find(block)
            val showQuotationMarks = showQuotesMatch?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: true

            list.add(Quote(id = id, text = text, author = author, showQuotationMarks = showQuotationMarks))
        }
        return list
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(str: String): String {
        return str.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}
