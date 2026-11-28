package com.rahmatulula.motocare.ui.home

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.rahmatulula.motocare.R
import com.rahmatulula.motocare.databinding.FragmentHomeBinding
import com.rahmatulula.motocare.ui.common.ReminderAdapter
import com.rahmatulula.motocare.ui.common.visible
import com.rahmatulula.motocare.util.Formatters
import com.rahmatulula.motocare.util.LocationHelper
import com.rahmatulula.motocare.util.UiState
import com.rahmatulula.motocare.viewmodel.MotoViewModel
import com.rahmatulula.motocare.viewmodel.WeatherViewModel

class HomeFragment : Fragment() {
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!
    private val vm: MotoViewModel by activityViewModels()
    private val weather: WeatherViewModel by viewModels()

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Ditolak: tetap berjalan normal memakai lokasi bawaan
                Snackbar.make(b.root, R.string.location_denied_info, Snackbar.LENGTH_LONG).show()
            }
            weather.load()
        }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHomeBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = FirebaseAuth.getInstance().currentUser?.displayName
        b.tvGreeting.text = getString(R.string.home_greeting, if (name.isNullOrBlank()) getString(R.string.default_user) else name)

        val adapter = ReminderAdapter(onClick = { findNavController().navigate(R.id.remindersFragment) })
        b.rvUpcoming.layoutManager = LinearLayoutManager(requireContext())
        b.rvUpcoming.adapter = adapter

        vm.upcomingReminders.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            b.tvUpcomingEmpty.visible(it.isEmpty())
            b.rvUpcoming.visible(it.isNotEmpty())
        }
        vm.serviceCount.observe(viewLifecycleOwner) { b.tvStatCount.text = it.toString() }
        vm.totalCost.observe(viewLifecycleOwner) { b.tvStatCost.text = Formatters.rupiah(it) }

        b.btnAddService.setOnClickListener {
            if (vm.motors.value.isNullOrEmpty()) {
                Snackbar.make(b.root, R.string.need_motor_first, Snackbar.LENGTH_LONG).show()
                findNavController().navigate(R.id.motorFormFragment, bundleOf("motorId" to 0L))
            } else {
                findNavController().navigate(R.id.serviceFormFragment, bundleOf("serviceId" to 0L))
            }
        }

        b.btnWeatherRetry.setOnClickListener { weather.load() }
        b.btnUseLocation.setOnClickListener { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }

        weather.state.observe(viewLifecycleOwner) { state ->
            b.weatherProgress.visible(state is UiState.Loading)
            b.weatherContent.visible(state is UiState.Success)
            b.weatherError.visible(state is UiState.Error)
            when (state) {
                is UiState.Success -> {
                    val w = state.data
                    b.tvWeatherTemp.text = getString(R.string.weather_temp_fmt, w.temperature)
                    b.tvWeatherDesc.text = getString(R.string.weather_desc_fmt, w.description, w.windSpeed)
                    b.tvWeatherAdvice.text = w.advice
                    b.tvWeatherLoc.text = getString(
                        if (weather.usingDefaultLocation) R.string.weather_loc_default else R.string.weather_loc_device
                    )
                    b.btnUseLocation.visible(!LocationHelper.hasPermission(requireContext()))
                }
                is UiState.Error -> b.tvWeatherError.text = state.message
                else -> Unit
            }
        }
        // Muat cuaca hanya sekali; saat layar diputar status dipertahankan ViewModel
        if (savedInstanceState == null && weather.state.value is UiState.Loading) weather.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
