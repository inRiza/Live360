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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.nimons360.ui.map.FamilyFilterOption
import com.example.nimons360.ui.map.MemberMapUi
import java.util.Locale

@Composable
fun UserMarkerOverlay(
	modifier: Modifier = Modifier,
	familyOptions: List<FamilyFilterOption>,
	selectedFamilyId: Int?,
	searchQuery: String,
	isConnected: Boolean,
	nearbyCount: Int,
	nearbyRadiusMeters: Double,
	favoritesCount: Int,
	isFavoritesPanelVisible: Boolean,
	currentUser: MemberMapUi?,
	onFamilySelected: (Int?) -> Unit,
	onSearchQueryChanged: (String) -> Unit,
	onFavoritesChipClick: () -> Unit
) {
	Box(modifier = modifier) {
		Column(modifier = Modifier.fillMaxWidth()) {
			FamilySelectorDropdown(
				familyOptions = familyOptions,
				selectedFamilyId = selectedFamilyId,
				onFamilySelected = onFamilySelected
			)

			Spacer(modifier = Modifier.height(8.dp))

			TextField(
				value = searchQuery,
				onValueChange = onSearchQueryChanged,
				modifier = Modifier
					.fillMaxWidth()
					.clip(RoundedCornerShape(24.dp)),
				singleLine = true,
				placeholder = {
					Text("Cari lokasi atau anggota...")
				}
			)

			Spacer(modifier = Modifier.height(8.dp))

			LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				val chips = listOf("Family", "Favorit")
				items(chips) { chip ->
					val selected =
						(chip == "Family" && !isFavoritesPanelVisible) ||
						(chip == "Favorit" && isFavoritesPanelVisible)
					val chipText = if (chip == "Favorit") "Favorit ($favoritesCount)" else chip
					Box(
						modifier = Modifier
							.clip(RoundedCornerShape(10.dp))
							.background(if (selected) Color(0xFF1565C0) else Color(0xFFEAEAEA))
							.clickable {
								if (chip == "Favorit" || chip == "Family") {
									onFavoritesChipClick()
								}
							}
							.padding(horizontal = 12.dp, vertical = 7.dp),
						contentAlignment = Alignment.Center
					) {
						Text(
							text = chipText,
							color = if (selected) Color.White else Color(0xFF1C1C1C)
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
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.BottomCenter)
					.clip(RoundedCornerShape(18.dp))
					.background(Color.White.copy(alpha = 0.94f))
					.padding(14.dp)
			) {
				Column {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Box(
							modifier = Modifier
								.clip(CircleShape)
								.background(Color(0xFF1E88E5))
								.size(44.dp),
							contentAlignment = Alignment.Center
						) {
							Text(
								text = user.fullName
									.trim()
									.split(" ")
									.filter { it.isNotBlank() }
									.take(2)
									.joinToString("") { it.first().uppercase() }
									.ifBlank { "?" },
								color = Color.White,
								fontWeight = FontWeight.Bold,
								fontSize = 16.sp,
								textAlign = TextAlign.Center,
								modifier = Modifier.width(44.dp)
							)
						}
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
							value = "${batteryIcon(user.batteryLevel)} ${user.batteryLevel}%",
							label = "👁️‍🗨️ Baterai"
						)
						MiniMetric(
							modifier = Modifier.weight(1f),
							value = String.format(Locale.US, "%.3f, %.3f", user.latitude, user.longitude),
							label = "📍 Lokasi"
						)
						MiniMetric(
							modifier = Modifier.weight(1f),
							value = internetStatusLabel(user.internetStatus),
							label = "🌐 Internet"
						)
					}
				}
			}
		}
	}
}

@Composable
private fun FamilySelectorDropdown(
	familyOptions: List<FamilyFilterOption>,
	selectedFamilyId: Int?,
	onFamilySelected: (Int?) -> Unit
) {
	var expanded by remember { mutableStateOf(false) }
	val selectedOption = familyOptions.firstOrNull { it.id == selectedFamilyId }
	val selectedLabel = selectedOption?.name ?: "Semua Familyku"

	Box(modifier = Modifier.fillMaxWidth()) {
		// Selected family display
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.clip(RoundedCornerShape(24.dp))
				.background(Color.White.copy(alpha = 0.92f))
				.clickable { expanded = !expanded }
				.padding(horizontal = 12.dp, vertical = 10.dp),
			contentAlignment = Alignment.CenterStart
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Box(
					modifier = Modifier
						.size(12.dp)
						.clip(CircleShape)
						.background(Color(selectedOption?.color ?: 0xFF2196F3.toInt()))
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text(
					text = selectedLabel,
					color = Color(0xFF1C1C1C),
					fontWeight = FontWeight.Medium
				)
				Spacer(modifier = Modifier.weight(1f))
				Text(
					text = if (expanded) "▲" else "▼",
					color = Color(0xFF666666)
				)
			}
		}

		// Dropdown menu
		if (expanded) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 50.dp)
					.clip(RoundedCornerShape(16.dp))
					.background(Color.White.copy(alpha = 0.98f))
					.shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
			) {
				Column(modifier = Modifier.padding(vertical = 4.dp)) {
					// "All Families" option
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.clickable {
								onFamilySelected(null)
								expanded = false
							}
							.padding(horizontal = 12.dp, vertical = 10.dp),
						contentAlignment = Alignment.CenterStart
					) {
						Row(
							modifier = Modifier.fillMaxWidth(),
							verticalAlignment = Alignment.CenterVertically
						) {
							Box(
								modifier = Modifier
									.size(12.dp)
									.clip(CircleShape)
									.background(Color(0xFF2196F3))
							)
							Spacer(modifier = Modifier.width(8.dp))
							Text("Semua Familyku", color = Color(0xFF1C1C1C))
							Spacer(modifier = Modifier.weight(1f))
							if (selectedFamilyId == null) {
								Text("✓", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
							}
						}
					}

					// Family options
					familyOptions.forEach { option ->
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.clickable {
									onFamilySelected(option.id)
									expanded = false
								}
								.padding(horizontal = 12.dp, vertical = 10.dp),
							contentAlignment = Alignment.CenterStart
						) {
							Row(
								modifier = Modifier.fillMaxWidth(),
								verticalAlignment = Alignment.CenterVertically
							) {
								Box(
									modifier = Modifier
										.size(12.dp)
										.clip(CircleShape)
										.background(Color(option.color))
								)
								Spacer(modifier = Modifier.width(8.dp))
								Text(option.name, color = Color(0xFF1C1C1C))
								Spacer(modifier = Modifier.weight(1f))
								if (option.id == selectedFamilyId) {
									Text("✓", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
								}
							}
						}
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
			.height(78.dp)
			.padding(horizontal = 10.dp, vertical = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
		Spacer(modifier = Modifier.height(2.dp))
		Text(text = label, color = Color(0xFF606060), fontSize = 10.sp)
	}
}

private fun internetStatusLabel(status: String): String {
	return when (status.lowercase()) {
		"wifi" -> "🛜 WIFI"
		"mobile" -> "📳 Mobile"
		else -> status
	}
}

private fun batteryIcon(level: Int): String = if (level <= 20) "🪫" else "🔋"
