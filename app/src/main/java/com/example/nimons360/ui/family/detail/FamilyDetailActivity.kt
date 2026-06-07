package com.example.nimons360.ui.family.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nimons360.ui.theme.Nimons360Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FamilyDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var familyId = intent.getIntExtra(EXTRA_FAMILY_ID, -1)
        val action: String? = intent.action
        val data: Uri? = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            // Cek URI nimons360://family/...
            if (data.scheme == "nimons360" && data.host == "family") {
                val pathSegment = data.pathSegments.firstOrNull() // ID family
                if (pathSegment != null) {
                    familyId = pathSegment.toIntOrNull() ?: -1
                }
            }
        }

        // guard if id is invalid
        if (familyId == -1) {
            finish()
            return
        }

        // Setup UI Compose
        setContent {
            Nimons360Theme {
                FamilyDetailScreen(
                    viewModel = hiltViewModel(),
                    familyId = familyId,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_FAMILY_ID = "extra_family_id"
    }
}