package com.example.nimons360.ui.family.create

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
    private val viewModel: CreateFamilyViewModel by viewModels()
    private lateinit var etFamilyName: TextInputEditText
    private lateinit var tvCreate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_family)

        val ivSelectedIcon = findViewById<ImageView>(R.id.ivSelectedIcon)
        val rvIcons = findViewById<RecyclerView>(R.id.rvIcons)
        val tvCancel = findViewById<TextView>(R.id.tvCancel)
        etFamilyName = findViewById(R.id.etFamilyName)
        tvCreate = findViewById(R.id.tvCreate)

        // Setup RecyclerView & Listener
        rvIcons.adapter = IconPickerAdapter(viewModel.availableIcons) { viewModel.setSelectedIcon(it) }
        rvIcons.layoutManager = GridLayoutManager(this, 5)

        tvCancel.setOnClickListener { finish() }
        tvCreate.setOnClickListener { viewModel.createFamily(etFamilyName.text.toString()) }

        observeViewModel(ivSelectedIcon)
    }

    private fun observeViewModel(ivSelectedIcon: ImageView) {
        lifecycleScope.launch {
            viewModel.selectedIcon.collect { ivSelectedIcon.setImageResource(it) }
        }

        lifecycleScope.launch {
            viewModel.createState.collect { result ->
                when (result) {
                    is Result.Loading -> tvCreate.isEnabled = false
                    is Result.Success -> {
                        val intent = Intent(this@CreateFamilyActivity, FamilyDetailActivity::class.java).apply {
                            putExtra(FamilyDetailActivity.EXTRA_FAMILY_ID, result.data.id)
                        }
                        startActivity(intent)
                        finish()
                    }
                    is Result.Error -> {
                        tvCreate.isEnabled = true
                        Toast.makeText(this@CreateFamilyActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}