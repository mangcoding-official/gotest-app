package com.gotest.app.feature.practice

import com.gotest.app.feature.practice.domain.ActiveQuestion
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.PracticeAttempt
import com.gotest.app.feature.practice.domain.PracticeCatalogPage
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.domain.PracticeResult
import com.gotest.app.feature.practice.domain.StartAccess
import java.io.IOException

/** Answers what the test sets (no connection otherwise); every call that changes something is recorded. */
internal class ScriptedPracticeRepository : PracticeRepository {
    var catalogReply: Result<PracticeCatalogPage> = offline()
    var accessReply: Result<StartAccess> = offline()
    var startReply: Result<PracticeAttempt> = offline()
    var attemptReply: Result<PracticeAttempt> = offline()
    var activateReply: (itemId: Long) -> Result<ActiveQuestion> = { offline() }
    var answerReply: Result<Unit> = Result.success(Unit)
    var submitReply: Result<PracticeAttempt> = offline()

    val starts = mutableListOf<Pair<Long, Boolean?>>()
    val ruangStarts = mutableListOf<Pair<String, Long>>()
    val answers = mutableListOf<Pair<Long, AnswerData>>()
    val activated = mutableListOf<Long>()
    var submitCalls = 0
    var attemptCalls = 0

    override suspend fun catalog(): Result<PracticeCatalogPage> = catalogReply

    override suspend fun startAccess(): Result<StartAccess> = accessReply

    override suspend fun start(
        packageId: Long,
        timerEnabled: Boolean?,
    ): Result<PracticeAttempt> {
        starts += packageId to timerEnabled
        return startReply
    }

    override suspend fun startInRuang(
        code: String,
        packageId: Long,
    ): Result<PracticeAttempt> {
        ruangStarts += code to packageId
        return startReply
    }

    override suspend fun attempt(attemptId: Long): Result<PracticeAttempt> {
        attemptCalls++
        return attemptReply
    }

    override suspend fun activate(
        attemptId: Long,
        itemId: Long,
    ): Result<ActiveQuestion> {
        activated += itemId
        return activateReply(itemId)
    }

    override suspend fun answer(
        attemptId: Long,
        questionId: Long,
        answer: AnswerData,
    ): Result<Unit> {
        answers += questionId to answer
        return answerReply
    }

    override suspend fun submit(attemptId: Long): Result<PracticeAttempt> {
        submitCalls++
        return submitReply
    }

    override suspend fun result(attemptId: Long): Result<PracticeResult> = offline()

    companion object {
        fun <T> offline(): Result<T> = Result.failure(IOException("offline"))
    }
}
