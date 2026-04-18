package com.example.nimons360.ui.families

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nimons360.ui.family.create.CreateFamilyActivity
import com.example.nimons360.ui.family.detail.FamilyDetailActivity
import com.example.nimons360.ui.family.list.FamilyScreen
import com.example.nimons360.ui.family.list.FamilyViewModel
import com.example.nimons360.ui.theme.Nimons360Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FamiliesFragment : Fragment() {
    private val viewModel: FamilyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Nimons360Theme {
                    FamilyScreen(
                        viewModel = viewModel,
                        onAddFamilyClick = {
                            val intent = Intent(requireContext(), CreateFamilyActivity::class.java)
                            startActivity(intent)
                        },
                        onFamilyClick = { familyId ->
                            val intent = Intent(requireContext(), FamilyDetailActivity::class.java)
                            val parsedId = familyId.toIntOrNull() ?: -1

                            intent.putExtra(FamilyDetailActivity.EXTRA_FAMILY_ID, parsedId)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchFamiliesData() // always fetch
    }
}