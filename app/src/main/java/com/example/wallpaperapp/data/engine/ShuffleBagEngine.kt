package com.example.wallpaperapp.data.engine

import com.example.wallpaperapp.domain.engine.ShuffleEngine
import kotlin.random.Random

class ShuffleBagEngine<T>(
    private val random: Random = Random.Default
) : ShuffleEngine<T> {

    private val lock = Any()

    override fun selectNext(
        pool: List<T>,
        historyQueue: List<String>,
        idProvider: (T) -> String
    ): Pair<T, List<String>>? = synchronized(lock) {
        if (pool.isEmpty()) return null

        val poolMap = pool.associateBy(idProvider)
        val validHistory = historyQueue.filter { poolMap.containsKey(it) }

        // Find remaining unserved items in the current cycle
        val unservedCandidates = pool.filter { idProvider(it) !in validHistory }

        if (unservedCandidates.isNotEmpty()) {
            // Pick randomly from unserved candidates in the current cycle
            val selected = unservedCandidates[random.nextInt(unservedCandidates.size)]
            val updatedHistory = validHistory + idProvider(selected)
            Pair(selected, updatedHistory)
        } else {
            // Current cycle is exhausted, start a new cycle.
            val lastServedId = validHistory.lastOrNull()

            val newCycleCandidates = if (pool.size > 1 && lastServedId != null) {
                // Ensure first item of new cycle does not match last item of previous cycle
                pool.filter { idProvider(it) != lastServedId }
            } else {
                pool
            }

            val selected = newCycleCandidates[random.nextInt(newCycleCandidates.size)]
            val newHistory = listOf(idProvider(selected))
            Pair(selected, newHistory)
        }
    }
}
