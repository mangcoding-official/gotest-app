package com.gotest.app.feature.home.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.feature.home.domain.DashboardPage
import com.gotest.app.feature.home.domain.DashboardRepository
import com.gotest.app.feature.home.domain.SearchGroup
import com.gotest.app.feature.home.domain.SearchItem
import com.gotest.app.feature.home.domain.SearchPage
import com.gotest.app.feature.home.domain.SearchRepository
import com.gotest.app.feature.home.domain.SearchTarget
import com.gotest.app.feature.home.ui.home.HomePreviewData
import javax.inject.Inject

/** Debug without the API: the sample Beranda of the previews (T-026 criterion 8). */
class FakeDashboardRepository
    @Inject
    constructor() : DashboardRepository {
        override suspend fun dashboard(): Result<DashboardPage> {
            simulateLatency()
            return Result.success(HomePreviewData.page)
        }
    }

/** Debug without the API: the sample packages, results, and a Bantuan topic whose title has the words. */
class FakeSearchRepository
    @Inject
    constructor() : SearchRepository {
        override suspend fun search(query: String): Result<SearchPage> {
            simulateLatency()
            val groups =
                SAMPLE.mapNotNull { group ->
                    group
                        .copy(items = group.items.filter { it.title.contains(query, ignoreCase = true) })
                        .takeIf { it.items.isNotEmpty() }
                }
            return Result.success(SearchPage(query = query, groups = groups))
        }

        private companion object {
            val SAMPLE =
                listOf(
                    SearchGroup(
                        "practice",
                        "Paket Latihan",
                        listOf(
                            SearchItem(
                                "practice-11",
                                "Latihan Penalaran Umum 1",
                                SearchTarget("practice_package", id = 11),
                                "Mulai Latihan",
                                listOf("UTBK-SNBT", "20 Soal", "3 Topik"),
                            ),
                        ),
                    ),
                    SearchGroup(
                        "tryout",
                        "Paket tryout",
                        listOf(
                            SearchItem(
                                "tryout-21",
                                "Try Out UTBK-SNBT 1",
                                SearchTarget("tryout_package", id = 21),
                                "Mulai Try Out",
                                listOf("UTBK-SNBT", "160 Soal", "195 Menit"),
                            ),
                        ),
                    ),
                    SearchGroup(
                        "help",
                        "Artikel & Bantuan",
                        listOf(
                            SearchItem(
                                "help-latihan",
                                "Latihan dan Try Out",
                                SearchTarget("help_topic", slug = "latihan-dan-tryout"),
                                "Lihat",
                                listOf("Cara mengerjakan latihan dan Try Out."),
                            ),
                        ),
                    ),
                )
        }
    }
