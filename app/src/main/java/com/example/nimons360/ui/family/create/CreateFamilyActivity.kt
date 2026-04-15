package com.example.nimons360.ui.family.create

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nimons360.R
import com.example.nimons360.ui.family.create.components.IconPickerAdapter
import kotlinx.coroutines.launch

class CreateFamilyActivity : AppCompatActivity() {
    private val viewModel: CreateFamilyViewModel by viewModels()
    private lateinit var ivSelectedIcon: ImageView
    private lateinit var rvIcons: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_family)

        ivSelectedIcon = findViewById(R.id.ivSelectedIcon)
        rvIcons = findViewById(R.id.rvIcons)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val adapter = IconPickerAdapter(viewModel.availableIcons) { iconResId ->
            viewModel.setSelectedIcon(iconResId)
        }
        rvIcons.adapter = adapter
        rvIcons.layoutManager = GridLayoutManager(this, 4)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.selectedIcon.collect { iconResId ->
                // Update foto profil besar di atas saat ikon diklik
                ivSelectedIcon.setImageResource(iconResId)
            }
        }
    }
}