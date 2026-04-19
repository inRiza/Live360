package com.example.nimons360.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import com.example.nimons360.ui.map.MemberMapUi
import com.example.nimons360.ui.theme.Blue100
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Blue900
import com.example.nimons360.ui.theme.Green600
import com.example.nimons360.ui.theme.Grey100
import com.example.nimons360.ui.theme.Grey300
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Grey900
import com.example.nimons360.ui.theme.Red600
import com.example.nimons360.ui.theme.White
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
	currentUserMarkerColor: Int?,
	onSearchQueryChanged: (String) -> Unit,
	onFavoritesChipClick: () -> Unit
) {
	Box(modifier = modifier) {
		Column(modifier = Modifier.fillMaxWidth()) {
			OutlinedTextField(
				value = searchQuery,
				onValueChange = onSearchQueryChanged,
				placeholder = {
					Text("Search lokasi atau anggota...", color = Grey600)
				},
				leadingIcon = {
					androidx.compose.material3.Icon(
						imageVector = Icons.Default.Search,
						contentDescription = null,
						tint = Grey600
					)
				},
				modifier = Modifier
					.fillMaxWidth()
					.height(52.dp),
				shape = RoundedCornerShape(25.dp),
				singleLine = true,
				colors = OutlinedTextFieldDefaults.colors(
					unfocusedContainerColor = White,
					focusedContainerColor = White,
					unfocusedBorderColor = Grey300,
					focusedBorderColor = Blue600,
					cursorColor = Blue600,
					focusedLeadingIconColor = Blue600,
					unfocusedLeadingIconColor = Grey600
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
					MapFilterChip(
						text = chipText,
						isSelected = selected,
						onClick = {
							if (chip == "Favorit" || chip == "Family") {
								onFavoritesChipClick()
							}
						}
					)
				}
			}

			Spacer(modifier = Modifier.height(8.dp))
			Text(
				text = if (isConnected) "Terhubung langsung" else "Menyambungkan ulang...",
				color = if (isConnected) Green600 else Red600,
				fontWeight = FontWeight.SemiBold
			)
			Spacer(modifier = Modifier.height(2.dp))
			Text(
				text = "Orang terdekat: $nearbyCount dalam ${(nearbyRadiusMeters / 1000.0).toInt()} km",
				color = Grey600,
				fontWeight = FontWeight.Medium
			)
		}

		currentUser?.let { user ->
			val avatarBadgeColor = currentUserMarkerColor?.let { Color(it) } ?: Blue600
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.BottomCenter)
					.clip(RoundedCornerShape(22.dp))
					.background(White)
					.border(1.dp, Grey300, RoundedCornerShape(22.dp))
					.padding(14.dp)
			) {
				Column {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Box(
							modifier = Modifier
								.clip(CircleShape)
								.background(avatarBadgeColor)
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
										color = White,
								fontWeight = FontWeight.Bold,
								fontSize = 16.sp,
								textAlign = TextAlign.Center,
								modifier = Modifier.width(44.dp)
							)
						}
						Spacer(modifier = Modifier.width(10.dp))
						Column {
							Text(text = user.fullName, fontWeight = FontWeight.SemiBold)
							Text(text = user.email, color = Grey600)
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
							label = "Baterai",
							icon = Icons.Default.BatteryFull
						)
						MiniMetric(
							modifier = Modifier.weight(1f),
							value = String.format(Locale.US, "%.4f\n%.4f", user.latitude, user.longitude),
							label = "Lokasi",
							icon = Icons.Default.LocationOn,
							valueMaxLines = 2,
							valueFontSize = 11.sp,
							valueLineHeight = 12.sp
						)
						MiniMetric(
							modifier = Modifier.weight(1f),
							value = internetStatusLabel(user.internetStatus),
							label = "Internet",
							icon = Icons.Default.Wifi
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
	label: String,
	icon: ImageVector,
	valueMaxLines: Int = 1,
	valueFontSize: TextUnit = 12.sp,
	valueLineHeight: TextUnit = 14.sp
) {
	Column(
		modifier = modifier
			.clip(RoundedCornerShape(12.dp))
			.background(Grey100)
			.height(96.dp)
			.padding(horizontal = 10.dp, vertical = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Icon(
			imageVector = icon,
			contentDescription = label,
			tint = Blue600,
			modifier = Modifier.size(16.dp)
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = label,
			color = Grey600,
			fontSize = 10.sp,
			maxLines = 1,
			textAlign = TextAlign.Center
		)
		Spacer(modifier = Modifier.height(2.dp))
		Text(
			text = value,
			fontWeight = FontWeight.SemiBold,
			fontSize = valueFontSize,
			lineHeight = valueLineHeight,
			maxLines = valueMaxLines,
			overflow = TextOverflow.Ellipsis,
			textAlign = TextAlign.Center
		)
	}
}

private fun internetStatusLabel(status: String): String {
	return when (status.lowercase()) {
		"wifi" -> "Wi-Fi"
		"mobile" -> "Mobile"
		else -> status
	}
}

@Composable
private fun MapFilterChip(
	text: String,
	isSelected: Boolean,
	onClick: () -> Unit
) {
	val backgroundColor = if (isSelected) Blue600 else White
	val textColor = if (isSelected) White else Grey900
	val borderStroke = if (!isSelected) BorderStroke(1.dp, Grey300) else null

	Surface(
		shape = RoundedCornerShape(10.dp),
		color = backgroundColor,
		border = borderStroke,
		modifier = Modifier.clickable { onClick() }
	) {
		Text(
			text = text,
			color = textColor,
			fontSize = 15.sp,
			modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
		)
	}
}
