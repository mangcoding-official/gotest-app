package com.gotest.app.core.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

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

    @POST("api/v1/auth/daftar/info")
    suspend fun signUpInfo(
        @Body request: MitraLinkRequest,
    ): SignUpInfoReply

    /** 201 with a token, or 202 `email_code_sent` with a `registration_token` and no account yet. */
    @POST("api/v1/auth/daftar")
    suspend fun signUp(
        @Body request: SignUpRequest,
    ): SignInReply

    @POST("api/v1/auth/daftar/kode")
    suspend fun confirmSignUpCode(
        @Body request: SignUpCodeRequest,
    ): SignInReply

    /** `{sent_to, resend_in}`, or 201 `email_verification_pending` with a token when the code could not go out. */
    @POST("api/v1/auth/daftar/kode/kirim-ulang")
    suspend fun resendSignUpCode(
        @Body request: SignUpResendRequest,
    ): SignInReply

    @GET("api/v1/persetujuan-orang-tua")
    suspend fun consent(): ConsentReply

    @POST("api/v1/persetujuan-orang-tua/kirim-ulang")
    suspend fun resendConsent(): ConsentReply

    @PUT("api/v1/persetujuan-orang-tua/nomor")
    suspend fun changeConsentContact(
        @Body request: ConsentContactRequest,
    ): ConsentReply
}
