package com.example.nimons360.ui.family.list.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.family.list.FamilyModel
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Nimons360Theme

@Composable
fun PinnedSection(
    title: String,
    families: List<FamilyModel>,
    onPinClick: (Int) -> Unit,
    onFamilyClick: (Int) -> Unit
) {
    // Jika tidak ada Pinned
    if (families.isEmpty()) {
        return // sembunyiin
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Grey600,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            families.forEach { family ->
                FamilyItem(
                    name = family.name,
                    isPinned = family.isPinned,
                    iconUrl = family.iconUrl,
                    onPinClick = { onPinClick(family.id) },
                    onItemClick = { onFamilyClick(family.id) }
                )
            }
        }
    }
}
