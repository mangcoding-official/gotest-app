package com.gotest.app.feature.multiplayer.data

import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.OptionCategory
import com.gotest.app.feature.multiplayer.domain.OptionSubject
import com.gotest.app.feature.multiplayer.domain.OptionTopic

/** Fixed data of the fake Main Bareng: the "Buat permainan" choices, past games, and codes to try. */
internal object FakeMainBarengData {
    const val ME_ID = 1L
    const val ME = "Gilang Permana"
    const val SCHOOL_CODE = "SMP1BTG"
    const val SCHOOL_NAME = "SMP 1 Bantargedung"

    /** Other players, by id; they join, start, and finish on their own as time passes. */
    val others = listOf(2L to "Rina Ayu", 3L to "Bima Saputra", 4L to "Dewi Lestari", 5L to "Fajar Nugroho")

    val options =
        listOf(
            OptionCategory(
                1,
                "TKA SMA",
                listOf(OptionSubject(21, "Matematika", 8, listOf(OptionTopic(211, "Fungsi", 8)))),
            ),
            OptionCategory(
                2,
                "UTBK-SNBT",
                listOf(
                    OptionSubject(
                        11,
                        "Penalaran Umum",
                        30,
                        listOf(
                            OptionTopic(111, "Penalaran Kuantitatif", 12),
                            OptionTopic(112, "Bilangan", 10),
                            OptionTopic(113, "Logika", 8),
                        ),
                    ),
                    OptionSubject(
                        12,
                        "Pengetahuan Kuantitatif",
                        24,
                        listOf(OptionTopic(121, "Aljabar", 15), OptionTopic(122, "Geometri", 9)),
                    ),
                    OptionSubject(13, "Literasi Bahasa Indonesia", 0),
                ),
            ),
        )

    /** Codes to try in "Gabung dengan kode"; any other code is not found. */
    const val OPEN_ROOM = "RNA7K2"
    const val FULL = "PENUH1"
    const val STARTED = "MULAI1"
    const val CLOSED = "SELSAI"
    const val OTHER_MITRA = "SMA3BD"
    const val OTHER_MITRA_NAME = "SMA 3 Bandung"

    private fun past(
        code: String,
        mode: String,
        subject: String,
        players: Int,
        closedAt: String,
        outcome: String? = null,
        rank: Int? = null,
        opponent: String? = null,
        status: String = GameState.STATUS_FINISHED,
    ) = GameHistoryItem(
        code = code,
        mode = mode,
        status = status,
        subjectName = subject,
        questionCount = 10,
        durationMinutes = 10,
        playerCount = players,
        opponentName = opponent,
        outcome = outcome,
        rank = rank,
        closedAt = closedAt,
    )

    /** Gotest.id history, newest first: six games, so "Lihat Semua" shows. */
    val history =
        listOf(
            past(
                "HX1A2B",
                GameState.MODE_DUEL,
                "Penalaran Umum",
                2,
                "2026-11-22T12:40:00Z",
                "win",
                opponent = "Rina Ayu",
            ),
            past("HX3C4D", GameState.MODE_ROOM, "Pengetahuan Kuantitatif", 5, "2026-11-21T09:15:00Z", rank = 2),
            past(
                "HX5E6F",
                GameState.MODE_DUEL,
                "Penalaran Umum",
                1,
                "2026-11-20T14:05:00Z",
                status = GameState.STATUS_CANCELLED,
            ),
            past(
                "HX7G8H",
                GameState.MODE_DUEL,
                "Penalaran Umum",
                2,
                "2026-11-19T10:30:00Z",
                "lose",
                opponent = "Bima Saputra",
            ),
            past(
                "HX9J1K",
                GameState.MODE_DUEL,
                "Pengetahuan Kuantitatif",
                2,
                "2026-11-18T08:20:00Z",
                "draw",
                opponent = "Dewi Lestari",
            ),
            past("HX2L3M", GameState.MODE_ROOM, "Penalaran Umum", 4, "2026-11-17T13:00:00Z", rank = 1),
        )

    /** The Mitra space keeps its own history (DEC-126, DEC-127). */
    val schoolHistory =
        listOf(
            past("SX1A2B", GameState.MODE_ROOM, "Penalaran Umum", 6, "2026-11-21T07:45:00Z", rank = 3),
        )
}
