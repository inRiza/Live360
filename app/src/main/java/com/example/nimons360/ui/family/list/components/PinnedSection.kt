package com.example.nimons360.ui.family.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.family.list.FamilyModel
import com.example.nimons360.ui.theme.Grey100
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Nimons360Theme
import com.example.nimons360.ui.theme.White

@Composable
fun PinnedSection(
    title: String,
    families: List<FamilyModel>,
    onPinClick: (String) -> Unit,
    onFamilyClick: (String) -> Unit
) {
    // Jika tidak ada Pinned
    if (families.isEmpty()) {
        return // sembunyiin
    }

    // Blok Konten List
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {

        // Title
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Grey600,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Card Item Keluarga
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                for (index in families.indices) {
                    val family = families[index]

                    FamilyItem(
                        name = family.name,
                        isPinned = family.isPinned,
                        iconUrl = family.iconUrl,
                        onPinClick = { onPinClick(family.id) },
                        onItemClick = { onFamilyClick(family.id) }
                    )

                    val isNotLastItem = index < families.size - 1
                    if (isNotLastItem) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .padding(horizontal = 15.dp)
                                .background(Grey100)
                        )
                    }
                }
            }
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun PinnedSectionPreview() {
    Nimons360Theme {
        PinnedSection(
            title = "PINNED",
            families = listOf(
                FamilyModel("1", "Keluarga Cemara", true, ""),
                FamilyModel("2", "Weekend Gang", true, "")
            ),
            onPinClick = {},
            onFamilyClick = {}
        )
    }
}