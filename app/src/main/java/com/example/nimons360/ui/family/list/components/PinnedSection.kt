package com.example.nimons360.ui.family.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.family.list.FamilyModel
import com.example.nimons360.ui.theme.Nimons360Theme

@Composable
fun PinnedSection(
    title: String,
    families: List<FamilyModel>,
    onPinClick: (String) -> Unit
) {
    if (families.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Hindari bayangan jika desain flat
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                families.forEachIndexed { index, family ->
                    FamilyItem(
                        name = family.name,
                        isPinned = family.isPinned,
                        iconBgColor = family.bgColor,
                        onPinClick = { onPinClick(family.id) }
                    )

                    // Pembatas antar item
                    if (index < families.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .padding(horizontal = 16.dp)
                                .background(Color(0xFFF0F0F0))
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
fun PinnedSectionPreview() {
    Nimons360Theme {
        PinnedSection(
            title = "PINNED",
            families = listOf(
                FamilyModel("1", "Maulana Family", true, Color(0xFFFFEBEE)),
                FamilyModel("2", "Study Group", true, Color(0xFFE3F2FD))
            ),
            onPinClick = {}
        )
    }
}