package com.example.nimons360.ui.family.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.theme.Nimons360Theme

@Composable
fun FamilyItem(
    name: String,
    isPinned: Boolean,
    iconBgColor: Color,
    onPinClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Tindakan saat item ditekan */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconBgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = Color.Gray)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        // Logika Perubahan Ikon Pin
        IconButton(onClick = onPinClick) {
            Icon(
                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Pin Family",
                tint = if (isPinned) MaterialTheme.colorScheme.primary else Color.LightGray,
                modifier = Modifier.rotate(45f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FamilyItemPinnedPreview() {
    Nimons360Theme {
        FamilyItem("Maulana Family", isPinned = true, iconBgColor = Color(0xFFFFEBEE)) {}
    }
}

@Preview(showBackground = true)
@Composable
fun FamilyItemUnpinnedPreview() {
    Nimons360Theme {
        FamilyItem("Camping Trip", isPinned = false, iconBgColor = Color(0xFFE8F5E9)) {}
    }
}