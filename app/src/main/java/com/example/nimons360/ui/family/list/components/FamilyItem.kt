package com.example.nimons360.ui.family.list.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nimons360.ui.theme.Blue100
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Grey100
import com.example.nimons360.ui.theme.Grey300
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Grey900
import com.example.nimons360.ui.theme.Nimons360Theme
import com.example.nimons360.ui.theme.White

@Composable
fun FamilyItem(
    name: String,
    isPinned: Boolean,
    iconUrl: String,
    onPinClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        onClick = onItemClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isPinned) Blue100 else Grey100),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = "Family Icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Grey900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isPinned) "Pinned family" else "Family",
                    fontSize = 12.sp,
                    color = Grey600
                )
            }

            IconButton(onClick = onPinClick) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin Family",
                    tint = if (isPinned) Blue600 else Grey300,
                    modifier = Modifier.rotate(45f)
                )
            }
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
