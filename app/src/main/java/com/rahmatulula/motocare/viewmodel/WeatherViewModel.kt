package com.rahmatulula.motocare.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rahmatulula.motocare.data.repository.WeatherInfo
import com.rahmatulula.motocare.data.repository.WeatherRepository
import com.rahmatulula.motocare.util.LocationHelper
import com.rahmatulula.motocare.util.UiState
import kotlinx.coroutines.launch

class WeatherViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WeatherRepository(app)

    private val _state = MutableLiveData<UiState<WeatherInfo>>(UiState.Loading)
    val state: LiveData<UiState<WeatherInfo>> = _state

    var usingDefaultLocation = true
        private set

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val c = LocationHelper.current(getApplication())
            usingDefaultLocation = c.isDefault
            _state.value = repo.load(c.lat, c.lon)
        }
    }
}
