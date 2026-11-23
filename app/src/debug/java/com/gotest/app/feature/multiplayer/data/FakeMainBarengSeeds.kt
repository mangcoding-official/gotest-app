package com.gotest.app.feature.multiplayer.data

import com.gotest.app.feature.multiplayer.domain.GameOrganization
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameState

/** The games behind the codes a friend shares in the fake: one to join and four that refuse. */
internal object FakeMainBarengSeeds {
    private const val MINUTE_MS = 60_000L
    private const val TEN = 10
    private val names = Triple("UTBK-SNBT", "Penalaran Umum", emptyList<String>())

    private fun settings(mode: String) = GameSettings(mode, 2, 11, emptyList(), TEN, TEN)

    private fun player(
        id: Long,
        at: Long,
        finishAfterMs: Long = 0,
        share: Double = 0.0,
    ): FakePlayer {
        val name = FakeMainBarengData.others.first { it.first == id }.second
        return FakePlayer(id, name, at, finishAfterMs, share)
    }

    /** A new game for [code] made at [now], with the names shown for it; null when the code is unknown. */
    fun seed(
        code: String,
        now: Long,
    ): Pair<FakeGame, Triple<String, String, List<String>>>? {
        val game =
            when (code) {
                FakeMainBarengData.OPEN_ROOM -> openRoom(now)
                FakeMainBarengData.FULL -> full(now)
                FakeMainBarengData.STARTED -> started(now)
                FakeMainBarengData.CLOSED -> closed(now)
                FakeMainBarengData.OTHER_MITRA -> otherMitra(now)
                else -> null
            } ?: return null
        return game to names
    }

    /** Rina's room with Bima waiting; she presses Mulai a few seconds after the viewer joins. */
    private fun openRoom(now: Long) =
        FakeGame(FakeMainBarengData.OPEN_ROOM, settings(GameState.MODE_ROOM), null, 2, now - MINUTE_MS).apply {
            players += player(2, now - MINUTE_MS, 35_000, 0.8)
            players += player(3, now - MINUTE_MS / 2, 50_000, 0.5)
            nextGameAfterMs = 6_000
        }

    private fun full(now: Long) =
        FakeGame(FakeMainBarengData.FULL, settings(GameState.MODE_DUEL), null, 4, now - MINUTE_MS).apply {
            players += player(4, now - MINUTE_MS)
            players += player(5, now - MINUTE_MS / 2)
        }

    private fun started(now: Long) =
        FakeGame(FakeMainBarengData.STARTED, settings(GameState.MODE_ROOM), null, 3, now - 2 * MINUTE_MS).apply {
            players += player(3, now - 2 * MINUTE_MS, 9 * MINUTE_MS)
            players += player(4, now - 2 * MINUTE_MS, 9 * MINUTE_MS)
            start(now - MINUTE_MS)
        }

    private fun closed(now: Long) =
        FakeGame(FakeMainBarengData.CLOSED, settings(GameState.MODE_DUEL), null, 5, now - 3 * MINUTE_MS).apply {
            players += player(5, now - 3 * MINUTE_MS)
            cancel("host", now - 2 * MINUTE_MS)
        }

    private fun otherMitra(now: Long) =
        FakeGame(
            FakeMainBarengData.OTHER_MITRA,
            settings(GameState.MODE_ROOM),
            GameOrganization(FakeMainBarengData.OTHER_MITRA_NAME, "SMA3BDG"),
            4,
            now - MINUTE_MS,
        ).apply { players += player(4, now - MINUTE_MS) }
}
