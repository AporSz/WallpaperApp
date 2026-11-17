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
  - `ImageRepository`: Interface for wallpaper image persistence & observation.
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

## Phase 3 - Step 1: Root Cause Diagnosis & Codebase Inspection

### Diagnosis Summary: Solid Grey Background Bug
1. **Missing Persistable URI Read Permissions (`PoolsScreen.kt`):**
   - When importing images via `PickMultipleVisualMedia()`, `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` was not invoked.
   - When `WallpaperWorker` runs in the background or app process restarts, `context.contentResolver.openInputStream(uri)` throws a `SecurityException`.
2. **Silent Failure & Solid Dark Grey Fallback (`CanvasWallpaperRenderer.kt`):**
   - `loadScaledBitmap()` caught all exceptions silently and returned `null`.
   - When `loadScaledBitmap()` returned `null`, `renderWallpaper()` silently fell back to `createDefaultBackground()`, filling the Canvas with solid dark grey (`Color.DKGRAY`).
3. **Bitmap Decoding & Stream Reuse:**
   - Need explicit `inMutable = true` and `inPreferredConfig = Bitmap.Config.ARGB_8888` on decoded bitmaps, plus logging diagnostics (`Log.e`).
4. **Invalid URI Pruning / Recovery (`RotateWallpaperUseCase.kt`):**
   - When an unreadable or revoked URI is encountered, the use case should log the error and advance/prune the invalid URI rather than rendering a solid grey screen.

### Design Plan: Interactive Bounding Box Editor (Step 3)
- Extract clean geometry math helper (`CropBoxGeometry.kt` / `NormalizedRect` extensions) for 8 handles: Top-Left, Top-Right, Bottom-Left, Bottom-Right, Top, Bottom, Left, Right + interior translation, with `[0f, 1f]` boundary clamping and `15% x 10%` minimum dimensions.
- Upgrade `ZoneEditorScreen.kt` with outer dimmed scrim, 8 visible drag handles, center guidelines, and live text orientation rotation preview.

---

## Phase 3 - Step 2: Fix Background Image Loading, Permissions & Canvas Compositing

### Fix & Resolution Summary
- **Persistable URI Permissions (`PoolsScreen.kt`):**
  - Integrated `context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` when importing images in `PoolsScreen.kt`.
- **Bitmap Decoding, EXIF & Error Visibility (`CanvasWallpaperRenderer.kt`):**
  - Updated `decodeUriDownsampled` requesting `inMutable = true` and `inPreferredConfig = Bitmap.Config.ARGB_8888`.
  - Added EXIF orientation detection via `ExifInterface` to automatically rotate image streams to upright orientation.
  - Replaced silent dark grey fallback with `Log.e` diagnostics, returning `null` when bitmap decoding fails.
- **Unreadable URI Recovery & Auto-Pruning (`RotateWallpaperUseCase.kt`):**
  - Added loop in `RotateWallpaperUseCase` to automatically prune invalid/unreadable URIs from `ImageRepository` and try candidate images in the pool instead of failing to a grey screen.

### Verification Status
- Built & verified Debug APK (`./gradlew assembleDebug`): **SUCCESS**.
- Installed & launched on physical device `39271FDJG001T8`: **SUCCESS**.
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (8/8 unit tests passed).

---

## Phase 3 - Step 3: Screenshot-Style Interactive Bounding Box Editor

### Interactive Editor Implementation Summary
- **Pure Geometry Math Engine (`CropBoxGeometry.kt` & `CropBoxGeometryTest.kt`):**
  - Created `CropBoxGeometry.kt` supporting 8-handle resizing and interior moving:
    - 4 corner handles (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`) resizing width and height simultaneously.
    - 4 edge handles (`TOP`, `BOTTOM`, `LEFT`, `RIGHT`) lengthening or shrinking single dimensions independently.
    - Interior drag (`CENTER`) translating the bounding box across screen boundaries.
  - Clamps coordinates strictly within `[0.0f, 1.0f]` and enforces minimum box constraints (`15%` width, `10%` height).
  - Added unit test suite in `CropBoxGeometryTest.kt` verifying handle drags, boundary clamping, and minimum constraints.
- **Screenshot-Style Crop UI (`ZoneEditorScreen.kt`):**
  - Dimmed outer background scrim (`Color.Black.copy(alpha = 0.55f)`).
  - Rule-of-thirds guide lines rendered dynamically inside the selection window while dragging.
  - 8 visible, high-contrast touch grab handles (white circles for corners, white pills for edges) with generous `36.dp` touch targets.
  - Real-time text preview rendering inside the active selection box (respecting current text orientation and contrast).

### Verification Status
- Built & verified Debug APK (`./gradlew assembleDebug`): **SUCCESS** (`app/build/outputs/apk/debug/app-debug.apk`).
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (13/13 unit tests passed).
