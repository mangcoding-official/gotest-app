package com.gotest.app.feature.home.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cari (T-026 A2c-1 = a): `GET /search?q=`, the same groups as the web's search panel: Paket Latihan, Paket
 * tryout, Hasil, and Artikel & Bantuan, at most three each.
 */
@Serializable
data class SearchPage(
    @SerialName("query") val query: String = "",
    @SerialName("groups") val groups: List<SearchGroup> = emptyList(),
)

@Serializable
data class SearchGroup(
    /** `practice`, `tryout`, `results`, or `help`. */
    @SerialName("key") val key: String,
    @SerialName("title") val title: String,
    @SerialName("items") val items: List<SearchItem> = emptyList(),
)

@Serializable
data class SearchItem(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    /** What a tap opens; the `href` beside it is a web address the app does not follow. */
    @SerialName("target") val target: SearchTarget? = null,
    @SerialName("actionLabel") val actionLabel: String = "",
    @SerialName("meta") val meta: List<String> = emptyList(),
)

@Serializable
data class SearchTarget(
    /**
     * `practice_package`, `tryout_package` (a package's start sheet), `practice_attempt`, `tryout_attempt` (the
     * attempt in progress), `practice_result`, `tryout_result` (a result), or `help_topic` ([slug]).
     */
    @SerialName("type") val type: String,
    @SerialName("id") val id: Long? = null,
    @SerialName("slug") val slug: String? = null,
)

interface SearchRepository {
    suspend fun search(query: String): Result<SearchPage>
}
