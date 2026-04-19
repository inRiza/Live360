package com.example.nimons360.data.repository

import com.example.nimons360.data.local.db.dao.PinnedFamilyDao
import com.example.nimons360.data.local.db.entity.PinnedFamilyEntity
import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.remote.dto.common.FamilyBasic
import com.example.nimons360.data.remote.dto.common.FamilyDiscover
import com.example.nimons360.data.remote.dto.common.MyFamilyDetail
import com.example.nimons360.data.remote.dto.request.CreateFamilyRequest
import com.example.nimons360.data.remote.dto.request.JoinFamilyRequest
import com.example.nimons360.data.remote.dto.request.LeaveFamilyRequest
import com.example.nimons360.data.remote.dto.response.FamilyDetailResponse
import com.example.nimons360.data.remote.dto.response.FamilyDetailWrappedResponse
import com.example.nimons360.data.remote.dto.response.JoinResponse
import com.example.nimons360.data.remote.dto.response.LeaveResponse
import com.example.nimons360.utils.Result
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class FamilyRepository @Inject constructor(
    private val apiService: ApiService,
    private val pinnedFamilyDao: PinnedFamilyDao
) {
    suspend fun getAllFamilies(): Result<List<FamilyBasic>> {
        return try {
            val response = apiService.getAllFamilies()
            if (response.isSuccessful) {
                Result.Success(response.body()?.data ?: emptyList())
            } else {
                Result.Error("Failed to get all families: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getMyFamilies(): Result<List<MyFamilyDetail>> {
        return try {
            val response = apiService.getMyFamilies()
            if (response.isSuccessful) {
                Result.Success(response.body()?.data ?: emptyList())
            } else {
                Result.Error("Failed to retrieve my families: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun discoverFamilies(): Result<List<FamilyDiscover>> {
        return try {
            val res = apiService.discoverFamilies()
            if (res.isSuccessful) Result.Success(res.body()?.data ?: emptyList())
            else Result.Error("Failed: ${res.code()}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getFamilyDetail(familyId: Int): Result<FamilyDetailResponse> {
        return try {
            val response = apiService.getFamilyDetail(familyId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.Success(data) else Result.Error("Families data not found")
            } else {
                Result.Error("Failed to retrieve family detail: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun createFamily(name: String, iconUrl: String): Result<FamilyDetailResponse> {
        return try {
            val request = CreateFamilyRequest(name = name, iconUrl = iconUrl)
            val response = apiService.createFamily(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.data != null) {
                    Result.Success(responseBody.data)
                } else {
                    Result.Error("Failed to create family")
                }
            } else {
                Result.Error("Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun joinFamily(familyId: Int, familyCode: String): Result<JoinResponse> {
        return try {
            val request = JoinFamilyRequest(familyId, familyCode)
            val response = apiService.joinFamily(request)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.Success(data) else Result.Error("Failed to join family")
            } else {
                Result.Error("Wrong code or error: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun leaveFamily(familyId: Int): Result<LeaveResponse> {
        return try {
            val request = LeaveFamilyRequest(familyId)
            val response = apiService.leaveFamily(request)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.Success(data) else Result.Error("Failed to leave family")
            } else {
                Result.Error("Failed to leave family: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    // Local Database

    fun getPinnedFamilies(): Flow<List<PinnedFamilyEntity>> {
        return pinnedFamilyDao.getAll()
    }

    suspend fun pinFamily(id: Int, name: String, iconUrl: String) {
        val entity = PinnedFamilyEntity(id = id, name = name, iconUrl = iconUrl)
        pinnedFamilyDao.insert(entity)
    }

    suspend fun unpinFamily(familyId: Int) {
        pinnedFamilyDao.deleteById(familyId)
    }

    suspend fun isPinned(familyId: Int): Boolean {
        return pinnedFamilyDao.isPinned(familyId)
    }
}