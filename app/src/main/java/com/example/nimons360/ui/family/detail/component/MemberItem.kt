package com.example.nimons360.ui.family.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.theme.Orange50
import com.example.nimons360.ui.theme.Orange800

import androidx.compose.ui.tooling.preview.Preview
import com.example.nimons360.ui.theme.Nimons360Theme

@Composable
fun MemberItem(
    name: String,
    email: String,
    initial: String,
    avatarColor: Color,
    isBlurred: Boolean = false,
    isYou: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            // Memberikan efek blur jika user belum bergabung
            .then(if (isBlurred) Modifier.blur(10.dp) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (isYou && !isBlurred) {
            Surface(color = Orange50, shape = CircleShape) {
                Text(
                    "You", color = Orange800, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }
}


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