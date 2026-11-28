package com.rahmatulula.motocare.data.repository

import android.content.Context
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.data.remote.RetrofitClient
import com.rahmatulula.motocare.util.UiState
import java.io.IOException
import retrofit2.HttpException

data class WeatherInfo(
    val temperature: Int,
    val description: String,
    val windSpeed: Int,
    val precipitation: Double,
    val advice: String
)

class WeatherRepository(private val context: Context) {


    suspend fun load(lat: Double, lon: Double): UiState<WeatherInfo> {
        return try {
            val cur = RetrofitClient.weatherApi.current(lat, lon).current
                ?: return UiState.Error(
                    context.getString(R.string.weather_error_data)
                )

            val code = cur.weatherCode ?: 0
            val rain = (cur.precipitation ?: 0.0) > 0.0 || code in 51..99

            UiState.Success(
                WeatherInfo(
                    temperature = Math.round(cur.temperature ?: 0.0).toInt(),
                    description = context.getString(describe(code)),
                    windSpeed = Math.round(cur.windSpeed ?: 0.0).toInt(),
                    precipitation = cur.precipitation ?: 0.0,
                    advice = context.getString(
                        if (rain) R.string.advice_rain else R.string.advice_dry
                    )
                )
            )
        } catch (e: IOException) {
            UiState.Error(context.getString(R.string.err_network))
        } catch (e: HttpException) {
            UiState.Error(context.getString(R.string.weather_error_server))
        } catch (e: Exception) {
            UiState.Error(context.getString(R.string.weather_error_data))
        }
    }


    private fun describe(code: Int): Int = when (code) {
        0 -> R.string.wx_clear
        1, 2, 3 -> R.string.wx_cloudy
        45, 48 -> R.string.wx_fog
        in 51..57 -> R.string.wx_drizzle
        in 61..67, in 80..82 -> R.string.wx_rain
        in 95..99 -> R.string.wx_storm
        else -> R.string.wx_other
    }
}
