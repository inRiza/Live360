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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.family.list.FamilyModel

@Composable
fun PinnedSection(
    title: String,
    families: List<FamilyModel>,
    onPinClick: (String) -> Unit,
    onFamilyClick: (String) -> Unit
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
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                families.forEachIndexed { index, family ->
                    FamilyItem(
                        name = family.name,
                        isPinned = family.isPinned,
                        iconUrl = family.iconUrl,
                        onPinClick = { onPinClick(family.id) },
                        onItemClick = { onFamilyClick(family.id) }
                    )

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