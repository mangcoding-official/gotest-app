package com.gotest.app.feature.ruang.ui.work

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamBlock
import com.gotest.app.feature.ruang.domain.ExamContent
import com.gotest.app.feature.ruang.domain.ExamItem
import com.gotest.app.feature.ruang.domain.ExamWorkPage

/** One item with its answerable part read once. */
@Immutable
data class WorkItem(
    val item: ExamItem,
    val content: ExamContent,
)

/** A message above the paper after saving (Work.tsx `notice`). */
sealed interface WorkNotice {
    data class Rejected(
        val numbers: String,
    ) : WorkNotice

    data object Stale : WorkNotice
}

/** Why the paper could not be opened. */
sealed interface WorkLoadProblem {
    data class Server(
        val message: String,
    ) : WorkLoadProblem

    data object Offline : WorkLoadProblem
}

@Immutable
data class SubmitDialogState(
    /** Item numbers without an answer, "1, 2 dan 3". */
    val unanswered: String?,
    val isSubmitting: Boolean = false,
    /** The server's message, or null for the app's own text in [unsent]/[failed]. */
    val message: String? = null,
    val unsent: Boolean = false,
    val failed: Boolean = false,
)

@Immutable
data class ExamWorkUiState(
    val isLoading: Boolean = true,
    val loadProblem: WorkLoadProblem? = null,
    val page: ExamWorkPage? = null,
    val items: Map<Long, WorkItem> = emptyMap(),
    val answers: Map<Long, ExamAnswer> = emptyMap(),
    val blockIndex: Int = 0,
    val secondsLeft: Long = 0,
    val timeUp: Boolean = false,
    val connectionLost: Boolean = false,
    /** Items whose answers wait on this phone, shown while the connection is lost. */
    val unsentItemIds: Set<Long> = emptySet(),
    val notice: WorkNotice? = null,
    /** A save refused by the server, with its message; null for the app's own text in [saveFailed]. */
    val errorMessage: String? = null,
    val saveFailed: Boolean = false,
    /** `not_member` and the like: everything is locked, with the server's message. */
    val stopped: String? = null,
    val violations: Int = 0,
    val violationLimit: Int = 0,
    /** The violation count to warn about, shown once in a dialog. */
    val violationWarning: Int? = null,
    val otherDevice: Boolean = false,
    val isTakingOver: Boolean = false,
    val takeOverMessage: String? = null,
    val takeOverFailed: Boolean = false,
    val submit: SubmitDialogState? = null,
    val showExit: Boolean = false,
    /** Android screen pinning is on (API-K8 A). */
    val pinned: Boolean = false,
) {
    /** An Ujian waits for the pinned screen: the paper is covered and answers are refused. */
    val awaitingPin: Boolean
        get() = page?.isQuiz == false && !pinned && !otherDevice && stopped == null && !timeUp
    val blocks: List<ExamBlock> get() = page?.blocks.orEmpty()
    val currentBlock: ExamBlock? get() = blocks.getOrNull(blockIndex)
    val isLastBlock: Boolean get() = blockIndex >= blocks.lastIndex

    /** Nothing can be changed: time is up, another device has the Ujian, or it was stopped. */
    val locked: Boolean get() = timeUp || otherDevice || stopped != null || submit?.isSubmitting == true

    val orderedItems: List<WorkItem> get() = items.values.sortedBy { it.item.number }

    /** Answered as the web counts it (`isExamAnswered`): partly filled Benar-Salah and matching count. */
    fun isAnswered(itemId: Long): Boolean = ExamAnswer.clean(answers[itemId]) != null

    val answeredCount: Int get() = items.keys.count(::isAnswered)
}

sealed interface ExamWorkEffect {
    data object OpenResult : ExamWorkEffect

    data object Leave : ExamWorkEffect
}
