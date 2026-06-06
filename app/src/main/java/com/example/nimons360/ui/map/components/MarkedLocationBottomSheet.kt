package com.example.nimons360.ui.map.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.nimons360.ui.map.AddEditMarkedLocationState
import com.example.nimons360.ui.map.MapViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkedLocationBottomSheet(
    state: AddEditMarkedLocationState,
    viewModel: MapViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = state.id != null
    var latText by remember(state.latitude) { mutableStateOf(String.format(Locale.US, "%.6f", state.latitude)) }
    var lngText by remember(state.longitude) { mutableStateOf(String.format(Locale.US, "%.6f", state.longitude)) }

    // Camera: prepare temp file URI + absolute path
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageAbsolutePath by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageAbsolutePath?.let { path ->
                viewModel.addNewPhotosToAddEdit(listOf(path))
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val paths = uris.mapNotNull { uri -> copyUriToTemp(context, uri) }
        if (paths.isNotEmpty()) viewModel.addNewPhotosToAddEdit(paths)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val (uri, absPath) = createTempImageFile(context)
            cameraImageUri = uri
            cameraImageAbsolutePath = absPath
            cameraLauncher.launch(uri)
        }
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
            // Header
            Text(
                text = if (isEditing) "Edit Lokasi" else "Tambah Lokasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B3D91),
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            // Name field
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateAddEditName,
                label = { Text("Nama Lokasi *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0B3D91),
                    focusedLabelColor = Color(0xFF0B3D91)
                )
            )

            Spacer(Modifier.height(12.dp))

            // Description field
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateAddEditDescription,
                label = { Text("Deskripsi *") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0B3D91),
                    focusedLabelColor = Color(0xFF0B3D91)
                )
            )

            Spacer(Modifier.height(12.dp))

            // Latitude / Longitude row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { v ->
                        latText = v
                        v.toDoubleOrNull()?.let { viewModel.updateAddEditLatitude(it) }
                    },
                    label = { Text("Latitude") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0B3D91),
                        focusedLabelColor = Color(0xFF0B3D91)
                    )
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { v ->
                        lngText = v
                        v.toDoubleOrNull()?.let { viewModel.updateAddEditLongitude(it) }
                    },
                    label = { Text("Longitude") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0B3D91),
                        focusedLabelColor = Color(0xFF0B3D91)
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Use current location button
            OutlinedButton(
                onClick = {
                    viewModel.useCurrentLocationForMarked()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0B3D91))
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gunakan Lokasi Saat Ini")
            }

            Spacer(Modifier.height(16.dp))

            // Photos section
            Text(
                "Foto Lokasi",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF424242),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            // Photo grid
            val allPhotoPaths = state.existingPhotoPaths + state.newPhotoPaths
            if (allPhotoPaths.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allPhotoPaths) { path ->
                        val isExisting = path in state.existingPhotoPaths
                        Box(modifier = Modifier.size(90.dp)) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "Foto lokasi",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
                            )
                            // Remove button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                                    .clickable {
                                        if (isExisting) viewModel.removeExistingPhotoFromAddEdit(path)
                                        else viewModel.removeNewPhotoFromAddEdit(path)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Add photo buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF424242))
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galeri", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = {
                        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            val (uri, absPath) = createTempImageFile(context)
                            cameraImageUri = uri
                            cameraImageAbsolutePath = absPath
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF424242))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kamera", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action buttons
            Button(
                onClick = { viewModel.saveMarkedLocation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B3D91))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simpan Lokasi", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal", color = Color(0xFF757575))
            }
        }
    }
}

/** Returns Pair(contentUri, absoluteFilePath) for camera capture */
private fun createTempImageFile(context: Context): Pair<Uri, String> {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
    val file = File(dir, "IMG_${timeStamp}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return Pair(uri, file.absolutePath)
}

private fun copyUriToTemp(context: Context, uri: Uri): String? {
    return runCatching {
        val dir = File(context.cacheDir, "gallery_photos").apply { mkdirs() }
        val file = File(dir, "IMG_${System.nanoTime()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }.getOrNull()
}
