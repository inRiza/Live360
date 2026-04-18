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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    // State Data
    val detailState by viewModel.familyDetailState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val context = LocalContext.current

    // Inisialisasi Data
    LaunchedEffect(familyId) {
        viewModel.initFamilyId(familyId)
    }

    // Toast Join/Leave
    LaunchedEffect(actionState) {
        val currentState = actionState

        if (currentState is Result.Success) {
            Toast.makeText(context, currentState.data, Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
        } else if (currentState is Result.Error) {
            Toast.makeText(context, currentState.message, Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
        }
    }

    val currentDetailState = detailState

    if (currentDetailState is Result.Loading) {
        // Loading
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (currentDetailState is Result.Error) {
        // Error
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Error: ${currentDetailState.message}",
                color = MaterialTheme.colorScheme.error
            )
        }
    } else if (currentDetailState is Result.Success) {
        // Success
        val family = currentDetailState.data

        FamilyDetailContent(
            familyName = family.name ?: "Unknown Family",
            memberCount = family.members?.size ?: 0,
            iconUrl = family.iconUrl ?: "", // Meneruskan iconUrl dari API
            isJoined = family.isMember ?: false,
            familyCode = family.familyCode ?: "",
            members = family.members ?: emptyList(),
            currentUserEmail = currentUserEmail,
            onBack = onBack,
            onJoinFamily = { code -> viewModel.joinFamily(code) },
            onLeaveFamily = { viewModel.leaveFamily() }
        )
    }
}

@Composable
fun FamilyDetailContent(
    familyName: String,
    memberCount: Int,
    iconUrl: String, // Tambahan parameter iconUrl
    isJoined: Boolean,
    familyCode: String,
    members: List<FamilyDetailResponseMembersInner>,
    currentUserEmail: String?,
    onBack: () -> Unit,
    onJoinFamily: (String) -> Unit,
    onLeaveFamily: () -> Unit
) {
    // State Dialog
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // Bagian Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = familyName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Card Informasi Keluarga
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(15.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Menampilkan Icon Keluarga menggunakan Coil
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = "Family Icon",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(15.dp))

                        Column {
                            Text(
                                text = familyName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "$memberCount members",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Kode Join Keluarga
                if (isJoined) {
                    FamilyCodeSection(code = familyCode)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text(
                    text = "MEMBERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                            isYou = (email == currentUserEmail)
                        )
                    }
                }

                // Button Join/Leave
                if (!isJoined) {
                    Surface(
                        color = Amber50,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Orange600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Join this family to see member details and map interaction.",
                                fontSize = 15.sp
                            )
                        }
                    }
                    Button(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp)
                            .height(50.dp)
                    ) {
                        Text("Join Family")
                    }
                } else {
                    TextButton(
                        onClick = { showLeaveDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 15.dp)
                    ) {
                        Text("Leave Family", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Komponen Dialog
    if (showJoinDialog) {
        JoinFamilyDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = {
                onJoinFamily(it)
                showJoinDialog = false
            }
        )
    }

    if (showLeaveDialog) {
        LeaveFamilyDialog(
            familyName = familyName,
            onDismiss = { showLeaveDialog = false },
            onConfirm = {
                onLeaveFamily()
                showLeaveDialog = false
            }
        )
    }
}

// Preview Layar
@Preview(showBackground = true, name = "1. Belum Bergabung")
@Composable
fun FamilyDetailNotJoinedPreview() {
    Nimons360Theme {
        FamilyDetailContent(
            familyName = "Keluarga Cemara",
            memberCount = 4,
            iconUrl = "",
            isJoined = false,
            familyCode = "XXXXXX",
            members = emptyList(),
            currentUserEmail = null,
            onBack = {},
            onJoinFamily = {},
            onLeaveFamily = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Sudah Bergabung")
@Composable
fun FamilyDetailJoinedPreview() {
    Nimons360Theme {
        FamilyDetailContent(
            familyName = "Keluarga Cemara",
            memberCount = 4,
            iconUrl = "",
            isJoined = true,
            familyCode = "MFA287",
            members = emptyList(),
            currentUserEmail = null,
            onBack = {},
            onJoinFamily = {},
            onLeaveFamily = {}
        )
    }
}