package com.example.nimons360.ui.family.detail

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.data.remote.dto.common.FamilyDetailResponseMembersInner
import com.example.nimons360.ui.family.detail.component.*
import com.example.nimons360.ui.theme.Amber50
import com.example.nimons360.ui.theme.Nimons360Theme
import com.example.nimons360.ui.theme.Orange600
import com.example.nimons360.utils.Result

@Composable
fun FamilyDetailScreen(
    viewModel: FamilyDetailViewModel,
    familyId: Int,
    onBack: () -> Unit
) {
    val detailState by viewModel.familyDetailState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val context = LocalContext.current

    // Inisialisasi data berdasarkan ID dari Activity
    LaunchedEffect(familyId) {
        viewModel.initFamilyId(familyId)
    }

    // Tampilkan Toast jika aksi Join/Leave selesai
    LaunchedEffect(actionState) {
        when (actionState) {
            is Result.Success -> {
                Toast.makeText(context, (actionState as Result.Success).data, Toast.LENGTH_SHORT).show()
                viewModel.clearActionState()
            }
            is Result.Error -> {
                Toast.makeText(context, (actionState as Result.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.clearActionState()
            }
            else -> {}
        }
    }

    // Tampilan berdasarkan State Utama
    when (val state = detailState) {
        is Result.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is Result.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        is Result.Success -> {
            val family = state.data
            FamilyDetailContent(
                familyName = family.name ?: "Unknown Family",
                memberCount = family.members?.size ?: 0,
                isJoined = family.isMember ?: false,
                familyCode = family.familyCode ?: "",
                members = family.members ?: emptyList(),
                onBack = onBack,
                onJoinFamily = { code -> viewModel.joinFamily(code) },
                onLeaveFamily = { viewModel.leaveFamily() }
            )
        }
    }
}

@Composable
fun FamilyDetailContent(
    familyName: String,
    memberCount: Int,
    isJoined: Boolean,
    familyCode: String,
    members: List<FamilyDetailResponseMembersInner>,
    onBack: () -> Unit,
    onJoinFamily: (String) -> Unit,
    onLeaveFamily: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Custom Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Text(familyName, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
            }

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header Card
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text(familyName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("$memberCount members", color = Color.White.copy(0.8f), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isJoined) {
                    FamilyCodeSection(code = familyCode)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text("MEMBERS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Daftar Anggota
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(members) { member ->
                        val name = member.fullName ?: "Unknown"
                        val email = member.email ?: "**********"
                        MemberItem(
                            name = name,
                            email = email,
                            initial = name.take(1).uppercase(),
                            avatarColor = MaterialTheme.colorScheme.secondary,
                            isBlurred = !isJoined,
                            isYou = false // TODO: update logika 'isYou' kalau sudah menyimpan ID User
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
                    Button(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(50.dp)
                    ) {
                        Text("Join Family")
                    }
                } else {
                    TextButton(
                        onClick = { showLeaveDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text("Leave Family", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showJoinDialog) JoinFamilyDialog(onDismiss = { showJoinDialog = false }, onJoin = { onJoinFamily(it); showJoinDialog = false })
    if (showLeaveDialog) LeaveFamilyDialog(familyName, onDismiss = { showLeaveDialog = false }, onConfirm = { onLeaveFamily(); showLeaveDialog = false })
}


// Preview
//@Preview(showBackground = true, name = "1. Belum Bergabung")
//@Composable
//fun FamilyDetailNotJoinedPreview() {
//    Nimons360Theme {
//        FamilyDetailContent(
//            isJoined = false, // Simulasi belum join
//            onBack = {}, onJoinFamily = {}, onLeaveFamily = {}
//        )
//    }
//}
//
//@Preview(showBackground = true, name = "2. Sudah Bergabung")
//@Composable
//fun FamilyDetailJoinedPreview() {
//    Nimons360Theme {
//        FamilyDetailContent(
//            isJoined = true, // Simulasi sudah join
//            onBack = {}, onJoinFamily = {}, onLeaveFamily = {}
//        )
//    }
//}