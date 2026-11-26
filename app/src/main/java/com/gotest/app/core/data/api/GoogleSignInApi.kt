package com.gotest.app.core.data.api

import retrofit2.http.Body
import retrofit2.http.POST

/** Masuk dengan Google of API module A; both answer 404 until the server has the app's Google client IDs. */
interface GoogleSignInApi {
    /** The sign-in reply with `linked`, or 422 `google_not_registered` with a `registration_token`, name, email. */
    @POST("api/v1/auth/google")
    suspend fun signIn(
        @Body request: GoogleSignInRequest,
    ): SignInReply

    @POST("api/v1/auth/google/daftar")
    suspend fun signUp(
        @Body request: GoogleSignUpRequest,
    ): SignInReply
}
