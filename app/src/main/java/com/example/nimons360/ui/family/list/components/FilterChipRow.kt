package com.example.nimons360.ui.family.list.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.theme.Nimons360Theme

@Composable
fun FilterChipRow(
    selectedFilter: String,
    onFilterSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CustomFilterChip(
            text = "All",
            isSelected = selectedFilter == "All",
            onClick = { onFilterSelect("All") }
        )
        CustomFilterChip(
            text = "My Families",
            isSelected = selectedFilter == "My Families",
            onClick = { onFilterSelect("My Families") }
        )
    }
}

@Composable
private fun CustomFilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
        border = if (!isSelected) BorderStroke(1.dp, Color.LightGray) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FilterChipRowPreview() {
    Nimons360Theme {
        FilterChipRow(selectedFilter = "All") {}
    }
}