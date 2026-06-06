package com.example.nimons360.ui.map.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.nimons360.data.local.db.dao.MarkedLocationWithPhotos
import com.example.nimons360.ui.map.MapViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ──────────────────────────────────────────────────────────────────────────────
// Portrait wrapper — tampil sebagai ModalBottomSheet
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MarkedLocationDetailBottomSheet(
    item: MarkedLocationWithPhotos,
    viewModel: MapViewModel,
    onDismiss: () -> Unit
) {
    // Delete confirmation dialogs perlu state lokal di luar Modal supaya tetap muncul
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Lokasi") },
            text = { Text("Yakin ingin menghapus \"${item.location.name}\"? Semua foto akan ikut terhapus.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMarkedLocation(item.location.id)
                        showDeleteDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Hapus", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF8F9FA),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 48.dp, height = 5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF9E9E9E).copy(alpha = 0.35f))
            )
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            MarkedLocationDetailContent(
                item = item,
                viewModel = viewModel,
                onDismiss = onDismiss,
                onRequestDelete = { showDeleteDialog = true },
                showTitleInContent = true
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Shared detail content — digunakan oleh BottomSheet (portrait) dan SideCard (landscape)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Konten detail lokasi yang bisa dipakai oleh
 * [MarkedLocationDetailBottomSheet] (portrait) maupun [LandscapeSideCard] (landscape).
 *
 * @param showTitleInContent jika true, judul & action icons ditampilkan di dalam konten.
 *   Set false jika SideCard sudah punya header dengan nama lokasi.
 * @param onRequestDelete callback untuk memunculkan konfirmasi hapus dari parent.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarkedLocationDetailContent(
    item: MarkedLocationWithPhotos,
    viewModel: MapViewModel,
    onDismiss: () -> Unit,
    onRequestDelete: () -> Unit = {},
    showTitleInContent: Boolean = true
) {
    val context = LocalContext.current
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val location = item.location
    val photos = item.photos

    // ── Header row (portrait: dalam konten; landscape: di SideCard header) ──
    if (showTitleInContent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B3D91),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(Date(location.createdAt))
                Text(
                    text = "Ditambahkan $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575)
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.openEditMarkedLocation(item) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0B3D91))
                }
                IconButton(onClick = onRequestDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFD32F2F))
                }
            }
        }
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(Modifier.height(12.dp))
    } else {
        // Landscape: tampilkan action icons (edit/delete) kompak di atas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(location.createdAt))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.openEditMarkedLocation(item) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0B3D91), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRequestDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
            }
        }
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(Modifier.height(8.dp))
    }

    // ── Deskripsi ────────────────────────────────────────────────────────────
    if (location.description.isNotBlank()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE8EAF6),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = location.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF263238),
                modifier = Modifier.padding(14.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    // ── Koordinat ────────────────────────────────────────────────────────────
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0B3D91), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Lat: %.6f".format(location.latitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF424242)
                )
                Text(
                    text = "Lng: %.6f".format(location.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF424242)
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // ── Foto ─────────────────────────────────────────────────────────────────
    if (photos.isNotEmpty()) {
        Text(
            "Foto (${photos.size})",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF424242),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos.indices.toList()) { idx ->
                val photo = photos[idx]
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = "Foto lokasi",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
                        .clickable { selectedPhotoIndex = idx }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // ── Navigasi ─────────────────────────────────────────────────────────────
    Button(
        onClick = {
            val gmmIntentUri = Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=${location.latitude},${location.longitude}")
                )
                context.startActivity(browserIntent)
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B3D91))
    ) {
        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Navigasi di Google Maps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }

    Spacer(Modifier.height(10.dp))

    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text("Tutup", color = Color(0xFF757575))
    }

    // ── Photo fullscreen viewer ───────────────────────────────────────────────
    selectedPhotoIndex?.let { startIdx ->
        PhotoViewerDialog(
            photos = photos.map { it.filePath },
            initialIndex = startIdx,
            onDismiss = { selectedPhotoIndex = null }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Fullscreen photo viewer with HorizontalPager swipe
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoViewerDialog(
    photos: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 8.dp
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(photos[page]),
                        contentDescription = "Foto ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Close button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.size(40.dp).clickable { onDismiss() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Page indicator
            if (photos.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .navigationBarsPadding()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${photos.size}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
