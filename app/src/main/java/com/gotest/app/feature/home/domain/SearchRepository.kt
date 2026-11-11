package com.gotest.app.feature.home.domain

import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** A finished attempt that matches the query ("Hasil Kamu"). */
data class SearchAttempt(
    val attemptId: String,
    val title: String,
    val type: PackageType,
    val completedOn: LocalDate,
    val score: Int,
    /** Leaderboard position, only known for tryouts. */
    val rank: Int?,
)

/** A Pusat Bantuan article ("Artikel & Bantuan"). */
data class HelpArticle(
    val id: String,
    val title: String,
    val readMinutes: Int,
)

data class SearchResults(
    val packages: List<StudyPackage>,
    val topics: List<String>,
    val attempts: List<SearchAttempt>,
    val articles: List<HelpArticle>,
)

/** Global search on Pencarian: packages, topics, the student's results and help articles. */
interface SearchRepository {
    fun search(query: String): Flow<SearchResults>
}
