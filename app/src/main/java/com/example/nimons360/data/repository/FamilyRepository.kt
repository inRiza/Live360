package com.example.nimons360.data.repository

import com.example.nimons360.data.local.db.dao.PinnedFamilyDao
import com.example.nimons360.data.local.db.entity.PinnedFamilyEntity
import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.remote.dto.common.FamilyBasic
import com.example.nimons360.data.remote.dto.common.FamilyDiscover
import com.example.nimons360.data.remote.dto.common.MyFamilyDetail
import com.example.nimons360.data.remote.dto.response.FamilyDetailResponse
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
    suspend fun getAllFamilies(): Result<List<FamilyBasic>> = Result.Error("not implemented")
    suspend fun getMyFamilies(): Result<List<MyFamilyDetail>> = Result.Error("not implemented")
    suspend fun discoverFamilies(): Result<List<FamilyDiscover>> = Result.Error("not implemented")
    suspend fun getFamilyDetail(familyId: Int): Result<FamilyDetailResponse> = Result.Error("not implemented")
    suspend fun createFamily(name: String, iconUrl: String): Result<FamilyDetailResponse> = Result.Error("not implemented")
    suspend fun joinFamily(familyId: Int, familyCode: String): Result<JoinResponse> = Result.Error("not implemented")
    suspend fun leaveFamily(familyId: Int): Result<LeaveResponse> = Result.Error("not implemented")
    fun getPinnedFamilies(): Flow<List<PinnedFamilyEntity>> = flowOf(emptyList())
    suspend fun pinFamily(family: FamilyBasic) {}
    suspend fun unpinFamily(familyId: Int) {}
    suspend fun isPinned(familyId: Int): Boolean = false
}
