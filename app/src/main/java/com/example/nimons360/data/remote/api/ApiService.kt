package com.example.nimons360.data.remote.api

import com.example.nimons360.data.remote.dto.request.CreateFamilyRequest
import com.example.nimons360.data.remote.dto.request.JoinFamilyRequest
import com.example.nimons360.data.remote.dto.request.LeaveFamilyRequest
import com.example.nimons360.data.remote.dto.request.LoginRequest
import com.example.nimons360.data.remote.dto.request.UpdateProfileRequest
import com.example.nimons360.data.remote.dto.response.BaseResponse
import com.example.nimons360.data.remote.dto.response.FamilyDetailResponse
import com.example.nimons360.data.remote.dto.response.FamilyResponse
import com.example.nimons360.data.remote.dto.response.JoinResponse
import com.example.nimons360.data.remote.dto.response.LeaveResponse
import com.example.nimons360.data.remote.dto.response.LoginResponse
import com.example.nimons360.data.remote.dto.response.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<BaseResponse<LoginResponse>>

    @GET("/api/me")
    suspend fun getProfile(): Response<BaseResponse<UserResponse>>

    @PATCH("/api/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<BaseResponse<UserResponse>>

    @GET("/api/families")
    suspend fun getAllFamilies(): Response<BaseResponse<List<FamilyResponse>>>

    @GET("/api/me/families")
    suspend fun getMyFamilies(): Response<BaseResponse<List<FamilyDetailResponse>>>

    @GET("/api/families/discover")
    suspend fun discoverFamilies(): Response<BaseResponse<List<FamilyDetailResponse>>>

    @GET("/api/families/{familyId}")
    suspend fun getFamilyDetail(@Path("familyId") familyId: Int): Response<BaseResponse<FamilyDetailResponse>>

    @POST("/api/families")
    suspend fun createFamily(@Body request: CreateFamilyRequest): Response<BaseResponse<FamilyDetailResponse>>

    @POST("/api/families/join")
    suspend fun joinFamily(@Body request: JoinFamilyRequest): Response<BaseResponse<JoinResponse>>

    @POST("/api/families/leave")
    suspend fun leaveFamily(@Body request: LeaveFamilyRequest): Response<BaseResponse<LeaveResponse>>
}
