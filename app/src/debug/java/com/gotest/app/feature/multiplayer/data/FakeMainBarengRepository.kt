package com.gotest.app.feature.multiplayer.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameOrganization
import com.gotest.app.feature.multiplayer.domain.GameQuestion
import com.gotest.app.feature.multiplayer.domain.GameReview
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.HubAccess
import com.gotest.app.feature.multiplayer.domain.HubPage
import com.gotest.app.feature.multiplayer.domain.MainBarengRepository
import com.gotest.app.feature.multiplayer.domain.SpaceOrganization
import com.gotest.app.feature.practice.domain.AnswerData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug data for Main Bareng (API module E; debug builds only, T-026 criterion 8). Gilang is Premium through his
 * Mitra, so he can create games in Gotest.id and in SMP 1 Bantargedung. Other players join, the other host starts,
 * and everyone finishes as time passes. Codes to try: [FakeMainBarengData.OPEN_ROOM] (room waiting for players),
 * `PENUH1` (full), `MULAI1` (started), `SELSAI` (finished), `SMA3BD` (another Mitra); any other is not found.
 * Messages are the server's (MainBarengService). Every call fails like a dropped connection in airplane mode.
 */
@Suppress("TooManyFunctions") // One function per route of the contract.
@Singleton
class FakeMainBarengRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : MainBarengRepository {
        private val games = mutableMapOf<String, FakeGame>()
        private val names = mutableMapOf<String, Triple<String, String, List<String>>>()
        private val histories =
            mutableMapOf<String?, MutableList<GameHistoryItem>>(
                null to FakeMainBarengData.history.toMutableList(),
                FakeMainBarengData.SCHOOL_CODE to FakeMainBarengData.schoolHistory.toMutableList(),
            )
        private val recorded = mutableSetOf<String>()
        private var nextCode = 0

        private fun now() = System.currentTimeMillis()

        override suspend fun hub(space: String?): Result<HubPage> =
            call {
                if (space != null && space != FakeMainBarengData.SCHOOL_CODE) return@call notFound()
                Result.success(
                    HubPage(
                        access = HubAccess("active"),
                        options = FakeMainBarengData.options,
                        organization =
                            space?.let {
                                SpaceOrganization(
                                    FakeMainBarengData.SCHOOL_NAME,
                                    FakeMainBarengData.SCHOOL_CODE,
                                    "siswa",
                                )
                            },
                    ),
                )
            }

        override suspend fun active(): Result<GameState?> = call { Result.success(activeGame()?.let(::stateOf)) }

        override suspend fun history(space: String?): Result<List<GameHistoryItem>> =
            call {
                advanceAll()
                Result.success(histories[space].orEmpty().toList())
            }

        override suspend fun create(
            space: String?,
            settings: GameSettings,
        ): Result<GameState> =
            call create@{
                if (activeGame() != null) return@create busyAny()
                val category = FakeMainBarengData.options.firstOrNull { it.id == settings.categoryId }
                val subject = category?.subjects?.firstOrNull { it.id == settings.subjectId }
                if (category == null || subject == null) return@create validation("subject_id", "Subtes tidak valid.")
                val topics = subject.topics.filter { it.id in settings.topicIds }
                val stock = if (topics.isEmpty()) subject.questionCount else topics.sumOf { it.questionCount }
                if (stock < settings.questionCount) return@create refuse("stock", stockMessage(stock))
                val organization = space?.let { GameOrganization(FakeMainBarengData.SCHOOL_NAME, it) }
                val game = newGame(settings, organization, FakeMainBarengData.ME_ID, now())
                names[game.code] = Triple(category.name, subject.name, topics.map { it.name })
                game.players += FakePlayer(FakeMainBarengData.ME_ID, FakeMainBarengData.ME, now())
                scheduleJoins(game)
                Result.success(stateOf(game))
            }

        override suspend fun lookup(code: String): Result<GameState> =
            call lookup@{
                val game = findForJoin(code) ?: return@lookup refuse("not_found", NOT_FOUND)
                refusal(game)?.let { return@lookup it }
                Result.success(stateOf(game))
            }

        override suspend fun join(code: String): Result<GameState> = call { joinNow(code) }

        private fun joinNow(code: String): Result<GameState> {
            val game = findForJoin(code) ?: return refuse("not_found", NOT_FOUND)
            if (game.player(FakeMainBarengData.ME_ID)?.left == false) return Result.success(stateOf(game))
            refusal(game)?.let { return it }
            activeGame()?.let { return busy(it) }
            game.players.removeAll { it.id == FakeMainBarengData.ME_ID }
            game.players += FakePlayer(FakeMainBarengData.ME_ID, FakeMainBarengData.ME, now())
            if (game.hostId != FakeMainBarengData.ME_ID) game.hostStartsAt = now() + HOST_STARTS_AFTER_MS
            return Result.success(stateOf(game))
        }

        override suspend fun state(code: String): Result<GameState> =
            call { mine(code)?.let { Result.success(stateOf(it)) } ?: notFound() }

        override suspend fun start(code: String): Result<GameState> =
            call start@{
                val game = mine(code) ?: return@start notFound()
                if (game.hostId != FakeMainBarengData.ME_ID) return@start forbidden()
                if (!game.waiting()) return@start refuse("closed", NOT_WAITING)
                if (game.players.count { !it.left } < 2) {
                    val message =
                        if (game.settings.mode == GameState.MODE_DUEL) {
                            "Tunggu lawanmu masuk ruang tunggu dulu."
                        } else {
                            "Permainan butuh minimal 2 pemain untuk dimulai."
                        }
                    return@start refuse("invalid", message)
                }
                game.start(now())
                Result.success(stateOf(game))
            }

        override suspend fun cancel(code: String): Result<GameState> =
            call cancel@{
                val game = mine(code) ?: return@cancel notFound()
                if (game.hostId != FakeMainBarengData.ME_ID) return@cancel forbidden()
                if (game.waiting()) game.cancel("host", now())
                record(game)
                Result.success(stateOf(game))
            }

        override suspend fun leave(code: String): Result<Unit> =
            call leave@{
                val game = games[code] ?: return@leave Result.success(Unit)
                val me = game.player(FakeMainBarengData.ME_ID) ?: return@leave Result.success(Unit)
                game.advance(now())
                when {
                    game.waiting() && game.hostId == FakeMainBarengData.ME_ID -> {
                        return@leave refuse(
                            "invalid",
                            "Pembuat tidak bisa keluar dari ruang tunggu. Batalkan permainan sebagai gantinya.",
                        )
                    }

                    game.waiting() -> {
                        me.left = true
                    }

                    game.status == GameState.STATUS_RUNNING -> {
                        me.left = true
                        if (me.finishedAt == null) me.finishedAt = now()
                    }
                }
                Result.success(Unit)
            }

        override suspend fun questions(code: String): Result<List<GameQuestion>> =
            call questions@{
                val game = mine(code) ?: return@questions notFound()
                playRefusal<List<GameQuestion>>(game, answering = false)?.let { return@questions it }
                val me = game.player(FakeMainBarengData.ME_ID)
                Result.success(
                    game.questions.map { (question, _) ->
                        question.copy(answer = me?.answers?.get(question.id)?.toJson(question.questionType))
                    },
                )
            }

        override suspend fun answer(
            code: String,
            itemId: Long,
            answer: AnswerData,
        ): Result<Unit> =
            call answer@{
                val game = mine(code) ?: return@answer notFound()
                playRefusal<Unit>(game, answering = true)?.let { return@answer it }
                if (game.questions.none { it.first.id == itemId }) return@answer notFound()
                game.player(FakeMainBarengData.ME_ID)?.answers?.set(itemId, answer)
                Result.success(Unit)
            }

        override suspend fun finish(code: String): Result<GameState> =
            call finish@{
                val game = mine(code) ?: return@finish notFound()
                if (game.status == GameState.STATUS_RUNNING) {
                    val start = game.startedAt ?: 0L
                    if (now() < start) return@finish refuse("invalid", COUNTDOWN)
                    game.player(FakeMainBarengData.ME_ID)?.let { if (it.finishedAt == null) it.finishedAt = now() }
                    if (game.advance(now())) record(game)
                }
                Result.success(stateOf(game))
            }

        override suspend fun result(code: String): Result<GameReview> =
            call result@{
                FakeMainBarengReviews.of(code, histories.values.flatten())?.let { return@result Result.success(it) }
                val game =
                    games[code]?.takeIf { it.player(FakeMainBarengData.ME_ID) != null } ?: return@result notFound()
                if (game.advance(now())) record(game)
                if (game.status != GameState.STATUS_FINISHED) {
                    return@result refuse("closed", "Hasil muncul setelah permainan selesai.")
                }
                Result.success(GameReview(stateOf(game), game.result(FakeMainBarengData.ME_ID)))
            }

        override suspend fun playAgain(code: String): Result<GameState> =
            call again@{
                val game =
                    games[code]?.takeIf { it.player(FakeMainBarengData.ME_ID) != null } ?: return@again notFound()
                game.advance(now())
                if (game.status !=
                    GameState.STATUS_FINISHED
                ) {
                    return@again refuse("invalid", "Permainan ini belum selesai.")
                }
                if (game.hostId == FakeMainBarengData.ME_ID) {
                    val next = newGame(game.settings, game.organization, FakeMainBarengData.ME_ID, now())
                    names[next.code] = names.getValue(game.code)
                    next.players += FakePlayer(FakeMainBarengData.ME_ID, FakeMainBarengData.ME, now())
                    scheduleJoins(next)
                    game.nextGameCode = next.code
                    return@again Result.success(stateOf(next))
                }
                val nextCode =
                    game.nextGameCode ?: return@again refuse("not_found", "Pembuat belum membuat permainan berikutnya.")
                joinNow(nextCode)
            }

        // The rules shared by the routes.

        private fun GameState.label() = "${if (mode == GameState.MODE_DUEL) "Duel" else "Room"} • $subjectName"

        private fun FakeGame.waiting() = status == GameState.STATUS_WAITING

        private fun stateOf(game: FakeGame): GameState {
            val (category, subject, topics) = names.getValue(game.code)
            return game.state(FakeMainBarengData.ME_ID, now(), subject, category, topics)
        }

        private fun activeGame(): FakeGame? {
            advanceAll()
            return games.values.firstOrNull { game ->
                val me = game.player(FakeMainBarengData.ME_ID)
                me != null && !me.left && (game.waiting() || game.status == GameState.STATUS_RUNNING)
            }
        }

        /** A game the viewer is in (and did not leave from the waiting room), moved on to now. */
        private fun mine(code: String): FakeGame? {
            val game = games[code] ?: return null
            val me = game.player(FakeMainBarengData.ME_ID) ?: return null
            if (me.left && game.status != GameState.STATUS_RUNNING) return null
            if (game.advance(now())) record(game)
            if (game.status == GameState.STATUS_CANCELLED) record(game)
            makeNextGame(game)
            return game
        }

        private fun advanceAll() {
            games.values.toList().forEach { game ->
                if (game.advance(now()) || game.status == GameState.STATUS_CANCELLED) record(game)
                makeNextGame(game)
            }
        }

        /** Another host makes the next game a little after the end ("Gabung Lagi"). */
        private fun makeNextGame(game: FakeGame) {
            val after = game.nextGameAfterMs ?: return
            val closed = game.closedAt ?: return
            if (game.status != GameState.STATUS_FINISHED || game.nextGameCode != null || now() < closed + after) return
            val next = newGame(game.settings, game.organization, game.hostId, now())
            names[next.code] = names.getValue(game.code)
            game.players.filter { it.id != FakeMainBarengData.ME_ID }.forEach { other ->
                next.players += FakePlayer(other.id, other.name, now(), other.finishAfterMs, other.correctShare)
            }
            next.nextGameAfterMs = after
            game.nextGameCode = next.code
        }

        private fun record(game: FakeGame) {
            if (game.player(FakeMainBarengData.ME_ID) == null || !recorded.add(game.code)) return
            val finished = game.status == GameState.STATUS_FINISHED
            val result = if (finished) game.result(FakeMainBarengData.ME_ID) else null
            val item =
                GameHistoryItem(
                    code = game.code,
                    mode = game.settings.mode,
                    status = game.status,
                    subjectName = names.getValue(game.code).second,
                    questionCount = game.settings.questionCount,
                    durationMinutes = game.settings.durationMinutes,
                    playerCount = game.players.count { !it.left || finished },
                    opponentName =
                        game.players
                            .firstOrNull { it.id != FakeMainBarengData.ME_ID }
                            ?.name
                            ?.takeIf { game.settings.mode == GameState.MODE_DUEL },
                    outcome = result?.outcome,
                    rank = result?.you?.rank,
                    closedAt = game.closedAt?.let(FakeGame::iso),
                )
            histories.getOrPut(game.organization?.code) { mutableListOf() }.add(0, item)
        }

        private fun newGame(
            settings: GameSettings,
            organization: GameOrganization?,
            hostId: Long,
            at: Long,
        ): FakeGame {
            nextCode++
            val code = "G" + nextCode.toString().padStart(CODE_DIGITS, '0') + CODE_TAIL
            return FakeGame(code, settings, organization, hostId, at).also { games[code] = it }
        }

        /** Others join a game the viewer made: one rival in a duel, three in a room. */
        private fun scheduleJoins(game: FakeGame) {
            val count = if (game.settings.mode == GameState.MODE_DUEL) 1 else ROOM_JOINERS
            FakeMainBarengData.others.take(count).forEachIndexed { index, (id, name) ->
                game.pendingJoins +=
                    FakePlayer(
                        id = id,
                        name = name,
                        joinedAt = now() + JOIN_STEP_MS * (index + 1),
                        finishAfterMs = FINISH_BASE_MS + FINISH_STEP_MS * index,
                        correctShare = SHARES[index],
                    )
            }
        }

        /** The code a friend shares; the seeded ones are made again when they are used up. */
        private fun findForJoin(code: String): FakeGame? {
            games[code]?.let { return it }
            val seed = FakeMainBarengSeeds.seed(code, now()) ?: return null
            games[code] = seed.first
            names[code] = seed.second
            return seed.first
        }

        private fun refusal(game: FakeGame): Result<GameState>? {
            game.advance(now())
            val organization = game.organization
            val mine = game.player(FakeMainBarengData.ME_ID)?.left == false
            return when {
                organization != null && organization.code != FakeMainBarengData.SCHOOL_CODE -> {
                    refuse("not_member", "Permainan ini hanya untuk anggota ${organization.name}.")
                }

                mine -> {
                    null
                }

                game.status == GameState.STATUS_RUNNING -> {
                    refuse(
                        "started",
                        "Kamu tidak bisa bergabung karena soal sudah mulai dikerjakan. Tunggu permainan berikutnya.",
                    )
                }

                !game.waiting() -> {
                    refuse("closed", "Permainan ini sudah dibatalkan atau selesai. Minta kode baru dari temanmu.")
                }

                game.players.count { !it.left } >= game.maxPlayers -> {
                    refuse(
                        "full",
                        if (game.settings.mode == GameState.MODE_DUEL) {
                            "Duel ini sudah ada 2 pemain."
                        } else {
                            "Room ini sudah berisi ${game.maxPlayers} pemain."
                        },
                    )
                }

                else -> {
                    null
                }
            }
        }

        private fun <T> playRefusal(
            game: FakeGame,
            answering: Boolean,
        ): Result<T>? {
            val start = game.startedAt
            val end = game.endsAt
            val me = game.player(FakeMainBarengData.ME_ID)
            return when {
                game.status != GameState.STATUS_RUNNING || start == null || end == null -> {
                    refuse("closed", "Permainan ini sedang tidak berjalan.")
                }

                now() < start -> {
                    refuse("invalid", COUNTDOWN)
                }

                answering && now() >= end -> {
                    refuse("closed", "Waktu permainan sudah habis.")
                }

                answering && me?.finishedAt != null -> {
                    refuse("closed", "Kamu sudah menyelesaikan permainan ini.")
                }

                else -> {
                    null
                }
            }
        }

        private fun <T> busy(current: FakeGame): Result<T> =
            refuse("busy", "Selesaikan atau keluar dari ${stateOf(current).label()} (kode ${current.code}) dulu.")

        private fun <T> busyAny(): Result<T> =
            refuse("busy", "Kamu masih ikut permainan lain. Selesaikan atau keluar dari permainan itu dulu.")

        private fun stockMessage(stock: Int) =
            if (stock == 0) {
                "Belum ada soal di pilihan ini. Pilih subtes atau topik lain."
            } else {
                "Pilihan ini hanya punya $stock soal. Kurangi jumlah soal menjadi $stock atau tambah topik lain."
            }

        private fun AnswerData.toJson(questionType: String?): JsonElement =
            when (questionType) {
                "multiple_select" -> JsonArray(selectedOptions.orEmpty().map(::JsonPrimitive))
                "statement_table" -> JsonArray(statementAnswers.orEmpty().map { it?.let(::JsonPrimitive) ?: JsonNull })
                else -> selectedOption?.let(::JsonPrimitive) ?: JsonNull
            }

        private suspend fun <T> call(block: () -> Result<T>): Result<T> {
            simulateLatency()
            if (!online()) return Result.failure(IOException("No network"))
            return block()
        }

        private fun online(): Boolean {
            val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        companion object {
            private const val NOT_FOUND = "Periksa lagi 6 huruf kodenya, atau minta tautan baru dari pembuat permainan."
            private const val NOT_WAITING = "Permainan ini sudah dibatalkan atau selesai. Minta kode baru dari temanmu."
            private const val COUNTDOWN = "Permainan belum dimulai. Tunggu hitung mundur selesai."
            private const val HOST_STARTS_AFTER_MS = 8_000L
            private const val JOIN_STEP_MS = 3_000L
            private const val FINISH_BASE_MS = 40_000L
            private const val FINISH_STEP_MS = 15_000L
            private const val ROOM_JOINERS = 3
            private const val CODE_DIGITS = 3
            private const val CODE_TAIL = "MB"
            private val SHARES = listOf(0.6, 0.5, 0.4)
            private const val UNPROCESSABLE = 422

            fun <T> refuse(
                kind: String,
                message: String,
            ): Result<T> = Result.failure(ApiError(UNPROCESSABLE, message, kind))

            fun <T> validation(
                field: String,
                message: String,
            ): Result<T> =
                Result.failure(ApiError(UNPROCESSABLE, message, "validation", mapOf(field to listOf(message))))

            fun <T> notFound(): Result<T> = Result.failure(ApiError(404, "Data tidak ditemukan.", "not_found"))

            fun <T> forbidden(): Result<T> = Result.failure(ApiError(403, "Akses ditolak.", "forbidden"))
        }
    }
