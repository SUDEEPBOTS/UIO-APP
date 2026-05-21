package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class StationStatus(
    @Json(name = "is_current_station") val isCurrentStation: Boolean?,
    @Json(name = "station_name") val stationName: String?,
    @Json(name = "distance") val distance: String?,
    @Json(name = "timing") val timing: String?,
    @Json(name = "delay") val delay: String?,
    @Json(name = "platform") val platform: String?,
    @Json(name = "halt") val halt: String?
)

@JsonClass(generateAdapter = true)
data class TrainResponse(
    @Json(name = "success") val success: Boolean?,
    @Json(name = "train_name") val trainName: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "updated_time") val updatedTime: String?,
    @Json(name = "data") val data: List<StationStatus>?
)

interface TrainApiService {
    @GET("apis/train.php")
    suspend fun getTrainRunningStatus(
        @Query("train_no") trainNo: String
    ): TrainResponse

    companion object {
        private const val BASE_URL = "https://rappid.in/"

        fun create(): TrainApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            return retrofit.create(TrainApiService::class.java)
        }
    }
}
