package com.example.nimons360.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nimons360.data.local.preference.PinInfo
import com.example.nimons360.ui.profile.CustomizePinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizePinScreen(
    viewModel: CustomizePinViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Pin", fontWeight = FontWeight.Bold, color = Color(0xFF0B3D91)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF0B3D91)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = Color(0xFFF6F5F8)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            // Active assignment card
            item {
                ActiveAssignmentCard(
                    activeBiasaPin = uiState.pins.firstOrNull { it.id == uiState.activeBiasaPinId },
                    activeFavoritePin = uiState.pins.firstOrNull { it.id == uiState.activeFavoritePinId },
                    onResetBiasa = viewModel::resetBiasa,
                    onResetFavorite = viewModel::resetFavorite
                )
            }

            // Section Header
            item {
                Text(
                    text = "Daftar Pin Tersedia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Pins list
            items(uiState.pins, key = { it.id }) { pin ->
                PinItemCard(
                    pin = pin,
                    activeBiasaPinId = uiState.activeBiasaPinId,
                    activeFavoritePinId = uiState.activeFavoritePinId,
                    onDownload = { viewModel.downloadPin(pin) },
                    onSetBiasa = { viewModel.selectPinForBiasa(pin) },
                    onSetFavorite = { viewModel.selectPinForFavorite(pin) }
                )
            }
        }
    }
}

@Composable
fun ActiveAssignmentCard(
    activeBiasaPin: PinInfo?,
    activeFavoritePin: PinInfo?,
    onResetBiasa: () -> Unit,
    onResetFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pin Aktif Saat Ini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B3D91)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Pin Biasa
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeBiasaPin?.localPath != null) {
                            AsyncImage(
                                model = File(activeBiasaPin.localPath),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Default circle placeholder
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1976D2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("U", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Pin Biasa (User/Member)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (activeBiasaPin != null) activeBiasaPin.name else "Default (Inisial)",
                            color = Color(0xFF757575),
                            fontSize = 12.sp
                        )
                    }
                }
                if (activeBiasaPin != null) {
                    IconButton(onClick = onResetBiasa) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Favorite Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeFavoritePin?.localPath != null) {
                            AsyncImage(
                                model = File(activeFavoritePin.localPath),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("♥", color = Color(0xFFD32F2F), fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Favorite Location", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (activeFavoritePin != null) activeFavoritePin.name else "Default (Hati Merah)",
                            color = Color(0xFF757575),
                            fontSize = 12.sp
                        )
                    }
                }
                if (activeFavoritePin != null) {
                    IconButton(onClick = onResetFavorite) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinItemCard(
    pin: PinInfo,
    activeBiasaPinId: String?,
    activeFavoritePinId: String?,
    onDownload: () -> Unit,
    onSetBiasa: () -> Unit,
    onSetFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview Image
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = if (pin.isDownloaded && pin.localPath != null) File(pin.localPath) else pin.url,
                    contentDescription = pin.name,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = pin.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF212121))
                Spacer(modifier = Modifier.height(4.dp))

                if (pin.isDownloading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { pin.downloadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF1976D2),
                            trackColor = Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mengunduh... ${pin.downloadProgress}%",
                            fontSize = 11.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                } else if (pin.isDownloaded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Set Biasa
                        val isBiasaActive = activeBiasaPinId == pin.id
                        Button(
                            onClick = onSetBiasa,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBiasaActive) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                contentColor = if (isBiasaActive) Color(0xFF2E7D32) else Color(0xFF1976D2)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isBiasaActive) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Biasa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Set Favorite
                        val isFavActive = activeFavoritePinId == pin.id
                        Button(
                            onClick = onSetFavorite,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFavActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                contentColor = if (isFavActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isFavActive) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Favorite", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(text = "Belum Diunduh", fontSize = 12.sp, color = Color(0xFF757575))
                }
            }

            if (!pin.isDownloaded && !pin.isDownloading) {
                IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Unduh",
                        tint = Color(0xFF1976D2)
                    )
                }
            }
        }
    }
}
