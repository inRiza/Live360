package com.example.nimons360.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.nimons360.ui.profile.components.CustomizePinScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomizePinFragment : Fragment() {

    private val viewModel: CustomizePinViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    CustomizePinScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}
