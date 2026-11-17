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

## Phase 4 - Step 2: Fix Text Auto-Fitting, Vertical Rotation & Hard Clipping

### Text Fitting & Clipping Resolution Summary
- **Hard Canvas Clipping Boundary (`CanvasWallpaperRenderer.kt`):**
  - Added `canvas.clipRect(leftPx, topPx, rightPx, bottomPx)` inside `canvas.save()` and `canvas.restore()`.
  - Guarantees 100% hard protection against any quote text or author glyph bleeding outside the bounding box or off-screen.
- **Long Quote Auto-Fit & Truncation Fallback:**
  - Lowered minimum font floor constraint to `10sp`.
  - Added automatic line height capacity calculation (`fitLines`) and `TextUtils.TruncateAt.END` ellipsis fallback in `findOptimalTextLayout()` when a quote exceeds layout capacity at minimum font size.
- **Rotated Text Alignment:**
  - Aligned matrix center pivot rotation for `VERTICAL_UP` (-90°) and `VERTICAL_DOWN` (+90°) cleanly inside the hard clip rectangle.

### Verification Status
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (15/15 unit tests passed, including `CanvasWallpaperRendererTest`).

---

## Phase 4 - Step 3: Refactor Zone Editor for 60fps Smooth Gestures & Exact Bounds Preview

### Zone Editor Gesture Refactoring Summary
- **Decoupled Drag State for Zero-Jank 60/120fps Rendering (`ZoneEditorScreen.kt`):**
  - Ephemeral `localRect` (`mutableStateOf(savedRect)`) handles drag movements smoothly in memory on every frame.
  - `viewModel.updateRect(currentLocalRect)` is called **only** on `onDragEnd` and `onDragCancel`, eliminating disk I/O latency during active drag gestures.
- **Eliminated PointerInput Re-Keying Stutter:**
  - Used `rememberUpdatedState` inside `.pointerInput(Unit)` so drag gesture detectors remain continuously active without being cancelled or restarted mid-gesture.
- **Aspect-Ratio-Locked Preview Canvas:**
  - Locked the editor preview container using `Modifier.aspectRatio(screenAspectRatio)` matching the device's physical screen dimensions (`screenWidth / screenHeight`), ensuring 1:1 coordinate parity between preview and final wallpaper.
- **Live Preview & "Long Text" Toggle:**
  - Added text bounds clipping (`clipToBounds()`) and ellipsis overflow handling.
  - Added a "Long Text" FilterChip toggle to test long quote layout behavior interactively.

### Verification Status
- Built & verified Debug APK (`./gradlew assembleDebug`): **SUCCESS** (`app/build/outputs/apk/debug/app-debug.apk`).
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (15/15 unit tests passed).
