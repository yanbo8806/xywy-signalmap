package com.xywy.signalmap.domain.search

import com.xywy.signalmap.domain.model.SectorRecord

object SearchResultMatcher {
    fun search(
        records: List<SectorRecord>,
        query: String,
        limit: Int = 20,
    ): List<SectorRecord> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank() || limit <= 0) {
            return emptyList()
        }
        val results = ArrayList<SectorRecord>(minOf(limit, records.size))
        records.forEach { record ->
            if (record.searchText.contains(normalizedQuery, ignoreCase = true)) {
                results += record
                if (results.size >= limit) {
                    return results
                }
            }
        }
        return results
    }
}
