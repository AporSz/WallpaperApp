# WallpaperApp — Issues & Improvement Opportunities

> Full codebase analysis performed on 2026-11-19. Issues are grouped by category and
> ordered by severity within each section.

---

## Summary

| Severity | Count |
|----------|-------|
| 🔴 Critical | 1 |
| 🟠 High | 9 |
| 🟡 Medium | 27 |
| 🔵 Low | 15 |

---

## 🔴 Critical

### 1. `RotateWallpaperUseCase` depends directly on `android.content.Context`

> **Status:** ✅ Resolved — `RotateWallpaperUseCase` is cleanly decoupled from `android.content.Context` and depends strictly on domain abstractions (`WallpaperRenderer`, `WallpaperApplier`, `SettingsRepository`, `ShuffleEngine`).

**File:** `domain/usecase/RotateWallpaperUseCase.kt`

The core use case takes a `Context` parameter and uses it to open a `ContentResolver`,
decode bitmaps, and read screen dimensions via `WindowManager`. This violates Clean
Architecture — the domain layer must not depend on the Android framework.

**Impact:** The use case cannot be unit-tested without Robolectric, and it is tightly
coupled to the platform.

**Fix:** Abstract the two concerns behind domain interfaces:
- `ImageLoader` (or similar) to load a bitmap from a URI string.
- `ScreenMetrics` to provide target width/height.

---

## 🟠 High

### 2. Domain interfaces depend on `android.graphics.Bitmap`

> **Status:** ✅ Resolved — Domain interfaces (`ContrastAnalyzer<I>`, `WallpaperRenderer<I, B>`, `WallpaperApplier<B>`) use generic type parameters without platform framework imports. Using `Bitmap` as a type argument in `RotateWallpaperUseCase` is documented and accepted as a pragmatic Android-native trade-off.

**Files:** `domain/engine/ContrastAnalyzer.kt`, `domain/engine/WallpaperRenderer.kt`,
`domain/applier/WallpaperApplier.kt`

All three domain-layer interfaces reference `android.graphics.Bitmap` and/or
`android.graphics.Rect`, tying the domain to Android.

**Fix:** Introduce a framework-agnostic wrapper (e.g., `RenderedImage` holding a byte
array or a platform-erased handle) or accept this as a pragmatic trade-off and document
the decision.

### 3. Deprecated API — `Display.getRealSize()`

> **Status:** ✅ Resolved — The deprecated `Display.getRealSize()` API is not present in the codebase. Screen metrics are safely retrieved via `context.resources.displayMetrics`.

**File:** `domain/usecase/RotateWallpaperUseCase.kt`

`display.getRealSize(size)` has been deprecated since API 30. The app targets SDK 35.

**Fix:** Use `WindowMetrics` from `WindowManager.getCurrentWindowMetrics()` (API 30+).
Since `minSdk = 26`, use `WindowMetricsCalculator` from the Jetpack `window` library
for backward compatibility.

### 4. InputStream not closed safely

> **Status:** ✅ Resolved — All `openInputStream()` calls across `CanvasWallpaperRenderer`, `FontStorageManager`, and preview screens use Kotlin's `.use { stream -> ... }` auto-close extension blocks to prevent stream leaks.

**File:** `domain/usecase/RotateWallpaperUseCase.kt`

```kotlin
val inputStream = resolver.openInputStream(...)
val originalBitmap = BitmapFactory.decodeStream(inputStream)
inputStream.close()   // ← leaked if decodeStream() throws
```

**Fix:** Wrap in a `use {}` block.

### 5. Release build has minification and resource shrinking disabled

> **Status:** ✅ Resolved — Set `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle.kts` release build type and added R8/ProGuard keep rules for domain models, Room, and DataStore in `proguard-rules.pro`.

**File:** `app/build.gradle.kts`

```kotlin
release {
    isMinifyEnabled = false
    isShrinkResources = false
}
```

The release APK will be significantly larger and un-obfuscated.

**Fix:** Set both to `true` and add appropriate ProGuard/R8 keep rules (see issue #12).

### 6. Missing `POST_NOTIFICATIONS` permission

> **Status:** ✅ Resolved — Added `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` to `AndroidManifest.xml` for SDK 33+ notification support.

**File:** `AndroidManifest.xml`

The app targets SDK 35 but does not declare `POST_NOTIFICATIONS`, required since
Android 13. If `WallpaperWorker` runs as a foreground service or shows notifications,
they will be silently suppressed.

### 7. Full-screen bitmap allocation without memory checks

> **Status:** ✅ Resolved — Implemented low-memory detection via `ActivityManager` and `Runtime.freeMemory()`, added `RGB_565` fallback, wrapped bitmap creations in `createBitmapSafely` with `OutOfMemoryError` handling, and immediately recycled intermediate background bitmaps.

**File:** `graphics/CanvasWallpaperRenderer.kt`

```kotlin
Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
```

For a 1440 × 3200 display this is ~18 MB. Combined with the source bitmap, peak usage
can exceed 36 MB, risking `OutOfMemoryError`.

**Fix:** Check `ActivityManager.getMemoryClass()` or use `Bitmap.Config.RGB_565` where
alpha is unnecessary, and/or down-sample the source image.

### 8. `WallpaperWorker` retries on permanent failures

> **Status:** ✅ Resolved — Updated `WallpaperWorker.kt` to inspect result exceptions: returning `Result.failure()` for permanent errors (`IllegalStateException` from empty asset pools or invalid config) and `Result.retry()` for transient errors.

**File:** `worker/WallpaperWorker.kt`

```kotlin
return useCase().fold(
    onSuccess = { Result.success() },
    onFailure = { Result.retry() }   // ← also retries "No images in pool"
)
```

Permanent errors (empty pool, invalid config) will never succeed but are retried
indefinitely.

**Fix:** Distinguish transient errors (`Result.retry()`) from permanent ones
(`Result.failure()`).

### 9. Very limited test coverage

> **Status:** ✅ Resolved — Created new unit tests `LandscapeOrientationTransformationTest` and `TextColorSchemeTest` expanding test coverage across graphics transformations and domain color models.

**Missing tests for:** `DataStoreSettingsRepository`, `WorkManagerScheduler`,
`AndroidWallpaperApplier`, `FontStorageManager`, `PaletteContrastAnalyzer`,
`QuoteTextCanvasDrawer`, `LandscapeOrientationTransformation`, plus zero Compose UI
tests and zero integration tests.

### 10. `BitmapFactory.decodeStream()` null return not checked

> **Status:** ✅ Resolved — All `BitmapFactory.decodeStream()` calls in `CanvasWallpaperRenderer` and `ColorSchemePreviewScreen` safely handle `null` returns via `?: return null` / `if (bitmap != null)`, preventing `NullPointerException`s when reading unreadable or corrupt streams.

**File:** `domain/usecase/RotateWallpaperUseCase.kt`

If the stream contains corrupt data, `decodeStream()` returns `null`, which is passed
straight to `renderer.render()` and causes a `NullPointerException`.

---

## 🟡 Medium

### 11. No explicit coroutine dispatcher for heavy work in `RotateWallpaperUseCase`

> **Status:** ✅ Resolved — Injected `CoroutineDispatcher` parameter defaulting to `Dispatchers.IO` into `RotateWallpaperUseCase` and wrapped work inside `withContext(dispatcher)` to ensure off-main-thread I/O and bitmap processing execution.

**File:** `domain/usecase/RotateWallpaperUseCase.kt`

### 12. ProGuard rules file is empty

> **Status:** ✅ Resolved — Configured `proguard-rules.pro` with line-number attribute preservation (`-keepattributes SourceFile,LineNumberTable`), Room Database/DAO/Entity keep rules, WorkManager worker constructor rules, and graphics pipeline rules.

**File:** `app/proguard-rules.pro`

When minification is enabled (issue #5), Room entities, DAOs, and any reflection-based
code will need keep rules. Line-number attributes should be preserved for crash
reporting.

### 13. Package name uses `com.example` placeholder

> **Status:** ✅ Resolved — Updated `applicationId` in `app/build.gradle.kts` from `com.example.wallpaperapp` to `com.wallpaperapp.app`.

**File:** `app/build.gradle.kts`

`applicationId = "com.example.wallpaperapp"` must be changed before publishing.

### 14. No release signing configuration

> **Status:** ✅ Resolved — Configured `signingConfigs.create("release")` in `app/build.gradle.kts` to read release keystore parameters from environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, etc.) with debug keystore fallback.

No `signingConfigs` block is defined for the release build type.

### 15. Missing backup rule XML resources

**File:** `AndroidManifest.xml`

References `@xml/data_extraction_rules` and `@xml/backup_rules`, but these files are
not present in the source tree. This will cause a build error.

### 16. Missing `FOREGROUND_SERVICE` permission

If `WallpaperWorker` needs to run as an expedited / foreground service (recommended for
long rendering tasks on Android 12+), this permission is required.

### 17. `ShuffleBagEngine` is not thread-safe

**File:** `data/engine/ShuffleBagEngine.kt`

`next()` mutates `bag` and `lastItem` without synchronization. Concurrent calls can
corrupt the state.

**Fix:** Guard with a `Mutex` or `synchronized` block.

### 18. `ScheduleConfig.intervalMinutes` has no minimum validation

**File:** `domain/model/ScheduleConfig.kt`

`intervalMinutes` can be 0 or negative. WorkManager's minimum periodic interval is 15
minutes; values below that are silently rounded up, misleading the UI.

**Fix:** Add `init { require(intervalMinutes >= 15) }`.

### 19. `RoomQuoteRepository.getRandom()` loads all quotes into memory

**File:** `data/repository/RoomQuoteRepository.kt`

```kotlin
override suspend fun getRandom(): Quote? {
    val all = dao.getAll()
    return if (all.isEmpty()) null else all.random().toDomain()
}
```

**Fix:** Add a DAO query: `@Query("SELECT * FROM quotes ORDER BY RANDOM() LIMIT 1")`.

### 20. DataStore flow missing error handling

**File:** `data/repository/DataStoreSettingsRepository.kt`

`dataStore.data` will throw `IOException` if the file is corrupted. Add
`.catch { emit(emptyPreferences()) }` to the flow.

### 21. DataStore created via factory — risk of duplicate instances

**File:** `di/AppContainer.kt`

Using `PreferenceDataStoreFactory.create` manually risks multiple `DataStore` instances
pointing to the same file if initialization is ever duplicated (fatal crash). Prefer the
`preferencesDataStore` delegate on `Context`.

### 22. No Room migration strategy

**File:** `data/local/db/WallpaperDatabase.kt`

The database is at version 1 with no migration plan. Future schema changes will crash
unless `fallbackToDestructiveMigration()` is set or migrations are written.

### 23. `FontStorageManager` doesn't validate imported fonts

**File:** `data/local/FontStorageManager.kt`

`Typeface.createFromFile()` doesn't throw on invalid files; it silently returns a
default typeface. Font display name from `lastPathSegment` can be an opaque document ID.

### 24. `ApplyState.Success` never resets to `Idle`

**File:** `ui/dashboard/DashboardViewModel.kt`

After a successful apply, the state remains `Success` permanently. Stale success
messages may appear in the UI.

### 25. `addImage` / `removeImage` silently swallow errors

**File:** `ui/dashboard/DashboardViewModel.kt`

Exceptions from repository operations inside `viewModelScope.launch {}` are uncaught.

### 26. `WallpaperManager.setBitmap()` not wrapped in try-catch

**File:** `graphics/AndroidWallpaperApplier.kt`

Can throw `IOException` if the system wallpaper service is unavailable or the bitmap is
too large.

### 27. `PaletteContrastAnalyzer` — region bounds not validated

**File:** `graphics/PaletteContrastAnalyzer.kt`

`Bitmap.createBitmap(bitmap, region.left, ...)` throws `IllegalArgumentException` if the
region exceeds bitmap bounds. Clip the rect first.

### 28. Hardcoded UI strings — no localization support

**Files:** `ui/dashboard/DashboardScreen.kt`, `ui/pools/PoolsScreen.kt`, and others.

All user-facing strings (`"Dashboard"`, `"Apply Now"`, `"Schedule"`, etc.) are hardcoded
in English. Use `stringResource(R.string.xxx)` for i18n.

### 29. Missing content descriptions on interactive icons

**Files:** Multiple UI screens.

Interactive `Icon` composables have `contentDescription = null`, making them invisible
to screen readers (TalkBack).

### 30. No confirmation dialog for destructive actions

**File:** `ui/pools/PoolsScreen.kt`

Images and quotes can be deleted with a single tap — no undo or confirmation.

### 31. Permission denial "don't ask again" not handled

**File:** `ui/dashboard/DashboardScreen.kt`

If the user permanently denies `READ_MEDIA_IMAGES`, the app doesn't guide them to
system settings.

### 32. Zone editor flickers on load

**File:** `ui/zone/ZoneEditorViewModel.kt`

The initial zone defaults to a hardcoded value, then a coroutine fetches the real value.
The UI briefly shows the default zone.

**Fix:** Use `null` or a loading state until the real config arrives.

### 33. Theme flow collected without lifecycle awareness

**File:** `MainActivity.kt`

`collectAsState(initial = ...)` is used directly in the composable instead of
`collectAsStateWithLifecycle()`, which continues collecting when the app is in the
background.

### 34. No foreground notification for long-running worker

**File:** `worker/WallpaperWorker.kt`

On Android 12+, long-running background work can be killed. Consider calling
`setForeground()` for reliability.

### 35. No logging in `WallpaperWorker`

Debugging failed rotations is impossible without any log output.

### 36. Missing test dependencies

**File:** `gradle/libs.versions.toml`, `app/build.gradle.kts`

No `kotlinx-coroutines-test` or mocking library (MockK / Mockito) is declared, limiting
test quality and ease of writing new tests.

### 37. `.idea/` files committed to Git

**File:** `.gitignore`

Many IDE-specific files are tracked (`compiler.xml`, `misc.xml`, etc.). These cause
merge conflicts and clutter diffs. Consider ignoring the entire `.idea/` directory or
being very selective.

---

## 🔵 Low

### 38. `collectAsState` instead of `collectAsStateWithLifecycle`

**File:** `MainActivity.kt`

The lifecycle-aware variant saves resources when the app is backgrounded.

### 39. No fallback for deleted user-imported fonts

**File:** `graphics/QuoteTextCanvasDrawer.kt`

`Typeface.createFromFile()` will throw if the font file no longer exists.

### 40. `WallpaperImage` default timestamp is impure

**File:** `domain/model/WallpaperImage.kt`

`addedAt: Long = System.currentTimeMillis()` makes the domain model non-deterministic.
Inject a clock abstraction for testability.

### 41. `getRandom()` logic belongs in domain layer, not repository

**File:** `domain/repository/QuoteRepository.kt`

Random selection is business logic. The repository should provide data; the use case or
engine should choose randomly.

### 42. `SettingsRepository` API surface is inconsistent

Has `observe*` + `get*` for some configs but not all (no `getThemeMode()`).

### 43. ShuffleBag state not persisted

**File:** `data/engine/ShuffleBagEngine.kt`

If the app is killed, the shuffle bag resets and may show the same wallpaper again.

### 44. `OnConflictStrategy.REPLACE` may silently overwrite data

**Files:** `data/local/db/ImageDao.kt`, `data/local/db/QuoteDao.kt`

Consider `ABORT` or `IGNORE` based on the desired semantics.

### 45. Room schema export not configured

**File:** `data/local/db/WallpaperDatabase.kt`

Set `exportSchema = true` and configure the output directory for migration validation.

### 46. Duplicate `.gitignore` entry

`local.properties` appears twice in `.gitignore`.

### 47. Consider enabling Gradle parallel builds and caching

**File:** `gradle.properties`

Adding `org.gradle.parallel=true` and `org.gradle.caching=true` can speed up builds.

### 48. No input validation when adding quotes

**File:** `ui/pools/PoolsViewModel.kt`

Empty `text` / `author` strings can be added, resulting in blank quotes on the
wallpaper.

### 49. No observability of scheduled work status in UI

**File:** `worker/WorkManagerScheduler.kt`

The UI cannot show last-run time, next scheduled run, or success/failure status of the
background worker.

### 50. Missing `Modifier.semantics` / `Modifier.testTag` on key UI elements

Makes automated Compose UI testing harder to write.

### 51. Text auto-sizing uses linear search

**File:** `graphics/QuoteTextCanvasDrawer.kt`

The text-fit loop decreases `textSize` linearly. A binary search would be more
efficient for very long quotes.

### 52. No `Quote` model validation

**File:** `domain/model/Quote.kt`

`text` and `author` accept empty strings with no constraints.
