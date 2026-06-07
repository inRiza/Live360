package com.example.nimons360.ui.family.detail.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.theme.Blue100
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.White
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMessageBottomSheet(
    showSheet: Boolean,
    familyName: String = "",
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    if (!showSheet) return

    val sheetState = rememberModalBottomSheetState()
    var messageInput by rememberSaveable { mutableStateOf("") }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val quickMessages = when {
        hour in 5..11 -> listOf(
            "Good Morning everyone!",
            "Rise and shine family!",
            "Have a great day ahead!"
        )
        hour in 12..17 -> listOf(
            "Good Afternoon everyone!",
            "Hope you're all doing well!",
            "Keep up the great work!"
        )
        else -> listOf(
            "Good Night everyone!",
            "Sweet dreams family!",
            "Rest well tonight!"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Send Message",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            if (familyName.isNotEmpty()) {
                Text(
                    text = "To: $familyName",
                    fontSize = 13.sp,
                    color = Grey600,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quick messages",
                fontSize = 13.sp,
                color = Grey600,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickMessages.forEach { msg ->
                    SuggestionChip(
                        onClick = { messageInput = msg },
                        label = { Text(msg, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Blue100,
                            labelColor = Blue600
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = Blue600.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue600,
                    focusedLabelColor = Blue600
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Grey600)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            onSend(messageInput)
                            messageInput = ""
                            onDismiss()
                        }
                    },
                    enabled = messageInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue600,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send")
                }
            }
        }
    }
}
