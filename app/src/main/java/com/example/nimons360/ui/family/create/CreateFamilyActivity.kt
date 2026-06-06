package com.example.nimons360.ui.family.create

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nimons360.R
import com.example.nimons360.ui.family.create.components.IconPickerAdapter
import com.example.nimons360.ui.family.detail.FamilyDetailActivity
import com.example.nimons360.utils.Result
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateFamilyActivity : AppCompatActivity() {

    // Deklarasi Variabel
    private val viewModel: CreateFamilyViewModel by viewModels()
    private lateinit var etFamilyName: TextInputEditText
    private lateinit var tvCreate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_family)

        // Inisialisasi View
        val ivSelectedIcon = findViewById<ImageView>(R.id.ivSelectedIcon)
        val rvIcons = findViewById<RecyclerView>(R.id.rvIcons)
        val tvCancel = findViewById<TextView>(R.id.tvCancel)
        etFamilyName = findViewById(R.id.etFamilyName)
        tvCreate = findViewById(R.id.tvCreate)

        // Dynamic Span Count by Orientation
        val orientation = resources.configuration.orientation
        val spanCount = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 4

        // Setup RecyclerView
        val adapter = IconPickerAdapter(
            icons = viewModel.availableIcons,
            initialSelectedIconId = viewModel.selectedIcon.value, // responsive
            onIconSelected = { selectedIconId ->
                viewModel.setSelectedIcon(selectedIconId)
            }
        )
        rvIcons.adapter = adapter
        rvIcons.layoutManager = GridLayoutManager(this, spanCount)

        // Setup Listener Tombol
        tvCancel.setOnClickListener {
            finish()
        }

        tvCreate.setOnClickListener {
            val familyName = etFamilyName.text.toString().trim()
            viewModel.createFamily(familyName)
        }

        // Observe
        observeViewModel(ivSelectedIcon)
    }

    private fun observeViewModel(ivSelectedIcon: ImageView) {
        // Pilihan Icon
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedIcon.collect { iconResId ->
                    ivSelectedIcon.setImageResource(iconResId)
                }
            }
        }

        // Status Pembuatan Keluarga
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createState.collect { result ->

                    if (result is Result.Loading) {
                        tvCreate.isEnabled = false
                    } else if (result is Result.Success) {
                        val intent = Intent(
                            this@CreateFamilyActivity,
                            FamilyDetailActivity::class.java
                        )
                        intent.putExtra(
                            FamilyDetailActivity.EXTRA_FAMILY_ID,
                            result.data.id
                        )

                        startActivity(intent)
                        finish()
                    } else if (result is Result.Error) {
                        tvCreate.isEnabled = true
                        Toast.makeText(
                            this@CreateFamilyActivity,
                            result.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }
        }
    }
}