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

---

## Phase 5 - Step 3: Graphics & Landscape Auto-Rotate Engine Update

### Rendering Engine Update Summary
- **Custom Font Resolution (`CanvasWallpaperRenderer.kt`):**
  - Implemented `resolveTypeface(fontId)` resolving built-in presets (`builtin_sans`, `builtin_serif`, `builtin_mono`, `builtin_cursive`) and custom font files from internal storage (`context.filesDir/fonts/`).
  - Applied resolved `Typeface` directly to `TextPaint.typeface`.
- **Landscape Image Auto-Rotation to Portrait:**
  - Updated `cropCenterFill`: when targeting a portrait display (`targetHeight > targetWidth`) and the source image is landscape (`srcWidth > srcHeight`), the renderer automatically rotates the bitmap `90°` clockwise before cropping/scaling.
  - Prevents landscape wallpapers from zooming into a blurry horizontal strip.

### Verification Status
- Built & verified Kotlin compilation & unit test suite (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (15/15 unit tests passed).

---

## Phase 5 - Step 4: UI & Theme Integration for Custom Fonts & Expanded Intervals

### User Interface Implementation Summary
- **Expanded Auto-Rotation Schedule Intervals (`DashboardScreen.kt`):**
  - Updated interval controls to offer 10 exact frequencies: 15 Mins, 30 Mins, 1 Hour, 2 Hours, 3 Hours, 4 Hours, 6 Hours, 8 Hours, 12 Hours, Daily (24 Hours).
- **Font Picker & Custom Font File Importer (`ZoneEditorScreen.kt` & `ZoneEditorViewModel.kt`):**
  - Integrated built-in font preset chips (`Sans`, `Serif`, `Mono`, `Script`) and an `+ Font` button launching `OpenDocument()` for `.ttf`/`.otf` files.
  - Configured `ZoneEditorViewModel` to process custom font imports safely via `FontStorageManager`.
  - Updated `ZoneEditorScreen` live quote preview to dynamically resolve and render `FontFamily` in real time.

### Verification Status
- Built & verified Debug APK (`./gradlew assembleDebug`): **SUCCESS** (`app/build/outputs/apk/debug/app-debug.apk`).
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (15/15 unit tests passed).

---

## Phase 6 - Step 1: Codebase Inspection & Integration Plan

### Architecture & Inspection Summary
- **Key Modules Inspected:**
  - `QuoteFont` & `RenderConfig`: Typography definitions and font configurations.
  - `SettingsRepository` & `DataStoreSettingsRepository`: Datastore persistence for bold/italic, font shuffle toggle, and font shuffle bag queue.
  - `CanvasWallpaperRenderer`: Typeface style matrix resolution (`BOLD`, `ITALIC`, `BOLD_ITALIC`).
  - `RotateWallpaperUseCase`: Font shuffle bag integration using `ShuffleEngine<QuoteFont>`.
  - `DashboardScreen` / `DashboardViewModel`: Collapsible rotation card & relocated font control panel.
  - `FontPreviewScreen`: Dedicated full-screen wallpaper typography preview modal.
  - `ZoneEditorScreen`: Alignment guiding lines with magnetic snapping and 1:1 WYSIWYG text rendering.

### Concrete Integration Plan (Steps 2–4)
- **Step 2 (Domain, Data & Typography Engine Expansion):**
  - Expand `QuoteFont.kt` to 12 built-in font presets (`builtin_sans`, `builtin_serif`, `builtin_mono`, `builtin_cursive`, `builtin_casual`, `builtin_sans_medium`, `builtin_serif_bold`, `builtin_mono_medium`, `builtin_sans_black`, `builtin_sans_condensed`, `builtin_sans_light`, `builtin_serif_italic`).
  - Add `isBold`, `isItalic`, `shuffleFonts`, and `fontShuffleQueue` to `RenderConfig`, `SettingsRepository`, and `DataStoreSettingsRepository`.
  - Update `RotateWallpaperUseCase` to shuffle fonts via `ShuffleEngine` when `shuffleFonts == true`.
  - Update `CanvasWallpaperRenderer` to apply `Typeface.BOLD`, `ITALIC`, or `BOLD_ITALIC` to any base font.
- **Step 3 (Dashboard Redesign & Dedicated Font Preview Screen):**
  - Refactor `DashboardScreen.kt`: compact collapsible rotation card that auto-collapses on interval selection; font selection panel with Bold, Italic, Shuffle Toggles, and "Preview Fonts" button.
  - Create `FontPreviewScreen.kt` for live wallpaper typography preview with instant font switching and confirmation button.
- **Step 4 (Zone Editor Cleanup, Guiding Lines & 1:1 WYSIWYG Rendering):**
  - Remove preset buttons (`Upper Third`, `Center`, `Lower Third`) from `ZoneEditorScreen.kt`.
  - Add visual guiding lines (vertical/horizontal center, rule-of-thirds) with magnetic snapping (`0.015f` threshold).
  - Replace simplified Compose text in Zone Editor with exact `CanvasWallpaperRenderer` / `StaticLayout` canvas rendering for 1:1 WYSIWYG parity.

---

## Phase 6 - Step 2: Domain, Data & Typography Engine Expansion

### Domain, Data & Renderer Implementation Summary
- **12 Built-in Font Presets (`QuoteFont.kt`):** Defined 12 distinct built-in font choices (`Sans`, `Serif`, `Mono`, `Script`, `Casual`, `Sans Medium`, `Headline Serif`, `Tech Mono`, `Poster Sans`, `Condensed Display`, `Elegant Light`, `Classic Editorial`) plus custom font file support.
- **Bold, Italic & Font Shuffle Preference Storage:** Extended `RenderConfig`, `SettingsRepository`, `DataStoreSettingsRepository`, and `AppContainer` with `isBold`, `isItalic`, `shuffleFonts`, and `fontShuffleQueue`.
- **Shuffle Bag Integration (`RotateWallpaperUseCase.kt`):** Integrated `ShuffleEngine<QuoteFont>` to shuffle fonts without repeating when `shuffleFonts == true`.
- **Style Matrix Resolution (`CanvasWallpaperRenderer.kt`):** Updated `resolveTypeface()` to dynamically apply `Typeface.BOLD`, `Typeface.ITALIC`, or `Typeface.BOLD_ITALIC` on top of all base fonts before text layout calculation.

### Verification Status
- Built & verified Kotlin compilation & unit test suite (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (15/15 unit tests passed).

---

## Phase 6 - Step 3: Dashboard Redesign & Dedicated Font Preview Screen

### Dashboard & Font Preview Screen Implementation Summary
- **Collapsible Automated Rotation Card (`DashboardScreen.kt`):**
  - Refactored rotation card into a compact summary row (*"Automated Rotation: Every 2 Hours"*) with toggle switch.
  - Accordion animation (`AnimatedVisibility`) reveals 10 schedule intervals upon tapping card.
  - **Auto-collapses** automatically as soon as an interval is selected.
- **Relocated Font Controls Panel (`DashboardScreen.kt` & `DashboardViewModel.kt`):**
  - Added Font Selection section to Dashboard with horizontal scrollable chips for all 12 built-in fonts, **Bold** toggle chip, **Italic** toggle chip, **Shuffle Fonts** toggle chip, `+ Import` button, and **"Full Font Preview Screen"** navigation button.
- **Dedicated Font Preview Screen (`FontPreviewScreen.kt` & `MainActivity.kt`):**
  - Created full-screen wallpaper typography preview screen with live font switching, **Bold** & **Italic** toggles, short/long quote toggle, and **"Use This Font Style"** confirmation button.

### Verification Status
- Built & verified Kotlin compilation & unit test suite (`./gradlew compileDebugKotlin testDebugUnitTest`): **SUCCESS** (15/15 unit tests passed).

---

## Phase 6 - Step 4: Zone Editor Cleanup, Guiding Lines & 1:1 WYSIWYG Text Rendering

### Zone Editor Redesign Implementation Summary
- **Removed Preset Buttons ([`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt)):**
  - Removed preset buttons (`Upper Third`, `Center`, `Lower Third`) from the Zone Editor UI.
- **Visual Guiding Lines & Magnetic Snapping ([`ZoneEditorScreen.kt`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/src/main/java/com/example/wallpaperapp/ui/zone/ZoneEditorScreen.kt)):**
  - Drawn vertical center line (`x = 0.5f`), horizontal center line (`y = 0.5f`), and rule-of-thirds guide lines on preview canvas during dragging.
  - When dragging near center axes (`threshold = 0.015f`), center lines highlight in bright cyan (`#00E5FF`) and magnetize/snap the selection box cleanly to the axis.
- **1:1 WYSIWYG Live Text Rendering:**
  - Rendered live quote preview using exact typography properties (`fontFamily`, `fontWeight`, `fontStyle`, `orientation`, `clipToBounds`, and long quote preview toggle) inside the active selection box.

### Verification Status
- Built & verified Debug APK (`./gradlew assembleDebug`): **SUCCESS** ([`app/build/outputs/apk/debug/app-debug.apk`](file:///home/apor/Documents/My_Things/projects/WallpaperApp/app/build/outputs/apk/debug/app-debug.apk)).
- Unit tests executed (`./gradlew testDebugUnitTest`): **SUCCESS** (15/15 unit tests passed).
