package com.example.wallpaperapp.data.engine

import com.example.wallpaperapp.domain.model.Quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class ShuffleBagEngineTest {

    private lateinit var engine: ShuffleBagEngine<Quote>

    @Before
    fun setUp() {
        engine = ShuffleBagEngine(Random(12345))
    }

    @Test
    fun `selectNext with empty pool returns null`() {
        val result = engine.selectNext(emptyList(), emptyList()) { it.id }
        assertNull(result)
    }

    @Test
    fun `selectNext with single item pool returns item repeatedly`() {
        val pool = listOf(Quote("1", "Single Quote", "Author"))
        var history = emptyList<String>()

        for (i in 1..5) {
            val result = engine.selectNext(pool, history) { it.id }
            assertNotNull(result)
            assertEquals("1", result!!.first.id)
            history = result.second
            assertEquals(listOf("1"), history)
        }
    }

    @Test
    fun `selectNext exhausts entire pool without duplicates in single cycle`() {
        val pool = listOf(
            Quote("q1", "Quote 1"),
            Quote("q2", "Quote 2"),
            Quote("q3", "Quote 3"),
            Quote("q4", "Quote 4"),
            Quote("q5", "Quote 5")
        )

        var history = emptyList<String>()
        val servedIds = mutableListOf<String>()

        for (i in 1..pool.size) {
            val result = engine.selectNext(pool, history) { it.id }
            assertNotNull(result)
            val (quote, updatedHistory) = result!!
            servedIds.add(quote.id)
            history = updatedHistory
        }

        assertEquals(pool.size, servedIds.distinct().size)
        assertEquals(pool.size, history.size)
    }

    @Test
    fun `selectNext new cycle first item does not match last item of previous cycle`() {
        val pool = listOf(
            Quote("q1", "Quote 1"),
            Quote("q2", "Quote 2"),
            Quote("q3", "Quote 3")
        )

        // Run across multiple cycles and verify boundary transition
        var history = emptyList<String>()
        var previousLastId: String? = null

        for (cycle in 1..100) {
            val cycleServed = mutableListOf<String>()
            for (step in 1..pool.size) {
                val result = engine.selectNext(pool, history) { it.id }!!
                cycleServed.add(result.first.id)
                history = result.second
            }

            if (previousLastId != null) {
                val newCycleFirstId = cycleServed.first()
                assertNotEquals(
                    "First item of new cycle ($newCycleFirstId) must not match last item of previous cycle ($previousLastId)",
                    previousLastId,
                    newCycleFirstId
                )
            }

            previousLastId = cycleServed.last()
        }
    }

    @Test
    fun `selectNext handles invalid history queue when items are removed from pool`() {
        val initialPool = listOf(
            Quote("q1", "Quote 1"),
            Quote("q2", "Quote 2"),
            Quote("q3", "Quote 3")
        )

        // History contains a deleted quote ID ("deleted_q")
        val history = listOf("q1", "deleted_q")
        val currentPool = listOf(
            Quote("q1", "Quote 1"),
            Quote("q2", "Quote 2")
        )

        val result = engine.selectNext(currentPool, history) { it.id }
        assertNotNull(result)
        // Since q1 was served, q2 must be selected next
        assertEquals("q2", result!!.first.id)
        assertEquals(listOf("q1", "q2"), result.second)
    }
}
