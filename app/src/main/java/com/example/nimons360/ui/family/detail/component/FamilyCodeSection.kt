package com.example.nimons360.ui.family.detail.component

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.theme.Blue100
import com.example.nimons360.ui.theme.Nimons360Theme

@Composable
fun FamilyCodeSection(code: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Card Family Code
    Card(
        colors = CardDefaults.cardColors(containerColor = Blue100),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Teks Informasi Kodenya
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FAMILY CODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = code,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 5.sp
                )
                Text(
                    text = "Share this code so others can join",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Button Copy
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(code))
                Toast.makeText(
                    context,
                    "Kode berhasil disalin",
                    Toast.LENGTH_SHORT
                ).show()
            }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun FamilyCodeSectionPreview() {
    Nimons360Theme {
        FamilyCodeSection(code = "MFA287")
    }
}