package com.example.nimons360.ui.family.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nimons360.ui.theme.Grey300
import com.example.nimons360.ui.theme.Grey50
import com.example.nimons360.ui.theme.Nimons360Theme

@Composable
fun FamilyItem(
    name: String,
    isPinned: Boolean,
    iconUrl: String,
    onPinClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Keluarga
        AsyncImage(
            model = iconUrl,
            contentDescription = "Family Icon",
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Grey50),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(15.dp))

        // Teks Nama Keluarga
        Text(
            text = name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )

        // Tombol Pin
        IconButton(onClick = onPinClick) {
            Icon(
                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Pin Family",
                tint = if (isPinned) MaterialTheme.colorScheme.primary else Grey300,
                modifier = Modifier.rotate(45f)
            )
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun FamilyItemPreview() {
    Nimons360Theme {
        FamilyItem(
            name = "Keluarga Cemara",
            isPinned = true,
            iconUrl = "",
            onPinClick = {},
            onItemClick = {}
        )
    }
}