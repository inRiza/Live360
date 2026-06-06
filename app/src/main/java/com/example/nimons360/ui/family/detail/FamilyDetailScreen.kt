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
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
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
import android.content.Intent
import coil.compose.AsyncImage
import com.example.nimons360.data.remote.dto.common.FamilyDetailResponseMembersInner
import com.example.nimons360.ui.family.detail.component.*
import com.example.nimons360.ui.theme.Blue100
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Grey100
import com.example.nimons360.ui.theme.Grey50
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Grey900
import com.example.nimons360.ui.theme.Nimons360Theme
import com.example.nimons360.ui.theme.Orange600
import com.example.nimons360.ui.theme.Orange50
import com.example.nimons360.ui.theme.Red600
import com.example.nimons360.ui.theme.White
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
            iconUrl = family.iconUrl ?: "",
            isJoined = family.isMember ?: false,
            familyCode = family.familyCode ?: "",
            members = family.members ?: emptyList(),
            currentUserEmail = currentUserEmail,
            onBack = onBack,
            onJoinFamily = { code -> viewModel.joinFamily(code) },
            onLeaveFamily = { viewModel.leaveFamily() },
            onShareFamilyLink = {
                // Android Share Sheet
                val shareMessage = "Ayo bergabung dengan keluarga ${family.name} di Nimons360!\nKlik link berikut untuk bergabung:\n\nnimons360://family/${family.id}?code=${family.familyCode}"

                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                    type = "text/plain"
                }

                val shareIntent = Intent.createChooser(sendIntent, "Share Family Link")
                context.startActivity(shareIntent)
            }
        )
    }
}

@Composable
fun FamilyDetailContent(
    familyName: String,
    memberCount: Int,
    iconUrl: String,
    isJoined: Boolean,
    familyCode: String,
    members: List<FamilyDetailResponseMembersInner>,
    currentUserEmail: String?,
    onBack: () -> Unit,
    onJoinFamily: (String) -> Unit,
    onLeaveFamily: () -> Unit,
    onShareFamilyLink: () -> Unit
) {
    // State Dialog & Bottom Sheet
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showSendMessageSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Grey50
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
                    color = Grey900,
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
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Menampilkan Icon Keluarga menggunakan Coil
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Blue100),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = iconUrl,
                                contentDescription = "Family Icon",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Grey100),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(15.dp))

                        Column {
                            Text(
                                text = familyName,
                                color = Grey900,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "$memberCount members",
                                color = Grey600,
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
                    text = "Members",
                    color = Grey600,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Daftar Anggota
                LazyColumn(modifier = Modifier.weight(1f)) {
                    val avaColors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFFFF9800))
                    items(members.withIndex().toList()) { (index, member) ->
                        val name = member.fullName ?: "Unknown"
                        val email = member.email ?: "**********"

                        MemberItem(
                            name = name,
                            email = email,
                            initial = name.trim().split(" ")
                                .filter { it.isNotEmpty() }
                                .map { it[0].uppercaseChar() }
                                .take(2)
                                .joinToString(""),
                            avatarColor = avaColors[index % avaColors.size],
                            isBlurred = !isJoined,
                            isYou = (email == currentUserEmail)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Items
                if (isJoined) {
                    ActionItem(
                        icon = Icons.Default.Message,
                        text = "Send Message",
                        onClick = { showSendMessageSheet = true }
                    )
                }

                ActionItem(
                    icon = Icons.Default.Share,
                    text = "Share Family Link",
                    onClick = onShareFamilyLink
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Button Join/Leave
                if (!isJoined) {
                    Surface(
                        color = Orange50,
                        shape = RoundedCornerShape(14.dp),
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue600,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp)
                            .height(52.dp)
                    ) {
                        Text("Join Family")
                    }
                } else {
                    Button(
                        onClick = { showLeaveDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red600,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(bottom = 15.dp)
                    ) {
                        Text("Leave Family")
                    }
                }
            }
        }
    }

    // Komponen Dialog & Bottom Sheet
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

    SendMessageBottomSheet(
        showSheet = showSendMessageSheet,
        onDismiss = { showSendMessageSheet = false },
        onSend = { message ->
            // TODO: Implementasi kirim pesan
        }
    )
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
            onLeaveFamily = {},
            onShareFamilyLink = {}
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
            onLeaveFamily = {},
            onShareFamilyLink = {}
        )
    }
}