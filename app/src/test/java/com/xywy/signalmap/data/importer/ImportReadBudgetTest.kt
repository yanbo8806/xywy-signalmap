package com.xywy.signalmap.data.importer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class ImportReadBudgetTest {
    @Test
    fun `exact limit is accepted but next byte is rejected`() {
        assertContentEquals(byteArrayOf(1, 2), ImportReadBudget(2).read(byteArrayOf(1, 2).inputStream(), 2, "test"))
        assertFailsWith<IllegalArgumentException> {
            ImportReadBudget(10).read(ByteArray(3).inputStream(), 2, "test")
        }
    }

    @Test
    fun `skipped entries count toward the cumulative decompression budget`() {
        val budget = ImportReadBudget(5)
        budget.discard(ByteArray(3).inputStream())
        assertFailsWith<IllegalArgumentException> { budget.read(ByteArray(3).inputStream(), 4, "test") }
    }
}
