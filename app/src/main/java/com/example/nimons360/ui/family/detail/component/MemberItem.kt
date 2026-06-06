package com.example.nimons360.ui.family.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.theme.Orange50
import com.example.nimons360.ui.theme.Orange800
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Nimons360Theme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Send

@Composable
fun MemberItem(
    name: String,
    email: String,
    initial: String,
    avatarColor: Color,
    isBlurred: Boolean = false,
    isYou: Boolean = false,
    showGreet: Boolean = false,
    onGreet: () -> Unit = {}
) {
    // Blurring State
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp)
        .then(if (isBlurred) Modifier.blur(10.dp) else Modifier)

    // Detail Anggota
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Foto Profil (pake inisial)
        Box(
            modifier = Modifier
                .size(45.dp)
                .background(avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Informasi Teks
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 15.dp)
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = email,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Label isYou
        if (isYou && !isBlurred) {
            Surface(color = Orange50, shape = CircleShape) {
                Text(
                    text = "You",
                    color = Orange800,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        if (showGreet) {
            IconButton(
                onClick = onGreet,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Greet",
                    tint = Blue600,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun MemberItemPreview() {
    Nimons360Theme {
        MemberItem(
            name = "Labpro ITB",
            email = "labpro@mail.com",
            initial = "L",
            avatarColor = MaterialTheme.colorScheme.primary,
            isBlurred = false,
            isYou = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MemberItemBlurredPreview() {
    Nimons360Theme {
        MemberItem(
            name = "Rafi Naufal",
            email = "rafi@mail.com",
            initial = "R",
            avatarColor = MaterialTheme.colorScheme.secondary,
            isBlurred = true,
            isYou = false
        )
    }
}