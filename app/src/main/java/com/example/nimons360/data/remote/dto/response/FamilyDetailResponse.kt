package com.example.nimons360.data.remote.dto.response

data class FamilyDetailResponse(
    val id: Int,
    val name: String,
    val iconUrl: String,
    val isMember: Boolean = false,
    val familyCode: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val members: List<MemberResponse> = emptyList()
)

data class MemberResponse(
    val id: Int? = null,
    val fullName: String,
    val email: String,
    val joinedAt: String? = null
)

data class JoinResponse(
    val joined: Boolean,
)

data class LeaveResponse(
    val left: Boolean,
)
