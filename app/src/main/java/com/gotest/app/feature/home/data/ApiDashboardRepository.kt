package com.gotest.app.feature.home.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.home.domain.DashboardPage
import com.gotest.app.feature.home.domain.DashboardRepository
import com.gotest.app.feature.home.domain.MODE_TRYOUT
import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.tryout.domain.TryoutPackage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/** Beranda against API module B (T-026 A2c-1 = a). */
@Singleton
class ApiDashboardRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : DashboardRepository {
        private val api = retrofit.create(HomeApi::class.java)

        override suspend fun dashboard(): Result<DashboardPage> = apiCall(json) { api.dashboard() }.map(::toPage)

        private fun toPage(reply: DashboardReply): DashboardPage {
            val rows = reply.packages.map(::asCatalogRow)
            val (tryout, practice) = rows.partition { it.string("mode") == MODE_TRYOUT }
            return DashboardPage(
                schoolSpaces = reply.schoolSpaces,
                mitraTasks = reply.mitraTasks,
                practicePackages = practice.map { json.decodeFromJsonElement(PracticePackage.serializer(), it) },
                tryoutPackages = tryout.map { json.decodeFromJsonElement(TryoutPackage.serializer(), it) },
                recentAttempts = reply.recentAttempts,
                stats = reply.stats,
                continueAttempt = reply.continueAttempt,
                leaderboard = reply.leaderboard,
                banner = reply.banner,
            )
        }
    }

/**
 * The dashboard names a package's category as `category {id, name}` and `category_id`; the catalog rows the start
 * sheets read call them `categoryId` and `categoryName`. Everything else has the same names.
 */
internal fun asCatalogRow(row: JsonObject): JsonObject {
    val category = (row["category"] as? JsonObject)
    val extra =
        buildMap<String, JsonElement> {
            (category?.get("id") ?: row["category_id"])?.let { put("categoryId", it) }
            category?.get("name")?.let { put("categoryName", it) }
        }
    return JsonObject(row + extra)
}

private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
