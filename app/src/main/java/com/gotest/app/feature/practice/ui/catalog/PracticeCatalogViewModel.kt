package com.gotest.app.feature.practice.ui.catalog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.CatalogSubject
import com.gotest.app.feature.practice.domain.PracticeCatalogPage
import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.domain.RecentAttempt
import com.gotest.app.feature.practice.domain.StartAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** "Tanggal Penyelesaian" choices of Aktivitas Terkini (web DateFilter). */
enum class DateFilter { All, Days7, Days30, Year }

/** A subject of one category with its packages (web `groupBySubject`); id 0 is "Umum". */
@Immutable
data class SubjectGroup(
    val subject: CatalogSubject,
    val packages: List<PracticePackage>,
    val topicCount: Int,
    val finishedPackages: Int,
)

@Immutable
data class CategoryGroup(
    val id: Long,
    val name: String,
    val description: String?,
    val packages: List<PracticePackage>,
    val subjects: List<SubjectGroup>,
)

/** Why a start failed, as the web's `requestMessage` words it. */
sealed interface StartProblem {
    /** A 422 with the server's own sentence. */
    data class Server(
        val message: String,
    ) : StartProblem

    /** "Koneksi terputus. Periksa internet Anda lalu coba lagi." */
    data object Offline : StartProblem

    /** "Paket belum dapat dimulai. Silakan coba lagi." */
    data object Fallback : StartProblem
}

/** The start sheet of one package (web PracticeStartModal). */
@Immutable
data class StartSheet(
    val item: PracticePackage,
    val useTimer: Boolean = true,
    val isStarting: Boolean = false,
    val error: StartProblem? = null,
)

@Immutable
data class PracticeCatalogUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val page: PracticeCatalogPage? = null,
    val access: StartAccess? = null,
    /** Where the catalog is: the front (null), a category, or a subject of it. */
    val categoryId: Long? = null,
    val subjectId: Long? = null,
    val draftActivityCategory: String? = null,
    val draftDateFilter: DateFilter = DateFilter.All,
    val activityCategory: String? = null,
    val dateFilter: DateFilter = DateFilter.All,
    val sheet: StartSheet? = null,
) {
    /** Only categories that have a Latihan package show. */
    val categories: List<CategoryGroup>
        get() =
            page
                ?.categories
                ?.map { category ->
                    val items = page.packages.filter { it.categoryId == category.id }
                    CategoryGroup(category.id, category.name, category.description, items, groupBySubject(items))
                }?.filter { it.packages.isNotEmpty() }
                .orEmpty()

    val category: CategoryGroup? get() = categories.firstOrNull { it.id == categoryId }

    val subject: SubjectGroup? get() = category?.subjects?.firstOrNull { it.subject.id == subjectId }

    val activity: List<RecentAttempt>
        get() =
            page?.recentAttempts.orEmpty().filter { attempt ->
                (activityCategory == null || attempt.categoryName == activityCategory) &&
                    matchesDate(attempt.submittedAt, dateFilter)
            }
}

sealed interface PracticeCatalogEffect {
    data class OpenRunner(
        val attemptId: Long,
        val packageName: String,
    ) : PracticeCatalogEffect

    data class OpenResult(
        val attemptId: Long,
        val packageName: String,
    ) : PracticeCatalogEffect
}

/**
 * Paket Latihan (web Practice/Index, PracticeCatalog): the front with categories and Aktivitas Terkini, a category
 * with its subtests, and a subtest with its packages. The same data at every level, read once per screen; the
 * start sheet works the same on each (web PracticeStartModal).
 */
@HiltViewModel
class PracticeCatalogViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: PracticeRepository,
    ) : ViewModel() {
        // Read by name, so the front (the Paket tab, without them) and each level share this view model.
        private val categoryId: Long? = savedStateHandle[ARG_CATEGORY]
        private val subjectId: Long? = savedStateHandle[ARG_SUBJECT]

        private val _uiState = MutableStateFlow(PracticeCatalogUiState(categoryId = categoryId, subjectId = subjectId))
        val uiState: StateFlow<PracticeCatalogUiState> = _uiState.asStateFlow()

        private val _effects = Channel<PracticeCatalogEffect>(Channel.BUFFERED)
        val effects: Flow<PracticeCatalogEffect> = _effects.receiveAsFlow()

        private var request: Job? = null

        init {
            load()
        }

        fun retry() = load()

        /** Back from a Latihan: progress and buttons as the server has them now. */
        fun refresh() {
            if (_uiState.value.page == null) return
            fetch()
        }

        private fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            fetch()
        }

        private fun fetch() {
            request?.cancel()
            request =
                viewModelScope.launch {
                    val access = async { repository.startAccess() }
                    repository
                        .catalog()
                        .onSuccess { page ->
                            val known = access.await().getOrNull()
                            _uiState.update {
                                it.copy(isLoading = false, loadFailed = false, page = page, access = known ?: it.access)
                            }
                        }.onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = it.page == null) } }
                }
        }

        fun onDraftActivityCategory(name: String?) = _uiState.update { it.copy(draftActivityCategory = name) }

        fun onDraftDateFilter(filter: DateFilter) = _uiState.update { it.copy(draftDateFilter = filter) }

        fun applyActivityFilter() =
            _uiState.update { it.copy(activityCategory = it.draftActivityCategory, dateFilter = it.draftDateFilter) }

        fun openSheet(item: PracticePackage) = _uiState.update { it.copy(sheet = StartSheet(item)) }

        fun closeSheet() = _uiState.update { if (it.sheet?.isStarting == true) it else it.copy(sheet = null) }

        fun onUseTimer(value: Boolean) = _uiState.update { it.copy(sheet = it.sheet?.copy(useTimer = value)) }

        /** Lanjutkan, Lihat Pembahasan, or a new start (with the timer choice when the timer is optional). */
        fun start() {
            val sheet = _uiState.value.sheet?.takeUnless { it.isStarting } ?: return
            val item = sheet.item
            val continueId = item.continueAttemptId
            val reviewId = item.reviewAttemptId
            when {
                continueId != null -> {
                    openRunner(continueId, item.name)
                }

                reviewId != null -> {
                    _uiState.update { it.copy(sheet = null) }
                    _effects.trySend(PracticeCatalogEffect.OpenResult(reviewId, item.name))
                }

                else -> {
                    startNew(sheet)
                }
            }
        }

        private fun startNew(sheet: StartSheet) {
            val item = sheet.item
            _uiState.update { it.copy(sheet = sheet.copy(isStarting = true, error = null)) }
            viewModelScope.launch {
                repository
                    .start(item.id, sheet.useTimer.takeIf { offersTimerChoice(item) })
                    .onSuccess { attempt -> openRunner(attempt.id, item.name) }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                sheet = it.sheet?.copy(isStarting = false, error = startError(error)),
                            )
                        }
                    }
            }
        }

        private fun openRunner(
            attemptId: Long,
            packageName: String,
        ) {
            _uiState.update { it.copy(sheet = null) }
            _effects.trySend(PracticeCatalogEffect.OpenRunner(attemptId, packageName))
        }

        companion object {
            const val ARG_CATEGORY = "categoryId"
            const val ARG_SUBJECT = "subjectId"

            /** Web `requestMessage`: a 422 says why; no connection says so; anything else the fallback. */
            fun startError(error: Throwable): StartProblem =
                when {
                    error !is ApiError -> {
                        StartProblem.Offline
                    }

                    error.status == UNPROCESSABLE -> {
                        StartProblem.Server(
                            error.fieldErrors.values
                                .firstOrNull()
                                ?.firstOrNull() ?: error.message,
                        )
                    }

                    else -> {
                        StartProblem.Fallback
                    }
                }

            private const val UNPROCESSABLE = 422
        }
    }

fun offersTimerChoice(item: PracticePackage): Boolean = item.timerOptional && item.continueAttemptId == null

/** Web `groupBySubject`: a package counts under each of its subjects; "Umum" (id 0) last, the rest by name. */
fun groupBySubject(packages: List<PracticePackage>): List<SubjectGroup> {
    val groups = linkedMapOf<Long, Triple<CatalogSubject, MutableList<PracticePackage>, MutableSet<Long>>>()
    packages.forEach { item ->
        val subjects = item.subjects.ifEmpty { listOf(GENERAL) }
        subjects.forEach { subject ->
            val group = groups.getOrPut(subject.id) { Triple(subject, mutableListOf(), mutableSetOf()) }
            group.second += item
            group.third += item.topicIds
        }
    }
    return groups.values
        .map { (subject, items, topics) ->
            SubjectGroup(subject, items, topics.size, items.count { it.finishedCount > 0 })
        }.sortedWith(compareBy<SubjectGroup> { it.subject.id == GENERAL.id }.thenBy { it.subject.name })
}

/** The "Umum" group; the screen writes its name and text from resources. */
val GENERAL = CatalogSubject(0, "", null)

private val jakarta = ZoneId.of("Asia/Jakarta")

/** Web `matchesDateFilter`. */
fun matchesDate(
    value: String?,
    filter: DateFilter,
    now: Instant = Instant.now(),
): Boolean {
    val submitted =
        value?.let {
            runCatching {
                java.time.OffsetDateTime
                    .parse(it)
                    .toInstant()
            }.getOrNull()
        }
    return when {
        filter == DateFilter.All -> true
        submitted == null -> false
        filter == DateFilter.Days7 -> !submitted.isBefore(now.minus(DAYS_7, ChronoUnit.DAYS))
        filter == DateFilter.Days30 -> !submitted.isBefore(now.minus(DAYS_30, ChronoUnit.DAYS))
        else -> submitted.atZone(jakarta).year == now.atZone(jakarta).year
    }
}

private const val DAYS_7 = 7L
private const val DAYS_30 = 30L
