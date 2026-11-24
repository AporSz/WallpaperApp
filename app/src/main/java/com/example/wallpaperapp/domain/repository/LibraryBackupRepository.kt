package com.example.wallpaperapp.domain.repository

import com.example.wallpaperapp.domain.model.BackupScope
import com.example.wallpaperapp.domain.model.ExportSummary
import com.example.wallpaperapp.domain.model.ImportSummary

interface LibraryBackupRepository {
    suspend fun exportLibrary(destinationUriString: String, scope: BackupScope): Result<ExportSummary>
    suspend fun importLibrary(sourceUriString: String, scope: BackupScope): Result<ImportSummary>
}
