package com.example.wallpaperapp.domain.engine

interface ShuffleEngine<T> {
    /**
     * Selects the next item from [pool] according to non-repeating shuffle bag rules.
     * Maintains [historyQueue] of item IDs already served in the current cycle.
     * When pool is exhausted, reshuffles ensuring the first item of the new cycle
     * does not match the last item served in the previous cycle.
     *
     * @return A pair of (selectedItem, updatedHistoryQueue) or null if pool is empty.
     */
    fun selectNext(
        pool: List<T>,
        historyQueue: List<String>,
        idProvider: (T) -> String
    ): Pair<T, List<String>>?
}
