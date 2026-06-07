package com.example.nimons360.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nimons360.ui.components.AppButton
import com.example.nimons360.ui.map.MemberMapUi
import com.example.nimons360.ui.theme.Blue100
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Blue900
import com.example.nimons360.ui.theme.Grey100
import com.example.nimons360.ui.theme.Grey300
import com.example.nimons360.ui.theme.Grey50
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Grey900
import com.example.nimons360.ui.theme.White
import com.example.nimons360.utils.Constants
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserInfoBottomSheet(
	member: MemberMapUi,
	memberMarkerColor: Int?,
	onDismiss: () -> Unit
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = Grey50,
		dragHandle = {
			Box(
				modifier = Modifier
					.padding(top = 10.dp, bottom = 4.dp)
					.size(width = 48.dp, height = 5.dp)
					.clip(RoundedCornerShape(999.dp))
					.background(Grey600.copy(alpha = 0.25f))
			)
		}
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 20.dp, vertical = 12.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			val initials = member.fullName
				.trim()
				.split(" ")
				.filter { it.isNotBlank() }
				.take(2)
				.joinToString("") { it.first().uppercase() }
				.ifBlank { "?" }
			val avatarBadgeColor = memberMarkerColor?.let { Color(it) } ?: Blue100
			val profileImageUrl = if (!member.profileImageUrl.isNullOrBlank()) {
				if (member.profileImageUrl.startsWith("http")) member.profileImageUrl
				else "${Constants.BASE_URL}${member.profileImageUrl}"
			} else null

			Surface(
				modifier = Modifier
					.fillMaxWidth()
					.border(1.dp, Grey300, RoundedCornerShape(22.dp)),
				shape = RoundedCornerShape(22.dp),
				color = White,
				tonalElevation = 1.dp
			) {
				Row(
					modifier = Modifier.padding(16.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Box(
						modifier = Modifier
							.size(54.dp)
							.clip(CircleShape)
							.background(avatarBadgeColor)
							.border(
								width = 1.dp,
								color = avatarBadgeColor.copy(alpha = 0.18f),
								shape = CircleShape
							),
						contentAlignment = Alignment.Center
					) {
						if (profileImageUrl != null) {
							AsyncImage(
								model = profileImageUrl,
								contentDescription = "Foto profil ${member.fullName}",
								contentScale = ContentScale.Crop,
								modifier = Modifier
									.size(54.dp)
									.clip(CircleShape)
							)
						} else {
							Text(
								text = initials,
								color = White,
								fontSize = 18.sp,
								fontWeight = FontWeight.Bold
							)
						}
					}

					Spacer(modifier = Modifier.size(12.dp))

					Column(modifier = Modifier.weight(1f)) {
						Text(
							text = member.fullName,
							style = MaterialTheme.typography.titleMedium,
							color = Grey900,
							fontWeight = FontWeight.SemiBold
						)
						Text(
							text = member.email,
							style = MaterialTheme.typography.bodyMedium,
							color = Grey600
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(16.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				MetricCard(
					modifier = Modifier.weight(1f),
					title = "Baterai",
					value = "${member.batteryLevel}%",
					icon = Icons.Default.BatteryFull
				)
				MetricCard(
					modifier = Modifier.weight(1f),
					title = "Lokasi",
					value = String.format(Locale.US, "%.4f\n%.4f", member.latitude, member.longitude),
					icon = Icons.Default.LocationOn,
					valueMaxLines = 2,
					valueFontSize = 11.sp,
					valueLineHeight = 12.sp
				)
				MetricCard(
					modifier = Modifier.weight(1f),
					title = "Internet",
					value = internetStatusLabel(member.internetStatus),
					icon = Icons.Default.Wifi
				)
			}

			Spacer(modifier = Modifier.height(16.dp))

			AppButton(text = "Tutup", onClick = onDismiss)

			Spacer(modifier = Modifier.height(18.dp))
		}
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
private fun MetricCard(
	modifier: Modifier = Modifier,
	title: String,
	value: String,
	icon: ImageVector,
	valueMaxLines: Int = 1,
	valueFontSize: TextUnit = 12.sp,
	valueLineHeight: TextUnit = 14.sp
) {
	Surface(
		modifier = modifier,
		shape = RoundedCornerShape(14.dp),
		color = Grey100
	) {
		Column(
			modifier = Modifier
				.height(96.dp)
				.padding(horizontal = 10.dp, vertical = 8.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Icon(
				imageVector = icon,
				contentDescription = title,
				tint = Blue600,
				modifier = Modifier.size(16.dp)
			)
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = title,
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
				color = Grey900,
				textAlign = TextAlign.Center
			)
		}
	}
}
