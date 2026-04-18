package com.example.nimons360.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nimons360.ui.map.MemberMapUi
import java.util.Locale

@Composable
fun UserMarkerOverlay(
	modifier: Modifier = Modifier,
	searchQuery: String,
	isConnected: Boolean,
	nearbyCount: Int,
	nearbyRadiusMeters: Double,
	favoritesCount: Int,
	isFavoritesPanelVisible: Boolean,
	currentUser: MemberMapUi?,
	onSearchQueryChanged: (String) -> Unit,
	onFavoritesChipClick: () -> Unit
) {
	Box(modifier = modifier) {
		Column(modifier = Modifier.fillMaxWidth()) {
			OutlinedTextField(
				value = searchQuery,
				onValueChange = onSearchQueryChanged,
				modifier = Modifier.fillMaxWidth(),
				shape = RoundedCornerShape(24.dp),
				singleLine = true,
				placeholder = {
					Text("Cari lokasi atau anggota...")
				},
				colors = TextFieldDefaults.colors(
					focusedContainerColor = Color.White.copy(alpha = 0.95f),
					unfocusedContainerColor = Color.White.copy(alpha = 0.92f)
				)
			)

			Spacer(modifier = Modifier.height(8.dp))

			LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				val chips = listOf("Family", "Favorit")
				items(chips) { chip ->
					val selected =
						(chip == "Family" && !isFavoritesPanelVisible) ||
						(chip == "Favorit" && isFavoritesPanelVisible)
					val chipText = if (chip == "Favorit") "Favorit ($favoritesCount)" else chip
					Surface(
						shape = RoundedCornerShape(10.dp),
						color = if (selected) Color(0xFF1565C0) else Color(0xFFEAEAEA),
						modifier = Modifier.clickable {
							if (chip == "Favorit" || chip == "Family") {
								onFavoritesChipClick()
							}
						}
					) {
						Text(
							text = chipText,
							color = if (selected) Color.White else Color(0xFF1C1C1C),
							modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(8.dp))
			Text(
				text = if (isConnected) "Terhubung langsung" else "Menyambungkan ulang...",
				color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
				fontWeight = FontWeight.SemiBold
			)
			Spacer(modifier = Modifier.height(2.dp))
			Text(
				text = "Orang terdekat: $nearbyCount dalam ${(nearbyRadiusMeters / 1000.0).toInt()} km",
				color = Color(0xFF455A64),
				fontWeight = FontWeight.Medium
			)
		}

		currentUser?.let { user ->
			Surface(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.BottomCenter),
				shape = RoundedCornerShape(18.dp),
				color = Color.White.copy(alpha = 0.94f)
			) {
				Column(modifier = Modifier.padding(14.dp)) {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							text = user.fullName
								.trim()
								.split(" ")
								.filter { it.isNotBlank() }
								.take(2)
								.joinToString("") { it.first().uppercase() }
								.ifBlank { "?" },
							modifier = Modifier
								.clip(CircleShape)
								.background(Color(0xFF1E88E5))
								.padding(horizontal = 13.dp, vertical = 10.dp),
							color = Color.White,
							fontWeight = FontWeight.Bold
						)
						Spacer(modifier = Modifier.width(10.dp))
						Column {
							Text(text = user.fullName, fontWeight = FontWeight.SemiBold)
							Text(text = user.email, color = Color(0xFF525252))
						}
					}

					Spacer(modifier = Modifier.height(10.dp))

					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						MiniMetric(
							modifier = Modifier.weight(1f),
							value = "${user.batteryLevel}%",
							label = "Baterai"
						)
						MiniMetric(
							modifier = Modifier.weight(1f),
							value = String.format(Locale.US, "%.3f, %.3f", user.latitude, user.longitude),
							label = "Lokasi"
						)
						MiniMetric(
							modifier = Modifier.weight(1f),
							value = user.internetStatus,
							label = "Internet"
						)
					}
				}
			}
		}
	}
}

@Composable
private fun MiniMetric(
	modifier: Modifier = Modifier,
	value: String,
	label: String
) {
	Column(
		modifier = modifier
			.clip(RoundedCornerShape(12.dp))
			.background(Color(0xFFF1F1F1))
			.padding(10.dp)
	) {
		Text(text = value, fontWeight = FontWeight.SemiBold)
		Spacer(modifier = Modifier.height(2.dp))
		Text(text = label, color = Color(0xFF606060))
	}
}
