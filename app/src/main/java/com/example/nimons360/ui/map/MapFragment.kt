package com.example.nimons360.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment() {

	private val viewModel: MapViewModel by viewModels()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return ComposeView(requireContext()).apply {
			setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
			setContent {
				MapComposeContent()
			}
		}
	}

	override fun onResume() {
		super.onResume()
		if (hasLocationPermission()) {
			com.example.nimons360.service.LocationForegroundService.start(requireContext())
		}
	}

	override fun onDestroy() {
		val changingConfig = activity?.isChangingConfigurations ?: false
		if (!changingConfig) {
			com.example.nimons360.service.LocationForegroundService.stop(requireContext())
		}
		super.onDestroy()
	}

	private fun hasLocationPermission(): Boolean {
		return androidx.core.content.ContextCompat.checkSelfPermission(
			requireContext(),
			android.Manifest.permission.ACCESS_FINE_LOCATION
		) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
				androidx.core.content.ContextCompat.checkSelfPermission(
					requireContext(),
					android.Manifest.permission.ACCESS_COARSE_LOCATION
				) == android.content.pm.PackageManager.PERMISSION_GRANTED
	}

	@Composable
	private fun MapComposeContent() {
		MaterialTheme {
			MapScreen(viewModel = viewModel)
		}
	}
}