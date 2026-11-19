package com.gotest.app.feature.account

import com.gotest.app.core.model.EducationLevel
import com.gotest.app.feature.account.domain.apiCode
import com.gotest.app.feature.account.ui.deleteaccount.joinNames
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileRulesTest {
    @Test
    fun `Mitra names join like the web, without an Oxford comma`() {
        assertEquals("", joinNames(emptyList()))
        assertEquals("SMP 1", joinNames(listOf("SMP 1")))
        assertEquals("SMP 1 dan Bimbel", joinNames(listOf("SMP 1", "Bimbel")))
        assertEquals("A, B dan C", joinNames(listOf("A", "B", "C")))
    }

    @Test
    fun `education levels use the API codes`() {
        assertEquals(listOf("sd", "smp", "sma", "kuliah", "umum"), EducationLevel.entries.map { it.apiCode })
    }
}
