package com.example.nimons360.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.map.MemberMapUi
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserInfoBottomSheet(
	member: MemberMapUi,
	onDismiss: () -> Unit
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = MaterialTheme.colorScheme.surface,
		dragHandle = { }
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 20.dp, vertical = 10.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			val initials = member.fullName
				.trim()
				.split(" ")
				.filter { it.isNotBlank() }
				.take(2)
				.joinToString("") { it.first().uppercase() }
				.ifBlank { "?" }

			Text(
				text = initials,
				modifier = Modifier
					.size(68.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.primary)
					.padding(top = 16.dp),
				color = MaterialTheme.colorScheme.onPrimary,
				fontSize = 28.sp,
				fontWeight = FontWeight.Bold
			)

			Spacer(modifier = Modifier.height(14.dp))

			Text(
				text = member.fullName,
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold
			)
			Text(
				text = member.email,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)

			Spacer(modifier = Modifier.height(16.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				MetricCard(
					modifier = Modifier.weight(1f),
					title = "Baterai",
					value = "${member.batteryLevel}%"
				)
				MetricCard(
					modifier = Modifier.weight(1f),
					title = "Lokasi",
					value = String.format(Locale.US, "%.4f, %.4f", member.latitude, member.longitude)
				)
				MetricCard(
					modifier = Modifier.weight(1f),
					title = "Internet",
					value = when (member.internetStatus.lowercase()) {
						"wifi" -> "wifi"
						"mobile" -> "mobile"
						else -> member.internetStatus
					}
				)
			}

			Spacer(modifier = Modifier.height(16.dp))

			Button(
				modifier = Modifier.fillMaxWidth(),
				shape = RoundedCornerShape(20.dp),
				onClick = onDismiss
			) {
				Text("Tutup")
			}

			Spacer(modifier = Modifier.height(18.dp))
		}
	}
}

@Composable
private fun MetricCard(
	modifier: Modifier = Modifier,
	title: String,
	value: String
) {
	Column(
		modifier = modifier
			.clip(RoundedCornerShape(12.dp))
			.background(MaterialTheme.colorScheme.surfaceVariant)
			.padding(horizontal = 8.dp, vertical = 10.dp)
	) {
		Text(
			text = value,
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.SemiBold
		)
		Spacer(modifier = Modifier.height(2.dp))
		Text(
			text = title,
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}
