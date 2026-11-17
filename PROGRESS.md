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

## Phase 4 - Step 1: Root Cause Diagnosis & Codebase Inspection

### Diagnosis Summary: Gesture Jank & Text Overflow Bugs
1. **Bounding Box Drag Jank & Stutter (`ZoneEditorScreen.kt`):**
   - **DataStore Writes inside `onDrag`:** `viewModel.updateRect()` was called on every single pixel move during gestures, writing to disk DataStore on every drag event.
   - **PointerInput Key Invalidation:** Passing `rect` into `.pointerInput(containerWidthPx, containerHeightPx, rect)` caused Compose to cancel and restart active drag gestures mid-drag whenever `rect` changed.
   - **Preview Container Aspect Ratio:** `Card` used `.weight(1f)` without matching the physical device display aspect ratio (`screenWidth / screenHeight`).
2. **Text Overflow & Off-Screen Clipping for Long Quotes (`CanvasWallpaperRenderer.kt`):**
   - **Missing Hard Canvas Clip:** Text drawing lacked `canvas.clipRect(leftPx, topPx, rightPx, bottomPx)` boundaries, allowing glyphs to bleed outside `NormalizedRect` or off-screen.
   - **Missing MaxLines & Truncation Fallback:** When a quote is long and font scaling hits `minFontSizeSp`, `createStaticLayout` did not calculate or enforce `.setMaxLines()` or `.setEllipsize(TextUtils.TruncateAt.END)`.
   - **Vertical Orientation Bounds & Matrix:** Rotated text required exact pivot translation and hard canvas clipping boundaries.

### Resolution & Action Plan (Steps 2 & 3)
- **Step 2 (Fix Text Auto-Fitting, Rotation & Hard Clipping):**
  - In `CanvasWallpaperRenderer.kt`, add `canvas.clipRect(leftPx, topPx, rightPx, bottomPx)` around text rendering.
  - In `findOptimalTextLayout` / `createStaticLayout`, lower font floor to `10sp`, calculate max lines, and apply `TextUtils.TruncateAt.END` fallback when font size hits minimum.
  - Add unit tests for long quote truncation and strict bounding.
- **Step 3 (Zone Editor 60fps Gesture Smoothness & Aspect Ratio Parity):**
  - In `ZoneEditorScreen.kt`, hold local ephemeral `mutableStateOf(rect)` for 60fps frame rendering during gestures, calling `viewModel.updateRect()` ONLY on `onDragEnd` / `onDragCancel`.
  - Use `rememberUpdatedState` inside `pointerInput(Unit)` so gestures remain continuously locked without mid-drag cancellation.
  - Lock preview container to true screen aspect ratio (`screenWidth / screenHeight`) and add a "Long Quote Preview" toggle.

---

## Phase 5 - Step 1: Codebase Inspection & Feature Integration Plan

### Architecture & Codebase Inspection Summary
- **Inspected Key Components:**
  - `RenderConfig` (`com.example.wallpaperapp.domain.model`): Holds rendering configuration.
  - `SettingsRepository` & `DataStoreSettingsRepository`: Manages preference persistence via Jetpack DataStore.
  - `CanvasWallpaperRenderer`: Handles bitmap decoding, contrast calculation, static layout fitting, and canvas rendering.
  - `DashboardScreen` / `DashboardViewModel`: Manages target selection, scheduling, and theme settings.
  - `ZoneEditorScreen` / `ZoneEditorViewModel`: Manages interactive zone selection and live quote typography preview.

### User Clarifications Confirmed
- **Git Push Policy:** Local Git commits ONLY. No `git push`.
- **Landscape Image Auto-Rotation:** Rotate landscape images (`width > height`) `90°` Clockwise when targeting portrait displays (`screenHeight > screenWidth`).

### Concrete Integration Plan (Steps 2–4)
- **Step 2 (Domain & Data Layer Extensions):**
  - Create pure Kotlin model `QuoteFont` with built-in font presets (`builtin_sans`, `builtin_serif`, `builtin_mono`, `builtin_cursive`) and custom font support.
  - Create `FontStorageManager` to safely copy custom `.ttf`/`.otf` files into `context.filesDir/fonts/`.
  - Extend `SettingsRepository` to observe/set `quoteFontId`.
  - Update `RenderConfig` to include `fontId: String = "builtin_sans"`.
  - Update `RotateWallpaperUseCase` to observe `quoteFontId` and pass it to `RenderConfig`.
- **Step 3 (Graphics & Landscape Auto-Rotate Engine Update):**
  - Update `CanvasWallpaperRenderer`:
    1. Resolve `Typeface` from `fontId` (built-in presets vs custom font file in `context.filesDir/fonts/`).
    2. Inspect bitmap dimensions `(srcWidth, srcHeight)` vs display dimensions `(targetWidth, targetHeight)`: if `targetHeight > targetWidth` (portrait screen) and `srcWidth > srcHeight` (landscape image), automatically rotate bitmap `90°` clockwise before cropping/scaling.
- **Step 4 (UI & Theme Integration for Custom Fonts & Expanded Intervals):**
  - Update `DashboardScreen.kt`: replace interval selection with exact 10 frequencies (15m, 30m, 1h, 2h, 3h, 4h, 6h, 8h, 12h, 24h/Daily).
  - Update `ZoneEditorScreen.kt`: add Font Picker UI (built-in preset chips + custom `.ttf`/`.otf` file importer button), rendering live typography preview using active typeface.
  - Re-verify debug APK build and unit test suite.

---

## Phase 5 - Step 2: Domain & Data Layer Extensions

### Domain & Data Layer Implementation Summary
- **Pure Kotlin Font Model (`QuoteFont.kt`):**
  - Created `QuoteFont.kt` defining `FontType` (`BUILTIN_SANS`, `BUILTIN_SERIF`, `BUILTIN_MONO`, `BUILTIN_CURSIVE`, `CUSTOM_FILE`) and built-in font presets.
- **DataStore & Custom Font Storage Persistence:**
  - Created `FontStorageManager.kt` to safely copy custom `.ttf`/`.otf` font files from Uris into `context.filesDir/fonts/`, maintaining persistent access across background worker runs.
  - Extended `SettingsRepository` & `DataStoreSettingsRepository` to observe/set `key_quote_font_id` with fallback `"builtin_sans"`.
  - Updated `RenderConfig` to include `fontId: String = "builtin_sans"`.
- **Use Case & Unit Tests:**
  - Updated `RotateWallpaperUseCase` to observe `quoteFontId` and pass it into `RenderConfig`.
  - Updated `RotateWallpaperUseCaseTest` fake repository.

### Verification Status
- Built & verified Kotlin compilation & unit test suite (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (15/15 unit tests passed).
