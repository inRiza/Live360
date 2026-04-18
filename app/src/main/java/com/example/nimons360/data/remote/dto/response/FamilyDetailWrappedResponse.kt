package com.example.nimons360.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class FamilyDetailWrappedResponse(
    @SerializedName("data")
    val data: FamilyDetailResponse? = null
)