package com.gotest.app.core.data.api

import retrofit2.http.Body
import retrofit2.http.POST

/** The code and family ways of signing in of API module A: kode email, Nomor HP, and Masuk Anak. */
interface SignInFlowApi {
    @POST("api/v1/auth/masuk/email/kode")
    suspend fun sendEmailCode(
        @Body request: EmailRequest,
    ): EmailCodeSentReply

    @POST("api/v1/auth/masuk/email/kode/kirim-ulang")
    suspend fun resendEmailCode(
        @Body request: EmailRequest,
    ): EmailCodeSentReply

    @POST("api/v1/auth/masuk/email")
    suspend fun signInWithEmailCode(
        @Body request: EmailCodeSignInRequest,
    ): SignInReply

    @POST("api/v1/auth/nomor-hp/kode")
    suspend fun sendPhoneCode(
        @Body request: PhoneRequest,
    ): PhoneCodeSentReply

    /** The sign-in reply, or 422 `phone_not_registered` with a `registration_token` for a new number. */
    @POST("api/v1/auth/nomor-hp")
    suspend fun signInWithPhone(
        @Body request: PhoneSignInRequest,
    ): SignInReply

    @POST("api/v1/auth/nomor-hp/daftar")
    suspend fun signUpWithPhone(
        @Body request: PhoneSignUpRequest,
    ): SignInReply

    @POST("api/v1/auth/masuk-anak/kode")
    suspend fun family(
        @Body request: FamilyCodeRequest,
    ): FamilyReply

    @POST("api/v1/auth/masuk-anak")
    suspend fun signInChild(
        @Body request: ChildSignInRequest,
    ): SignInReply
}
