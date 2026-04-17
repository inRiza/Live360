package com.example.nimons360.data.remote.api

import com.example.nimons360.data.remote.dto.request.*
import com.example.nimons360.data.remote.dto.response.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiLoginPost200Response>

    @GET("api/me")
    suspend fun getProfile(): Response<ApiMeGet200Response>

    @PATCH("api/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiMeGet200Response>

    @GET("api/families")
    suspend fun getAllFamilies(): Response<ApiFamiliesGet200Response>

    @GET("api/me/families")
    suspend fun getMyFamilies(): Response<ApiMeFamiliesGet200Response>

    @GET("api/families/discover")
    suspend fun discoverFamilies(): Response<ApiFamiliesDiscoverGet200Response>

    @GET("api/families/{familyId}")
    suspend fun getFamilyDetail(@Path("familyId") familyId: Int): Response<FamilyDetailResponse>

    @POST("api/families")
    suspend fun createFamily(@Body request: CreateFamilyRequest): Response<ApiFamiliesPost200Response>

    @POST("api/families/join")
    suspend fun joinFamily(@Body request: JoinFamilyRequest): Response<ApiFamiliesJoinPost200Response>

    @POST("api/families/leave")
    suspend fun leaveFamily(@Body request: LeaveFamilyRequest): Response<ApiFamiliesLeavePost200Response>
}
