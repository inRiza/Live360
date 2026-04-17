package com.example.nimons360.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nimons360.databinding.FragmentHomeBinding
import com.example.nimons360.ui.home.adapter.DiscoverFamilyAdapter
import com.example.nimons360.ui.home.adapter.MyFamilyAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var myFamilyAdapter: MyFamilyAdapter
    private lateinit var discoverAdapter: DiscoverFamilyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        myFamilyAdapter = MyFamilyAdapter(emptyList()) { family ->
            // TODO: navigate ke detail
        }
        binding.rvMyFamilies.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = myFamilyAdapter
            isNestedScrollingEnabled = false
        }

        discoverAdapter = DiscoverFamilyAdapter(
            emptyList(),
            onJoinClick = { family ->
                // TODO: join family
            },
            onItemClick = { family ->
                // TODO: navigate ke detail
            }
        )
        binding.rvDiscoverFamilies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = discoverAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.myFamilies.collect { families ->
                        myFamilyAdapter = MyFamilyAdapter(families) { family ->
                            // TODO: navigate ke detail
                        }
                        binding.rvMyFamilies.adapter = myFamilyAdapter

                        val isEmpty = families.isEmpty()
                        binding.rvMyFamilies.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        binding.tvEmptyMyFamilies.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.discoverFamilies.collect { families ->
                        discoverAdapter = DiscoverFamilyAdapter(
                            families,
                            onJoinClick = { family ->
                                // TODO: join family
                            },
                            onItemClick = { family ->
                                // TODO: navigate ke detail
                            }
                        )
                        binding.rvDiscoverFamilies.adapter = discoverAdapter

                        val isEmpty = families.isEmpty()
                        binding.rvDiscoverFamilies.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        binding.tvEmptyDiscoverFamilies.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}