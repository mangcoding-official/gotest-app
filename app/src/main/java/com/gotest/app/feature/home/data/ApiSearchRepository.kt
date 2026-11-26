package com.gotest.app.feature.home.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.home.domain.SearchPage
import com.gotest.app.feature.home.domain.SearchRepository
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/** Cari against API module B (T-026 A2c-1 = a). */
@Singleton
class ApiSearchRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : SearchRepository {
        private val api = retrofit.create(HomeApi::class.java)

        override suspend fun search(query: String): Result<SearchPage> = apiCall(json) { api.search(query) }
    }
