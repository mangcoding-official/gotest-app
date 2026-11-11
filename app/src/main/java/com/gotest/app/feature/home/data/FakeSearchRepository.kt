package com.gotest.app.feature.home.data

import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.home.domain.HelpArticle
import com.gotest.app.feature.home.domain.SearchAttempt
import com.gotest.app.feature.home.domain.SearchRepository
import com.gotest.app.feature.home.domain.SearchResults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Searches the shared dummy catalogue and attempt history, plus a fixed list of topics and
 * help articles. "utbk" reproduces the design: two tryouts, six UTBK topics, the UTBK results
 * and two articles.
 */
@Singleton
class FakeSearchRepository
    @Inject
    constructor(
        private val studyPackageRepository: StudyPackageRepository,
        private val examRepository: ExamRepository,
    ) : SearchRepository {
        override fun search(query: String): Flow<SearchResults> {
            val needle = query.trim()
            return combine(
                studyPackageRepository.observePackages(PackageType.Tryout, levelLabel = null),
                studyPackageRepository.observePackages(PackageType.Practice, levelLabel = null),
                examRepository.observeHistory(),
            ) { tryouts, practices, history ->
                val packages = tryouts + practices
                SearchResults(
                    packages = packages.filter { it.title.contains(needle, ignoreCase = true) },
                    topics = topics.filter { it.matches(needle) }.map { it.name },
                    attempts =
                        history
                            .filter { it.title.contains(needle, ignoreCase = true) }
                            .map { it.toSearchAttempt(packages) },
                    articles = articles.filter { it.title.contains(needle, ignoreCase = true) },
                )
            }
        }

        private fun AttemptSummary.toSearchAttempt(packages: List<StudyPackage>) =
            SearchAttempt(
                attemptId = attemptId,
                title = title,
                type = type,
                completedOn = completedOn,
                score = score,
                rank = packages.firstOrNull { it.id == packageId && it.type == PackageType.Tryout }?.rank,
            )

        private data class SearchTopic(
            val name: String,
            /** Extra words the topic is found by, e.g. the exam it belongs to. */
            val keywords: String,
        ) {
            fun matches(needle: String): Boolean =
                name.contains(needle, ignoreCase = true) || keywords.contains(needle, ignoreCase = true)
        }

        private companion object {
            const val UTBK = "utbk snbt"
            const val CPNS = "cpns skd"
            const val MATH = "matematika"
            const val RPL = "smk rpl pemrograman"

            val topics =
                listOf(
                    SearchTopic("Penalaran Umum", UTBK),
                    SearchTopic("Literasi B. Indonesia", UTBK),
                    SearchTopic("Literasi B. Inggris", UTBK),
                    SearchTopic("Penalaran Matematika", UTBK),
                    SearchTopic("Pengetahuan Kuantitatif", UTBK),
                    SearchTopic("PPU", "$UTBK pengetahuan dan pemahaman umum"),
                    SearchTopic("TWK", "$CPNS tes wawasan kebangsaan"),
                    SearchTopic("TIU", "$CPNS tes intelegensia umum"),
                    SearchTopic("TKP", "$CPNS tes karakteristik pribadi"),
                    SearchTopic("Matematika Dasar", MATH),
                    SearchTopic("Logaritma", MATH),
                    SearchTopic("Aljabar", MATH),
                    SearchTopic("Aljabar Linear", MATH),
                    SearchTopic("Dasar-Dasar Pemrograman", RPL),
                    SearchTopic("Pengembangan Aplikasi", RPL),
                )

            val articles =
                listOf(
                    HelpArticle(
                        id = "help_panduan_tryout_utbk",
                        title = "Panduan Mengerjakan Tryout UTBK-SNBT",
                        readMinutes = 5,
                    ),
                    HelpArticle(id = "help_strategi_utbk", title = "Strategi dan Tips UTBK", readMinutes = 4),
                    HelpArticle(id = "help_membaca_hasil", title = "Cara Membaca Hasil Tryout", readMinutes = 3),
                    HelpArticle(id = "help_penilaian_irt", title = "Mengenal Penilaian IRT", readMinutes = 6),
                    HelpArticle(id = "help_tips_skd_cpns", title = "Tips Lolos SKD CPNS", readMinutes = 5),
                    HelpArticle(id = "help_main_bareng", title = "Cara Bermain Main Bareng", readMinutes = 3),
                )
        }
    }
