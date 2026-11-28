package com.rahmatulula.motocare.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** API publik Open-Meteo (tanpa API key). */
interface WeatherApi {
    @GET("v1/forecast")
    suspend fun current(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,precipitation,weather_code,wind_speed_10m",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}

data class WeatherResponse(
    @SerializedName("current") val current: CurrentWeather?
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("precipitation") val precipitation: Double?,
    @SerializedName("weather_code") val weatherCode: Int?,
    @SerializedName("wind_speed_10m") val windSpeed: Double?
)

object RetrofitClient {
    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}
