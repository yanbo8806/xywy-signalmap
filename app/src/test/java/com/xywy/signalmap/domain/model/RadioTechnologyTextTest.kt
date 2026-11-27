package com.xywy.signalmap.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RadioTechnologyTextTest {
    @Test
    fun `unknown radio technology label is readable chinese`() {
        assertEquals("未知", RadioTechnology.UNKNOWN.label)
    }
}
