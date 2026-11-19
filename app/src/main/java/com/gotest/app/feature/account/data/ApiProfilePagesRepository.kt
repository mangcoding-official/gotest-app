package com.gotest.app.feature.account.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.account.domain.CurrentPassword
import com.gotest.app.feature.account.domain.DeleteConfirmation
import com.gotest.app.feature.account.domain.EmailCodeRequest
import com.gotest.app.feature.account.domain.EmailSave
import com.gotest.app.feature.account.domain.PasswordChange
import com.gotest.app.feature.account.domain.PhoneCodeRequest
import com.gotest.app.feature.account.domain.PhoneSave
import com.gotest.app.feature.account.domain.ProfilePage
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import com.gotest.app.feature.account.domain.ProfileUpdate
import com.gotest.app.feature.account.domain.ProfileWriteReply
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import javax.inject.Inject
import javax.inject.Singleton

/** Profil routes of API module F. */
interface ProfileApi {
    @GET("api/v1/profil")
    suspend fun page(): ProfilePage

    @PATCH("api/v1/profil")
    suspend fun update(
        @Body update: ProfileUpdate,
    ): ProfileWriteReply

    @POST("api/v1/profil/email/kode")
    suspend fun resendEmailCode(
        @Body request: EmailCodeRequest,
    ): ProfileWriteReply

    @PUT("api/v1/profil/email")
    suspend fun saveEmail(
        @Body save: EmailSave,
    ): ProfileWriteReply

    @Multipart
    @POST("api/v1/profil/foto")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part,
    ): ProfileWriteReply

    @DELETE("api/v1/profil/foto")
    suspend fun deletePhoto(): ProfileWriteReply

    @PUT("api/v1/profil/kata-sandi")
    suspend fun changePassword(
        @Body change: PasswordChange,
    ): ProfileWriteReply

    @POST("api/v1/profil/tautan-sandi")
    suspend fun sendPasswordLink(): ProfileWriteReply

    @POST("api/v1/profil/nomor-hp/kode")
    suspend fun sendPhoneCode(
        @Body request: PhoneCodeRequest,
    ): ProfileWriteReply

    @PUT("api/v1/profil/nomor-hp")
    suspend fun savePhone(
        @Body save: PhoneSave,
    ): ProfileWriteReply

    @DELETE("api/v1/profil/nomor-hp")
    suspend fun removePhone(): ProfileWriteReply

    @HTTP(method = "DELETE", path = "api/v1/profil/perangkat", hasBody = true)
    suspend fun signOutOtherDevices(
        @Body password: CurrentPassword,
    ): ProfileWriteReply

    @HTTP(method = "DELETE", path = "api/v1/profil", hasBody = true)
    suspend fun deleteAccount(
        @Body confirmation: DeleteConfirmation,
    ): ProfileWriteReply
}

@Singleton
class ApiProfilePagesRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : ProfilePagesRepository {
        private val api = retrofit.create(ProfileApi::class.java)

        override suspend fun page(): Result<ProfilePage> = apiCall(json) { api.page() }

        override suspend fun update(update: ProfileUpdate): Result<ProfileWriteReply> =
            apiCall(json) { api.update(update) }

        override suspend fun resendEmailCode(email: String): Result<ProfileWriteReply> =
            apiCall(json) { api.resendEmailCode(EmailCodeRequest(email)) }

        override suspend fun saveEmail(
            email: String,
            code: String,
        ): Result<ProfileWriteReply> = apiCall(json) { api.saveEmail(EmailSave(email, code)) }

        override suspend fun uploadPhoto(jpeg: ByteArray): Result<ProfileWriteReply> =
            apiCall(json) {
                val body = jpeg.toRequestBody("image/jpeg".toMediaType())
                api.uploadPhoto(MultipartBody.Part.createFormData("photo", "avatar.jpg", body))
            }

        override suspend fun deletePhoto(): Result<ProfileWriteReply> = apiCall(json) { api.deletePhoto() }

        override suspend fun changePassword(change: PasswordChange): Result<ProfileWriteReply> =
            apiCall(json) { api.changePassword(change) }

        override suspend fun sendPasswordLink(): Result<ProfileWriteReply> = apiCall(json) { api.sendPasswordLink() }

        override suspend fun sendPhoneCode(phone: String): Result<ProfileWriteReply> =
            apiCall(json) { api.sendPhoneCode(PhoneCodeRequest(phone)) }

        override suspend fun savePhone(
            phone: String,
            code: String,
        ): Result<ProfileWriteReply> = apiCall(json) { api.savePhone(PhoneSave(phone, code)) }

        override suspend fun removePhone(): Result<ProfileWriteReply> = apiCall(json) { api.removePhone() }

        override suspend fun signOutOtherDevices(currentPassword: String): Result<ProfileWriteReply> =
            apiCall(json) { api.signOutOtherDevices(CurrentPassword(currentPassword)) }

        override suspend fun deleteAccount(confirmation: String): Result<ProfileWriteReply> =
            apiCall(json) { api.deleteAccount(DeleteConfirmation(confirmation)) }
    }
