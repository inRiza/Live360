package com.example.nimons360.ui.family.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.family.detail.component.*
import com.example.nimons360.ui.theme.Amber50
import com.example.nimons360.ui.theme.Nimons360Theme
import com.example.nimons360.ui.theme.Orange600

@Composable
fun FamilyDetailScreen(viewModel: FamilyDetailViewModel, onBack: () -> Unit) {
    val isJoined by viewModel.isJoined.collectAsState()

    FamilyDetailContent(
        isJoined = isJoined,
        onBack = onBack,
        onJoinFamily = { code -> viewModel.joinFamily(code) },
        onLeaveFamily = { viewModel.leaveFamily() }
    )
}

@Composable
fun FamilyDetailContent(
    isJoined: Boolean,
    onBack: () -> Unit,
    onJoinFamily: (String) -> Unit,
    onLeaveFamily: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Text("Maulana Family", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
            }

            // Main Content
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Maulana Family", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("4 members · Created Mar 2024", color = Color.White.copy(0.8f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isJoined) {
                    FamilyCodeSection(code = "MFA287")
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text("MEMBERS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(listOf("Labpro ITB", "Rafi Naufal", "Anisa Putri", "Bagas Wibowo")) { name ->
                        MemberItem(
                            name = name, email = "${name.lowercase().replace(" ", "")}@mail.com",
                            initial = name.take(1), avatarColor = MaterialTheme.colorScheme.secondary,
                            isBlurred = !isJoined, isYou = name == "Labpro ITB"
                        )
                    }
                }

                if (!isJoined) {
                    Surface(color = Amber50, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.Info, null, tint = Orange600, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join this family to see member details and map interaction.", fontSize = 12.sp)
                        }
                    }
                    Button(onClick = { showJoinDialog = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(50.dp)) {
                        Text("Join Family")
                    }
                } else {
                    TextButton(onClick = { showLeaveDialog = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text("Leave Family", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showJoinDialog) JoinFamilyDialog(onDismiss = { showJoinDialog = false }, onJoin = { onJoinFamily(it); showJoinDialog = false })
    if (showLeaveDialog) LeaveFamilyDialog("Maulana Family", onDismiss = { showLeaveDialog = false }, onConfirm = { onLeaveFamily(); showLeaveDialog = false })
}


// Preview

@Preview(showBackground = true, name = "1. Belum Bergabung")
@Composable
fun FamilyDetailNotJoinedPreview() {
    Nimons360Theme {
        FamilyDetailContent(
            isJoined = false, // Simulasi belum join
            onBack = {}, onJoinFamily = {}, onLeaveFamily = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Sudah Bergabung")
@Composable
fun FamilyDetailJoinedPreview() {
    Nimons360Theme {
        FamilyDetailContent(
            isJoined = true, // Simulasi sudah join
            onBack = {}, onJoinFamily = {}, onLeaveFamily = {}
        )
    }
}