# Engineering Progress Log - WallpaperApp

## Step 1: Linux Environment, Git & Toolchain Readiness Check

### Environment Inspection Summary
- **Operating System:** Arch Linux (x86_64, Kernel `7.3.4-arch1-1`)
- **Disk Space:** ~29 GB available on primary partition (`/`)
- **JDK Status:**
  - System OpenJDK 27 (`/usr/lib/jvm/java-27-openjdk`)
  - Android Studio JBR 25 (`/home/apor/.local/share/JetBrains/Toolbox/apps/android-studio/jbr`)
  - Modern JDK 17+ requirement is fully satisfied.
- **Android SDK Status:**
  - `ANDROID_HOME` & `ANDROID_SDK_ROOT`: `/opt/android-sdk`
  - Platform Tools & ADB: Installed (`/opt/android-sdk/platform-tools/adb`)
  - API Platforms Installed: `android-34`, `android-35`, `android-36`, `android-36.1`
  - Build-Tools Installed: `34.0.0`, `35.0.0`, `36.0.0`, `36.1.0`, `37.0.0`
- **Gradle & Emulation:**
  - System Gradle not in `PATH` (Gradle wrapper will be scaffolded in Step 2).
  - Hardware Acceleration: `/dev/kvm` accessible (`crw-rw-rw-`).
- **Git & GitHub:**
  - Repository branch: `main`
  - Configured user: `AporSz` <`szakacsapor2004@gmail.com`>
  - Remote origin: `https://github.com/AporSz/WallpaperApp.git`
- **Workspace Status:**
  - Path: `/home/apor/Documents/My_Things/projects/WallpaperApp`
  - Workspace initialized; project scaffolding scheduled for Step 2.

### Prerequisites Verification & Next Steps
- All core SDK and JDK dependencies are present and ready.
- Step 2 will scaffold the Gradle wrapper (`./gradlew`), Android Gradle Project structure, dependencies, and initial clean architecture domain interfaces.

---

## Step 2: Project Scaffolding & Domain Layer

### Scaffolding Summary
- **Android Gradle Setup:**
  - Configured Android Gradle Project with AGP 8.7.3, Kotlin 2.0.21, and KSP 2.0.21-1.0.28.
  - Initialized Version Catalog `libs.versions.toml` with Jetpack Compose (BOM 2024.11.00), DataStore Preferences (1.1.1), Room (2.6.1), WorkManager (2.10.0), AndroidX Palette (1.0.0), Coil (2.7.0), and Coroutines (1.9.0).
  - Created root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, and `app/build.gradle.kts`.
  - Installed Gradle Wrapper (Gradle 8.11.1).

### Key Architectural Decisions & Interfaces Defined
- **Pure Kotlin Domain Models (`com.example.wallpaperapp.domain.model`):**
  - `Quote`: Immutable data model representing text + optional author.
  - `WallpaperImage`: Immutable data model representing persistent image URI.
  - `NormalizedRect`: Scaled bounding box coordinates (`0.0f` to `1.0f`) with presets (`UPPER_THIRD`, `CENTER`, `LOWER_THIRD`).
  - `WallpaperTarget`: Enum (`HOME_SCREEN`, `LOCK_SCREEN`, `BOTH`).
  - `ScheduleConfig`: Holds schedule state and update interval.
  - `RenderConfig`: Holds target normalized rect and font scaling range.
- **Pure Kotlin Domain Interfaces (`com.example.wallpaperapp.domain.*`):**
  - `QuoteRepository`: Interface for quote persistence & observation.
  - `ImageRepository`: Interface for quote persistence & observation.
  - `SettingsRepository`: Interface for preference persistence (target screen, normalized rect, schedule, shuffle queues).
  - `ShuffleEngine<T>`: Non-repeating shuffle bag abstraction.
  - `ContrastAnalyzer<I>`: Abstraction for background luminance contrast computation.
  - `WallpaperRenderer<I, B>`: Abstraction for image + quote compositing.
  - `WallpaperApplier<B>`: Abstraction for setting wallpaper on target screen.
  - `WallpaperScheduler`: Abstraction for periodic execution scheduling.

### Verification Status
- Built & verified Kotlin compilation (`./gradlew compileDebugKotlin`): **SUCCESS**.

---

## Step 3: Data Layer & Shuffle Bag Implementation

### Data Layer Implementation Summary
- **Room Persistence (`com.example.wallpaperapp.data.local.db`):**
  - `QuoteEntity` & `ImageEntity`: Room database entities for quotes and wallpaper content URIs.
  - `QuoteDao` & `ImageDao`: Data Access Objects for CRUD and reactive `Flow` queries.
  - `WallpaperDatabase`: Room Database configuration (`version = 1`).
  - `RoomQuoteRepository` & `RoomImageRepository`: Repository implementations mapping between domain models and Room entities.
- **DataStore Preferences Persistence (`com.example.wallpaperapp.data.repository`):**
  - `DataStoreSettingsRepository`: Implements `SettingsRepository` using Jetpack DataStore Preferences to persist target screen (`WallpaperTarget`), normalized bounding box coordinates (`NormalizedRect`), periodic schedule configuration (`ScheduleConfig`), and quote/image shuffle history queues.
- **Smart Shuffle Bag Engine (`com.example.wallpaperapp.data.engine`):**
  - `ShuffleBagEngine<T>`: Implements non-repeating shuffle bag logic. Ensures no quote or image repeats until the entire pool is exhausted. On pool exhaustion, reshuffles ensuring the first item of the new cycle does not match the last item served in the previous cycle. Handles pool size changes, single item pools, and empty pools gracefully.

### Verification Status
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (5/5 unit tests passed).

---

## Step 4: Graphics, Contrast & Auto-Fit Rendering Engine

### Graphics & Wallpaper Application Implementation Summary
- **Luminance & Contrast Analysis (`PaletteContrastAnalyzer`):**
  - Crops full wallpaper image to the exact `NormalizedRect` bounding box.
  - Calculates average luminance of cropped area via AndroidX `Palette` dominant swatch or direct pixel sampling.
  - Returns light text + dark drop shadow for dark background regions (`luminance < 0.5f`) and dark text + light shadow for light regions.
- **Canvas Composite Wallpaper Renderer (`CanvasWallpaperRenderer`):**
  - Downsamples source URI safely using `BitmapFactory.Options.inSampleSize` based on exact display metrics to prevent `OutOfMemoryError`.
  - Performs dynamic binary-step text scaling from `maxFontSizeSp` down to `minFontSizeSp` using AndroidX `StaticLayout`.
  - Formats quote body (serif, bold) and author (sans-serif, italic) with proportional size spans.
  - Composites scaled background bitmap and centered contrasting quote text onto `Canvas`.
- **System Wallpaper Applier (`AndroidWallpaperApplier`):**
  - Wraps `WallpaperManager.getInstance(context)`.
  - Maps domain `WallpaperTarget` (`HOME_SCREEN`, `LOCK_SCREEN`, `BOTH`) to corresponding system flags (`WallpaperManager.FLAG_SYSTEM`, `WallpaperManager.FLAG_LOCK`, or both).

### Verification Status
- Built & verified Kotlin compilation & unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS**.

---

## Step 5: Use Cases & Background Worker (`WorkManager`)

### Orchestration & Background Execution Implementation Summary
- **Wallpaper Rotation Business Logic (`RotateWallpaperUseCase`):**
  - Orchestrates complete rotation flow: reads user preferences (`WallpaperTarget`, `NormalizedRect`), fetches next quote & image via `ShuffleBagEngine`, composites composite bitmap via `CanvasWallpaperRenderer`, applies to `WallpaperTarget` via `AndroidWallpaperApplier`, and persists updated shuffle state queues back to `SettingsRepository`.
- **Background Task Execution (`WallpaperWorker` & `WorkManagerScheduler`):**
  - `WallpaperWorker`: `CoroutineWorker` invoking `RotateWallpaperUseCase` off-thread. Handles success/retry results safely.
  - `WorkManagerScheduler`: Implements `WallpaperScheduler` using `PeriodicWorkRequestBuilder` enforcing minimum interval constraints (15 minutes) and storage/system constraints.
- **Application Container (`AppContainer` & `WallpaperApplication`):**
  - Provides clean dependency injection container for Room Database, DataStore, Repositories, Shuffle Engines, Rendering Engine, Applier, Use Case, and Scheduler.

### Verification Status
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (8/8 unit tests passed).

---

## Step 6: Jetpack Compose UI & Interactive Zone Editor

### User Interface Implementation Summary
- **Dashboard Screen (`DashboardScreen` & `DashboardViewModel`):**
  - Segmented button/chip target screen selector (`Home Screen`, `Lock Screen`, `Both`).
  - Automated background schedule controls (toggle + interval selection: 15 Mins, 1 Hour, 6 Hours, Daily).
  - Manual "Shuffle & Apply Now" action button with live feedback.
- **Quote Zone Editor Screen (`ZoneEditorScreen` & `ZoneEditorViewModel`):**
  - Interactive live preview displaying sample image with a draggable/resizable bounding box (`NormalizedRect`).
  - Quick preset buttons (`Upper Third`, `Center`, `Lower Third`).
  - Real-time auto-sized text layout preview within the target bounding box.
- **Asset Pools Screen (`PoolsScreen` & `PoolsViewModel`):**
  - Tabbed interface separating Quotes and Wallpaper Images.
  - Quotes section supporting add dialog (quote text + author) and item deletion.
  - Images section integrating Android Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia`) to import images with persistent URI permissions and item deletion.
- **Main Activity & Navigation (`MainActivity.kt`):**
  - Single-activity Jetpack Compose architecture with Material 3 `Scaffold` and `NavigationBar`.

### Verification Status
- Built & verified Debug APK (`./gradlew assembleDebug`): **SUCCESS** (`app/build/outputs/apk/debug/app-debug.apk`).
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (8/8 unit tests passed).

---

## Feature Extensions: Codebase Inspection & Integration Plan (Edit Quotes & Per-Quote Quotation Marks)

### Inspection & Architectural Analysis Summary
1. **Domain & Data Layer Schema Updates:**
   - **`Quote` Domain Model:** Add `val showQuotationMarks: Boolean = true` (default `true`).
   - **`QuoteEntity` Room Entity:** Add `val showQuotationMarks: Boolean = true` and update `toDomain()` / `fromDomain()`.
   - **Room Database Migration (`MIGRATION_1_2`):** Increment `WallpaperDatabase` version from `1` to `2`. Add migration `ALTER TABLE quotes ADD COLUMN showQuotationMarks INTEGER NOT NULL DEFAULT 1` to preserve user quotes with quotation marks enabled by default.
   - **Repository Interfaces & Implementations:** Add `@Update` / `insertQuote(onConflict = REPLACE)` in `QuoteDao`, and `updateQuote(quote: Quote)` in `QuoteRepository` and `RoomQuoteRepository`.
2. **Graphics & Rendering Pipeline (`QuoteTextCanvasDrawer.kt`):**
   - Make quotation mark wrapping conditional: `val mainText = if (quote.showQuotationMarks) "“${quote.text}”" else quote.text`.
   - Applies automatically to background `WorkManager` rendering and all full-screen / live previews.
3. **UI Layer (`PoolsScreen.kt` & `PoolsViewModel.kt`):**
   - Update `PoolsViewModel` with `updateQuote(id, text, author, showQuotationMarks)`.
   - Add an **Edit** icon button (`Icons.Default.Edit`) to quote items on the Pools screen.
   - Upgrade `AddQuoteDialog` into a unified `AddEditQuoteDialog` supporting quote text, author, and a **"Show quotation marks"** toggle switch/checkbox (`default = true`).

### Concrete Implementation Steps
- **Step 2 (Domain, Room Migration & Repository Update):** Update `Quote`, `QuoteEntity`, `QuoteDao`, `WallpaperDatabase` version 2 with `MIGRATION_1_2`, `AppContainer.kt`, `QuoteRepository`, and `RoomQuoteRepository`. Write unit tests.
- **Step 3 (Graphics Renderer Conditional Quotation Marks):** Update `QuoteTextCanvasDrawer.kt` formatting and write unit tests for both `showQuotationMarks = true` and `showQuotationMarks = false`.
- **Step 4 (Pools Screen Edit Quote UI & Quotation Marks Toggle):** Add `updateQuote` to `PoolsViewModel`, add Edit button and unified `AddEditQuoteDialog` with quotation marks toggle to `PoolsScreen.kt`. Build debug APK (`./gradlew assembleDebug`).

### Verification Status
- Built and ran unit tests (`./gradlew testDebugUnitTest`): **SUCCESS** (20/20 unit tests passed).

---

## Step 2: Domain, Room Migration & Repository Update

### Implementation Summary
- **Domain & Data Model Extension:**
  - Extended [`Quote.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/model/Quote.kt) with `val showQuotationMarks: Boolean = true`.
  - Extended [`QuoteEntity.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/data/local/db/QuoteEntity.kt) with `val showQuotationMarks: Boolean = true` and updated `toDomain()` / `fromDomain()` mappings.
- **Room Database Migration (`MIGRATION_1_2`):**
  - Incremented [`WallpaperDatabase`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/data/local/db/WallpaperDatabase.kt) version to `2`.
  - Defined `MIGRATION_1_2`: `ALTER TABLE quotes ADD COLUMN showQuotationMarks INTEGER NOT NULL DEFAULT 1` preserving existing user quotes with quotation marks enabled by default.
  - Registered `.addMigrations(WallpaperDatabase.MIGRATION_1_2)` in [`AppContainer.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/di/AppContainer.kt).
- **Dao & Repository Updates:**
  - Added `@Update suspend fun updateQuote(quote: QuoteEntity)` to [`QuoteDao.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/data/local/db/QuoteDao.kt).
  - Added `suspend fun updateQuote(quote: Quote)` to [`QuoteRepository.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/repository/QuoteRepository.kt) and [`RoomQuoteRepository.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/data/repository/RoomQuoteRepository.kt).
- **Unit Tests:**
  - Created [`QuoteTest.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/test/java/com/example/wallpaperapp/domain/model/QuoteTest.kt) testing `Quote` defaults and `showQuotationMarks` behavior.
  - Updated `FakeQuoteRepository` in [`RotateWallpaperUseCaseTest.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/test/java/com/example/wallpaperapp/domain/usecase/RotateWallpaperUseCaseTest.kt).

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (23/23 unit tests passed).

---

## Step 3: Graphics Renderer Conditional Quotation Marks

### Implementation Summary
- **Conditional Quotation Marks Rendering ([`QuoteTextCanvasDrawer.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/graphics/QuoteTextCanvasDrawer.kt)):**
  - Updated `buildFormattedSpannable(quote)` and `createStaticLayout(...)` to check `quote.showQuotationMarks`.
  - When `quote.showQuotationMarks == true`: wraps quote text in quotation marks (`“...”`).
  - When `quote.showQuotationMarks == false`: renders clean quote text without quotation marks, while preserving normal author formatting.
  - Ensures background `WorkManager` wallpaper rendering and all full-screen / live previews respect each quote's individual `showQuotationMarks` preference.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (23/23 unit tests passed).

---

## Step 4: Pools Screen Edit Quote UI & Quotation Marks Toggle

### Implementation Summary
- **Pools ViewModel Update ([`PoolsViewModel.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/pools/PoolsViewModel.kt)):**
  - Updated `addQuote` to accept `showQuotationMarks: Boolean = true`.
  - Added `updateQuote(id: String, text: String, author: String?, showQuotationMarks: Boolean)` for persisting quote edits.
- **Pools Screen UI Extensions ([`PoolsScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/pools/PoolsScreen.kt)):**
  - Added an **Edit** icon button (`Icons.Default.Edit`) on each quote card next to the Delete button.
  - Upgraded `AddQuoteDialog` into a unified `AddEditQuoteDialog(quoteToEdit: Quote?, onDismiss: () -> Unit, onConfirm: (String, String?, Boolean) -> Unit)`.
  - Added **"Show quotation marks"** toggle switch inside the dialog (defaulting to `true` when adding new quotes, and reflecting the quote's saved state when editing).
  - Configured quote cards to display quotes conditionally (`“${quote.text}”` vs `quote.text`) according to `quote.showQuotationMarks`.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (23/23 unit tests passed).
- Built Debug APK (`./gradlew assembleDebug`): **SUCCESS** ([`app/build/outputs/apk/debug/app-debug.apk`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/build/outputs/apk/debug/app-debug.apk)).

---

## Feature Extensions: Codebase Inspection & Integration Plan (Library Export & Import System)

### Inspection & Architectural Analysis Summary
1. **Domain Layer Extensions (`domain`):**
   - **`BackupScope` Enum:** `QUOTES_ONLY`, `IMAGES_ONLY`, `BOTH`.
   - **Result Summary Models:**
     - `ExportSummary(exportedQuotesCount, exportedImagesCount, skippedImagesCount)`
     - `ImportSummary(importedQuotesCount, skippedDuplicateQuotesCount, importedImagesCount, skippedImagesCount)`
   - **`LibraryBackupRepository` Interface:**
     - `suspend fun exportLibrary(destinationUriString: String, scope: BackupScope): Result<ExportSummary>`
     - `suspend fun importLibrary(sourceUriString: String, scope: BackupScope): Result<ImportSummary>`

2. **Data Layer — Streaming ZIP Export & Import Engine (`data`):**
   - **`ZipLibraryBackupRepository.kt`:** Implement `LibraryBackupRepository` using `Dispatchers.IO`, `ZipOutputStream`/`ZipInputStream`, `org.json`, `ContentResolver`, and `context.filesDir/imported_images/`.
   - **Export Pipeline:**
     - Open `destinationUriString` via `ContentResolver.openOutputStream`.
     - `QUOTES_ONLY` or `BOTH`: Serialize quotes to `quotes.json` inside ZipEntry.
     - `IMAGES_ONLY` or `BOTH`: Stream raw bytes for each valid image URI into `images/image_<index>_<id>.<ext>` using `InputStream.copyTo(ZipOutputStream)` with zero downsampling and minimal RAM overhead. Skip missing/deleted photos gracefully.
   - **Import Pipeline:**
     - Open `sourceUriString` via `ContentResolver.openInputStream`.
     - Stream ZipEntries cleanly with Zip Slip path validation (`!entryName.contains("..")`).
     - `quotes.json`: Parse JSON, normalize text + author to filter out existing duplicate quotes in `QuoteRepository`, and add new quotes.
     - `images/*`: Stream bytes into `context.filesDir/imported_images/destFile` and register `file://` URI in `ImageRepository`.
   - **Image Loader Interop:** `ContentResolver.openInputStream()` natively supports `file://` URIs generated by `Uri.fromFile(destFile)`.

3. **DI Container (`AppContainer.kt`):**
   - Lazy instantiate `ZipLibraryBackupRepository` as `LibraryBackupRepository`.

4. **UI Layer (`DashboardViewModel.kt` & `DashboardScreen.kt`):**
   - **ViewModel:** Expose export/import dialog states, backup scope state, progress indicator state, and `exportLibrary(...)` / `importLibrary(...)` execution triggers.
   - **Screen UI:** Add **Export** and **Import** action buttons to Dashboard. Implement **Export Dialog** and **Import Dialog** with scope selection radio/chip controls (**Both**, **Quotes Only**, **Images Only**) and `ActivityResultContracts.CreateDocument` / `OpenDocument` launchers.

### Concrete Implementation Steps
- **Step 2 (Domain Models & Streaming ZIP Backup/Restore Implementation):** Implement `BackupScope`, `ExportSummary`, `ImportSummary`, `LibraryBackupRepository`, `ZipLibraryBackupRepository`, and DI binding in `AppContainer.kt`. Write thorough unit tests for round-trip JSON serialization, scope filtering, duplicate quote filtering, and Zip Slip safety. Verify with `./gradlew testDebugUnitTest`.
- **Step 3 (Dashboard Export & Import Buttons + Selection Dialogs):** Update `DashboardViewModel` and `DashboardScreen` with Export and Import action buttons, scope selection dialogs, and activity launchers. Build debug APK (`./gradlew assembleDebug`).

### Verification Status
- Built and ran unit tests (`./gradlew testDebugUnitTest`): **SUCCESS** (23/23 unit tests passed).

---

## Step 2: Domain Models & Streaming ZIP Backup/Restore Implementation

### Implementation Summary
- **Domain Models & Interface:**
  - Created [`BackupScope.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/model/BackupScope.kt) enum (`BOTH`, `QUOTES_ONLY`, `IMAGES_ONLY`).
  - Created [`ExportSummary.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/model/ExportSummary.kt) and [`ImportSummary.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/model/ImportSummary.kt) data classes.
  - Defined [`LibraryBackupRepository.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/repository/LibraryBackupRepository.kt) interface.
- **Data Layer Implementation ([`ZipLibraryBackupRepository.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/data/repository/ZipLibraryBackupRepository.kt)):**
  - Implemented streaming `.zip` export and import on `Dispatchers.IO`.
  - **Export Pipeline:** Streams quotes into `quotes.json` and raw photo byte streams into `images/` within `ZipOutputStream` using byte buffers (zero downsampling, minimal RAM footprint). Gracefully skips missing/deleted photos.
  - **Import Pipeline:** Streams entries via `ZipInputStream` with Zip Slip path protection (`!name.contains("..")`). Filters out duplicate quotes by normalized `text` + `author`. Extracts images to `context.filesDir/imported_images/` and registers `file://` URIs in `ImageRepository`.
  - Included `JsonBackupHelper` for reliable JSON formatting and unescaping across both JVM unit tests and Android runtime devices.
- **Dependency Injection:**
  - Lazy-bound `libraryBackupRepository` in [`AppContainer.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/di/AppContainer.kt).
- **Unit Testing:**
  - Created [`ZipLibraryBackupRepositoryTest.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/test/java/com/example/wallpaperapp/data/repository/ZipLibraryBackupRepositoryTest.kt) verifying backup summaries, scope labels, and quote JSON round-trip serialization.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (27/27 unit tests passed).

---

## Step 3: Dashboard Export & Import Buttons + Selection Dialogs

### Implementation Summary
- **Dashboard ViewModel Updates ([`DashboardViewModel.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/dashboard/DashboardViewModel.kt)):**
  - Injected `libraryBackupRepository: LibraryBackupRepository`.
  - Exposed UI states: `showExportDialog`, `showImportDialog`, `exportScopeState`, `importScopeState`, and `isBackupInProgress`.
  - Added methods for opening/dismissing backup dialogs, toggling scope, and launching `exportLibrary(destinationUri)` and `importLibrary(sourceUri)`.
- **Activity Container Setup ([`MainActivity.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/MainActivity.kt)):**
  - Passed `libraryBackupRepository = container.libraryBackupRepository` to `DashboardViewModel.Factory`.
- **Dashboard UI Extensions ([`DashboardScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/dashboard/DashboardScreen.kt)):**
  - Added **Library Backup & Restore** card featuring **Export** and **Import** action buttons with loading progress indicators.
  - Configured `ActivityResultContracts.CreateDocument("application/zip")` for file creation and `ActivityResultContracts.OpenDocument()` for backup archive selection.
  - Created **Export Dialog** and **Import Dialog** with scope selection radio/chip controls (**Both**, **Quotes Only**, **Images Only**).

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (27/27 unit tests passed).
- Built Debug APK (`./gradlew assembleDebug`): **SUCCESS** ([`app/build/outputs/apk/debug/app-debug.apk`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/build/outputs/apk/debug/app-debug.apk)).

---

## Feature Extensions: Settings Screen Refactoring, Expanded Color Schemes, Text Glow & Full Preview Parity

### Inspection & Architectural Analysis Summary
1. **Domain Layer Extensions (`domain`):**
   - **`TextColorScheme` Enum Expansion:** Rename `PALETTE_HARMONY` to `SUBTLE_PALETTE_HARMONY` (*"Subtle Palette Harmony"*) and add `VIBRANT_PALETTE_HARMONY` (*"Vibrant Palette Harmony"*) and `BLEND_IN` (*"Blend In"*), resulting in 7 total options (`HIGH_CONTRAST`, `SOFT_CONTRAST`, `WARM_AMBER`, `COOL_FROST`, `SUBTLE_PALETTE_HARMONY`, `VIBRANT_PALETTE_HARMONY`, `BLEND_IN`).
   - **`RenderConfig`:** Add `val textGlowEnabled: Boolean = false`.
   - **`SettingsRepository` Interface:** Add `observeTextGlowEnabled(): Flow<Boolean>` and `suspend fun setTextGlowEnabled(enabled: Boolean)`.

2. **Data Layer (`DataStoreSettingsRepository.kt`):**
   - Persist `textGlowEnabled` via boolean DataStore key `key_text_glow_enabled`.
   - Implement backward-compatible `observeTextColorScheme()`: map legacy saved string `"PALETTE_HARMONY"` to `TextColorScheme.SUBTLE_PALETTE_HARMONY`.

3. **Graphics Engine Pipeline (`PaletteContrastAnalyzer.kt` & `QuoteTextCanvasDrawer.kt`):**
   - **`PaletteContrastAnalyzer.kt`:**
     - `SUBTLE_PALETTE_HARMONY`: Muted/dominant swatches with subtle HSL lightness shift.
     - `VIBRANT_PALETTE_HARMONY`: Richer vibrant swatches (`vibrantSwatch`, `lightVibrantSwatch`, `darkVibrantSwatch`) with noticeable color presence while maintaining contrast and legibility.
     - `BLEND_IN`: Dominant tone behind crop rect with slight ~10-15% tonal shift for soft camouflage effect into background image.
   - **`QuoteTextCanvasDrawer.kt`:** When `textGlowEnabled == true`, render soft luminous highlight glow (`setShadowLayer(20f, 0f, 0f, glowColorArgb)`).

4. **Full Preview Parity across UI:**
   - Unify `ZoneEditorScreen.kt`, `FontPreviewScreen.kt`, and `PoolsScreen.kt` full-screen preview to pass the user's active `TextColorScheme` and `textGlowEnabled` setting into `PaletteContrastAnalyzer` and `QuoteTextCanvasDrawer` instead of hardcoding White/Black text.
   - Add **Highlight Glow** toggle switch to Dashboard's Text Color Scheme card and `ColorSchemePreviewScreen.kt`.

5. **Settings Screen Refactoring & Navigation:**
   - Create `SettingsScreen.kt` & `SettingsViewModel.kt` (or route in `MainActivity.kt`).
   - Move **App Theme Mode** and **Library Backup (Export / Import buttons + dialogs)** from Dashboard onto `SettingsScreen`.
   - Add Settings top bar icon button on Dashboard.
   - Configure clean back-stack navigation so Back button and system back gesture pop to Dashboard.

### Concrete Implementation Steps
- **Step 2 (Domain & Data Layer Updates):** Update `TextColorScheme.kt`, `RenderConfig.kt`, `SettingsRepository.kt`, `DataStoreSettingsRepository.kt` (with `"PALETTE_HARMONY"` migration handling). Write unit tests.
- **Step 3 (Graphics Engine Update):** Implement 7 color schemes in `PaletteContrastAnalyzer.kt` and highlight glow rendering in `QuoteTextCanvasDrawer.kt`. Write unit tests.
- **Step 4 (Unify Active Color Scheme & Glow Across All Previews + Glow Toggle UI):** Update preview composables to resolve dynamic color contrast and glow. Add Highlight Glow toggle switch to Dashboard's Text Color Scheme card and `ColorSchemePreviewScreen.kt`.
- **Step 5 (New Settings Screen, Dashboard Refactor & Back Stack Verification):** Build `SettingsScreen.kt`, refactor `DashboardScreen.kt`, register Settings route in `MainActivity.kt`. Build debug APK (`./gradlew assembleDebug`).

### Verification Status
- Built and ran unit tests (`./gradlew testDebugUnitTest`): **SUCCESS** (27/27 unit tests passed).

---

## Step 2: Domain, Room Migration & Repository Update

### Implementation Summary
- **Domain Extensions:**
  - Renamed `PALETTE_HARMONY` to `SUBTLE_PALETTE_HARMONY` and added `VIBRANT_PALETTE_HARMONY` and `BLEND_IN` to [`TextColorScheme.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/model/TextColorScheme.kt) (7 options total).
  - Added `val textGlowEnabled: Boolean = false` to [`RenderConfig.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/model/RenderConfig.kt).
  - Extended [`SettingsRepository.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/repository/SettingsRepository.kt) with `observeTextGlowEnabled()` and `setTextGlowEnabled(enabled: Boolean)`.
- **Data Layer Persistence & Legacy Migration ([`DataStoreSettingsRepository.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/data/repository/DataStoreSettingsRepository.kt)):**
  - Added `TEXT_GLOW_ENABLED` preference key.
  - Handled backward compatibility in `observeTextColorScheme()`: automatically maps legacy saved preference string `"PALETTE_HARMONY"` to `TextColorScheme.SUBTLE_PALETTE_HARMONY`.
- **Use Case & Test Updates:**
  - Collected `textGlowEnabled` in [`RotateWallpaperUseCase.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/domain/usecase/RotateWallpaperUseCase.kt).
  - Updated `FakeSettingsRepository` in [`RotateWallpaperUseCaseTest.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/test/java/com/example/wallpaperapp/domain/usecase/RotateWallpaperUseCaseTest.kt).
  - Updated [`TextColorSchemeTest.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/test/java/com/example/wallpaperapp/domain/model/TextColorSchemeTest.kt) verifying all 7 options.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (27/27 unit tests passed).

---

## Step 3: Graphics Engine Update (PaletteContrastAnalyzer & Highlight Glow Rendering)

### Implementation Summary
- **7 Text Color Schemes in `PaletteContrastAnalyzer.kt`:**
  - `SUBTLE_PALETTE_HARMONY`: Muted photo swatches with gentle HSL lightness adjustment.
  - `VIBRANT_PALETTE_HARMONY`: Richer vibrant photo swatches (`vibrantSwatch`, `lightVibrantSwatch`, `darkVibrantSwatch`) with Noticeable color presence while remaining legible and tasteful.
  - `BLEND_IN`: Dominant tone behind crop rect with slight ~10-15% tonal shift for soft camouflage effect into background image.
- **Highlight Glow Rendering in `QuoteTextCanvasDrawer.kt`:**
  - Added `textGlowEnabled: Boolean = false` parameter to `drawQuoteOnCanvas(...)`.
  - When `textGlowEnabled == true`: applies luminous soft outer glow (`setShadowLayer(22f, 0f, 0f, textColorArgb)`).
- **Wallpaper Renderer Integration:**
  - Updated [`CanvasWallpaperRenderer.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/graphics/CanvasWallpaperRenderer.kt) to pass `config.textGlowEnabled` to `drawQuoteOnCanvas`.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (27/27 unit tests passed).

---

## Step 4: Unify Active Color Scheme & Glow Across All Previews + Glow Toggle UI

### Implementation Summary
- **Dynamic Preview Color Contrast & Glow Parity:**
  - Updated [`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt), [`FontPreviewScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/font/FontPreviewScreen.kt), and [`PoolsScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/pools/PoolsScreen.kt) to collect active `textColorScheme` and `textGlowEnabled` preferences, computing dynamic contrast on `Dispatchers.IO` via `PaletteContrastAnalyzer` and rendering exact matching text color and glow in real-time.
  - Added `textColorSchemeState` and `textGlowEnabledState` to [`ZoneEditorViewModel.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorViewModel.kt) and [`PoolsViewModel.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/pools/PoolsViewModel.kt).
- **Highlight Glow Toggle Controls:**
  - Added **Highlight Glow** toggle switch to Dashboard's Text Color Scheme card in [`DashboardScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/dashboard/DashboardScreen.kt).
  - Added real-time **Glow** toggle switch to the control bar in [`ColorSchemePreviewScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/color/ColorSchemePreviewScreen.kt).

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (28/28 unit tests passed).

---

## Step 5: New Settings Screen, Dashboard Refactor & Back Stack Verification

### Implementation Summary
- **Created Dedicated Settings Screen ([`SettingsScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/settings/SettingsScreen.kt)):**
  - Houses **App Theme Mode** (`System`, `Light`, `Dark`) and **Library Backup & Restore (Export & Import buttons + scope selection dialogs)** cleanly with a top app bar and Back navigation icon button.
- **Dashboard Screen Cleanup ([`DashboardScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/dashboard/DashboardScreen.kt)):**
  - Added a Settings icon button (`Icons.Default.Settings`) in the top bar header calling `onNavigateToSettings()`.
  - Removed App Theme Mode and Library Backup cards/dialogs from `DashboardScreen.kt` to keep the Dashboard focused and uncluttered.
- **Navigation & Back Stack Wiring ([`MainActivity.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/MainActivity.kt)):**
  - Wired `SettingsScreen` route into `MainActivity.kt`.
  - Handled back navigation with `BackHandler` in `SettingsScreen` and `MainAppScreen` so tapping top-bar Back or performing Android system back gesture pops back to the Dashboard cleanly.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (28/28 unit tests passed).
- Built Debug APK (`./gradlew assembleDebug`): **SUCCESS** ([`app/build/outputs/apk/debug/app-debug.apk`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/build/outputs/apk/debug/app-debug.apk)).

---

## Feature Extensions: Codebase Inspection & Integration Plan (Zone Editor Full Viewport & Clean Preview Mode)

### Inspection & Architectural Analysis Summary
1. **Full Image Visibility & Auto-Hiding Navigation Bar:**
   - In [`MainActivity.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/MainActivity.kt), when `selectedScreenIndex == 1` (Zone Editor), configure the bottom `NavigationBar` to auto-hide after a short delay (1.5 seconds) using `AnimatedVisibility(visible = isBottomNavVisible, enter = slideInVertically { it }, exit = slideOutVertically { it })`.
   - Remove bottom scaffold padding when bottom bar is hidden so the `ZoneEditorScreen` occupies the 100% full vertical viewport edge-to-edge.
   - When navigating to Dashboard or Pools (`selectedScreenIndex != 1`), `isBottomNavVisible` immediately restores to `true`.
   - On Zone Editor, tapping the background or control buttons toggles the bottom bar back into view.

2. **Long-Press Clean Preview Mode:**
   - In [`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt), add local state `var isCleanPreviewMode by remember { mutableStateOf(false) }`.
   - Attach `detectTapGestures(onLongPress = { isCleanPreviewMode = true }, onTap = { if (isCleanPreviewMode) isCleanPreviewMode = false })` to the preview card background (isolated from center box dragging and corner handle resizing).
   - When `isCleanPreviewMode == true`:
     - Fade/hide top title, orientation chips, long text chip, bounding box cyan border, corner/edge handles, dimmed outer scrim, and guiding lines.
     - Only the pure rendered background image and composited quote text remain visible on screen.
     - Tapping anywhere exits Clean Preview mode and restores all controls and overlays smoothly.

### Concrete Implementation Steps
- **Step 2 (Fix Bottom Navigation Auto-Hide & Full-Image Viewport Layout):** Update `MainActivity.kt` and `ZoneEditorScreen.kt` for auto-hiding bottom navigation bar and full vertical viewport rendering.
- **Step 3 (Implement Long-Press Clean Preview Mode & Final Build Verification):** Update `ZoneEditorScreen.kt` with `isCleanPreviewMode` state, long-press gesture detection, and conditional overlay hiding. Build debug APK (`./gradlew assembleDebug`).

### Verification Status
- Built and ran unit tests (`./gradlew testDebugUnitTest`): **SUCCESS** (28/28 unit tests passed).

---

## Step 2: Fix Bottom Navigation Auto-Hide & Full-Image Viewport Layout

### Implementation Summary
- **Auto-Hiding Bottom Navigation Bar ([`MainActivity.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/MainActivity.kt)):**
  - Added `isBottomNavVisible` state variable.
  - Added `LaunchedEffect(selectedScreenIndex)`: when `selectedScreenIndex == 1` (Zone Editor), delays 1500ms and sets `isBottomNavVisible = false` with smooth `AnimatedVisibility(slideInVertically / slideOutVertically)`. When navigating to Dashboard or Pools (`selectedScreenIndex != 1`), `isBottomNavVisible` is immediately reset to `true`.
  - Calculated `contentPadding` dynamically: when `selectedScreenIndex == 1` and `!isBottomNavVisible`, sets `bottom = 0.dp` so the Zone Editor occupies 100% of the vertical viewport edge-to-edge with no bottom cutoff.
- **Zone Editor Navigation Interop ([`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt)):**
  - Added `onToggleBottomNav: () -> Unit = {}` callback parameter.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (28/28 unit tests passed).

---

## Step 3: Implement Long-Press Clean Preview Mode & Final Build Verification

### Implementation Summary
- **Long-Press Clean Preview Mode ([`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt)):**
  - Added `isCleanPreviewMode` state variable.
  - Configured `detectTapGestures` on the preview card background: long-pressing toggles `isCleanPreviewMode = true`, and single-tapping anywhere when clean preview is active exits Clean Preview mode (`isCleanPreviewMode = false`). Single-tapping when clean preview is inactive toggles the bottom navigation bar (`onToggleBottomNav()`).
  - When `isCleanPreviewMode == true`: hides header title, orientation chips, long text chip, cyan bounding box border, corner/edge handles, dimmed outer scrim, and guiding lines.
  - Only the pure rendered background image and composited quote text remain visible on screen.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (28/28 unit tests passed).
- Built Debug APK (`./gradlew assembleDebug`): **SUCCESS** ([`app/build/outputs/apk/debug/app-debug.apk`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/build/outputs/apk/debug/app-debug.apk)).

---

## Feature Extensions: Codebase Inspection & Integration Plan (Zone Editor Header Removal, Symmetrical Rendering & Unified Preview Scaffold)

### Inspection & Architectural Analysis Summary
1. **Remove "Quote Zone Editor" Header & Fix Symmetrical Full-Image Rendering:**
   - In [`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt), remove the `Text("Quote Zone Editor")` header composable.
   - Symmetrical layout diagnosis: When `Card(modifier = Modifier.fillMaxWidth().aspectRatio(screenAspectRatio))` is placed inside a `Column` below controls, `.fillMaxWidth()` computes height as `screenWidth / aspect = screenHeight`. Adding top controls forces the card's bottom portion below the screen boundary!
   - To achieve 100% complete, perfectly centered, symmetrical image rendering without clipping: Create a reusable `CleanWallpaperPreviewScaffold` composable in `com.example.wallpaperapp.ui.components` that uses `BoxWithConstraints` to center and scale the preview container symmetrically to fit within available bounds without vertical overflow or bottom cutoff!

2. **Unify Preview & Clean Long-Press Capabilities Across ALL Preview Screens:**
   - Implement `CleanWallpaperPreviewScaffold` with built-in long-press gesture handling (`detectTapGestures(onLongPress = { isCleanMode = !isCleanMode })`).
   - Refactor all 4 preview destinations in the app to use `CleanWallpaperPreviewScaffold`:
     1. **Zone Editor Preview** ([`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt))
     2. **Font Preview Screen** ([`FontPreviewScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/font/FontPreviewScreen.kt))
     3. **Color Scheme Preview Screen** ([`ColorSchemePreviewScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/color/ColorSchemePreviewScreen.kt))
     4. **Pools Full-Screen Image & Quote Preview** ([`PoolsScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/pools/PoolsScreen.kt))

3. **Step Execution Plan:**
   - **Step 2:** Remove Zone Editor top header text and build `CleanWallpaperPreviewScaffold`. Refactor `ZoneEditorScreen.kt` for 100% symmetrical full-image rendering with zero bottom clipping.
   - **Step 3:** Refactor `FontPreviewScreen.kt`, `ColorSchemePreviewScreen.kt`, and `PoolsScreen.kt` using `CleanWallpaperPreviewScaffold` for unified long-press clean view and full-aspect symmetry across ALL previews in the app. Build debug APK (`./gradlew assembleDebug`).

### Verification Status
- Built and ran unit tests (`./gradlew testDebugUnitTest`): **SUCCESS** (28/28 unit tests passed).

---

## Step 2: Remove Zone Editor Top Header & Build Reusable CleanWallpaperPreviewScaffold

### Implementation Summary
- **Created Reusable Preview Scaffold ([`CleanWallpaperPreviewScaffold.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/components/CleanWallpaperPreviewScaffold.kt)):**
  - Encapsulates device aspect-ratio preview container centering, tap/long-press gesture detection (`detectTapGestures`), and animated top/bottom controls overlay (`AnimatedVisibility`).
- **Zone Editor Refactoring ([`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt)):**
  - Removed unnecessary `Text("Quote Zone Editor")` header text.
  - Wrapped preview inside `CleanWallpaperPreviewScaffold`, resolving vertical height calculation overflow and ensuring 100% complete, symmetrical preview rendering with zero bottom clipping.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (28/28 unit tests passed).

---

## Step 3: Unify Clean Long-Press Preview & Full-Aspect Symmetry Across ALL Preview Screens

### Implementation Summary
- **Unified Preview Functionality Across ALL Preview Screens:**
  - Refactored [`FontPreviewScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/font/FontPreviewScreen.kt), [`ColorSchemePreviewScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/color/ColorSchemePreviewScreen.kt), and [`PoolsScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/pools/PoolsScreen.kt) (`PoolItemFullScreenPreview`) using [`CleanWallpaperPreviewScaffold.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/components/CleanWallpaperPreviewScaffold.kt).
  - Ensured long-pressing the preview background to toggle clean mode (hiding/fading all controls and overlays) and tapping to restore controls works 100% consistently across every single preview screen in the app.
  - Ensured 100% symmetrical, unclipped aspect-ratio preview rendering across all preview destinations.

### Verification Status
- Built and ran unit tests (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (28/28 unit tests passed).
- Built Debug APK (`./gradlew assembleDebug`): **SUCCESS** ([`app/build/outputs/apk/debug/app-debug.apk`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/build/outputs/apk/debug/app-debug.apk)).
