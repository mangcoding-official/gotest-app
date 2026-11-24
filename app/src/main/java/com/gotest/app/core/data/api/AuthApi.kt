package com.gotest.app.core.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** API module A routes used by every account (API-CONTRACTS "API Android modul A"). */
interface AuthApi {
    @GET("api/v1/referensi")
    suspend fun reference(): Reference

    @POST("api/v1/auth/masuk")
    suspend fun signIn(
        @Body request: SignInRequest,
    ): SignInReply

    @POST("api/v1/auth/keluar")
    suspend fun signOut(): MessageReply

    @POST("api/v1/auth/lupa-sandi")
    suspend fun forgotPassword(
        @Body request: EmailRequest,
    ): MessageReply

    @GET("api/v1/akun")
    suspend fun account(): AccountPage
}
