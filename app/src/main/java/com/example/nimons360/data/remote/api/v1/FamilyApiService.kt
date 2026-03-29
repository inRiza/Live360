package com.example.nimons360.data.remote.api.v1

import com.example.nimons360.data.remote.dto.request.*
import com.example.nimons360.data.remote.dto.response.*
import retrofit2.Response
import retrofit2.http.*

interface FamilyApiService {

    @GET("api/me")
    suspend fun getProfile(): Response<BaseResponse<UserResponse>>

    @PATCH("api/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<BaseResponse<UserResponse>>

    @GET("api/families")
    suspend fun getAllFamilies(): Response<BaseResponse<List<FamilyResponse>>>

    @POST("api/families")
    suspend fun createFamily(
        @Body request: CreateFamilyRequest
    ): Response<BaseResponse<FamilyDetailResponse>>

    @GET("api/me/families")
    suspend fun getMyFamilies(): Response<BaseResponse<List<FamilyDetailResponse>>>

    @GET("api/families/discover")
    suspend fun discoverFamilies(): Response<BaseResponse<List<FamilyDetailResponse>>>

    @GET("api/families/{familyId}")
    suspend fun getFamilyDetail(
        @Path("familyId") familyId: Int
    ): Response<BaseResponse<FamilyDetailResponse>>

    @POST("api/families/join")
    suspend fun joinFamily(
        @Body request: JoinFamilyRequest
    ): Response<BaseResponse<JoinResponse>>

    @POST("api/families/leave")
    suspend fun leaveFamily(
        @Body request: LeaveFamilyRequest
    ): Response<BaseResponse<LeaveResponse>>
}