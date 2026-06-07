package com.example.nimons360.data.repository

import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.remote.dto.request.SendFamilyNotificationRequest
import com.example.nimons360.data.remote.dto.request.SendGreetingRequest
import com.example.nimons360.data.remote.dto.request.SubscribeRequest
import com.example.nimons360.data.remote.dto.response.SendFamilyNotificationResponse
import com.example.nimons360.data.remote.dto.response.SendGreetingResponse
import com.example.nimons360.data.remote.dto.response.SubscribeResponse
import com.example.nimons360.data.remote.dto.response.UnsubscribeResponse
import com.example.nimons360.utils.Result
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun subscribeToken(fcmToken: String): Result<SubscribeResponse> {
        return try {
            val res = apiService.subscribeToken(SubscribeRequest(fcmToken))

            if (res.isSuccessful) {
                val data = res.body()?.data
                
                if (data != null) Result.Success(data)
                else Result.Error("Empty response")
            } else {
                Result.Error("Failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun unsubscribeToken(): Result<UnsubscribeResponse> {
        return try {
            val res = apiService.unsubscribeToken()

            if (res.isSuccessful) {
                val data = res.body()?.data

                if (data != null) Result.Success(data)
                else Result.Error("Empty response")
            } else {
                Result.Error("Failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun sendFamilyNotification(familyId: Int, message: String): Result<SendFamilyNotificationResponse> {
        return try {
            val res = apiService.sendNotification(SendFamilyNotificationRequest(familyId, message))

            if (res.isSuccessful) {
                val data = res.body()?.data

                if (data != null) Result.Success(data)
                else Result.Error("Empty response")
            } else {
                Result.Error("Failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun sendGreeting(familyId: Int, targetUserId: Int, message: String): Result<SendGreetingResponse> {
        return try {
            val res = apiService.sendGreetingNotification(SendGreetingRequest(familyId, targetUserId, message))

            if (res.isSuccessful) {
                val data = res.body()?.data

                if (data != null) Result.Success(data)
                else Result.Error("Empty response")
            } else {
                Result.Error("Failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
