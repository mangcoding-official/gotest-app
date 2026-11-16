package com.gotest.app.feature.multiplayer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {
    @Test
    fun `a code keeps six letters and digits, upper-cased`() {
        assertEquals("KTR4QZ", GameRules.normalizeCode("ktr-4qz"))
        assertEquals("ABC123", GameRules.normalizeCode("abc 123 xyz"))
        assertEquals("", GameRules.normalizeCode("--"))
    }

    @Test
    fun `a game needs more than thirty seconds per question`() {
        // 10 minutes for 20 questions is exactly 30 seconds each: not enough.
        assertFalse(GameRules.isDurationAllowed(durationMinutes = 10, questionCount = 20))
        assertTrue(GameRules.isDurationAllowed(durationMinutes = 15, questionCount = 20))
    }

    @Test
    fun `creating needs Premium`() {
        assertEquals(CreateBlock.PremiumRequired, GameRules.createBlock(isPremium = false, activeGame = null))
        assertNull(GameRules.createBlock(isPremium = true, activeGame = null))
    }

    @Test
    fun `most correct ranks first and a tie goes to the faster time`() {
        val ranked =
            GameRules.rank(
                listOf(
                    standing("lambat", correct = 8, seconds = 300),
                    standing("cepat", correct = 8, seconds = 200),
                    standing("juara", correct = 9, seconds = 400),
                ),
            )
        assertEquals(listOf("juara", "cepat", "lambat"), ranked.map { it.playerId })
        assertEquals(listOf(1, 2, 3), ranked.map { it.rank })
    }

    @Test
    fun `seconds until a moment round up and never go below zero`() {
        assertEquals(2, GameRules.secondsUntil(targetMillis = 1_500, nowMillis = 0))
        assertEquals(0, GameRules.secondsUntil(targetMillis = 0, nowMillis = 5_000))
    }

    private fun standing(
        id: String,
        correct: Int,
        seconds: Int,
    ) = PlayerStanding(
        rank = 0,
        playerId = id,
        name = id,
        avatarUrl = null,
        correct = correct,
        totalSeconds = seconds,
        isCurrentUser = false,
    )
}
