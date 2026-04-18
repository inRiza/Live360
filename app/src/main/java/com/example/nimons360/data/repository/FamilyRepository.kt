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
import kotlinx.coroutines.flow.flowOf

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
                Result.Error("Gagal mengambil daftar keluarga: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan")
        }
    }

    suspend fun getMyFamilies(): Result<List<MyFamilyDetail>> {
        return try {
            val response = apiService.getMyFamilies()
            if (response.isSuccessful) {
                Result.Success(response.body()?.data ?: emptyList())
            } else {
                Result.Error("Gagal mengambil keluarga Anda: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan")
        }
    }

    suspend fun getFamilyDetail(familyId: Int): Result<FamilyDetailWrappedResponse> {
        return try {
            val response = apiService.getFamilyDetail(familyId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.Success(data) else Result.Error("Data keluarga tidak ditemukan")
            } else {
                Result.Error("Gagal mengambil detail: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan")
        } as Result<FamilyDetailWrappedResponse>
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
                    Result.Error("Gagal mendapatkan data family")
                }
            } else {
                Result.Error("Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan")
        }
    }

    suspend fun joinFamily(familyId: Int, familyCode: String): Result<JoinResponse> {
        return try {
            val request = JoinFamilyRequest(familyId, familyCode)
            val response = apiService.joinFamily(request)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.Success(data) else Result.Error("Gagal bergabung")
            } else {
                Result.Error("Kode keluarga salah atau error: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan")
        }
    }

    suspend fun leaveFamily(familyId: Int): Result<LeaveResponse> {
        return try {
            val request = LeaveFamilyRequest(familyId)
            val response = apiService.leaveFamily(request)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.Success(data) else Result.Error("Gagal keluar dari keluarga")
            } else {
                Result.Error("Gagal keluar: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan")
        }
    }

    // Mengambil data real-time dari Room Database
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

    suspend fun getMyFamilies(): Result<List<MyFamilyDetail>> {
        return try {
            val res = apiService.getMyFamilies()
            if (res.isSuccessful) Result.Success(res.body()?.data ?: emptyList())
            else Result.Error("Failed: ${res.code()}")
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
    suspend fun isPinned(familyId: Int): Boolean = false
}
