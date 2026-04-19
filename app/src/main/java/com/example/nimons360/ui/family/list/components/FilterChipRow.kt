package com.example.nimons360.ui.family.list.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Grey300
import com.example.nimons360.ui.theme.Grey900
import com.example.nimons360.ui.theme.Nimons360Theme
import com.example.nimons360.ui.theme.White

@Composable
fun FilterChipRow(
    selectedFilter: String,
    onFilterSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
private fun CustomFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Blue600 else White
    val textColor = if (isSelected) White else Grey900
    val borderStroke = if (!isSelected) BorderStroke(1.dp, Grey300) else null

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = borderStroke,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
        )
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun FilterChipRowPreview() {
    Nimons360Theme {
        FilterChipRow(selectedFilter = "All") {}
    }
}