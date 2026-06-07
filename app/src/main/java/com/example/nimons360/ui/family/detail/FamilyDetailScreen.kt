package com.example.nimons360.ui.family.detail

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nimons360.data.remote.dto.common.FamilyDetailResponseMembersInner
import com.example.nimons360.ui.family.detail.component.*
import com.example.nimons360.ui.theme.Blue100
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Grey100
import com.example.nimons360.ui.theme.Grey50
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Grey900
import com.example.nimons360.ui.theme.Orange600
import com.example.nimons360.ui.theme.Orange50
import com.example.nimons360.ui.theme.Red600
import com.example.nimons360.ui.theme.White
import com.example.nimons360.utils.Result

@Composable
fun FamilyDetailScreen(
    viewModel: FamilyDetailViewModel,
    familyId: Int,
    onBack: () -> Unit,
    onSendMessage: (familyId: Int, familyName: String) -> Unit = { _, _ -> },
    onSendGreeting: (familyId: Int, targetUserId: Int, targetName: String) -> Unit = { _, _, _ -> }
) {
    val detailState by viewModel.familyDetailState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(familyId) {
        viewModel.initFamilyId(familyId)
    }

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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (currentDetailState is Result.Error) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Error: ${currentDetailState.message}",
                color = MaterialTheme.colorScheme.error
            )
        }
    } else if (currentDetailState is Result.Success) {
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
            onSendMessage = { message -> viewModel.sendFamilyNotification(familyId, message) },
            onSendGreeting = { memberId, message -> viewModel.sendGreeting(familyId, memberId, message) },
            onShareFamilyLink = {
                val shareMessage = "Ayo bergabung dengan keluarga ${family.name} di Nimons360!\nKlik link berikut untuk bergabung:\n\nnimons360://family/${family.id}?code=${family.familyCode}"
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Family Link"))
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
    onSendMessage: (message: String) -> Unit = {},
    onSendGreeting: (memberId: Int, message: String) -> Unit = { _, _ -> },
    onShareFamilyLink: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showSendMessageSheet by remember { mutableStateOf(false) }
    var greetTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(containerColor = Grey50) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Top Bar
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

            if (isLandscape) {
                // LANDSCAPE: 2 kolom
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    // Kolom Kiri
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(end = 10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FamilyInfoCard(familyName, memberCount, iconUrl)
                        Spacer(modifier = Modifier.height(15.dp))

                        if (isJoined) {
                            FamilyCodeSection(code = familyCode)
                            Spacer(modifier = Modifier.height(15.dp))
                            Text(
                                text = "Actions",
                                color = Grey600,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            ActionItem(
                                icon = Icons.Default.Message,
                                text = "Send Message",
                                onClick = { showSendMessageSheet = true }
                            )
                            ActionItem(
                                icon = Icons.Default.Share,
                                text = "Share Family Link",
                                onClick = onShareFamilyLink
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showLeaveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = White),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 15.dp)
                            ) {
                                Text("Leave Family")
                            }
                        } else {
                            JoinPromptSection()
                            Button(
                                onClick = { showJoinDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600, contentColor = White),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp).height(52.dp)
                            ) {
                                Text("Join Family")
                            }
                        }
                    }

                    // Kolom Kanan: Members
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .padding(start = 10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Members",
                            color = Grey600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val avaColors = listOf(
                            Color(0xFF4CAF50), Color(0xFF2196F3),
                            Color(0xFFE91E63), Color(0xFFFF9800)
                        )
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                    }
                }
            } else {
                // PORTRAIT: 1 kolom
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FamilyInfoCard(familyName, memberCount, iconUrl)
                    Spacer(modifier = Modifier.height(20.dp))

                    if (isJoined) {
                        FamilyCodeSection(code = familyCode)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    val avaColors = listOf(
                        Color(0xFF4CAF50), Color(0xFF2196F3),
                        Color(0xFFE91E63), Color(0xFFFF9800)
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(members.withIndex().toList()) { (index, member) ->
                            val name = member.fullName ?: "Unknown"
                            val email = member.email ?: "**********"
                            val memberId = member.id
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
                                isYou = (email == currentUserEmail),
                                showGreet = isJoined && email != currentUserEmail && memberId != null,
                                profileImageUrl = member.profileImageUrl,
                                onGreet = {
                                    if (memberId != null) {
                                        greetTarget = Pair(memberId, name)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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

                    if (!isJoined) {
                        JoinPromptSection()
                        Button(
                            onClick = { showJoinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600, contentColor = White),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = White),
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
    }

    // Dialogs & Bottom Sheets (harus di luar Scaffold)
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
        familyName = familyName,
        onDismiss = { showSendMessageSheet = false },
        onSend = { message -> onSendMessage(message) }
    )

    SendGreetingBottomSheet(
        showSheet = greetTarget != null,
        targetName = greetTarget?.second ?: "",
        onDismiss = { greetTarget = null },
        onSend = { message ->
            greetTarget?.let { (memberId, _) ->
                onSendGreeting(memberId, message)
            }
        }
    )
}

@Composable
private fun FamilyInfoCard(familyName: String, memberCount: Int, iconUrl: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
}

@Composable
private fun JoinPromptSection() {
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
                fontSize = 14.sp
            )
        }
    }
}
