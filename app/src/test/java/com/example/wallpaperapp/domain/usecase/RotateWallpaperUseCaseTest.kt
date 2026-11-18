package com.example.wallpaperapp.domain.usecase

import android.graphics.Bitmap
import com.example.wallpaperapp.data.engine.ShuffleBagEngine
import com.example.wallpaperapp.domain.applier.WallpaperApplier
import com.example.wallpaperapp.domain.engine.WallpaperRenderer
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.LandscapeHandlingMode
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.RenderConfig
import com.example.wallpaperapp.domain.model.ScheduleConfig
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.domain.model.WallpaperImage
import com.example.wallpaperapp.domain.model.WallpaperTarget
import com.example.wallpaperapp.domain.repository.ImageRepository
import com.example.wallpaperapp.domain.repository.QuoteRepository
import com.example.wallpaperapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RotateWallpaperUseCaseTest {

    private class FakeQuoteRepository : QuoteRepository {
        var quotes = mutableListOf<Quote>()
        override suspend fun getAllQuotes(): List<Quote> = quotes
        override suspend fun getQuoteById(id: String): Quote? = quotes.find { it.id == id }
        override suspend fun addQuote(quote: Quote) { quotes.add(quote) }
        override suspend fun addQuotes(quotes: List<Quote>) { this.quotes.addAll(quotes) }
        override suspend fun deleteQuote(id: String) { quotes.removeAll { it.id == id } }
        override fun observeAllQuotes(): Flow<List<Quote>> = MutableStateFlow(quotes)
    }

    private class FakeImageRepository : ImageRepository {
        var images = mutableListOf<WallpaperImage>()
        override suspend fun getAllImages(): List<WallpaperImage> = images
        override suspend fun getImageById(id: String): WallpaperImage? = images.find { it.id == id }
        override suspend fun addImage(image: WallpaperImage) { images.add(image) }
        override suspend fun addImages(images: List<WallpaperImage>) { this.images.addAll(images) }
        override suspend fun deleteImage(id: String) { images.removeAll { it.id == id } }
        override fun observeAllImages(): Flow<List<WallpaperImage>> = MutableStateFlow(images)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var target = WallpaperTarget.BOTH
        var rect = NormalizedRect.CENTER
        var schedule = ScheduleConfig()
        var quoteQueue = emptyList<String>()
        var imageQueue = emptyList<String>()
        var appThemeMode = AppThemeMode.SYSTEM
        var textOrientation = TextOrientation.HORIZONTAL

        var quoteFontId = "builtin_sans"

        private val targetState = MutableStateFlow(target)
        private val rectState = MutableStateFlow(rect)
        private val scheduleState = MutableStateFlow(schedule)
        private val quoteQueueState = MutableStateFlow(quoteQueue)
        private val imageQueueState = MutableStateFlow(imageQueue)
        private val themeModeState = MutableStateFlow(appThemeMode)
        private val orientationState = MutableStateFlow(textOrientation)
        private val fontIdState = MutableStateFlow(quoteFontId)

        override fun observeWallpaperTarget(): Flow<WallpaperTarget> = targetState
        override suspend fun setWallpaperTarget(target: WallpaperTarget) {
            this.target = target
            targetState.value = target
        }

        override fun observeNormalizedRect(): Flow<NormalizedRect> = rectState
        override suspend fun setNormalizedRect(rect: NormalizedRect) {
            this.rect = rect
            rectState.value = rect
        }

        override fun observeScheduleConfig(): Flow<ScheduleConfig> = scheduleState
        override suspend fun setScheduleConfig(config: ScheduleConfig) {
            this.schedule = config
            scheduleState.value = config
        }

        override fun observeQuoteShuffleQueue(): Flow<List<String>> = quoteQueueState
        override suspend fun setQuoteShuffleQueue(queue: List<String>) {
            this.quoteQueue = queue
            quoteQueueState.value = queue
        }

        override fun observeImageShuffleQueue(): Flow<List<String>> = imageQueueState
        override suspend fun setImageShuffleQueue(queue: List<String>) {
            this.imageQueue = queue
            imageQueueState.value = queue
        }

        override fun observeAppThemeMode(): Flow<AppThemeMode> = themeModeState
        override suspend fun setAppThemeMode(themeMode: AppThemeMode) {
            this.appThemeMode = themeMode
            themeModeState.value = themeMode
        }

        override fun observeTextOrientation(): Flow<TextOrientation> = orientationState
        override suspend fun setTextOrientation(orientation: TextOrientation) {
            this.textOrientation = orientation
            orientationState.value = orientation
        }

        override fun observeQuoteFontId(): Flow<String> = fontIdState
        override suspend fun setQuoteFontId(fontId: String) {
            this.quoteFontId = fontId
            fontIdState.value = fontId
        }

        private val boldState = MutableStateFlow(false)
        private val italicState = MutableStateFlow(false)
        private val shuffleFontsState = MutableStateFlow(false)
        private val fontQueueState = MutableStateFlow(emptyList<String>())
        private val landscapeModeState = MutableStateFlow(LandscapeHandlingMode.ROTATE_90_CW)

        override fun observeIsBold(): Flow<Boolean> = boldState
        override suspend fun setIsBold(isBold: Boolean) { boldState.value = isBold }

        override fun observeIsItalic(): Flow<Boolean> = italicState
        override suspend fun setIsItalic(isItalic: Boolean) { italicState.value = isItalic }

        override fun observeShuffleFonts(): Flow<Boolean> = shuffleFontsState
        override suspend fun setShuffleFonts(enabled: Boolean) { shuffleFontsState.value = enabled }

        override fun observeFontShuffleQueue(): Flow<List<String>> = fontQueueState
        override suspend fun setFontShuffleQueue(queue: List<String>) { fontQueueState.value = queue }

        override fun observeLandscapeHandlingMode(): Flow<LandscapeHandlingMode> = landscapeModeState
        override suspend fun setLandscapeHandlingMode(mode: LandscapeHandlingMode) { landscapeModeState.value = mode }
    }

    private class FakeWallpaperRenderer : WallpaperRenderer<Any, Bitmap> {
        val dummyBitmap: Bitmap = run {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val field = unsafeClass.getDeclaredField("theUnsafe")
            field.isAccessible = true
            val unsafe = field.get(null)
            val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
            allocateInstance.invoke(unsafe, Bitmap::class.java) as Bitmap
        }

        override suspend fun renderWallpaper(
            imageInput: Any,
            quote: Quote,
            config: RenderConfig
        ): Bitmap {
            return dummyBitmap
        }
    }

    private class FakeWallpaperApplier : WallpaperApplier<Bitmap> {
        var appliedTarget: WallpaperTarget? = null
        override suspend fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget): Boolean {
            appliedTarget = target
            return true
        }
    }

    private lateinit var quoteRepository: FakeQuoteRepository
    private lateinit var imageRepository: FakeImageRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var renderer: FakeWallpaperRenderer
    private lateinit var applier: FakeWallpaperApplier
    private lateinit var useCase: RotateWallpaperUseCase

    @Before
    fun setUp() {
        quoteRepository = FakeQuoteRepository()
        imageRepository = FakeImageRepository()
        settingsRepository = FakeSettingsRepository()
        renderer = FakeWallpaperRenderer()
        applier = FakeWallpaperApplier()

        useCase = RotateWallpaperUseCase(
            quoteRepository = quoteRepository,
            imageRepository = imageRepository,
            settingsRepository = settingsRepository,
            quoteShuffleEngine = ShuffleBagEngine(),
            imageShuffleEngine = ShuffleBagEngine(),
            wallpaperRenderer = renderer,
            wallpaperApplier = applier
        )
    }

    @Test
    fun `invoke returns failure when quotes pool is empty`() = runTest {
        imageRepository.images.add(WallpaperImage("img1", "content://media/1"))
        val result = useCase()
        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke returns failure when images pool is empty`() = runTest {
        quoteRepository.quotes.add(Quote("q1", "Text 1"))
        val result = useCase()
        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke successfully rotates wallpaper and updates shuffle queues`() = runTest {
        quoteRepository.quotes.add(Quote("q1", "Text 1"))
        imageRepository.images.add(WallpaperImage("img1", "content://media/1"))

        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(WallpaperTarget.BOTH, applier.appliedTarget)
        assertEquals(listOf("q1"), settingsRepository.quoteQueue)
        assertEquals(listOf("img1"), settingsRepository.imageQueue)
    }
}
